package lv.continuum.lobby.model

import lv.continuum.lobby.protocol.Protocol._
import lv.continuum.lobby.protocol.TestData
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class LobbySpec extends AnyWordSpec with Matchers with TestData {

  private val lobby = Lobby(tables = Vector(tableJamesBond, tableMissionImpossible))
  private val lobbyEmpty = Lobby(tables = Vector.empty)

  "Lobby" should {
    "have correct nextTableId upon initialization" in {
      lobby.nextTableId shouldBe tableMissionImpossible.id.inc
      lobbyEmpty.nextTableId shouldBe TableId.Initial
    }

    "add a table in front" in {
      val result = lobby.addTable(TableId.Absent, tableToAddFooFighters)
      result should contain(
        (
          Lobby(
            tables = Vector(
              tableFooFighters,
              tableJamesBond,
              tableMissionImpossible,
            ),
            nextTableId = lobby.nextTableId.inc,
          ),
          tableFooFighters,
        )
      )
    }
    "add a table after another table" in {
      val result = lobby.addTable(tableJamesBond.id, tableToAddFooFighters)
      result should contain(
        (
          Lobby(
            tables = Vector(
              tableJamesBond,
              tableFooFighters,
              tableMissionImpossible,
            ),
            nextTableId = lobby.nextTableId.inc,
          ),
          tableFooFighters,
        )
      )
    }
    "not add a table if afterId does not exist" in {
      lobby.addTable(tableIdInvalid, tableToAddFooFighters) shouldBe None
    }

    "update a table" in {
      val tableJamesBondUpdated = tableJamesBond.copy(name = TableName("table - 007"))
      val result = lobby.updateTable(tableJamesBondUpdated)
      result should contain(
        Lobby(
          tables = Vector(
            tableJamesBondUpdated,
            tableMissionImpossible,
          ),
          nextTableId = lobby.nextTableId,
        )
      )
    }
    "not update a table if table does not exist" in {
      lobby.updateTable(tableFooFighters) shouldBe None
    }

    "remove a table" in {
      val result = lobby.removeTable(tableMissionImpossible.id)
      result should contain(
        Lobby(
          tables = Vector(
            tableJamesBond,
          ),
          nextTableId = lobby.nextTableId,
        )
      )
    }
    "not remove a table if tableId does not exist" in {
      lobby.removeTable(tableIdInvalid) shouldBe None
    }
  }
}
