package lv.continuum.evolution.cats

import cats.effect.concurrent.Ref
import cats.effect.IO
import cats.implicits._
import fs2.concurrent.Queue
import lv.continuum.evolution.auth.{Authenticator => CommonAuthenticator}
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

  private val lobbySessionIO: IO[LobbySession[IO]] = for {
    lobbyRef <- Ref.of[IO, Lobby](Lobby())
    subscribersRef <- Ref.of[IO, Subscribers[IO]](Set.empty)
    sessionParamsRef <- Ref.of[IO, SessionParams](SessionParams())
    subscriber <- Queue.unbounded[IO, PushOut]
    lobbySession = LobbySession[IO](
      // TODO Replace with mock implementation
      authenticator = Authenticator[IO](new CommonAuthenticator),
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
          out <- lobbySession.process(Login(Username("test"), Password("test")).asRight)
        } yield out should contain(loginFailed._2)
      }
    }
  }
}
