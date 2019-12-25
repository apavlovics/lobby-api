package lv.continuum.evolution.akka

import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox}
import io.circe.ParsingFailure
import lv.continuum.evolution.akka.SessionActor.SessionCommand
import lv.continuum.evolution.akka.TableActor.TableCommand
import lv.continuum.evolution.protocol.Protocol.In._
import lv.continuum.evolution.protocol.Protocol.Out._
import lv.continuum.evolution.protocol.Protocol.UserType._
import lv.continuum.evolution.protocol.Protocol._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SessionActorSpec
  extends AnyWordSpec
    with Matchers {

  trait NotAuthenticated {
    val tableActorInbox: TestInbox[TableCommand] = TestInbox()
    val pushActorInbox: TestInbox[PushOut] = TestInbox()
    val replyToInbox: TestInbox[Option[Out]] = TestInbox()
    val testKit: BehaviorTestKit[SessionCommand] =
      BehaviorTestKit(SessionActor(tableActorInbox.ref, pushActorInbox.ref))

    protected def authenticate(username: Username, password: Password): Unit = {
      testKit.run(SessionCommand(
        in = Right(LoginIn(
          username = username,
          password = password,
        )),
        replyTo = replyToInbox.ref,
      ))
    }
  }

  trait AuthenticatedAsUser extends NotAuthenticated {
    authenticate(Username("user"), Password("user"))
  }

  "SessionActor" when {
    "not authenticated" should {
      "authenticate a user upon valid credentials" in new NotAuthenticated {
        authenticate(Username("user"), Password("user"))
        replyToInbox.expectMessage(Some(LoginSuccessfulOut(userType = User)))
      }
      "authenticate an admin upon valid credentials" in new NotAuthenticated {
        authenticate(Username("admin"), Password("admin"))
        replyToInbox.expectMessage(Some(LoginSuccessfulOut(userType = Admin)))
      }
      "decline authentication upon invalid credentials" in new NotAuthenticated {
        authenticate(Username("invalid"), Password("invalid"))
        replyToInbox.expectMessage(Some(ErrorOut(OutType.LoginFailed)))
      }
      "decline forwarding messages to TableActor when not authenticated" in new NotAuthenticated {
        testKit.run(SessionCommand(
          in = Right(SubscribeTablesIn),
          replyTo = replyToInbox.ref,
        ))
        replyToInbox.expectMessage(Some(ErrorOut(OutType.NotAuthenticated)))
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
      // TODO Complete implementation
    }
    "authenticated as admin" should {
      // TODO Complete implementation
    }
  }
}
