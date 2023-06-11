package lv.continuum.lobby.zio

import lv.continuum.lobby.model.ParsingError
import lv.continuum.lobby.protocol.Protocol.*
import lv.continuum.lobby.protocol.Protocol.UserType.{Admin, User}
import lv.continuum.lobby.protocol.Protocol.In.*
import lv.continuum.lobby.protocol.Protocol.Out.*
import lv.continuum.lobby.zio.layer.{Authenticator, LobbyHolder, SessionHolder, Subscriber, SubscribersHolder}
import zio.*

object LobbySession {

  private type Env = Authenticator & LobbyHolder & SessionHolder & SubscribersHolder

  def process(
    in: Either[ParsingError, In],
    subscriber: Subscriber,
  ): ZIO[Env, Nothing, Option[Out]] = in match {
    case Right(in) =>
      for {
        params <- ZIO.serviceWithZIO[SessionHolder](_.params())
        out <- params.userType match {
          case None           => processUnauthenticated(in)
          case Some(userType) => processAuthenticated(userType, in, subscriber)
        }
      } yield out
    case Left(error) =>
      ZIO.logWarning(s"Issue while parsing JSON: $error") *> ZIO.succeed(Some(InvalidMessage))
  }

  private def processUnauthenticated(
    in: In,
  ): ZIO[Authenticator & SessionHolder, Nothing, Option[Out]] = in match {
    case Login(username, password) => login(username, password)
    case _                         => ZIO.succeed(Some(NotAuthenticated))
  }

  private def processAuthenticated(
    userType: UserType,
    in: In,
    subscriber: Subscriber,
  ): ZIO[Env, Nothing, Option[Out]] = (userType, in) match {
    case (_, Login(username, password)) => login(username, password)

    case (_, Ping(seq)) => ZIO.succeed(Some(Pong(seq = seq)))

    case (_, SubscribeTables) =>
      for {
        _   <- ZIO.serviceWithZIO[SubscribersHolder](_.add(subscriber))
        out <- ZIO.serviceWithZIO[LobbyHolder](_.tables).map(tables => Some(TableList(tables)))
      } yield out

    case (_, UnsubscribeTables) =>
      ZIO.serviceWithZIO[SubscribersHolder](_.remove(subscriber)).as(None)

    case (User, _: AdminTableIn) =>
      ZIO.succeed(Some(NotAuthorized))

    case (Admin, in: AddTable) =>
      for {
        table <- ZIO.serviceWithZIO[LobbyHolder](_.addTable(in.afterId, in.table))
        _ <- table.fold(ZIO.unit) { table =>
          ZIO.serviceWithZIO[SubscribersHolder](_.broadcast(TableAdded(in.afterId, table)))
        }
      } yield table.map(TableAdded(in.afterId, _))

    // TODO Complete implementation
    case _ => ZIO.succeed(None)
  }

  private def login(
    username: Username,
    password: Password,
  ): ZIO[Authenticator & SessionHolder, Nothing, Option[Out]] = for {
    userType <- ZIO.serviceWithZIO[Authenticator](_.authenticate(username, password))
    _        <- ZIO.serviceWithZIO[SessionHolder](_.updateUserType(userType))
  } yield userType match {
    case Some(userType) => Some(LoginSuccessful(userType))
    case None           => Some(LoginFailed)
  }
}
