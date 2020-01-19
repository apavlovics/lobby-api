package lv.continuum.evolution.io

import fs2.concurrent.Queue
import lv.continuum.evolution.protocol.Protocol._
import org.http4s.websocket.WebSocketFrame

// TODO Split tables and subscribers into separate refs
case class TableState[F[_]](
  tables: List[Table],
  subscribers: Set[Queue[F, WebSocketFrame]],
)

object TableState {

  /** Initial `TableState` that holds some sample data. */
  def initial[F[_]]: TableState[F] = TableState(
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
    subscribers = Set.empty,
  )
}
