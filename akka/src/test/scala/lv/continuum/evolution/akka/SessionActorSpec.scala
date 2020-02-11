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
      verifyReplyTo(Login(username, password), Some(out))

    protected def verifyReportParsingErrors(): Unit = {
      testKit.run(SessionCommand(
        in = Left(ParsingFailure("Parsing failed", new Exception("BANG!"))),
        replyTo = replyToInbox.ref,
      ))
      replyToInbox.expectMessage(Some(invalidMessage._2))
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
    verifyLogin(Username("user"), Password("user"), loginSuccessfulUser._2)
  }

  trait AuthenticatedAsAdmin extends NotAuthenticated {
    verifyLogin(Username("admin"), Password("admin"), loginSuccessfulAdmin._2)
  }

  "SessionActor" when {

    "not authenticated" should {
      "decline authentication upon invalid credentials" in new NotAuthenticated {
        verifyLogin(Username("invalid"), Password("invalid"), loginFailed._2)
      }
      "not respond to pings" in new NotAuthenticated {
        verifyReplyTo(ping._2, Some(notAuthenticated._2))
      }
      "decline forwarding TableIn messages to TableActor" in new NotAuthenticated {
        verifyReplyTo(subscribeTables._2, Some(notAuthenticated._2))
        tableActorInbox.hasMessages shouldBe false
      }
      "decline forwarding AdminTableIn messages to TableActor" in new NotAuthenticated {
        verifyReplyTo(addTable._2, Some(notAuthenticated._2))
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
        verifyReplyTo(ping._2, Some(pong._2))
      }
      "forward TableIn messages to TableActor" in new AuthenticatedAsUser {
        verifyReplyTo(subscribeTables._2, None)
        tableActorInbox.expectMessage(TableCommand(subscribeTables._2, pushActorInbox.ref))
      }
      "decline forwarding AdminTableIn messages to TableActor" in new AuthenticatedAsUser {
        verifyReplyTo(addTable._2, Some(notAuthorized._2))
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
        verifyReplyTo(ping._2, Some(pong._2))
      }
      "forward TableIn messages to TableActor" in new AuthenticatedAsAdmin {
        verifyReplyTo(subscribeTables._2, None)
        tableActorInbox.expectMessage(TableCommand(subscribeTables._2, pushActorInbox.ref))
      }
      "forward AdminTableIn messages to TableActor" in new AuthenticatedAsAdmin {
        verifyReplyTo(addTable._2, None)
        tableActorInbox.expectMessage(TableCommand(addTable._2, pushActorInbox.ref))
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
