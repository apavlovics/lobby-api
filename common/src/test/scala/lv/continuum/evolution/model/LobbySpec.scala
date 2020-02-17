package lv.continuum.evolution.model

import lv.continuum.evolution.protocol.Protocol._
import lv.continuum.evolution.protocol.TestData
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class LobbySpec
  extends AnyWordSpec
    with Matchers
    with TestData {

  "Lobby" should {
    "have correct nextTableId upon initialization" in {
      val lobby1 = Lobby(tables = Vector.empty)
      lobby1.nextTableId shouldBe TableId.Initial

      val lobby2 = Lobby(tables = Vector(tableJamesBond, tableFooFighters))
      lobby2.nextTableId shouldBe tableFooFighters.id.inc
    }
    "allow adding a table in front" in {
      val lobby = Lobby(tables = Vector(tableJamesBond, tableMissionImpossible))
      val result = lobby.addTable(TableId.Absent, tableToAddFooFighters)
      result should contain((
        Lobby(
          tables = Vector(
            tableFooFighters,
            tableJamesBond,
            tableMissionImpossible,
          ),
          nextTableId = lobby.nextTableId.inc,
        ), tableFooFighters
      ))
    }
    "allow adding a table after another table" in {
      val lobby = Lobby(tables = Vector(tableJamesBond, tableMissionImpossible))
      val result = lobby.addTable(tableJamesBond.id, tableToAddFooFighters)
      result should contain((
        Lobby(
          tables = Vector(
            tableJamesBond,
            tableFooFighters,
            tableMissionImpossible,
          ),
          nextTableId = lobby.nextTableId.inc,
        ), tableFooFighters
      ))
    }
    // TODO Complete implementation
  }
}
