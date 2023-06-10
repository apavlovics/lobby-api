package lv.continuum.lobby.zio

import com.typesafe.config.ConfigFactory
import lv.continuum.lobby.config.LobbyServerConfig
import lv.continuum.lobby.zio.layer.AuthenticatorLive
import zio.*
import zio.http.*

object LobbyServerZIO extends ZIOAppDefault {

  override def run: Task[Unit] = for {
    config            <- ZIO.attemptBlocking(ConfigFactory.load())
    lobbyServerConfig <- ZIO.attemptBlocking(LobbyServerConfig.loadOrThrow(config))

    _ <- ZIO.logInfo(s"Starting server at ${lobbyServerConfig.host}:${lobbyServerConfig.port}")
    lobbyServerLayer = Server.defaultWith(_.binding(lobbyServerConfig.host, lobbyServerConfig.port))
    _ <- Server
      .serve(LobbyHttpApp.app)
      .provide(
        lobbyServerLayer,
        AuthenticatorLive.layer,
      )
  } yield ()
}
