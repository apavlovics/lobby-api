package lv.continuum.lobby.zio

import com.typesafe.config.ConfigFactory
import lv.continuum.lobby.config.LobbyServerConfig
import zio.*

object LobbyServerZIO extends ZIOAppDefault {

  override def run: ZIO[Any, Throwable, Unit] = for {
    config            <- ZIO.attemptBlocking(ConfigFactory.load())
    lobbyServerConfig <- ZIO.attemptBlocking(LobbyServerConfig.loadOrThrow(config))
    _ <- ZIO.logInfo(message = s"Server started at ${lobbyServerConfig.host}:${lobbyServerConfig.port}")
  } yield ()
}
