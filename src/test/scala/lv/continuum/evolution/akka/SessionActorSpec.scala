package lv.continuum.evolution.akka

import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox}
import io.circe.ParsingFailure
import lv.continuum.evolution.akka.SessionActor.SessionCommand
import lv.continuum.evolution.akka.TableActor.TableCommand
import lv.continuum.evolution.protocol.Protocol.In._
import lv.continuum.evolution.protocol.Protocol.Out._
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

    protected def verifyLogin(username: Username, password: Password, out: Out): Unit =
      verifyReplyTo(LoginIn(username, password), out)

    protected def verifyReportParsingErrors(): Unit = {
      testKit.run(SessionCommand(
        in = Left(ParsingFailure("Parsing failed", new Exception("BANG!"))),
        replyTo = replyToInbox.ref,
      ))
      replyToInbox.expectMessage(Some(ErrorOut(OutType.InvalidMessage)))
    }

    protected def verifyReplyTo(in: In, out: Out): Unit = {
      testKit.run(SessionCommand(
        in = Right(in),
        replyTo = replyToInbox.ref,
      ))
      replyToInbox.expectMessage(Some(out))
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
        verifyReplyTo(pingIn._2, errorOutNotAuthenticated._2)
      }
      "decline forwarding messages to TableActor when not authenticated" in new NotAuthenticated {
        verifyReplyTo(SubscribeTablesIn, errorOutNotAuthenticated._2)
        tableActorInbox.hasMessages shouldBe false
      }
      "report parsing errors" in new NotAuthenticated {
        verifyReportParsingErrors()
      }
    }
    "authenticated as User" should {
      "respond to pings" in new AuthenticatedAsUser {
        verifyReplyTo(pingIn._2, pongOut._2)
      }
      // TODO Complete implementation
      "report parsing errors" in new AuthenticatedAsUser {
        verifyReportParsingErrors()
      }
    }
    "authenticated as Admin" should {
      "respond to pings" in new AuthenticatedAsAdmin {
        verifyReplyTo(pingIn._2, pongOut._2)
      }
      // TODO Complete implementation
      "report parsing errors" in new AuthenticatedAsAdmin {
        verifyReportParsingErrors()
      }
    }
  }
}
