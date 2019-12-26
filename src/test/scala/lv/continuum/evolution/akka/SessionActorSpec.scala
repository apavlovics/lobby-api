package lv.continuum.evolution.akka

import akka.actor.testkit.typed.Effect.Watched
import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox}
import akka.actor.typed.Terminated
import io.circe.ParsingFailure
import lv.continuum.evolution.akka.SessionActor.SessionCommand
import lv.continuum.evolution.akka.TableActor.TableCommand
import lv.continuum.evolution.protocol.Protocol.In._
import lv.continuum.evolution.protocol.Protocol._
import lv.continuum.evolution.protocol.TestData
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SessionActorSpec
  extends AnyWordSpec
    with Matchers
    with TestData {

  trait NotAuthenticated {
    val tableActorInbox: TestInbox[TableCommand] = TestInbox()
    val pushActorInbox: TestInbox[PushOut] = TestInbox()
    val replyToInbox: TestInbox[Option[Out]] = TestInbox()
    val testKit: BehaviorTestKit[SessionCommand] =
      BehaviorTestKit(SessionActor(tableActorInbox.ref, pushActorInbox.ref))

    // Verify that PushActor is being watched
    testKit.expectEffect(Watched(pushActorInbox.ref))

    protected def verifyLogin(username: Username, password: Password, out: Out): Unit =
      verifyReplyTo(LoginIn(username, password), Some(out))

    protected def verifyReportParsingErrors(): Unit = {
      testKit.run(SessionCommand(
        in = Left(ParsingFailure("Parsing failed", new Exception("BANG!"))),
        replyTo = replyToInbox.ref,
      ))
      replyToInbox.expectMessage(Some(errorOutInvalidMessage._2))
    }

    protected def verifyReplyTo(in: In, out: Option[Out]): Unit = {
      testKit.run(SessionCommand(
        in = Right(in),
        replyTo = replyToInbox.ref,
      ))
      replyToInbox.expectMessage(out)
    }

    protected def verifyStop(): Unit = {
      testKit.signal(Terminated(pushActorInbox.ref))
      testKit.isAlive shouldBe false
    }
  }

  trait AuthenticatedAsUser extends NotAuthenticated {
    verifyLogin(Username("user"), Password("user"), loginSuccessfulOutUser._2)
  }

  trait AuthenticatedAsAdmin extends NotAuthenticated {
    verifyLogin(Username("admin"), Password("admin"), loginSuccessfulOutAdmin._2)
  }

  "SessionActor" when {
    "not authenticated" should {
      "decline authentication upon invalid credentials" in new NotAuthenticated {
        verifyLogin(Username("invalid"), Password("invalid"), errorOutLoginFailed._2)
      }
      "not respond to pings" in new NotAuthenticated {
        verifyReplyTo(pingIn._2, Some(errorOutNotAuthenticated._2))
      }
      "decline forwarding TableIn messages to TableActor" in new NotAuthenticated {
        verifyReplyTo(subscribeTablesIn._2, Some(errorOutNotAuthenticated._2))
        tableActorInbox.hasMessages shouldBe false
      }
      "decline forwarding AdminTableIn messages to TableActor" in new NotAuthenticated {
        verifyReplyTo(addTableIn._2, Some(errorOutNotAuthenticated._2))
        tableActorInbox.hasMessages shouldBe false
      }
      "report parsing errors" in new NotAuthenticated {
        verifyReportParsingErrors()
      }
      "stop when PushActor terminates" in new NotAuthenticated {
        verifyStop()
      }
    }
    "authenticated as User" should {
      "respond to pings" in new AuthenticatedAsUser {
        verifyReplyTo(pingIn._2, Some(pongOut._2))
      }
      "forward TableIn messages to TableActor" in new AuthenticatedAsUser {
        verifyReplyTo(subscribeTablesIn._2, None)
        tableActorInbox.expectMessage(TableCommand(subscribeTablesIn._2, pushActorInbox.ref))
      }
      "decline forwarding AdminTableIn messages to TableActor" in new AuthenticatedAsUser {
        verifyReplyTo(addTableIn._2, Some(errorOutNotAuthorized._2))
        tableActorInbox.hasMessages shouldBe false
      }
      "report parsing errors" in new AuthenticatedAsUser {
        verifyReportParsingErrors()
      }
      "stop when PushActor terminates" in new AuthenticatedAsUser {
        verifyStop()
      }
    }
    "authenticated as Admin" should {
      "respond to pings" in new AuthenticatedAsAdmin {
        verifyReplyTo(pingIn._2, Some(pongOut._2))
      }
      "forward TableIn messages to TableActor" in new AuthenticatedAsAdmin {
        verifyReplyTo(subscribeTablesIn._2, None)
        tableActorInbox.expectMessage(TableCommand(subscribeTablesIn._2, pushActorInbox.ref))
      }
      "forward AdminTableIn messages to TableActor" in new AuthenticatedAsAdmin {
        verifyReplyTo(addTableIn._2, None)
        tableActorInbox.expectMessage(TableCommand(addTableIn._2, pushActorInbox.ref))
      }
      "report parsing errors" in new AuthenticatedAsAdmin {
        verifyReportParsingErrors()
      }
      "stop when PushActor terminates" in new AuthenticatedAsAdmin {
        verifyStop()
      }
    }
  }
}
