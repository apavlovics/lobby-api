package lv.continuum.lobby.zio

import lv.continuum.lobby.model.ParsingError
import lv.continuum.lobby.protocol.Protocol.*
import lv.continuum.lobby.protocol.Protocol.In.*
import lv.continuum.lobby.protocol.Protocol.Out.*
import zio.*

object LobbySession {

  def process(
    in: Either[ParsingError, In],
  ): ZIO[Session, Nothing, Option[Out]] = in match {
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
  ): UIO[Option[Out]] = {
    in match {
      case Login(username, password) => ZIO.succeed(None)
      case _                         => ZIO.succeed(Some(NotAuthenticated))
    }
  }

  private def processAuthenticated(
    in: In,
  ): UIO[Option[Out]] = {
    in match {
      case _ => ZIO.succeed(None)
    }
  }
}
