package lv.continuum.lobby.akka

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import lv.continuum.lobby.model.Lobby
import lv.continuum.lobby.protocol.Protocol.In.*
import lv.continuum.lobby.protocol.Protocol.Out.*
import lv.continuum.lobby.protocol.Protocol.*

object TableActor {

  case class TableCommand(in: TableIn, replyTo: ActorRef[PushOut])

  private case class TableState(
    lobby: Lobby,
    subscribers: Set[ActorRef[PushOut]],
  )

  def apply(): Behavior[TableCommand] =
    process(
      TableState(
        lobby = Lobby(),
        subscribers = Set.empty,
      )
    )

  private def process(state: TableState): Behavior[TableCommand] = {
    Behaviors.receive { (_, command) =>
      command.in match {
        case SubscribeTables =>
          command.replyTo ! TableList(tables = state.lobby.tables)
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
  ): Behavior[TableCommand] =
    state.lobby
      .addTable(in.afterId, in.table)
      .fold[Behavior[TableCommand]] {
        replyTo ! TableAddFailed
        Behaviors.same
      } { case (lobby, table) =>
        val tableAdded = TableAdded(
          afterId = in.afterId,
          table = table,
        )
        state.subscribers.foreach(_ ! tableAdded)
        process(state.copy(lobby = lobby))
      }

  private def updateTable(
    state: TableState,
    in: UpdateTable,
    replyTo: ActorRef[PushOut],
  ): Behavior[TableCommand] =
    state.lobby
      .updateTable(in.table)
      .fold[Behavior[TableCommand]] {
        replyTo ! TableUpdateFailed(
          id = in.table.id,
        )
        Behaviors.same
      } { lobby =>
        val tableUpdated = TableUpdated(table = in.table)
        state.subscribers.foreach(_ ! tableUpdated)
        process(state.copy(lobby = lobby))
      }

  private def removeTable(
    state: TableState,
    in: RemoveTable,
    replyTo: ActorRef[PushOut],
  ): Behavior[TableCommand] = {
    state.lobby
      .removeTable(in.id)
      .fold[Behavior[TableCommand]] {
        replyTo ! TableRemoveFailed(
          id = in.id,
        )
        Behaviors.same
      } { lobby =>
        val tableRemoved = TableRemoved(id = in.id)
        state.subscribers.foreach(_ ! tableRemoved)
        process(state.copy(lobby = lobby))
      }
  }
}
