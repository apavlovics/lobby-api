package lv.continuum.evolution.akka

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import lv.continuum.evolution.protocol.Protocol.In._
import lv.continuum.evolution.protocol.Protocol.Out._
import lv.continuum.evolution.protocol.Protocol._

object TableActor {

  case class TableCommand(in: TableIn, replyTo: ActorRef[PushOut])

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
      val nextId = tables.map(_.id).maxByOption(_.value).map(_.inc).getOrElse(TableId.Initial)
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
          command.replyTo ! TableListOut(tables = state.tables)
          process(state.copy(subscribers = state.subscribers + command.replyTo))

        case UnsubscribeTablesIn =>
          process(state.copy(subscribers = state.subscribers - command.replyTo))

        case in: AddTableIn =>
          addTable(state, in, command.replyTo)

        case in: UpdateTableIn =>
          updateTable(state, in, command.replyTo)

        case in: RemoveTableIn =>
          removeTable(state, in, command.replyTo)
      }
    }
  }

  private def addTable(
    state: TableState,
    in: AddTableIn,
    replyTo: ActorRef[PushOut],
  ): Behavior[TableCommand] = {

    def tableToAdd: Table = in.table.toTable(state.nextId)

    val newTables = {
      if (in.afterId == TableId.BeforeAll) {
        tableToAdd :: state.tables
      } else {
        state.tables.flatMap { t =>
          if (t.id == in.afterId) List(t, tableToAdd) else List(t)
        }
      }
    }
    if (newTables.size != state.tables.size) {
      val tableAddedOut = TableAddedOut(
        afterId = in.afterId,
        table = tableToAdd,
      )
      state.subscribers.foreach(_ ! tableAddedOut)
      process(state.copy(
        nextId = state.nextId.inc,
        tables = newTables,
      ))
    } else {
      replyTo ! ErrorOut(
        $type = OutType.TableAddFailed,
      )
      Behaviors.same
    }
  }

  private def updateTable(
    state: TableState,
    in: UpdateTableIn,
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
      val tableUpdatedOut = TableUpdatedOut(table = in.table)
      state.subscribers.foreach(_ ! tableUpdatedOut)
      process(state.copy(tables = newTables))
    } else {
      replyTo ! TableErrorOut(
        $type = OutType.TableUpdateFailed,
        id = in.table.id,
      )
      Behaviors.same
    }
  }

  private def removeTable(
    state: TableState,
    in: RemoveTableIn,
    replyTo: ActorRef[PushOut],
  ): Behavior[TableCommand] = {
    val newTables = state.tables.filterNot(_.id == in.id)
    if (newTables.size != state.tables.size) {
      val tableRemovedOut = TableRemovedOut(id = in.id)
      state.subscribers.foreach(_ ! tableRemovedOut)
      process(state.copy(tables = newTables))
    } else {
      replyTo ! TableErrorOut(
        $type = OutType.TableRemoveFailed,
        id = in.id,
      )
      Behaviors.same
    }
  }
}
