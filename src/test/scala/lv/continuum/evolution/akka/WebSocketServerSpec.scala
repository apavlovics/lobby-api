package lv.continuum.evolution.akka

import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

// TODO: Add more specific tests.
class WebSocketServerSpec
  extends AnyWordSpec
    with Matchers
    with ScalatestRouteTest {

  private val route = new WebSocketServer().route

  "WebSocketServer" should {

    "handle WebSocket connections" in {
      val wsProbe = WSProbe()
      WS("/ws_api", wsProbe.flow) ~> route ~> check {
        isWebSocketUpgrade shouldBe true
      }
    }
  }
}
