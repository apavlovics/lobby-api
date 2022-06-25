package lv.continuum.lobby.protocol

import lv.continuum.lobby.protocol.Protocol.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{Assertion, EitherValues}
import org.skyscreamer.jsonassert.JSONAssert

class ProtocolFormatSpec
  extends AnyWordSpec
    with Matchers
    with EitherValues
    with ProtocolFormat
    with TestData {

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
    fromJson[In](tuple._1).value shouldBe tuple._2

  private def verifyEncodeOut(tuple: (String, Out)): Assertion =
    noException should be thrownBy {
      val strict = true
      JSONAssert.assertEquals(tuple._1, toJson(tuple._2), strict)
    }
}
