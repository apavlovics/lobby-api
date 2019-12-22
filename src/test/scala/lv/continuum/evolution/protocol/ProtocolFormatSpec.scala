package lv.continuum.evolution.protocol

import cats.scalatest.EitherValues
import io.circe.parser._
import io.circe.syntax._
import lv.continuum.evolution.protocol.Protocol.In._
import lv.continuum.evolution.protocol.Protocol.Out._
import lv.continuum.evolution.protocol.Protocol._
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ProtocolFormatSpec
  extends AnyWordSpec
    with Matchers
    with EitherValues
    with ProtocolFormat {

  // Test data

  private val tableJamesBond = Table(
    id = TableId(1),
    name = TableName("table - James Bond"),
    participants = 7,
  )
  private val tableMissionImpossible = Table(
    id = TableId(2),
    name = TableName("table - Mission Impossible"),
    participants = 9,
  )
  private val tableFooFighters = Table(
    id = TableId(3),
    name = TableName("table - Foo Fighters"),
    participants = 4,
  )

  "ProtocolFormat" should {

    "provide correct decoders for In ADTs" in {
      verifyDecodeIn(
        json =
          """|{
             |  "$type": "login",
             |  "username": "user",
             |  "password": "pass"
             |}""".stripMargin,
        in =
          LoginIn(
            username = Username("user"),
            password = Password("pass"),
          ),
      )
      verifyDecodeIn(
        json =
          """
            |{
            |  "$type": "ping",
            |  "seq": 12345
            |}""".stripMargin,
        in =
          PingIn(
            seq = Seq(12345),
          ),
      )
      verifyDecodeIn(
        json =
          """
            |{
            |  "$type": "subscribe_tables"
            |}""".stripMargin,
        in =
          SubscribeTablesIn,
      )
      verifyDecodeIn(
        json =
          """
            |{
            |  "$type": "unsubscribe_tables"
            |}""".stripMargin,
        in =
          UnsubscribeTablesIn,
      )
      verifyDecodeIn(
        json =
          """
            |{
            |  "$type": "add_table",
            |  "after_id": 1,
            |  "table": {
            |    "name": "table - Foo Fighters",
            |    "participants": 4
            |  }
            |}""".stripMargin,
        in =
          AddTableIn(
            afterId = TableId(1),
            table = TableToAdd(
              name = TableName("table - Foo Fighters"),
              participants = 4,
            ),
          ),
      )
      verifyDecodeIn(
        json =
          """
            |{
            |  "$type": "update_table",
            |  "table": {
            |    "id": 3,
            |    "name": "table - Foo Fighters",
            |    "participants": 4
            |  }
            |}""".stripMargin,
        in =
          UpdateTableIn(
            table = tableFooFighters,
          ),
      )
      verifyDecodeIn(
        json =
          """
            |{
            |  "$type": "remove_table",
            |  "id": 3
            |}""".stripMargin,
        in =
          RemoveTableIn(
            id = TableId(3),
          ),
      )
    }

    "provide correct encoders for Out ADTs" in {
      verifyEncodeOut(
        out =
          LoginSuccessfulOut(
            userType = UserType.User,
          ),
        json =
          """
            |{
            |  "$type": "login_successful",
            |  "user_type": "user"
            |}""".stripMargin,
      )
      verifyEncodeOut(
        out =
          PongOut(
            seq = Seq(12345),
          ),
        json =
          """
            |{
            |  "$type": "pong",
            |  "seq": 12345
            |}""".stripMargin,
      )
      verifyEncodeOut(
        out =
          TableListOut(
            tables = Vector(
              tableJamesBond,
              tableMissionImpossible,
            ),
          ),
        json =
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
            |}""".stripMargin,
      )
      verifyEncodeOut(
        out =
          TableAddedOut(
            afterId = TableId(1),
            table = tableFooFighters,
          ),
        json =
          """
            |{
            |  "$type": "table_added",
            |  "after_id": 1,
            |  "table": {
            |    "id": 3,
            |    "name": "table - Foo Fighters",
            |    "participants": 4
            |  }
            |}""".stripMargin,
      )
      verifyEncodeOut(
        out =
          TableUpdatedOut(
            table = tableFooFighters,
          ),
        json =
          """
            |{
            |  "$type": "table_updated",
            |  "table": {
            |    "id": 3,
            |    "name": "table - Foo Fighters",
            |    "participants": 4
            |  }
            |}""".stripMargin,
      )
      verifyEncodeOut(
        out =
          TableRemovedOut(
            id = TableId(3),
          ),
        json =
          """
            |{
            |  "$type": "table_removed",
            |  "id": 3
            |}""".stripMargin,
      )
      verifyEncodeOut(
        out =
          TableErrorOut(
            $type = OutType.TableRemoveFailed,
            id = TableId(3),
          ),
        json =
          """
            |{
            |  "$type": "table_remove_failed",
            |  "id": 3
            |}""".stripMargin,
      )
      verifyEncodeOut(
        out =
          ErrorOut(
            $type = OutType.UnknownError,
          ),
        json =
          """
            |{
            |  "$type": "unknown_error"
            |}""".stripMargin,
      )
    }
  }

  private def verifyDecodeIn[A <: In](json: String, in: A): Assertion =
    decode[In](json).value shouldBe in

  private def verifyEncodeOut(out: Out, json: String): Assertion =
    out.asJson shouldBe parse(json).value
}
