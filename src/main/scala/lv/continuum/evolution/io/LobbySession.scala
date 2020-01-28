package lv.continuum.evolution.io

import cats.Monad
import cats.effect.concurrent.Ref
import cats.implicits._
import fs2.concurrent.Queue
import io.chrisdavenport.log4cats.Logger
import io.circe.Error
import lv.continuum.evolution.protocol.Protocol.In._
import lv.continuum.evolution.protocol.Protocol.Out._
import lv.continuum.evolution.protocol.Protocol.UserType._
import lv.continuum.evolution.protocol.Protocol._
import org.http4s.websocket.WebSocketFrame

class LobbySession[F[_] : Monad](
  tablesRef: Ref[F, Tables],
  subscribersRef: Ref[F, Subscribers[F]],
  sessionParamsRef: Ref[F, SessionParams],
  queue: Queue[F, WebSocketFrame],
  logger: Logger[F],
) {

  def process(
    in: Either[Error, In],
  ): F[Option[Out]] = for {
    sessionParams <- sessionParamsRef.get
    out <- sessionParams.userType match {
      case None           => processUnauthenticated(in)
      case Some(userType) => processAuthenticated(in, userType)
    }
  } yield out

  private def processUnauthenticated(
    in: Either[Error, In],
  ): F[Option[Out]] = {
    in match {
      case Right(LoginIn(username, password)) =>
        login(username, password)

      case Right(_) =>
        F.pure(ErrorOut(OutType.NotAuthenticated).some)

      case Left(e) => error(e)
    }
  }

  private def processAuthenticated(
    in: Either[Error, In],
    userType: UserType,
  ): F[Option[Out]] = in match {
    case Right(LoginIn(username, password)) =>
      login(username, password)

    case Right(PingIn(seq)) =>
      F.pure(PongOut(seq = seq).some)

    case Right(SubscribeTablesIn) => for {
      _ <- subscribersRef.update(_ + queue)
      tables <- tablesRef.get
    } yield TableListOut(tables = tables).some

    case Right(UnsubscribeTablesIn) =>
      subscribersRef.update(_ - queue).as(None)

    // TODO Complete implementation
    case Right(_) => F.pure(None)

    case Left(e) => error(e)
  }

  private def login(
    username: Username,
    password: Password,
  ): F[Option[Out]] = (username, password) match {
    case (Username("admin"), Password("admin")) =>
      sessionParamsRef.update(_.copy(userType = Admin.some))
        .as(LoginSuccessfulOut(userType = Admin).some)

    case (Username("user"), Password("user")) =>
      sessionParamsRef.update(_.copy(userType = User.some))
        .as(LoginSuccessfulOut(userType = User).some)

    case _ =>
      sessionParamsRef.update(_.copy(userType = None))
        .as(ErrorOut(OutType.LoginFailed).some)
  }

  private def error(error: Error): F[Option[Out]] =
    logger.info(s"Issue while parsing JSON: ${ error.getMessage }") *>
      F.pure(ErrorOut(OutType.InvalidMessage).some)
}

object LobbySession {
  def apply[F[_] : Monad](
    tablesRef: Ref[F, Tables],
    subscribersRef: Ref[F, Subscribers[F]],
    sessionParamsRef: Ref[F, SessionParams],
    queue: Queue[F, WebSocketFrame],
    logger: Logger[F],
  ): LobbySession[F] = new LobbySession(tablesRef, subscribersRef, sessionParamsRef, queue, logger)
}
