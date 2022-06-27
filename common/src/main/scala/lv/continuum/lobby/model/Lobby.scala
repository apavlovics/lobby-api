package lv.continuum.lobby.model

import cats.syntax.option.*
import lv.continuum.lobby.protocol.Protocol.*

case class Lobby(
  tables: Vector[Table],
  nextTableId: TableId,
) {

  def addTable(afterId: TableId, tableToAdd: TableToAdd): Option[(Lobby, Table)] = {
    lazy val table: Table = tableToAdd.toTable(nextTableId)
    val newTables = {
      if afterId == TableId.Absent then {
        table +: tables
      } else {
        tables.flatMap { t =>
          if t.id == afterId then Vector(t, table) else Vector(t)
        }
      }
    }
    if newTables.size != tables.size then {
      (copy(tables = newTables, nextTableId = nextTableId.inc), table).some
    } else None
  }

  def updateTable(table: Table): Option[Lobby] = {
    var updated = false
    val newTables = tables.map { t =>
      if t.id == table.id then {
        updated = true
        table
      } else t
    }
    if updated then copy(tables = newTables).some else None
  }

  def removeTable(tableId: TableId): Option[Lobby] = {
    val newTables = tables.filterNot(_.id == tableId)
    if newTables.size != tables.size then copy(tables = newTables).some else None
  }
}

object Lobby {

  def apply(
    tables: Vector[Table],
  ): Lobby = {
    val nextTableId = tables.map(_.id).maxByOption(_.value).map(_.inc).getOrElse(TableId.Initial)
    Lobby(tables, nextTableId)
  }

  /** Initialize `Lobby` that has some sample tables inside. */
  def apply(): Lobby =
    apply(
      tables = Vector(
        Table(
          id = TableId(1),
          name = TableName("table - James Bond"),
          participants = 7,
        ),
        Table(
          id = TableId(2),
          name = TableName("table - Mission Impossible"),
          participants = 9,
        ),
      ),
    )
}
