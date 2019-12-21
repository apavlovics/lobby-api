package lv.continuum.evolution.akka

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import lv.continuum.evolution.protocol.Protocol.In._
import lv.continuum.evolution.protocol.Protocol.Out._
import lv.continuum.evolution.protocol.Protocol.{In, PushOut, Table}

object TableActor {

  case class TableCommand(in: In, pushTo: ActorRef[PushOut])

  private case class TableState(
    tables: List[Table],
    subscribers: Set[ActorRef[PushOut]],
  )

  def apply(): Behavior[TableCommand] = process(
    // Initial TableState that holds some sample data
    TableState(
      tables = List(
        Table(
          id = 1,
          name = "table - James Bond",
          participants = 7,
        ),
        Table(
          id = 2,
          name = "table - Mission Impossible",
          participants = 4,
        ),
      ),
      subscribers = Set.empty,
    )
  )

  private def process(state: TableState): Behavior[TableCommand] = {
    Behaviors.receive { (_, command) =>
      command.in match {
        case SubscribeTablesIn =>
          command.pushTo ! TableListOut(tables = state.tables)
          process(state.copy(subscribers = state.subscribers + command.pushTo))

        case UnsubscribeTablesIn =>
          process(state.copy(subscribers = state.subscribers - command.pushTo))

        case RemoveTableIn(id) =>
          state.subscribers.foreach(_ ! TableRemovedOut(id = id))
          process(state.copy(tables = state.tables.filterNot(_.id == id)))

        case _ => Behaviors.same
      }
    }
  }
}
