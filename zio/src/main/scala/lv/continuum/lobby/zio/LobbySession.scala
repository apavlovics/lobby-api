package lv.continuum.lobby.zio

import lv.continuum.lobby.model.ParsingError
import lv.continuum.lobby.protocol.Protocol.*
import lv.continuum.lobby.protocol.Protocol.Out.*
import zio.*

object LobbySession {

  def process(
    in: Either[ParsingError, In],
  ): Task[Option[Out]] = in match {
    case Right(in) =>
      // TODO Complete implementation
      ZIO.succeed(None)
    case Left(error) =>
      ZIO.logWarning(s"Issue while parsing JSON: $error") *> ZIO.succeed(Some(InvalidMessage))
  }
}
