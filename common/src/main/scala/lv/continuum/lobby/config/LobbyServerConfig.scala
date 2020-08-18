package lv.continuum.lobby.config

import cats.effect.{Blocker, ContextShift, Sync}
import com.typesafe.config.Config
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import pureconfig.module.catseffect.syntax._

case class LobbyServerConfig(
  host: String,
  port: Int,
)
object LobbyServerConfig {

  def loadOrThrow(config: Config): LobbyServerConfig =
    ConfigSource.fromConfig(config).at(namespace = "lobby-server").loadOrThrow[LobbyServerConfig]

  def load[F[_]: ContextShift: Sync](config: Config, blocker: Blocker): F[LobbyServerConfig] =
    ConfigSource.fromConfig(config).at(namespace = "lobby-server").loadF[F, LobbyServerConfig](blocker)
}