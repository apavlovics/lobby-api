package lv.continuum.lobby.cats

import cats.effect.concurrent.Ref
import cats.effect.{Concurrent, IO, Sync}
import cats.implicits._
import cats.{Applicative, Parallel}
import fs2.concurrent.Queue
import io.circe.Error
import io.odin.Logger
import lv.continuum.lobby.auth.{Authenticator => CommonAuthenticator}
import lv.continuum.lobby.cats.LobbySessionSpec.Fixture
import lv.continuum.lobby.model.Lobby
import lv.continuum.lobby.protocol.Protocol.UserType._
import lv.continuum.lobby.protocol.Protocol._
import lv.continuum.lobby.protocol.TestData
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import scala.concurrent.duration._

class LobbySessionSpec
  extends AsyncWordSpec
    with IOSpec
    with Matchers
    with TestData {

  implicit private val limit: Duration = 5.seconds

  private def stubAuthenticator[F[_] : Sync](userType: Option[UserType]): Authenticator[F] = {
    val stub = new CommonAuthenticator {
      override def authenticate(username: Username, password: Password): Option[UserType] = userType
    }
    Authenticator[F](stub)
  }

  private def fixtureF[F[_] : Concurrent : Logger : Parallel](
    authenticator: Authenticator[F],
  ): F[Fixture[F]] =
    for {
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
  ): F[Fixture[F]] =
    for {
      fixture <- fixtureF(stubAuthenticator(userType.some))
      out <- fixture.lobbySession.process(login._2.asRight)
      _ <- Sync[F].delay(out should contain(expectedOut))
    } yield fixture

  private def verifyInOut[F[_] : Sync](
    fixture: Fixture[F],
    in: Either[Error, In],
    expectedOut: Option[Out],
  ): F[Assertion] =
    for {
      out <- fixture.lobbySession.process(in)
      _ <- Sync[F].delay(out shouldBe expectedOut)
    } yield succeed

  private def verifyInOut[F[_] : Sync](
    fixtureF: F[Fixture[F]],
    in: Either[Error, In],
    expectedOut: Option[Out],
  ): F[Assertion] =
    for {
      fixture <- fixtureF
      _ <- verifyInOut(fixture, in, expectedOut)
    } yield succeed

  private def verifyRespondToPings[F[_] : Sync](fixtureF: F[Fixture[F]]): F[Assertion] =
    verifyInOut(fixtureF, ping._2.asRight, pong._2.some)

  private def verifyReportInvalidMessages[F[_] : Sync](fixtureF: F[Fixture[F]]): F[Assertion] =
    verifyInOut(fixtureF, error.asLeft, invalidMessage._2.some)

  private def verifyPushOut[F[_] : Sync](
    fixture: Fixture[F],
    expectedPushOut: Option[PushOut],
  ): F[Assertion] =
    for {
      pushOut <- fixture.subscriber.tryDequeue1
      _ <- Sync[F].delay(pushOut shouldBe expectedPushOut)
    } yield succeed

  private def verifySubscribe[F[_] : Sync](fixture: Fixture[F]): F[Assertion] =
    for {
      _ <- verifyInOut(fixture, subscribeTables._2.asRight, tableList._2.some)
      whenSubscribed <- fixture.subscribersRef.get
      _ <- Sync[F].delay(whenSubscribed should contain(fixture.subscriber))
    } yield succeed

  private def verifyUnsubscribe[F[_] : Sync](fixture: Fixture[F]): F[Assertion] =
    for {
      _ <- verifyInOut(fixture, unsubscribeTables._2.asRight, None)
      whenUnsubscribed <- fixture.subscribersRef.get
      _ <- Sync[F].delay(whenUnsubscribed should not contain fixture.subscriber)
    } yield succeed

  private def verifySubscribeUnsubscribe[F[_] : Sync](
    fixtureF: F[Fixture[F]],
  )(
    verify: Fixture[F] => F[Assertion],
  ): F[Assertion] =
    for {
      fixture <- fixtureF
      _ <- verifySubscribe(fixture)
      _ <- verify(fixture)
      _ <- verifyUnsubscribe(fixture)
    } yield succeed

  "LobbySession" when {

    "not authenticated" should {

      val notAuthenticatedFixtureIO = fixtureF[IO](stubAuthenticator(None))

      "decline authentication upon invalid credentials" in runTimed[Assertion] {
        verifyInOut(
          fixtureF = notAuthenticatedFixtureIO,
          in = login._2.asRight,
          expectedOut = loginFailed._2.some,
        )
      }
      "decline responding to pings" in runTimed[Assertion] {
        verifyInOut(
          fixtureF = notAuthenticatedFixtureIO,
          in = ping._2.asRight,
          expectedOut = notAuthenticated._2.some,
        )
      }
      "decline processing TableIn messages" in runTimed[Assertion] {
        verifyInOut(
          fixtureF = notAuthenticatedFixtureIO,
          in = subscribeTables._2.asRight,
          expectedOut = notAuthenticated._2.some,
        )
      }
      "decline processing AdminTableIn messages" in runTimed[Assertion] {
        verifyInOut(
          fixtureF = notAuthenticatedFixtureIO,
          in = addTable._2.asRight,
          expectedOut = notAuthenticated._2.some,
        )
      }
      "report invalid messages" in runTimed[Assertion] {
        verifyReportInvalidMessages(notAuthenticatedFixtureIO)
      }
    }

    "authenticated as User" should {

      val userFixtureIO = authenticatedFixtureF[IO](User, loginSuccessfulUser._2)

      "respond to pings" in runTimed[Assertion] {
        verifyRespondToPings(userFixtureIO)
      }
      "subscribe and unsubscribe via TableIn messages" in runTimed[Assertion] {
        verifySubscribeUnsubscribe(userFixtureIO)(_ => Applicative[IO].pure(succeed))
      }
      "decline processing AdminTableIn messages" in runTimed[Assertion] {
        verifyInOut(
          fixtureF = userFixtureIO,
          in = addTable._2.asRight,
          expectedOut = notAuthorized._2.some,
        )
      }
      "report invalid messages" in runTimed[Assertion] {
        verifyReportInvalidMessages(userFixtureIO)
      }
    }

    "authenticated as Admin" should {

      val adminFixtureIO = authenticatedFixtureF[IO](Admin, loginSuccessfulAdmin._2)

      "respond to pings" in runTimed[Assertion] {
        verifyRespondToPings(adminFixtureIO)
      }
      "subscribe and unsubscribe via TableIn messages" in runTimed[Assertion] {
        verifySubscribeUnsubscribe(adminFixtureIO)(_ => Applicative[IO].pure(succeed))
      }
      "handle AddTable, UpdateTable and RemoveTable messages and notify subscribers" in runAsFuture {
        verifySubscribeUnsubscribe(adminFixtureIO) { fixture =>
          for {
            _ <- verifyInOut(fixture, addTable._2.asRight, None)
            _ <- verifyPushOut(fixture, tableAdded._2.some)
            _ <- verifyInOut(fixture, updateTable._2.asRight, None)
            _ <- verifyPushOut(fixture, tableUpdated._2.some)
            _ <- verifyInOut(fixture, removeTable._2.asRight, None)
            _ <- verifyPushOut(fixture, tableRemoved._2.some)
          } yield succeed
        }
      }
      "handle failure upon AddTable message" in runAsFuture {
        verifySubscribeUnsubscribe(adminFixtureIO) { fixture =>
          for {
            _ <- verifyInOut(fixture, addTableInvalid.asRight, tableAddFailed._2.some)
            _ <- verifyPushOut(fixture, None)
          } yield succeed
        }
      }
      "handle failure upon UpdateTable message" in runAsFuture {
        verifySubscribeUnsubscribe(adminFixtureIO) { fixture =>
          for {
            _ <- verifyInOut(fixture, updateTableInvalid.asRight, tableUpdateFailed._2.some)
            _ <- verifyPushOut(fixture, None)
          } yield succeed
        }
      }
      "handle failure upon RemoveTable message" in runAsFuture {
        verifySubscribeUnsubscribe(adminFixtureIO) { fixture =>
          for {
            _ <- verifyInOut(fixture, removeTableInvalid.asRight, tableRemoveFailed._2.some)
            _ <- verifyPushOut(fixture, None)
          } yield succeed
        }
      }
      "report invalid messages" in runTimed[Assertion] {
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
