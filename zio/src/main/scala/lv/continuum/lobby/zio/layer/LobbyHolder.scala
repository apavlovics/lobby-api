package lv.continuum.lobby.zio.layer

import lv.continuum.lobby.model.Lobby
import lv.continuum.lobby.protocol.Protocol.Table
import zio.*

trait LobbyHolder {

  def tables: UIO[Vector[Table]]
}

class LobbyHolderLive private (
  lobbyRef: Ref[Lobby],
) extends LobbyHolder {

  override def tables: UIO[Vector[Table]] = lobbyRef.get.map(_.tables)
}

object LobbyHolderLive {

  val layer: ULayer[LobbyHolder] =
    ZLayer {
      for {
        _        <- ZIO.logDebug("Creating new lobby holder")
        lobbyRef <- Ref.make(Lobby())
      } yield LobbyHolderLive(lobbyRef)
    }
}
