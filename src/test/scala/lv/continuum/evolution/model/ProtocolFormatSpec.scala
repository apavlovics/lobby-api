package lv.continuum.evolution.model

import cats.scalatest.EitherValues
import io.circe.parser._
import io.circe.syntax._
import lv.continuum.evolution.model.Protocol.In._
import lv.continuum.evolution.model.Protocol.Out.LoginSuccessfulOut
import lv.continuum.evolution.model.Protocol.{In, Out, UserType}
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
            id = 3,
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
    }
  }

  private def verifyDecodeIn[A <: In](json: String, in: A): Assertion =
    decode[In](json).value shouldBe in

  private def verifyEncodeOut(out: Out, json: String): Assertion =
    out.asJson shouldBe parse(json).value
}
