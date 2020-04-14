package lv.continuum.evolution.cats

import cats.effect.concurrent.Ref
import cats.implicits._
import cats.{Monad, Parallel}
import io.circe.Error
import io.odin.Logger
import lv.continuum.evolution.model.Lobby
import lv.continuum.evolution.protocol.Protocol.In._
import lv.continuum.evolution.protocol.Protocol.Out._
import lv.continuum.evolution.protocol.Protocol.UserType._
import lv.continuum.evolution.protocol.Protocol._

class LobbySession[F[_] : Logger : Monad : Parallel](
  authenticator: Authenticator[F],
  lobbyRef: Ref[F, Lobby],
  subscribersRef: Ref[F, Subscribers[F]],
  sessionParamsRef: Ref[F, SessionParams],
  subscriber: Subscriber[F],
) {

  def process(
    in: Either[Error, In],
  ): F[Option[Out]] = for {
    sessionParams <- sessionParamsRef.get
    out <- sessionParams.userType match {
      case None           => processUnauthenticated(in)
      case Some(userType) => processAuthenticated(userType, in)
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
    userType: UserType,
    in: Either[Error, In],
  ): F[Option[Out]] = (userType, in) match {
    case (_, Right(Login(username, password))) =>
      login(username, password)

    case (_, Right(Ping(seq))) =>
      Monad[F].pure(Pong(seq = seq).some)

    case (_, Right(SubscribeTables)) => for {
      _ <- subscribersRef.update(_ + subscriber)
      lobby <- lobbyRef.get
    } yield TableList(tables = lobby.tables).some

    case (_, Right(UnsubscribeTables)) =>
      subscribersRef.update(_ - subscriber).as(None)

    case (User, Right(_: AdminTableIn)) =>
      Monad[F].pure(NotAuthorized.some)

    case (Admin, Right(in: AddTable)) => for {
      tableAdded <- lobbyRef.modify { lobby =>
        lobby.addTable(in.afterId, in.table).fold {
          (lobby, Option.empty[Table])
        } { result =>
          (result._1, result._2.some)
        }
      }
      out <- tableAdded.fold {
        Monad[F].pure(TableAddFailed.some)
      } { table =>
        val tableAdded = TableAdded(afterId = in.afterId, table = table)
        for {
          subscribers <- subscribersRef.get
          _ <- subscribers.map(_.enqueue1(tableAdded)).toVector.parSequence
        } yield None
      }
    } yield out

    case (Admin, Right(in: UpdateTable)) => for {
      tableUpdated <- lobbyRef.modify { lobby =>
        lobby.updateTable(in.table).fold { (lobby, false) } { (_, true) }
      }
      out <- if (tableUpdated) {
        val tableUpdated = TableUpdated(table = in.table)
        for {
          subscribers <- subscribersRef.get
          _ <- subscribers.map(_.enqueue1(tableUpdated)).toVector.parSequence
        } yield None
      } else Monad[F].pure(TableUpdateFailed(in.table.id).some)
    } yield out

    case (Admin, Right(in: RemoveTable)) => for {
      tableRemoved <- lobbyRef.modify { lobby =>
        lobby.removeTable(in.id).fold { (lobby, false) } { (_, true) }
      }
      out <- if (tableRemoved) {
        val tableRemoved = TableRemoved(id = in.id)
        for {
          subscribers <- subscribersRef.get
          _ <- subscribers.map(_.enqueue1(tableRemoved)).toVector.parSequence
        } yield None
      } else Monad[F].pure(TableRemoveFailed(in.id).some)
    } yield out

    case (_, Left(e)) => error(e)
  }

  private def login(
    username: Username,
    password: Password,
  ): F[Option[Out]] = for {
    userType <- authenticator.authenticate(username, password)
    out <- userType match {
      case Some(userType) =>
        sessionParamsRef.update(_.copy(userType = userType.some))
          .as(LoginSuccessful(userType).some)

      case None =>
        sessionParamsRef.update(_.copy(userType = None))
          .as(LoginFailed.some)
    }
  } yield out

  private def error(error: Error): F[Option[Out]] =
    Logger[F].warn(s"Issue while parsing JSON: ${ error.getMessage }") *>
      Monad[F].pure(InvalidMessage.some)
}

object LobbySession {
  def apply[F[_] : Logger : Monad : Parallel](
    authenticator: Authenticator[F],
    lobbyRef: Ref[F, Lobby],
    subscribersRef: Ref[F, Subscribers[F]],
    sessionParamsRef: Ref[F, SessionParams],
    subscriber: Subscriber[F],
  ): LobbySession[F] = new LobbySession(authenticator, lobbyRef, subscribersRef, sessionParamsRef, subscriber)
}
