package lv.continuum.lobby.protocol

import cats.syntax.option.*

object Protocol {

  opaque type Username = String
  object Username {
    def apply(value: String): Username = value
    def unapply(value: Username): Option[String] = value.some
  }

  opaque type Password = String
  object Password {
    def apply(value: String): Password = value
    def unapply(value: Password): Option[String] = value.some
  }

  opaque type Seq = Long
  object Seq {
    def apply(value: Long): Seq = value
  }

  opaque type TableId = Long
  object TableId {

    def apply(value: Long): TableId = value

    /** `TableId` to use as an absent (special, nonexistent) value. */
    val Absent: TableId = -1

    /** `TableId` to use as the initial value. */
    val Initial: TableId = 0
  }
  extension (tableId: TableId) {
    def value: Long = tableId
    def inc: TableId = tableId + 1
  }

  opaque type TableName = String
  object TableName {
    def apply(value: String): TableName = value
  }

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

  enum UserType(val value: String) {
    case User extends UserType(value = "user")
    case Admin extends UserType(value = "admin")
  }
  object UserType {
    lazy val valuesMap: Map[String, UserType] =
      UserType.values.map(userType => userType.value -> userType).toMap
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
