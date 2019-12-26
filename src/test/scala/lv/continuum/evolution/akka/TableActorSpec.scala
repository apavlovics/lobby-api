package lv.continuum.evolution.akka

import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox}
import lv.continuum.evolution.akka.TableActor.TableCommand
import lv.continuum.evolution.protocol.Protocol.Out._
import lv.continuum.evolution.protocol.Protocol._
import lv.continuum.evolution.protocol.TestData
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TableActorSpec
  extends AnyWordSpec
    with Matchers
    with TestData {

  trait Fixture {
    val testKit: BehaviorTestKit[TableCommand] = BehaviorTestKit(TableActor())
    val pushActorInboxSubscriber: TestInbox[PushOut] = TestInbox()
    val pushActorInboxAdmin: TestInbox[PushOut] = TestInbox()

    subscribe(pushActorInboxSubscriber)

    protected def subscribe(pushActorInbox: TestInbox[PushOut]): Unit = {
      testKit.run(TableCommand(subscribeTablesIn._2, pushActorInbox.ref))
      pushActorInbox.receiveMessage() shouldBe a[TableListOut]
    }

    protected def unsubscribe(pushActorInbox: TestInbox[PushOut]): Unit = {
      testKit.run(TableCommand(unsubscribeTablesIn._2, pushActorInbox.ref))
      pushActorInbox.hasMessages shouldBe false
    }
  }

  "TableActor" should {
    "handle AddTableIn, UpdateTableIn and RemoveTableIn commands and notify subscribers" in new Fixture {
      testKit.run(TableCommand(addTableIn._2, pushActorInboxAdmin.ref))
      pushActorInboxSubscriber.expectMessage(tableAddedOut._2)

      testKit.run(TableCommand(updateTableIn._2, pushActorInboxAdmin.ref))
      pushActorInboxSubscriber.expectMessage(tableUpdatedOut._2)

      testKit.run(TableCommand(removeTableIn._2, pushActorInboxAdmin.ref))
      pushActorInboxSubscriber.expectMessage(tableRemovedOut._2)

      unsubscribe(pushActorInboxSubscriber)

      testKit.run(TableCommand(addTableIn._2, pushActorInboxAdmin.ref))
      pushActorInboxSubscriber.hasMessages shouldBe false
      pushActorInboxAdmin.hasMessages shouldBe false
    }
    "handle failure upon AddTableIn command" in new Fixture {
      testKit.run(TableCommand(addTableInInvalid, pushActorInboxAdmin.ref))
      pushActorInboxSubscriber.hasMessages shouldBe false
      pushActorInboxAdmin.expectMessage(errorOutTableAddFailed._2)
    }
    "handle failure upon UpdateTableIn command" in new Fixture {
      testKit.run(TableCommand(updateTableInInvalid, pushActorInboxAdmin.ref))
      pushActorInboxSubscriber.hasMessages shouldBe false
      pushActorInboxAdmin.expectMessage(tableErrorOutTableUpdateFailed._2)
    }
    "handle failure upon RemoveTableIn command" in new Fixture {
      testKit.run(TableCommand(removeTableInInvalid, pushActorInboxAdmin.ref))
      pushActorInboxSubscriber.hasMessages shouldBe false
      pushActorInboxAdmin.expectMessage(tableErrorOutTableRemoveFailed._2)
    }
  }
}
