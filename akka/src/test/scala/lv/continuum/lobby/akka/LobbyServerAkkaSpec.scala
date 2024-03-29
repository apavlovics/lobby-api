package lv.continuum.lobby.akka

import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class LobbyServerAkkaSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {

  private val route = LobbyServerAkka().route

  "LobbyServerAkka" should {
    "handle WebSocket connections" in {
      val wsProbe = WSProbe()
      WS("/lobby_api", wsProbe.flow) ~> route ~> check {
        isWebSocketUpgrade shouldBe true
      }
    }
  }
}
