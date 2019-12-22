package lv.continuum.evolution.protocol

import enumeratum.EnumEntry.Snakecase
import enumeratum.{Enum, EnumEntry}

object Protocol {

  case class Username(value: String) extends AnyVal
  case class Password(value: String) extends AnyVal
  case class Seq(value: Long) extends AnyVal
  case class TableId(value: Long) extends AnyVal
  case class TableName(value: String) extends AnyVal

  case class Table(
    id: TableId,
    name: TableName,
    participants: Long,
  )
  case class TableToAdd(
    name: TableName,
    participants: Long,
  )

  sealed trait InType extends EnumEntry with Snakecase
  object InType extends Enum[InType] {

    val values: IndexedSeq[InType] = findValues

    case object Login extends InType
    case object Ping extends InType
    case object SubscribeTables extends InType
    case object UnsubscribeTables extends InType
    case object AddTable extends InType
    case object UpdateTable extends InType
    case object RemoveTable extends InType
  }

  sealed trait In
  sealed trait TableIn extends In
  sealed trait AdminTableIn extends TableIn
  object In {

    case class LoginIn(
      username: Username,
      password: Password,
    ) extends In

    case class PingIn(
      seq: Seq,
    ) extends In

    case object SubscribeTablesIn extends TableIn

    case object UnsubscribeTablesIn extends TableIn

    case class AddTableIn(
      afterId: TableId,
      table: TableToAdd,
    ) extends AdminTableIn

    case class UpdateTableIn(
      table: Table,
    ) extends AdminTableIn

    case class RemoveTableIn(
      id: TableId,
    ) extends AdminTableIn
  }

  sealed trait OutType extends EnumEntry with Snakecase
  object OutType extends Enum[OutType] {

    val values: IndexedSeq[OutType] = findValues

    case object LoginSuccessful extends OutType
    case object LoginFailed extends OutType
    case object Pong extends OutType
    case object TableList extends OutType
    case object TableAdded extends OutType
    case object TableUpdated extends OutType
    case object TableRemoved extends OutType
    case object TableAddFailed extends OutType
    case object TableUpdateFailed extends OutType
    case object TableRemoveFailed extends OutType
    case object NotAuthorized extends OutType
    case object NotAuthenticated extends OutType
    case object InvalidMessage extends OutType
    case object UnknownError extends OutType
  }

  sealed trait UserType extends EnumEntry with Snakecase
  object UserType extends Enum[UserType] {

    val values: IndexedSeq[UserType] = findValues

    case object User extends UserType
    case object Admin extends UserType
  }

  sealed trait Out {
    def $type: OutType
  }
  sealed trait PushOut extends Out
  object Out {

    import OutType._

    case class LoginSuccessfulOut(
      $type: OutType = LoginSuccessful,
      userType: UserType,
    ) extends Out

    case class PongOut(
      $type: OutType = Pong,
      seq: Seq,
    ) extends Out

    case class TableListOut(
      $type: OutType = TableList,
      tables: Vector[Table],
    ) extends PushOut

    case class TableAddedOut(
      $type: OutType = TableAdded,
      afterId: TableId,
      table: Table,
    ) extends PushOut

    case class TableUpdatedOut(
      $type: OutType = TableUpdated,
      table: Table,
    ) extends PushOut

    case class TableRemovedOut(
      $type: OutType = TableRemoved,
      id: TableId,
    ) extends PushOut

    case class TableErrorOut(
      $type: OutType,
      id: TableId,
    ) extends PushOut

    case class ErrorOut(
      $type: OutType,
    ) extends Out
  }
}
