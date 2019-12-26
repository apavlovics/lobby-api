package lv.continuum.evolution.protocol

import lv.continuum.evolution.protocol.Protocol.In._
import lv.continuum.evolution.protocol.Protocol.Out._
import lv.continuum.evolution.protocol.Protocol.UserType._
import lv.continuum.evolution.protocol.Protocol._

trait TestData {

  // Common

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
        |  "after_id": 1,
        |  "table": {
        |    "name": "table - Foo Fighters",
        |    "participants": 4
        |  }
        |}""".stripMargin
    val in =
      AddTableIn(
        afterId = TableId(1),
        table = TableToAdd(
          name = TableName("table - Foo Fighters"),
          participants = 4,
        ),
      )
    (json, in)
  }

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
        tables = List(
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
        |  "after_id": 1,
        |  "table": {
        |    "id": 3,
        |    "name": "table - Foo Fighters",
        |    "participants": 4
        |  }
        |}""".stripMargin
    val out =
      TableAddedOut(
        afterId = TableId(1),
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

  val tableErrorOut: (String, TableErrorOut) = {
    val json =
      """
        |{
        |  "$type": "table_remove_failed",
        |  "id": 3
        |}""".stripMargin
    val out =
      TableErrorOut(
        $type = OutType.TableRemoveFailed,
        id = TableId(3),
      )
    (json, out)
  }

  val errorOut: (String, ErrorOut) = {
    val json =
      """
        |{
        |  "$type": "unknown_error"
        |}""".stripMargin
    val out =
      ErrorOut(
        $type = OutType.UnknownError,
      )
    (json, out)
  }
}
