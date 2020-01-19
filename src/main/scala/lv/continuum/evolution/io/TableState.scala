package lv.continuum.evolution.io

import lv.continuum.evolution.protocol.Protocol._

case class TableState(tables: List[Table])

object TableState {

  /** Initial `TableState` that holds some sample data. */
  val initial: TableState = TableState(
    tables = List(
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
