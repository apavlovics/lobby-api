package lv.continuum.evolution.model

import cats.syntax.option._
import lv.continuum.evolution.protocol.Protocol._

case class Lobby(
  tables: Vector[Table],
  nextTableId: TableId,
) {

  def addTable(tableToAdd: TableToAdd, afterId: TableId): Option[(Lobby, Table)] = {
    lazy val table: Table = tableToAdd.toTable(nextTableId)
    val newTables = {
      if (afterId == TableId.Absent) {
        table +: tables
      } else {
        tables.flatMap { t =>
          if (t.id == afterId) Vector(t, table) else Vector(t)
        }
      }
    }
    if (newTables.size != tables.size) {
      (copy(tables = newTables, nextTableId = nextTableId.inc), table).some
    } else None
  }

  def updateTable(table: Table): Option[Lobby] = {
    var updated = false
    val newTables = tables.map { t =>
      if (t.id == table.id) {
        updated = true
        table
      } else t
    }
    if (updated) copy(tables = newTables).some else None
  }

  def removeTable(tableId: TableId): Option[Lobby] = {
    val newTables = tables.filterNot(_.id == tableId)
    if (newTables.size != tables.size) copy(tables = newTables).some else None
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
