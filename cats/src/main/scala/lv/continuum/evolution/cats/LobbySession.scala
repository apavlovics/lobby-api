package lv.continuum.evolution.cats

import cats.Monad
import cats.effect.concurrent.Ref
import cats.implicits._
import fs2.concurrent.Queue
import io.circe.Error
import io.odin.Logger
import lv.continuum.evolution.protocol.Protocol.In._
import lv.continuum.evolution.protocol.Protocol.Out._
import lv.continuum.evolution.protocol.Protocol.UserType._
import lv.continuum.evolution.protocol.Protocol._
import org.http4s.websocket.WebSocketFrame

class LobbySession[F[_] : Monad : Logger](
  tablesRef: Ref[F, Tables],
  subscribersRef: Ref[F, Subscribers[F]],
  sessionParamsRef: Ref[F, SessionParams],
  queue: Queue[F, WebSocketFrame],
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
      case Right(Login(username, password)) =>
        login(username, password)

      case Right(_) =>
        Monad[F].pure(NotAuthenticated.some)

      case Left(e) => error(e)
    }
  }

  private def processAuthenticated(
    in: Either[Error, In],
    userType: UserType,
  ): F[Option[Out]] = in match {
    case Right(Login(username, password)) =>
      login(username, password)

    case Right(Ping(seq)) =>
      Monad[F].pure(Pong(seq = seq).some)

    case Right(SubscribeTables) => for {
      _ <- subscribersRef.update(_ + queue)
      tables <- tablesRef.get
    } yield TableList(tables = tables).some

    case Right(UnsubscribeTables) =>
      subscribersRef.update(_ - queue).as(None)

    // TODO Complete implementation
    case Right(_) => Monad[F].pure(None)

    case Left(e) => error(e)
  }

  private def login(
    username: Username,
    password: Password,
  ): F[Option[Out]] = (username, password) match {
    case (Username("admin"), Password("admin")) =>
      sessionParamsRef.update(_.copy(userType = Admin.some))
        .as(LoginSuccessful(userType = Admin).some)

    case (Username("user"), Password("user")) =>
      sessionParamsRef.update(_.copy(userType = User.some))
        .as(LoginSuccessful(userType = User).some)

    case _ =>
      sessionParamsRef.update(_.copy(userType = None))
        .as(LoginFailed.some)
  }

  private def error(error: Error): F[Option[Out]] =
    Logger[F].warn(s"Issue while parsing JSON: ${ error.getMessage }") *>
      Monad[F].pure(InvalidMessage.some)
}

object LobbySession {
  def apply[F[_] : Monad : Logger](
    tablesRef: Ref[F, Tables],
    subscribersRef: Ref[F, Subscribers[F]],
    sessionParamsRef: Ref[F, SessionParams],
    queue: Queue[F, WebSocketFrame],
  ): LobbySession[F] = new LobbySession(tablesRef, subscribersRef, sessionParamsRef, queue)
}
