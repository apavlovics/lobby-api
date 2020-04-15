package lv.continuum.evolution.akka

import akka.actor.testkit.typed.Effect.Watched
import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox}
import akka.actor.typed.Terminated
import cats.syntax.either._
import cats.syntax.option._
import lv.continuum.evolution.akka.SessionActor.SessionCommand
import lv.continuum.evolution.akka.TableActor.TableCommand
import lv.continuum.evolution.auth.Authenticator
import lv.continuum.evolution.protocol.Protocol.UserType._
import lv.continuum.evolution.protocol.Protocol._
import lv.continuum.evolution.protocol.TestData
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SessionActorSpec
  extends AnyWordSpec
    with Matchers
    with MockFactory
    with TestData {

  trait Fixture {
    val authenticator: Authenticator = mock[Authenticator]
    val tableActorInbox: TestInbox[TableCommand] = TestInbox()
    val pushActorInbox: TestInbox[PushOut] = TestInbox()
    val replyToInbox: TestInbox[Option[Out]] = TestInbox()
    val testKit: BehaviorTestKit[SessionCommand] =
      BehaviorTestKit(SessionActor(authenticator, tableActorInbox.ref, pushActorInbox.ref))

    // Verify that PushActor is being watched
    testKit.expectEffect(Watched(pushActorInbox.ref))

    protected def verifyLogin(out: Out): Unit =
      verifyReplyTo(login._2, out.some)

    protected def verifyReportInvalidMessages(): Unit = {
      testKit.run(SessionCommand(
        in = error.asLeft,
        replyTo = replyToInbox.ref,
      ))
      replyToInbox.expectMessage(invalidMessage._2.some)
    }

    protected def verifyReplyTo(in: In, out: Option[Out]): Unit = {
      testKit.run(SessionCommand(
        in = in.asRight,
        replyTo = replyToInbox.ref,
      ))
      replyToInbox.expectMessage(out)
    }

    protected def verifyStop(): Unit = {
      testKit.signal(Terminated(pushActorInbox.ref))
      testKit.isAlive shouldBe false
    }
  }

  trait NotAuthenticated extends Fixture {
    (authenticator.authenticate _).expects(*, *).returning(None).noMoreThanOnce()
  }

  trait AuthenticatedAsUser extends Fixture {
    (authenticator.authenticate _).expects(*, *).returning(User.some).once()
    verifyLogin(loginSuccessfulUser._2)
  }

  trait AuthenticatedAsAdmin extends Fixture {
    (authenticator.authenticate _).expects(*, *).returning(Admin.some).once()
    verifyLogin(loginSuccessfulAdmin._2)
  }

  "SessionActor" when {

    "not authenticated" should {
      "decline authentication upon invalid credentials" in new NotAuthenticated {
        verifyLogin(loginFailed._2)
      }
      "decline responding to pings" in new NotAuthenticated {
        verifyReplyTo(ping._2, notAuthenticated._2.some)
      }
      "decline forwarding TableIn messages to TableActor" in new NotAuthenticated {
        verifyReplyTo(subscribeTables._2, notAuthenticated._2.some)
        tableActorInbox.hasMessages shouldBe false
      }
      "decline forwarding AdminTableIn messages to TableActor" in new NotAuthenticated {
        verifyReplyTo(addTable._2, notAuthenticated._2.some)
        tableActorInbox.hasMessages shouldBe false
      }
      "report parsing errors" in new NotAuthenticated {
        verifyReportInvalidMessages()
      }
      "stop when PushActor terminates" in new NotAuthenticated {
        verifyStop()
      }
    }

    "authenticated as User" should {
      "respond to pings" in new AuthenticatedAsUser {
        verifyReplyTo(ping._2, pong._2.some)
      }
      "forward TableIn messages to TableActor" in new AuthenticatedAsUser {
        verifyReplyTo(subscribeTables._2, None)
        tableActorInbox.expectMessage(TableCommand(subscribeTables._2, pushActorInbox.ref))
      }
      "decline forwarding AdminTableIn messages to TableActor" in new AuthenticatedAsUser {
        verifyReplyTo(addTable._2, notAuthorized._2.some)
        tableActorInbox.hasMessages shouldBe false
      }
      "report invalid messages" in new AuthenticatedAsUser {
        verifyReportInvalidMessages()
      }
      "stop when PushActor terminates" in new AuthenticatedAsUser {
        verifyStop()
      }
    }

    "authenticated as Admin" should {
      "respond to pings" in new AuthenticatedAsAdmin {
        verifyReplyTo(ping._2, pong._2.some)
      }
      "forward TableIn messages to TableActor" in new AuthenticatedAsAdmin {
        verifyReplyTo(subscribeTables._2, None)
        tableActorInbox.expectMessage(TableCommand(subscribeTables._2, pushActorInbox.ref))
      }
      "forward AdminTableIn messages to TableActor" in new AuthenticatedAsAdmin {
        verifyReplyTo(addTable._2, None)
        tableActorInbox.expectMessage(TableCommand(addTable._2, pushActorInbox.ref))
      }
      "report invalid messages" in new AuthenticatedAsAdmin {
        verifyReportInvalidMessages()
      }
      "stop when PushActor terminates" in new AuthenticatedAsAdmin {
        verifyStop()
      }
    }
  }
}
