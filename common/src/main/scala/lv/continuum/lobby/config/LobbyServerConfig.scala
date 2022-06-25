package lv.continuum.lobby.config

import com.typesafe.config.Config
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.generic.derivation.default.*

case class LobbyServerConfig(
  host: String,
  port: Int,
) derives ConfigReader

object LobbyServerConfig {

  private val namespace = "lobby-server"

  def loadOrThrow(config: Config): LobbyServerConfig =
    ConfigSource.fromConfig(config).at(namespace = namespace).loadOrThrow[LobbyServerConfig]
}
