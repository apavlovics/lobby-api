package lv.continuum.lobby.protocol

import io.circe.{Error, ParsingFailure}
import lv.continuum.lobby.protocol.Protocol.In._
import lv.continuum.lobby.protocol.Protocol.Out._
import lv.continuum.lobby.protocol.Protocol.UserType._
import lv.continuum.lobby.protocol.Protocol._

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
  val tableToAddFooFighters: TableToAdd = TableToAdd(
    name = TableName("table - Foo Fighters"),
    participants = 4,
  )

  // In

  val error: Error = ParsingFailure("Parsing failed", new Exception("BANG!"))

  val login: (String, Login) = {
    val json =
      """|{
         |  "$type": "login",
         |  "username": "user",
         |  "password": "pass"
         |}""".stripMargin
    val in =
      Login(
        username = Username("user"),
        password = Password("pass"),
      )
    (json, in)
  }

  val ping: (String, Ping) = {
    val json =
      """
        |{
        |  "$type": "ping",
        |  "seq": 12345
        |}""".stripMargin
    val in =
      Ping(
        seq = Seq(12345),
      )
    (json, in)
  }

  val subscribeTables: (String, SubscribeTables.type) = {
    val json =
      """
        |{
        |  "$type": "subscribe_tables"
        |}""".stripMargin
    (json, SubscribeTables)
  }

  val unsubscribeTables: (String, UnsubscribeTables.type) = {
    val json =
      """
        |{
        |  "$type": "unsubscribe_tables"
        |}""".stripMargin
    (json, UnsubscribeTables)
  }

  val addTable: (String, AddTable) = {
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
      AddTable(
        afterId = TableId.Absent,
        table = tableToAddFooFighters,
      )
    (json, in)
  }

  val addTableInvalid: AddTable =
    addTable._2.copy(afterId = tableIdInvalid)

  val updateTable: (String, UpdateTable) = {
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
      UpdateTable(
        table = tableFooFighters,
      )
    (json, in)
  }

  val updateTableInvalid: UpdateTable =
    updateTable._2.copy(table = updateTable._2.table.copy(id = tableIdInvalid))

  val removeTable: (String, RemoveTable) = {
    val json =
      """
        |{
        |  "$type": "remove_table",
        |  "id": 3
        |}""".stripMargin
    val in =
      RemoveTable(
        id = TableId(3),
      )
    (json, in)
  }

  val removeTableInvalid: RemoveTable =
    removeTable._2.copy(id = tableIdInvalid)

  // Out

  val loginSuccessfulUser: (String, LoginSuccessful) = {
    val json =
      """
        |{
        |  "$type": "login_successful",
        |  "user_type": "user"
        |}""".stripMargin
    val out =
      LoginSuccessful(
        userType = User,
      )
    (json, out)
  }

  val loginSuccessfulAdmin: (String, LoginSuccessful) = {
    val json =
      """
        |{
        |  "$type": "login_successful",
        |  "user_type": "admin"
        |}""".stripMargin
    val out =
      LoginSuccessful(
        userType = Admin,
      )
    (json, out)
  }

  val loginFailed: (String, LoginFailed.type) = {
    val json =
      """
        |{
        |  "$type": "login_failed"
        |}""".stripMargin
    (json, LoginFailed)
  }

  val pong: (String, Pong) = {
    val json =
      """
        |{
        |  "$type": "pong",
        |  "seq": 12345
        |}""".stripMargin
    val out =
      Pong(
        seq = Seq(12345),
      )
    (json, out)
  }

  val tableList: (String, TableList) = {
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
      TableList(
        tables = Vector(
          tableJamesBond,
          tableMissionImpossible,
        ),
      )
    (json, out)
  }

  val tableAdded: (String, TableAdded) = {
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
      TableAdded(
        afterId = TableId.Absent,
        table = tableFooFighters,
      )
    (json, out)
  }

  val tableUpdated: (String, TableUpdated) = {
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
      TableUpdated(
        table = tableFooFighters,
      )
    (json, out)
  }

  val tableRemoved: (String, TableRemoved) = {
    val json =
      """
        |{
        |  "$type": "table_removed",
        |  "id": 3
        |}""".stripMargin
    val out =
      TableRemoved(
        id = TableId(3),
      )
    (json, out)
  }

  val tableAddFailed: (String, TableAddFailed.type) = {
    val json =
      """
        |{
        |  "$type": "table_add_failed"
        |}""".stripMargin
    (json, TableAddFailed)
  }

  val tableUpdateFailed: (String, TableUpdateFailed) = {
    val json =
      """
        |{
        |  "$type": "table_update_failed",
        |  "id": 99999
        |}""".stripMargin
    val out =
      TableUpdateFailed(
        id = tableIdInvalid,
      )
    (json, out)
  }

  val tableRemoveFailed: (String, TableRemoveFailed) = {
    val json =
      """
        |{
        |  "$type": "table_remove_failed",
        |  "id": 99999
        |}""".stripMargin
    val out =
      TableRemoveFailed(
        id = tableIdInvalid,
      )
    (json, out)
  }

  val notAuthorized: (String, NotAuthorized.type) = {
    val json =
      """
        |{
        |  "$type": "not_authorized"
        |}""".stripMargin
    (json, NotAuthorized)
  }

  val notAuthenticated: (String, NotAuthenticated.type) = {
    val json =
      """
        |{
        |  "$type": "not_authenticated"
        |}""".stripMargin
    (json, NotAuthenticated)
  }

  val invalidMessage: (String, InvalidMessage.type) = {
    val json =
      """
        |{
        |  "$type": "invalid_message"
        |}""".stripMargin
    (json, InvalidMessage)
  }
}
