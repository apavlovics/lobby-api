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

    protected def authenticate(username: Username, password: Password, out: Out): Unit =
      verifyReplyTo(LoginIn(username, password), out)

    protected def verifyReplyTo(in: In, out: Out): Unit = {
      testKit.run(SessionCommand(
        in = Right(in),
        replyTo = replyToInbox.ref,
      ))
      replyToInbox.expectMessage(Some(out))
    }
  }

  trait AuthenticatedAsUser extends NotAuthenticated {
    authenticate(Username("user"), Password("user"), loginSuccessfulOutUser._2)
  }

  "SessionActor" when {
    "not authenticated" should {
      "authenticate a user upon valid credentials" in new NotAuthenticated {
        authenticate(Username("user"), Password("user"), loginSuccessfulOutUser._2)
      }
      "authenticate an admin upon valid credentials" in new NotAuthenticated {
        authenticate(Username("admin"), Password("admin"), loginSuccessfulOutAdmin._2)
      }
      "decline authentication upon invalid credentials" in new NotAuthenticated {
        authenticate(Username("invalid"), Password("invalid"), ErrorOut(OutType.LoginFailed))
      }
      "not respond to pings" in new NotAuthenticated {
        verifyReplyTo(pingIn._2, ErrorOut(OutType.NotAuthenticated))
      }
      "decline forwarding messages to TableActor when not authenticated" in new NotAuthenticated {
        verifyReplyTo(SubscribeTablesIn, ErrorOut(OutType.NotAuthenticated))
        tableActorInbox.hasMessages shouldBe false
      }
      "report parsing errors" in new NotAuthenticated {
        testKit.run(SessionCommand(
          in = Left(ParsingFailure("Parsing failed", new Exception("BANG!"))),
          replyTo = replyToInbox.ref,
        ))
        replyToInbox.expectMessage(Some(ErrorOut(OutType.InvalidMessage)))
      }
    }
    "authenticated as user" should {
      "respond to pings" in new AuthenticatedAsUser {
        verifyReplyTo(pingIn._2, pongOut._2)
      }
      // TODO Complete implementation
    }
    "authenticated as admin" should {
      "respond to pings" in new AuthenticatedAsUser {
        verifyReplyTo(pingIn._2, pongOut._2)
      }
      // TODO Complete implementation
    }
  }
}
