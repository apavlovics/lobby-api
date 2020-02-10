package lv.continuum.evolution.config

import cats.effect.Sync
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

  def load[F[_] : Sync](config: Config): F[LobbyServerConfig] =
    ConfigSource.fromConfig(config).at(namespace = "lobby-server").loadF[F, LobbyServerConfig]
}
