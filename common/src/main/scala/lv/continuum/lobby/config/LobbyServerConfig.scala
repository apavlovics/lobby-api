package lv.continuum.lobby.config

import com.comcast.ip4s.{Host, Port}
import com.typesafe.config.Config
import pureconfig.error.CannotConvert
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.generic.derivation.default.*

case class LobbyServerConfig(
  host: Host,
  port: Port,
) derives ConfigReader

object LobbyServerConfig {

  private val namespace = "lobby-server"

  implicit private val hostReader: ConfigReader[Host] =
    ConfigReader.fromString[Host] { hostString =>
      Host
        .fromString(hostString)
        .toRight(
          CannotConvert(
            value = hostString,
            toType = "Host",
            because = "Invalid host string",
          )
        )
    }

  implicit private val portReader: ConfigReader[Port] =
    ConfigReader[Int].emap { portInt =>
      Port
        .fromInt(portInt)
        .toRight(
          CannotConvert(
            value = portInt.toString,
            toType = "Port",
            because = "Invalid port number",
          )
        )
    }

  def loadOrThrow(config: Config): LobbyServerConfig =
    ConfigSource.fromConfig(config).at(namespace = namespace).loadOrThrow[LobbyServerConfig]
}
