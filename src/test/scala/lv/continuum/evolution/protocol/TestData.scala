package lv.continuum.evolution.protocol

import lv.continuum.evolution.protocol.Protocol.In._
import lv.continuum.evolution.protocol.Protocol.Out._
import lv.continuum.evolution.protocol.Protocol.UserType._
import lv.continuum.evolution.protocol.Protocol._

trait TestData {

  // Common

  val tableIdInvalid: TableId = TableId(99999)
  val tableJamesBond: Table = Table(
    id = TableId(1),
    name = TableName("table - James Bond"),
    participants = 7,
  )
  val tableMissionImpossible: Table = Table(
    id = TableId(2),
    name = TableName("table - Mission Impossible"),
    participants = 9,
  )
  val tableFooFighters: Table = Table(
    id = TableId(3),
    name = TableName("table - Foo Fighters"),
    participants = 4,
  )

  // In

  val loginIn: (String, LoginIn) = {
    val json =
      """|{
         |  "$type": "login",
         |  "username": "user",
         |  "password": "pass"
         |}""".stripMargin
    val in =
      LoginIn(
        username = Username("user"),
        password = Password("pass"),
      )
    (json, in)
  }

  val pingIn: (String, PingIn) = {
    val json =
      """
        |{
        |  "$type": "ping",
        |  "seq": 12345
        |}""".stripMargin
    val in =
      PingIn(
        seq = Seq(12345),
      )
    (json, in)
  }

  val subscribeTablesIn: (String, SubscribeTablesIn.type) = {
    val json =
      """
        |{
        |  "$type": "subscribe_tables"
        |}""".stripMargin
    (json, SubscribeTablesIn)
  }

  val unsubscribeTablesIn: (String, UnsubscribeTablesIn.type) = {
    val json =
      """
        |{
        |  "$type": "unsubscribe_tables"
        |}""".stripMargin
    (json, UnsubscribeTablesIn)
  }

  val addTableIn: (String, AddTableIn) = {
    val json =
      """
        |{
        |  "$type": "add_table",
        |  "after_id": -1,
        |  "table": {
        |    "name": "table - Foo Fighters",
        |    "participants": 4
        |  }
        |}""".stripMargin
    val in =
      AddTableIn(
        afterId = TableId.Absent,
        table = TableToAdd(
          name = TableName("table - Foo Fighters"),
          participants = 4,
        ),
      )
    (json, in)
  }

  val addTableInInvalid: AddTableIn =
    addTableIn._2.copy(afterId = tableIdInvalid)

  val updateTableIn: (String, UpdateTableIn) = {
    val json =
      """
        |{
        |  "$type": "update_table",
        |  "table": {
        |    "id": 3,
        |    "name": "table - Foo Fighters",
        |    "participants": 4
        |  }
        |}""".stripMargin
    val in =
      UpdateTableIn(
        table = tableFooFighters,
      )
    (json, in)
  }

  val updateTableInInvalid: UpdateTableIn =
    updateTableIn._2.copy(table = updateTableIn._2.table.copy(id = tableIdInvalid))

  val removeTableIn: (String, RemoveTableIn) = {
    val json =
      """
        |{
        |  "$type": "remove_table",
        |  "id": 3
        |}""".stripMargin
    val in =
      RemoveTableIn(
        id = TableId(3),
      )
    (json, in)
  }

  val removeTableInInvalid: RemoveTableIn =
    removeTableIn._2.copy(id = tableIdInvalid)

  // Out

  val loginSuccessfulOutUser: (String, LoginSuccessfulOut) = {
    val json =
      """
        |{
        |  "$type": "login_successful",
        |  "user_type": "user"
        |}""".stripMargin
    val out =
      LoginSuccessfulOut(
        userType = User,
      )
    (json, out)
  }

  val loginSuccessfulOutAdmin: (String, LoginSuccessfulOut) = {
    val json =
      """
        |{
        |  "$type": "login_successful",
        |  "user_type": "admin"
        |}""".stripMargin
    val out =
      LoginSuccessfulOut(
        userType = Admin,
      )
    (json, out)
  }

  val pongOut: (String, PongOut) = {
    val json =
      """
        |{
        |  "$type": "pong",
        |  "seq": 12345
        |}""".stripMargin
    val out =
      PongOut(
        seq = Seq(12345),
      )
    (json, out)
  }

  val tableListOut: (String, TableListOut) = {
    val json =
      """
        |{
        |  "$type": "table_list",
        |  "tables": [
        |    {
        |      "id": 1,
        |      "name": "table - James Bond",
        |      "participants": 7
        |    }, {
        |      "id": 2,
        |      "name": "table - Mission Impossible",
        |      "participants": 9
        |    }
        |  ]
        |}""".stripMargin
    val out =
      TableListOut(
        tables = Vector(
          tableJamesBond,
          tableMissionImpossible,
        ),
      )
    (json, out)
  }

  val tableAddedOut: (String, TableAddedOut) = {
    val json =
      """
        |{
        |  "$type": "table_added",
        |  "after_id": -1,
        |  "table": {
        |    "id": 3,
        |    "name": "table - Foo Fighters",
        |    "participants": 4
        |  }
        |}""".stripMargin
    val out =
      TableAddedOut(
        afterId = TableId.Absent,
        table = tableFooFighters,
      )
    (json, out)
  }

  val tableUpdatedOut: (String, TableUpdatedOut) = {
    val json =
      """
        |{
        |  "$type": "table_updated",
        |  "table": {
        |    "id": 3,
        |    "name": "table - Foo Fighters",
        |    "participants": 4
        |  }
        |}""".stripMargin
    val out =
      TableUpdatedOut(
        table = tableFooFighters,
      )
    (json, out)
  }

  val tableRemovedOut: (String, TableRemovedOut) = {
    val json =
      """
        |{
        |  "$type": "table_removed",
        |  "id": 3
        |}""".stripMargin
    val out =
      TableRemovedOut(
        id = TableId(3),
      )
    (json, out)
  }

  val tableErrorOutTableUpdateFailed: (String, TableErrorOut) = {
    val json =
      """
        |{
        |  "$type": "table_update_failed",
        |  "id": 99999
        |}""".stripMargin
    val out =
      TableErrorOut(
        $type = OutType.TableUpdateFailed,
        id = tableIdInvalid,
      )
    (json, out)
  }

  val tableErrorOutTableRemoveFailed: (String, TableErrorOut) = {
    val json =
      """
        |{
        |  "$type": "table_remove_failed",
        |  "id": 99999
        |}""".stripMargin
    val out =
      TableErrorOut(
        $type = OutType.TableRemoveFailed,
        id = tableIdInvalid,
      )
    (json, out)
  }

  val errorOutLoginFailed: (String, ErrorOut) = {
    val json =
      """
        |{
        |  "$type": "login_failed"
        |}""".stripMargin
    val out =
      ErrorOut(
        $type = OutType.LoginFailed,
      )
    (json, out)
  }

  val errorOutTableAddFailed: (String, ErrorOut) = {
    val json =
      """
        |{
        |  "$type": "table_add_failed"
        |}""".stripMargin
    val out =
      ErrorOut(
        $type = OutType.TableAddFailed,
      )
    (json, out)
  }

  val errorOutNotAuthorized: (String, ErrorOut) = {
    val json =
      """
        |{
        |  "$type": "not_authorized"
        |}""".stripMargin
    val out =
      ErrorOut(
        $type = OutType.NotAuthorized,
      )
    (json, out)
  }

  val errorOutNotAuthenticated: (String, ErrorOut) = {
    val json =
      """
        |{
        |  "$type": "not_authenticated"
        |}""".stripMargin
    val out =
      ErrorOut(
        $type = OutType.NotAuthenticated,
      )
    (json, out)
  }

  val errorOutInvalidMessage: (String, ErrorOut) = {
    val json =
      """
        |{
        |  "$type": "invalid_message"
        |}""".stripMargin
    val out =
      ErrorOut(
        $type = OutType.InvalidMessage,
      )
    (json, out)
  }
}
