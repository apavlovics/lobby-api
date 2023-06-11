package lv.continuum.lobby.zio.layer

import lv.continuum.lobby.model.Lobby
import lv.continuum.lobby.protocol.Protocol.{Table, TableId, TableToAdd}
import zio.*

trait LobbyHolder {

  def tables: UIO[Vector[Table]]

  def addTable(afterId: TableId, tableToAdd: TableToAdd): UIO[Option[Table]]

  def updateTable(table: Table): UIO[Boolean]

  def removeTable(tableId: TableId): UIO[Boolean]
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

  override def updateTable(table: Table): UIO[Boolean] =
    lobbyRef.modify { lobby =>
      lobby.updateTable(table).fold { (false, lobby) } { (true, _) }
    }

  override def removeTable(tableId: TableId): UIO[Boolean] =
    lobbyRef.modify { lobby =>
      lobby.removeTable(tableId).fold { (false, lobby) } { (true, _) }
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
