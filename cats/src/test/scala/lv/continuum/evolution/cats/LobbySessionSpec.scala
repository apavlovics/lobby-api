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
import org.scalatest.Assertion
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

  // TODO Verify changes to refs and queues
  private def lobbySessionIO(authenticator: Authenticator[IO]): IO[LobbySession[IO]] = for {
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

  private def authenticatedLobbySessionIO(
    userType: UserType,
    expectedOut: Out,
  ): IO[LobbySession[IO]] = for {
    lobbySession <- lobbySessionIO(stubAuthenticator(userType.some))
    out <- lobbySession.process(login._2.asRight)
    _ <- IO {
      out should contain(expectedOut)
    }
  } yield lobbySession

  private def verifyRespondToPings(lobbySessionIO: IO[LobbySession[IO]]): IO[Assertion] =
    for {
      lobbySession <- lobbySessionIO
      out <- lobbySession.process(ping._2.asRight)
    } yield out should contain(pong._2)

  private def verifySubscribeUnsubscribe(lobbySessionIO: IO[LobbySession[IO]]): IO[Assertion] =
    for {
      lobbySession <- lobbySessionIO
      subscribeTablesOut <- lobbySession.process(subscribeTables._2.asRight)
      _ <- IO {
        subscribeTablesOut should contain(tableList._2)
      }
      unsubscribeTablesOut <- lobbySession.process(unsubscribeTables._2.asRight)
    } yield unsubscribeTablesOut shouldBe None

  private def verifyReportInvalidMessages(lobbySessionIO: IO[LobbySession[IO]]): IO[Assertion] =
    for {
      lobbySession <- lobbySessionIO
      out <- lobbySession.process(error.asLeft)
    } yield out should contain(invalidMessage._2)

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
        verifyReportInvalidMessages(notAuthenticatedLobbySessionIO)
      }
    }

    "authenticated as User" should {

      val userLobbySessionIO = authenticatedLobbySessionIO(User, loginSuccessfulUser._2)

      "respond to pings" in run {
        verifyRespondToPings(userLobbySessionIO)
      }
      "subscribe and unsubscribe via TableIn messages" in run {
        verifySubscribeUnsubscribe(userLobbySessionIO)
      }
      "decline processing AdminTableIn messages" in run {
        for {
          lobbySession <- userLobbySessionIO
          out <- lobbySession.process(addTable._2.asRight)
        } yield out should contain(notAuthorized._2)
      }
      "report invalid messages" in run {
        verifyReportInvalidMessages(userLobbySessionIO)
      }
    }

    "authenticated as Admin" should {

      val adminLobbySessionIO = authenticatedLobbySessionIO(Admin, loginSuccessfulAdmin._2)

      "respond to pings" in run {
        verifyRespondToPings(adminLobbySessionIO)
      }
      "subscribe and unsubscribe via TableIn messages" in run {
        verifySubscribeUnsubscribe(adminLobbySessionIO)
      }
      "report invalid messages" in run {
        verifyReportInvalidMessages(adminLobbySessionIO)
      }
      // TODO Complete implementation
    }
  }
}
