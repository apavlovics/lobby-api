package lv.continuum.evolution.cats

import cats.{Monad, Parallel}
import cats.effect.concurrent.Ref
import cats.implicits._
import io.circe.Error
import io.odin.Logger
import lv.continuum.evolution.protocol.Protocol.In._
import lv.continuum.evolution.protocol.Protocol.Out._
import lv.continuum.evolution.protocol.Protocol.UserType._
import lv.continuum.evolution.protocol.Protocol._

class LobbySession[F[_] : Logger : Monad : Parallel](
  tablesRef: Ref[F, Tables],
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
      _ <- subscribersRef.update(_ + subscriber)
      tables <- tablesRef.get
    } yield TableList(tables = tables).some

    case Right(UnsubscribeTables) =>
      subscribersRef.update(_ - subscriber).as(None)

    case Right(in: UpdateTable) => for {
      tableUpdated <- tablesRef.modify { tables =>
        var tableUpdated = false
        val newTables = tables.map { table =>
          if (table.id == in.table.id) {
            tableUpdated = true
            in.table
          }
          else table
        }
        (newTables, tableUpdated)
      }
      out <- {
        if (tableUpdated) {
          val tableUpdated = TableUpdated(table = in.table)
          for {
            subscribers <- subscribersRef.get
            _ <- subscribers.map(_.enqueue1(tableUpdated)).toVector.parSequence
          } yield None
        } else Monad[F].pure(TableUpdateFailed(in.table.id).some)
      }
    } yield out

    case Right(in: RemoveTable) => for {
      tableRemoved <- tablesRef.modify { tables =>
        val newTables = tables.filterNot(_.id == in.id)
        (newTables, newTables.size != tables.size)
      }
      out <- {
        if (tableRemoved) {
          val tableRemoved = TableRemoved(id = in.id)
          for {
            subscribers <- subscribersRef.get
            _ <- subscribers.map(_.enqueue1(tableRemoved)).toVector.parSequence
          } yield None
        } else Monad[F].pure(TableRemoveFailed(in.id).some)
      }
    } yield out

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
  def apply[F[_] : Logger : Monad : Parallel](
    tablesRef: Ref[F, Tables],
    subscribersRef: Ref[F, Subscribers[F]],
    sessionParamsRef: Ref[F, SessionParams],
    subscriber: Subscriber[F],
  ): LobbySession[F] = new LobbySession(tablesRef, subscribersRef, sessionParamsRef, subscriber)
}
