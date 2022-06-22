package lv.continuum.lobby.protocol

import enumeratum.EnumEntry.Snakecase
import enumeratum.{Enum, EnumEntry}
import zio.json.{SnakeCase, jsonDiscriminator, jsonHint, jsonMemberNames}

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

  @jsonDiscriminator(name = "$type")
  sealed trait In
  sealed trait TableIn extends In
  sealed trait AdminTableIn extends TableIn
  object In {

    @jsonHint(name = "login")
    @jsonMemberNames(SnakeCase)
    case class Login(
      username: Username,
      password: Password,
    ) extends In

    @jsonHint(name = "ping")
    @jsonMemberNames(SnakeCase)
    case class Ping(
      seq: Seq,
    ) extends In

    @jsonHint(name = "subscribe_tables")
    case object SubscribeTables extends TableIn

    @jsonHint(name = "unsubscribe_tables")
    case object UnsubscribeTables extends TableIn

    @jsonHint(name = "add_table")
    @jsonMemberNames(SnakeCase)
    case class AddTable(
      afterId: TableId,
      table: TableToAdd,
    ) extends AdminTableIn

    @jsonHint(name = "update_table")
    @jsonMemberNames(SnakeCase)
    case class UpdateTable(
      table: Table,
    ) extends AdminTableIn

    @jsonHint(name = "remove_table")
    @jsonMemberNames(SnakeCase)
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

  @jsonDiscriminator(name = "$type")
  sealed trait Out
  sealed trait PushOut extends Out
  object Out {

    @jsonHint(name = "login_successful")
    @jsonMemberNames(SnakeCase)
    case class LoginSuccessful(
      userType: UserType,
    ) extends Out

    @jsonHint(name = "login_failed")
    case object LoginFailed extends Out

    @jsonHint(name = "pong")
    @jsonMemberNames(SnakeCase)
    case class Pong(
      seq: Seq,
    ) extends Out

    @jsonHint(name = "table_list")
    @jsonMemberNames(SnakeCase)
    case class TableList(
      tables: Vector[Table],
    ) extends PushOut

    @jsonHint(name = "table_added")
    @jsonMemberNames(SnakeCase)
    case class TableAdded(
      afterId: TableId,
      table: Table,
    ) extends PushOut

    @jsonHint(name = "table_updated")
    @jsonMemberNames(SnakeCase)
    case class TableUpdated(
      table: Table,
    ) extends PushOut

    @jsonHint(name = "table_removed")
    @jsonMemberNames(SnakeCase)
    case class TableRemoved(
      id: TableId,
    ) extends PushOut

    @jsonHint(name = "table_add_failed")
    case object TableAddFailed extends PushOut

    @jsonHint(name = "table_update_failed")
    @jsonMemberNames(SnakeCase)
    case class TableUpdateFailed(
      id: TableId,
    ) extends PushOut

    @jsonHint(name = "table_remove_failed")
    @jsonMemberNames(SnakeCase)
    case class TableRemoveFailed(
      id: TableId,
    ) extends PushOut

    @jsonHint(name = "not_authorized")
    case object NotAuthorized extends PushOut

    @jsonHint(name = "not_authenticated")
    case object NotAuthenticated extends PushOut

    @jsonHint(name = "invalid_message")
    case object InvalidMessage extends PushOut
  }
}
