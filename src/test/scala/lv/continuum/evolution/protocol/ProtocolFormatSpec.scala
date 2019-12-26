package lv.continuum.evolution.protocol

import cats.scalatest.EitherValues
import io.circe.parser._
import io.circe.syntax._
import lv.continuum.evolution.protocol.Protocol._
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ProtocolFormatSpec
  extends AnyWordSpec
    with Matchers
    with EitherValues
    with ProtocolFormat
    with TestData {

  "ProtocolFormat" should {
    "provide correct decoders for In ADTs" in {
      List(
        loginIn,
        pingIn,
        subscribeTablesIn,
        unsubscribeTablesIn,
        addTableIn,
        updateTableIn,
        removeTableIn,
      ).map(verifyDecodeIn)
    }
    "provide correct encoders for Out ADTs" in {
      List(
        loginSuccessfulOutUser,
        loginSuccessfulOutAdmin,
        pongOut,
        tableListOut,
        tableAddedOut,
        tableUpdatedOut,
        tableRemovedOut,
        tableErrorOutTableUpdateFailed,
        tableErrorOutTableRemoveFailed,
        errorOutLoginFailed,
        errorOutTableAddFailed,
        errorOutNotAuthorized,
        errorOutNotAuthenticated,
        errorOutInvalidMessage,
      ).map(verifyEncodeOut)
    }
  }

  private def verifyDecodeIn[A <: In](tuple: (String, A)): Assertion =
    decode[In](tuple._1).value shouldBe tuple._2

  private def verifyEncodeOut(tuple: (String, Out)): Assertion =
    tuple._2.asJson shouldBe parse(tuple._1).value
}
