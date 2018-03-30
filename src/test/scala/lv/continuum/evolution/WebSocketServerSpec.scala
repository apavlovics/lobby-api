package lv.continuum.evolution

import akka.actor._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.ws.{ Message, BinaryMessage }
import akka.http.scaladsl.testkit.{ ScalatestRouteTest, WSProbe }
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString

import de.knutwalker.akka.stream.support.CirceStreamSupport

import io.circe.parser.decode
import io.circe.syntax._

import lv.continuum.evolution.exception._
import lv.continuum.evolution.model._

import org.scalatest.{ WordSpec, Matchers }

import scala.concurrent.duration._

class WebSocketServerSpec extends WordSpec with Matchers with ScalatestRouteTest {

  val route = new WebSocketServer().route

  val validCredentials = BasicHttpCredentials("admin", "admin")
  val invalidCredentials = BasicHttpCredentials("invalid", "invalid")

  "Basic authentication" should {

    "deny access when no credentials are supplied" in {
      Get("/ws_api") ~> route ~> check {
        status shouldEqual StatusCodes.Unauthorized
        responseAs[String] shouldEqual "The resource requires authentication, which was not supplied with the request"
        header[`WWW-Authenticate`].get.challenges.head shouldEqual HttpChallenge("Basic", Some("secure-realm"), Map("charset" → "UTF-8"))
      }
    }

    "deny access when invalid credentials are supplied" in {
      Get("/ws_api") ~>
        addCredentials(invalidCredentials) ~> route ~> check {
          status shouldEqual StatusCodes.Unauthorized
          responseAs[String] shouldEqual "The supplied authentication is invalid"
          header[`WWW-Authenticate`].get.challenges.head shouldEqual HttpChallenge("Basic", Some("secure-realm"), Map("charset" → "UTF-8"))
        }
    }

    "allow access when valid credentials are supplied" in {
      Get("/ws_api") ~> addCredentials(validCredentials) ~> route ~> check {
        status shouldEqual StatusCodes.NotFound
        responseAs[String] shouldEqual "The requested resource could not be found."
      }
    }
  }

  "Web socket server" should {

    "handle web socket connections in echo flow" in {
      val wsProbe = WSProbe()
      WS("/ws_api/echo", wsProbe.flow) ~> addCredentials(validCredentials) ~> route ~> check {
        isWebSocketUpgrade shouldEqual true

        wsProbe.sendMessage(SampleIn(1, "Test").asJson.toString)
        checkSampleOut(wsProbe.inProbe.requestNext(), "Echo: Test")

        wsProbe.sendMessage(BinaryMessage(ByteString("abcde")))
        wsProbe.expectNoMessage()

        wsProbe.sendMessage(SampleIn(1, "Other").asJson.toString)
        checkSampleOut(wsProbe.inProbe.requestNext(), "Echo: Other")

        wsProbe.sendCompletion()
        wsProbe.expectCompletion()
      }
    }

    "handle web socket connections in broadcast flow" in {
      val wsProbe = WSProbe()
      WS("/ws_api/broadcast", wsProbe.flow) ~> addCredentials(validCredentials) ~> route ~> check {
        isWebSocketUpgrade shouldEqual true

        wsProbe.sendMessage(SampleIn(1, "Test").asJson.toString)
        checkSampleOut(wsProbe.inProbe.requestNext(), "Broadcast: Test")

        wsProbe.sendMessage(BinaryMessage(ByteString("abcde")))
        wsProbe.expectNoMessage()

        wsProbe.sendMessage(SampleIn(1, "Other").asJson.toString)
        checkSampleOut(wsProbe.inProbe.requestNext(), "Echo: Other")

        wsProbe.sendCompletion()
      }
    }

    "handle web socket connections in push flow" in {
      val wsProbe = WSProbe()
      WS("/ws_api/push", wsProbe.flow) ~> addCredentials(validCredentials) ~> route ~> check {
        isWebSocketUpgrade shouldEqual true

        checkSampleOut(wsProbe.inProbe.requestNext(2.second), "Push message")

        wsProbe.sendCompletion()
      }
    }
  }

  private def checkSampleOut(message: Message, content: String) {
    message.asTextMessage.getStreamedText
      .map(ByteString(_))
      .via(CirceStreamSupport.decode[SampleOut])
      .map(sampleOut => sampleOut.content shouldEqual content)
  }
}
