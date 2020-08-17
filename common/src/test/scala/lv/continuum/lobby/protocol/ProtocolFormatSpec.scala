package lv.continuum.lobby.protocol

import cats.scalatest.EitherValues
import io.circe.parser._
import io.circe.syntax._
import lv.continuum.lobby.protocol.Protocol._
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ProtocolFormatSpec extends AnyWordSpec with Matchers with EitherValues with ProtocolFormat with TestData {

  "ProtocolFormat" should {
    "provide correct decoders for In ADTs" in {
      List(
        login,
        ping,
        subscribeTables,
        unsubscribeTables,
        addTable,
        updateTable,
        removeTable,
      ).map(verifyDecodeIn)
    }
    "provide correct encoders for Out ADTs" in {
      List(
        loginSuccessfulUser,
        loginSuccessfulAdmin,
        loginFailed,
        pong,
        tableList,
        tableAdded,
        tableUpdated,
        tableRemoved,
        tableAddFailed,
        tableUpdateFailed,
        tableRemoveFailed,
        notAuthorized,
        notAuthenticated,
        invalidMessage,
      ).map(verifyEncodeOut)
    }
  }

  private def verifyDecodeIn(tuple: (String, In)): Assertion =
    decode[In](tuple._1).value shouldBe tuple._2

  private def verifyEncodeOut(tuple: (String, Out)): Assertion =
    tuple._2.asJson shouldBe parse(tuple._1).value
}
