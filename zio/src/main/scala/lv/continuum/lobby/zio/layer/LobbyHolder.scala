package lv.continuum.lobby.zio.layer

import lv.continuum.lobby.model.Lobby
import lv.continuum.lobby.protocol.Protocol.{Table, TableId, TableToAdd}
import zio.*

trait LobbyHolder {

  def tables: UIO[Vector[Table]]

  def addTable(afterId: TableId, tableToAdd: TableToAdd): UIO[Option[Table]]
}

class LobbyHolderLive private (
  lobbyRef: Ref[Lobby],
) extends LobbyHolder {

  override def tables: UIO[Vector[Table]] = lobbyRef.get.map(_.tables)

  override def addTable(afterId: TableId, tableToAdd: TableToAdd): UIO[Option[Table]] =
    lobbyRef.modify { lobby =>
      lobby.addTable(afterId, tableToAdd) match {
        case Some((lobby, table)) => (Some(table), lobby)
        case None                 => (None, lobby)
      }
    }
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
