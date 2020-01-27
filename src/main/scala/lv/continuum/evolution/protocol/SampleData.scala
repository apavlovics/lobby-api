package lv.continuum.evolution.protocol

import lv.continuum.evolution.protocol.Protocol._

object SampleData {

  val tables: List[Table] = List(
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
  )
}
