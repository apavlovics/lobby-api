package lv.continuum.lobby.akka

import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox}
import lv.continuum.lobby.akka.TableActor.TableCommand
import lv.continuum.lobby.protocol.Protocol.Out._
import lv.continuum.lobby.protocol.Protocol._
import lv.continuum.lobby.protocol.TestData
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
      testKit.run(TableCommand(subscribeTables._2, pushActorInbox.ref))
      pushActorInbox.receiveMessage() shouldBe a[TableList]
    }

    protected def unsubscribe(pushActorInbox: TestInbox[PushOut]): Unit = {
      testKit.run(TableCommand(unsubscribeTables._2, pushActorInbox.ref))
      pushActorInbox.hasMessages shouldBe false
    }
  }

  "TableActor" should {
    "handle AddTable, UpdateTable and RemoveTable messages and notify subscribers" in new Fixture {
      testKit.run(TableCommand(addTable._2, pushActorInboxAdmin.ref))
      pushActorInboxSubscriber.expectMessage(tableAdded._2)

      testKit.run(TableCommand(updateTable._2, pushActorInboxAdmin.ref))
      pushActorInboxSubscriber.expectMessage(tableUpdated._2)

      testKit.run(TableCommand(removeTable._2, pushActorInboxAdmin.ref))
      pushActorInboxSubscriber.expectMessage(tableRemoved._2)

      unsubscribe(pushActorInboxSubscriber)

      testKit.run(TableCommand(addTable._2, pushActorInboxAdmin.ref))
      pushActorInboxSubscriber.hasMessages shouldBe false
      pushActorInboxAdmin.hasMessages shouldBe false
    }
    "handle failure upon AddTable message" in new Fixture {
      testKit.run(TableCommand(addTableInvalid, pushActorInboxAdmin.ref))
      pushActorInboxSubscriber.hasMessages shouldBe false
      pushActorInboxAdmin.expectMessage(tableAddFailed._2)
    }
    "handle failure upon UpdateTable message" in new Fixture {
      testKit.run(TableCommand(updateTableInvalid, pushActorInboxAdmin.ref))
      pushActorInboxSubscriber.hasMessages shouldBe false
      pushActorInboxAdmin.expectMessage(tableUpdateFailed._2)
    }
    "handle failure upon RemoveTable message" in new Fixture {
      testKit.run(TableCommand(removeTableInvalid, pushActorInboxAdmin.ref))
      pushActorInboxSubscriber.hasMessages shouldBe false
      pushActorInboxAdmin.expectMessage(tableRemoveFailed._2)
    }
  }
}
