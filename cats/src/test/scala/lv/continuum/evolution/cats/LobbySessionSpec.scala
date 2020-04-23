package lv.continuum.evolution.cats

import cats.effect.concurrent.Ref
import cats.effect.{Concurrent, IO, Sync}
import cats.implicits._
import cats.{Monad, Parallel}
import fs2.concurrent.Queue
import io.circe.Error
import io.odin.Logger
import lv.continuum.evolution.auth.{Authenticator => CommonAuthenticator}
import lv.continuum.evolution.cats.LobbySessionSpec.Fixture
import lv.continuum.evolution.model.Lobby
import lv.continuum.evolution.protocol.Protocol.UserType._
import lv.continuum.evolution.protocol.Protocol._
import lv.continuum.evolution.protocol.TestData
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import scala.concurrent.duration._

class LobbySessionSpec
  extends AsyncWordSpec
    with IOSpec
    with Matchers
    with TestData {

  private implicit val limit: Duration = 5.seconds

  private def stubAuthenticator[F[_] : Sync](userType: Option[UserType]): Authenticator[F] = {
    val stub = new CommonAuthenticator {
      override def authenticate(username: Username, password: Password): Option[UserType] = userType
    }
    Authenticator[F](stub)
  }

  private def fixtureF[F[_] : Concurrent : Logger : Parallel](
    authenticator: Authenticator[F],
  ): F[Fixture[F]] = for {
    lobbyRef <- Ref.of[F, Lobby](Lobby())
    subscribersRef <- Ref.of[F, Subscribers[F]](Set.empty)
    sessionParamsRef <- Ref.of[F, SessionParams](SessionParams())
    subscriber <- Queue.unbounded[F, PushOut]
    lobbySession = LobbySession[F](
      authenticator = authenticator,
      lobbyRef = lobbyRef,
      subscribersRef = subscribersRef,
      sessionParamsRef = sessionParamsRef,
      subscriber = subscriber,
    )
  } yield Fixture[F](lobbySession, subscribersRef, subscriber)

  private def authenticatedFixtureF[F[_] : Concurrent : Logger : Parallel](
    userType: UserType,
    expectedOut: Out,
  ): F[Fixture[F]] = for {
    fixture <- fixtureF(stubAuthenticator(userType.some))
    out <- fixture.lobbySession.process(login._2.asRight)
    _ <- Sync[F].delay {
      out should contain(expectedOut)
    }
  } yield fixture

  private def verifyInOut[F[_] : Monad : Sync](
    fixtureF: F[Fixture[F]],
    in: Either[Error, In],
    expectedOut: Option[Out],
  ): F[Assertion] = for {
    fixture <- fixtureF
    out <- fixture.lobbySession.process(in)
    _ <- Sync[F].delay {
      out shouldBe expectedOut
    }
  } yield succeed

  private def verifyRespondToPings[F[_] : Monad : Sync](fixtureF: F[Fixture[F]]): F[Assertion] =
    verifyInOut(fixtureF, ping._2.asRight, pong._2.some)

  private def verifyReportInvalidMessages[F[_] : Monad : Sync](fixtureF: F[Fixture[F]]): F[Assertion] =
    verifyInOut(fixtureF, error.asLeft, invalidMessage._2.some)

  private def verifySubscribeUnsubscribe[F[_] : Monad : Sync](fixtureF: F[Fixture[F]]): F[Assertion] = for {
    fixture <- fixtureF
    subscribeTablesOut <- fixture.lobbySession.process(subscribeTables._2.asRight)
    _ <- Sync[F].delay {
      subscribeTablesOut should contain(tableList._2)
    }
    whenSubscribed <- fixture.subscribersRef.get
    _ <- Sync[F].delay {
      whenSubscribed should contain(fixture.subscriber)
    }
    unsubscribeTablesOut <- fixture.lobbySession.process(unsubscribeTables._2.asRight)
    _ <- Sync[F].delay {
      unsubscribeTablesOut shouldBe None
    }
    whenUnsubscribed <- fixture.subscribersRef.get
    _ <- Sync[F].delay {
      whenUnsubscribed should not contain fixture.subscriber
    }
  } yield succeed

  private def verifyAdministerTables[F[_] : Monad : Sync](fixtureF: F[Fixture[F]]): F[Assertion] = for {
    fixture <- fixtureF
    subscribeTablesOut <- fixture.lobbySession.process(subscribeTables._2.asRight)
    _ <- Sync[F].delay {
      subscribeTablesOut should contain(tableList._2)
    }
    addTableOut <- fixture.lobbySession.process(addTable._2.asRight)
    addTablePushOut <- fixture.subscriber.tryDequeue1
    _ <- Sync[F].delay {
      addTableOut shouldBe None
      addTablePushOut should contain(tableAdded._2)
    }
    updateTableOut <- fixture.lobbySession.process(updateTable._2.asRight)
    updateTablePushOut <- fixture.subscriber.tryDequeue1
    _ <- Sync[F].delay {
      updateTableOut shouldBe None
      updateTablePushOut should contain(tableUpdated._2)
    }
    removeTableOut <- fixture.lobbySession.process(removeTable._2.asRight)
    removeTablePushOut <- fixture.subscriber.tryDequeue1
    _ <- Sync[F].delay {
      removeTableOut shouldBe None
      removeTablePushOut should contain(tableRemoved._2)
    }
    unsubscribeTablesOut <- fixture.lobbySession.process(unsubscribeTables._2.asRight)
    _ <- Sync[F].delay {
      unsubscribeTablesOut shouldBe None
    }
  } yield succeed

  "LobbySession" when {

    "not authenticated" should {

      val notAuthenticatedFixtureIO = fixtureF[IO](stubAuthenticator(None))

      "decline authentication upon invalid credentials" in run[Assertion] {
        verifyInOut(
          fixtureF = notAuthenticatedFixtureIO,
          in = login._2.asRight,
          expectedOut = loginFailed._2.some,
        )
      }
      "decline responding to pings" in run[Assertion] {
        verifyInOut(
          fixtureF = notAuthenticatedFixtureIO,
          in = ping._2.asRight,
          expectedOut = notAuthenticated._2.some,
        )
      }
      "decline processing TableIn messages" in run[Assertion] {
        verifyInOut(
          fixtureF = notAuthenticatedFixtureIO,
          in = subscribeTables._2.asRight,
          expectedOut = notAuthenticated._2.some,
        )
      }
      "decline processing AdminTableIn messages" in run[Assertion] {
        verifyInOut(
          fixtureF = notAuthenticatedFixtureIO,
          in = addTable._2.asRight,
          expectedOut = notAuthenticated._2.some,
        )
      }
      "report invalid messages" in run[Assertion] {
        verifyReportInvalidMessages(notAuthenticatedFixtureIO)
      }
    }

    "authenticated as User" should {

      val userFixtureIO = authenticatedFixtureF[IO](User, loginSuccessfulUser._2)

      "respond to pings" in run[Assertion] {
        verifyRespondToPings(userFixtureIO)
      }
      "subscribe and unsubscribe via TableIn messages" in run[Assertion] {
        verifySubscribeUnsubscribe(userFixtureIO)
      }
      "decline processing AdminTableIn messages" in run[Assertion] {
        verifyInOut(
          fixtureF = userFixtureIO,
          in = addTable._2.asRight,
          expectedOut = notAuthorized._2.some,
        )
      }
      "report invalid messages" in run[Assertion] {
        verifyReportInvalidMessages(userFixtureIO)
      }
    }

    "authenticated as Admin" should {

      val adminFixtureIO = authenticatedFixtureF[IO](Admin, loginSuccessfulAdmin._2)

      "respond to pings" in run[Assertion] {
        verifyRespondToPings(adminFixtureIO)
      }
      "subscribe and unsubscribe via TableIn messages" in run[Assertion] {
        verifySubscribeUnsubscribe(adminFixtureIO)
      }
      "administer tables via AdminTableIn messages" in {
        val future = runAsFuture(verifyAdministerTables(adminFixtureIO))
        context.tick()
        future
      }
      "report invalid messages" in run[Assertion] {
        verifyReportInvalidMessages(adminFixtureIO)
      }
    }
  }
}

object LobbySessionSpec {

  private case class Fixture[F[_]](
    lobbySession: LobbySession[F],
    subscribersRef: Ref[F, Subscribers[F]],
    subscriber: Subscriber[F],
  )
}
