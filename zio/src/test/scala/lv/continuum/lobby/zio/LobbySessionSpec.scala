package lv.continuum.lobby.zio

import zio.Scope
import zio.test.*

object LobbySessionSpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment with Scope, Nothing] =
    suite("LobbySessionSpec")(
      suite("when not authenticated")(
        test("decline authentication upon invalid credentials")(
          assertTrue(true)
        )
      )
    )
}
