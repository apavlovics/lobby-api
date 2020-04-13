package lv.continuum.evolution.cats

import cats.effect.concurrent.Ref
import cats.effect.IO
import cats.implicits._
import fs2.concurrent.Queue
import io.odin.formatter.Formatter
import io.odin.{Logger, consoleLogger}
import lv.continuum.evolution.model.Lobby
import lv.continuum.evolution.protocol.Protocol.In._
import lv.continuum.evolution.protocol.Protocol._
import lv.continuum.evolution.protocol.TestData
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration._

class LobbySessionSpec
  extends AnyWordSpec
    with IOSpec
    with Matchers
    with TestData {

  private implicit val limit: Duration = 30.seconds

  private implicit val logger: Logger[IO] = consoleLogger[IO](formatter = Formatter.colorful)
  private val lobbySessionIO: IO[LobbySession[IO]] = for {
    lobbyRef <- Ref.of[IO, Lobby](Lobby())
    subscribersRef <- Ref.of[IO, Subscribers[IO]](Set.empty)
    sessionParamsRef <- Ref.of[IO, SessionParams](SessionParams())
    subscriber <- Queue.unbounded[IO, PushOut]
    lobbySession = LobbySession[IO](
      lobbyRef = lobbyRef,
      subscribersRef = subscribersRef,
      sessionParamsRef = sessionParamsRef,
      subscriber = subscriber,
    )
  } yield lobbySession

  "LobbySession" when {

    "not authenticated" should {
      "decline authentication upon invalid credentials" in run {
        for {
          lobbySession <- lobbySessionIO
          // TODO Make common authentication component
          out <- lobbySession.process(Login(Username("invalid"), Password("invalid")).asRight)
        } yield out should contain(loginFailed._2)
      }
    }
  }
}
