package lv.continuum.evolution.cats

import cats.effect.IO
import cats.effect.concurrent.Ref
import cats.implicits._
import fs2.concurrent.Queue
import lv.continuum.evolution.auth.{Authenticator => CommonAuthenticator}
import lv.continuum.evolution.model.Lobby
import lv.continuum.evolution.protocol.Protocol.UserType._
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

  private implicit val limit: Duration = 5.seconds

  private def stubAuthenticator(userType: Option[UserType]): Authenticator[IO] = {
    val stub = new CommonAuthenticator {
      override def authenticate(username: Username, password: Password): Option[UserType] = userType
    }
    Authenticator[IO](stub)
  }

  private def lobbySessionIO(authenticator: Authenticator[IO]): IO[LobbySession[IO]] =
    for {
      lobbyRef <- Ref.of[IO, Lobby](Lobby())
      subscribersRef <- Ref.of[IO, Subscribers[IO]](Set.empty)
      sessionParamsRef <- Ref.of[IO, SessionParams](SessionParams())
      subscriber <- Queue.unbounded[IO, PushOut]
      lobbySession = LobbySession[IO](
        authenticator = authenticator,
        lobbyRef = lobbyRef,
        subscribersRef = subscribersRef,
        sessionParamsRef = sessionParamsRef,
        subscriber = subscriber,
      )
    } yield lobbySession

  "LobbySession" when {

    "not authenticated" should {

      val notAuthenticatedLobbySessionIO = lobbySessionIO(stubAuthenticator(None))

      "decline authentication upon invalid credentials" in run {
        for {
          lobbySession <- notAuthenticatedLobbySessionIO
          out <- lobbySession.process(login._2.asRight)
        } yield out should contain(loginFailed._2)
      }
      "decline responding to pings" in run {
        for {
          lobbySession <- notAuthenticatedLobbySessionIO
          out <- lobbySession.process(ping._2.asRight)
        } yield out should contain(notAuthenticated._2)
      }
      "decline processing TableIn messages" in run {
        for {
          lobbySession <- notAuthenticatedLobbySessionIO
          out <- lobbySession.process(subscribeTables._2.asRight)
        } yield out should contain(notAuthenticated._2)
      }
      "decline processing AdminTableIn messages" in run {
        for {
          lobbySession <- notAuthenticatedLobbySessionIO
          out <- lobbySession.process(addTable._2.asRight)
        } yield out should contain(notAuthenticated._2)
      }
      "report invalid messages" in run {
        for {
          lobbySession <- notAuthenticatedLobbySessionIO
          out <- lobbySession.process(error.asLeft)
        } yield out should contain(invalidMessage._2)
      }
    }

    "authenticated as User" should {

      val userLobbySessionIO = for {
        lobbySession <- lobbySessionIO(stubAuthenticator(User.some))
        out <- lobbySession.process(login._2.asRight)
        _ <- IO {
          out should contain(loginSuccessfulUser._2)
        }
      } yield lobbySession

      "respond to pings" in run {
        for {
          lobbySession <- userLobbySessionIO
          out <- lobbySession.process(ping._2.asRight)
        } yield out should contain(pong._2)
      }
      // TODO Complete implementation
      "report invalid messages" in run {
        for {
          lobbySession <- userLobbySessionIO
          out <- lobbySession.process(error.asLeft)
        } yield out should contain(invalidMessage._2)
      }
    }
  }
}
