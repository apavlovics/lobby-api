package lv.continuum.evolution.akka

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import lv.continuum.evolution.protocol.Protocol.In._
import lv.continuum.evolution.protocol.Protocol.Out._
import lv.continuum.evolution.protocol.Protocol._

object TableActor {

  case class TableCommand(in: TableIn, pushTo: ActorRef[PushOut])

  private case class TableState(
    tables: List[Table],
    subscribers: Set[ActorRef[PushOut]],
    nextId: TableId,
  )
  private object TableState {
    def apply(
      tables: List[Table],
      subscribers: Set[ActorRef[PushOut]],
    ): TableState = {
      val nextId = tables.map(_.id).maxByOption(_.value).map(_.inc).getOrElse(TableId(0))
      TableState(tables, subscribers, nextId)
    }
  }

  def apply(): Behavior[TableCommand] = process(
    // Initial TableState that holds some sample data
    TableState(
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
  )

  private def process(state: TableState): Behavior[TableCommand] = {
    Behaviors.receive { (_, command) =>
      command.in match {
        case SubscribeTablesIn =>
          command.pushTo ! TableListOut(tables = state.tables)
          process(state.copy(subscribers = state.subscribers + command.pushTo))

        case UnsubscribeTablesIn =>
          process(state.copy(subscribers = state.subscribers - command.pushTo))

        case AddTableIn(afterId, tableToAdd) =>

          def table: Table = tableToAdd.toTable(state.nextId)

          val newTables = {
            if (afterId == TableId(-1)) {
              table :: state.tables
            } else {
              state.tables.flatMap { t =>
                if (t.id == afterId) List(t, table) else List(t)
              }
            }
          }
          if (newTables.size != state.tables.size) {
            val tableAddedOut = TableAddedOut(
              afterId = afterId,
              table = table,
            )
            state.subscribers.foreach(_ ! tableAddedOut)
            process(state.copy(
              nextId = state.nextId.inc,
              tables = newTables,
            ))
          } else {
            command.pushTo ! ErrorOut(
              $type = OutType.TableAddFailed,
            )
            Behaviors.same
          }

        case UpdateTableIn(tableToUpdate) =>
          var updated = false
          val newTables = state.tables.map { table =>
            if (table.id == tableToUpdate.id) {
              updated = true
              tableToUpdate
            }
            else table
          }
          if (updated) {
            val tableUpdatedOut = TableUpdatedOut(table = tableToUpdate)
            state.subscribers.foreach(_ ! tableUpdatedOut)
            process(state.copy(tables = newTables))
          } else {
            command.pushTo ! TableErrorOut(
              $type = OutType.TableUpdateFailed,
              id = tableToUpdate.id,
            )
            Behaviors.same
          }

        case RemoveTableIn(tableIdToRemove) =>
          val newTables = state.tables.filterNot(_.id == tableIdToRemove)
          if (newTables.size != state.tables.size) {
            val tableRemovedOut = TableRemovedOut(id = tableIdToRemove)
            state.subscribers.foreach(_ ! tableRemovedOut)
            process(state.copy(tables = newTables))
          } else {
            command.pushTo ! TableErrorOut(
              $type = OutType.TableRemoveFailed,
              id = tableIdToRemove,
            )
            Behaviors.same
          }
      }
    }
  }
}
