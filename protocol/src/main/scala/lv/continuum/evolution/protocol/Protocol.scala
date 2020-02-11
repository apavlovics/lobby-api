package lv.continuum.evolution.protocol

import enumeratum.EnumEntry.Snakecase
import enumeratum.{Enum, EnumEntry}

object Protocol {

  case class Username(value: String) extends AnyVal
  case class Password(value: String) extends AnyVal
  case class Seq(value: Long) extends AnyVal
  case class TableId(value: Long) extends AnyVal {
    def inc: TableId = TableId(value + 1)
  }
  object TableId {

    /** `TableId` to use as an absent (special, nonexistent) value. */
    val Absent: TableId = TableId(-1)

    /** `TableId` to use as the initial value. */
    val Initial: TableId = TableId(0)
  }
  case class TableName(value: String) extends AnyVal

  case class Table(
    id: TableId,
    name: TableName,
    participants: Long,
  )
  case class TableToAdd(
    name: TableName,
    participants: Long,
  ) {
    def toTable(id: TableId): Table = Table(id, name, participants)
  }

  sealed trait In
  sealed trait TableIn extends In
  sealed trait AdminTableIn extends TableIn
  object In {

    case class Login(
      username: Username,
      password: Password,
    ) extends In

    case class Ping(
      seq: Seq,
    ) extends In

    case object SubscribeTables extends TableIn

    case object UnsubscribeTables extends TableIn

    case class AddTable(
      afterId: TableId,
      table: TableToAdd,
    ) extends AdminTableIn

    case class UpdateTable(
      table: Table,
    ) extends AdminTableIn

    case class RemoveTable(
      id: TableId,
    ) extends AdminTableIn
  }

  sealed trait UserType extends EnumEntry with Snakecase
  object UserType extends Enum[UserType] {

    val values: IndexedSeq[UserType] = findValues

    case object User extends UserType
    case object Admin extends UserType
  }

  sealed trait Out
  sealed trait PushOut extends Out
  object Out {

    case class LoginSuccessful(
      userType: UserType,
    ) extends Out

    case object LoginFailed extends Out

    case class Pong(
      seq: Seq,
    ) extends Out

    case class TableList(
      tables: Vector[Table],
    ) extends PushOut

    case class TableAdded(
      afterId: TableId,
      table: Table,
    ) extends PushOut

    case class TableUpdated(
      table: Table,
    ) extends PushOut

    case class TableRemoved(
      id: TableId,
    ) extends PushOut

    case object TableAddFailed extends PushOut

    case class TableUpdateFailed(
      id: TableId,
    ) extends PushOut

    case class TableRemoveFailed(
      id: TableId,
    ) extends PushOut

    case object NotAuthorized extends PushOut

    case object NotAuthenticated extends PushOut

    case object InvalidMessage extends PushOut
  }
}
