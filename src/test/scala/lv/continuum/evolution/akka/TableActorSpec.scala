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
    val pushActorInbox1: TestInbox[PushOut] = TestInbox()
    val pushActorInbox2: TestInbox[PushOut] = TestInbox()

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
    "handle SubscribeTablesIn and UnsubscribeTablesIn commands" in new Fixture {
      subscribe(pushActorInbox1)
      unsubscribe(pushActorInbox1)
    }
    "handle AddTableIn, UpdateTableIn and RemoveTableIn commands and notify subscribers" in new Fixture {
      subscribe(pushActorInbox1)

      testKit.run(TableCommand(addTableIn._2, pushActorInbox2.ref))
      pushActorInbox1.expectMessage(tableAddedOut._2)

      testKit.run(TableCommand(updateTableIn._2, pushActorInbox2.ref))
      pushActorInbox1.expectMessage(tableUpdatedOut._2)

      testKit.run(TableCommand(removeTableIn._2, pushActorInbox2.ref))
      pushActorInbox1.expectMessage(tableRemovedOut._2)

      unsubscribe(pushActorInbox1)

      testKit.run(TableCommand(addTableIn._2, pushActorInbox2.ref))
      pushActorInbox1.hasMessages shouldBe false
      pushActorInbox2.hasMessages shouldBe false
    }
    "handle failures upon AddTableIn commands" in new Fixture {
      subscribe(pushActorInbox1)

      testKit.run(TableCommand(addTableInInvalid, pushActorInbox2.ref))
      pushActorInbox2.expectMessage(errorOutTableAddFailed._2)

      pushActorInbox1.hasMessages shouldBe false
    }
    "handle failures upon UpdateTableIn commands" in new Fixture {
      subscribe(pushActorInbox1)

      testKit.run(TableCommand(updateTableInInvalid, pushActorInbox2.ref))
      pushActorInbox2.expectMessage(tableErrorOutTableUpdateFailed._2)

      pushActorInbox1.hasMessages shouldBe false
    }
    "handle failures upon RemoveTableIn commands" in new Fixture {
      subscribe(pushActorInbox1)

      testKit.run(TableCommand(removeTableInInvalid, pushActorInbox2.ref))
      pushActorInbox2.expectMessage(tableErrorOutTableRemoveFailed._2)

      pushActorInbox1.hasMessages shouldBe false
    }
  }
}
