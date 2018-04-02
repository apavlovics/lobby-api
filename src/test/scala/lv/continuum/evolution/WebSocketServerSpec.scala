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

import lv.continuum.evolution.model._

import org.scalatest.{ WordSpec, Matchers }

import scala.concurrent.duration._

class WebSocketServerSpec extends WordSpec with Matchers with ScalatestRouteTest {

  val route = new WebSocketServer().route

  "Web socket server" should {

    "handle web socket connections in flow" in {
      val wsProbe = WSProbe()
      WS("/ws_api", wsProbe.flow) ~> route ~> check {
        isWebSocketUpgrade shouldEqual true
      }
    }
  }
}
