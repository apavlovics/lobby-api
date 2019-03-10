package lv.continuum.evolution

import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import org.scalatest.{WordSpec, Matchers}

// TODO: Add more specific tests.
class WebSocketServerSpec extends WordSpec with Matchers with ScalatestRouteTest {

  private val route = new WebSocketServer().route

  "Web socket server" should {

    "handle web socket connections" in {
      val wsProbe = WSProbe()
      WS("/ws_api", wsProbe.flow) ~> route ~> check {
        isWebSocketUpgrade shouldBe true
      }
    }
  }
}
