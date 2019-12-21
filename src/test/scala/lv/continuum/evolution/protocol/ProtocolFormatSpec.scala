package lv.continuum.evolution.protocol

import cats.scalatest.EitherValues
import io.circe.parser._
import io.circe.syntax._
import lv.continuum.evolution.protocol.Protocol._
import lv.continuum.evolution.protocol.Protocol.In._
import lv.continuum.evolution.protocol.Protocol.Out._
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ProtocolFormatSpec
  extends AnyWordSpec
    with Matchers
    with EitherValues
    with ProtocolFormat {

  "ProtocolFormat" should {
    "provide correct decoders for In ADTs" in {
      verifyDecodeIn(
        json =
          """|{
             |  "$type": "login",
             |  "username": "user1234",
             |  "password": "password1234"
             |}""".stripMargin,
        in =
          LoginIn(
            username = "user1234",
            password = "password1234",
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
            seq = 12345,
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
            seq = 12345,
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
            tables = List(
              Table(
                id = TableId(1),
                name = "table - James Bond",
                participants = 7,
              ),
              Table(
                id = TableId(2),
                name = "table - Mission Impossible",
                participants = 4,
              ),
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
            |      "participants": 4
            |    }
            |  ]
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
            $type = OutType.RemovalFailed,
            id = TableId(3),
          ),
        json =
          """
            |{
            |  "$type": "removal_failed",
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
