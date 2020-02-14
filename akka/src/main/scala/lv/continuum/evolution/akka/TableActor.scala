package lv.continuum.evolution.akka

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import lv.continuum.evolution.protocol.Protocol.In._
import lv.continuum.evolution.protocol.Protocol.Out._
import lv.continuum.evolution.protocol.Protocol._
import lv.continuum.evolution.protocol.SampleData._

object TableActor {

  case class TableCommand(in: TableIn, replyTo: ActorRef[PushOut])

  private case class TableState(
    tables: Vector[Table],
    subscribers: Set[ActorRef[PushOut]],
    nextId: TableId,
  )
  private object TableState {
    def apply(
      tables: Vector[Table],
      subscribers: Set[ActorRef[PushOut]],
    ): TableState = {
      val nextId = tables.map(_.id).maxByOption(_.value).map(_.inc).getOrElse(TableId.Initial)
      TableState(tables, subscribers, nextId)
    }
  }

  def apply(): Behavior[TableCommand] = process(
    // Initial TableState that holds some sample data
    TableState(
      tables = tables,
      subscribers = Set.empty,
    )
  )

  private def process(state: TableState): Behavior[TableCommand] = {
    Behaviors.receive { (_, command) =>
      command.in match {
        case SubscribeTables =>
          command.replyTo ! TableList(tables = state.tables)
          process(state.copy(subscribers = state.subscribers + command.replyTo))

        case UnsubscribeTables =>
          process(state.copy(subscribers = state.subscribers - command.replyTo))

        case in: AddTable =>
          addTable(state, in, command.replyTo)

        case in: UpdateTable =>
          updateTable(state, in, command.replyTo)

        case in: RemoveTable =>
          removeTable(state, in, command.replyTo)
      }
    }
  }

  private def addTable(
    state: TableState,
    in: AddTable,
    replyTo: ActorRef[PushOut],
  ): Behavior[TableCommand] = {

    def tableToAdd: Table = in.table.toTable(state.nextId)

    val newTables = {
      if (in.afterId == TableId.Absent) {
        tableToAdd +: state.tables
      } else {
        state.tables.flatMap { t =>
          if (t.id == in.afterId) Vector(t, tableToAdd) else Vector(t)
        }
      }
    }
    if (newTables.size != state.tables.size) {
      val tableAdded = TableAdded(
        afterId = in.afterId,
        table = tableToAdd,
      )
      state.subscribers.foreach(_ ! tableAdded)
      process(state.copy(
        nextId = state.nextId.inc,
        tables = newTables,
      ))
    } else {
      replyTo ! TableAddFailed
      Behaviors.same
    }
  }

  private def updateTable(
    state: TableState,
    in: UpdateTable,
    replyTo: ActorRef[PushOut],
  ): Behavior[TableCommand] = {
    var updated = false
    val newTables = state.tables.map { table =>
      if (table.id == in.table.id) {
        updated = true
        in.table
      }
      else table
    }
    if (updated) {
      val tableUpdated = TableUpdated(table = in.table)
      state.subscribers.foreach(_ ! tableUpdated)
      process(state.copy(tables = newTables))
    } else {
      replyTo ! TableUpdateFailed(
        id = in.table.id,
      )
      Behaviors.same
    }
  }

  private def removeTable(
    state: TableState,
    in: RemoveTable,
    replyTo: ActorRef[PushOut],
  ): Behavior[TableCommand] = {
    val newTables = state.tables.filterNot(_.id == in.id)
    if (newTables.size != state.tables.size) {
      val tableRemoved = TableRemoved(id = in.id)
      state.subscribers.foreach(_ ! tableRemoved)
      process(state.copy(tables = newTables))
    } else {
      replyTo ! TableRemoveFailed(
        id = in.id,
      )
      Behaviors.same
    }
  }
}
