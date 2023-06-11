package lv.continuum.lobby.zio

import lv.continuum.lobby.model.ParsingError
import lv.continuum.lobby.protocol.Protocol.*
import lv.continuum.lobby.protocol.Protocol.In.*
import lv.continuum.lobby.protocol.Protocol.Out.*
import lv.continuum.lobby.zio.layer.{Authenticator, Session}
import zio.*

object LobbySession {

  def process(
    in: Either[ParsingError, In],
  ): ZIO[Authenticator & Session, Nothing, Option[Out]] = in match {
    case Right(in) =>
      for {
        params <- ZIO.serviceWithZIO[Session](_.params())
        out <- params.userType match {
          case None           => processUnauthenticated(in)
          case Some(userType) => processAuthenticated(in)
        }
      } yield out
    case Left(error) =>
      ZIO.logWarning(s"Issue while parsing JSON: $error") *> ZIO.succeed(Some(InvalidMessage))
  }

  private def processUnauthenticated(
    in: In,
  ): ZIO[Authenticator & Session, Nothing, Option[Out]] = {
    in match {
      case Login(username, password) =>
        for {
          userType <- ZIO.serviceWithZIO[Authenticator](_.authenticate(username, password))
          _        <- ZIO.serviceWithZIO[Session](_.updateUserType(userType))
        } yield userType match {
          case Some(userType) => Some(LoginSuccessful(userType))
          case None           => Some(LoginFailed)
        }
      case _ => ZIO.succeed(Some(NotAuthenticated))
    }
  }

  private def processAuthenticated(
    in: In,
  ): UIO[Option[Out]] = {
    in match {
      // TODO Complete implementation
      case _ => ZIO.succeed(None)
    }
  }
}
