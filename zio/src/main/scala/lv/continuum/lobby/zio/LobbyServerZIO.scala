package lv.continuum.lobby.zio

import com.typesafe.config.ConfigFactory
import lv.continuum.lobby.config.LobbyServerConfig
import lv.continuum.lobby.zio.layer.{AuthenticatorLive, LobbyHolderLive, SubscribersHolderLive}
import zio.*
import zio.http.*
import zio.logging.backend.SLF4J

object LobbyServerZIO extends ZIOAppDefault {

  override val bootstrap: ZLayer[Any, Any, Unit] =
    Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  override def run: Task[Unit] = for {
    config            <- ZIO.attemptBlocking(ConfigFactory.load())
    lobbyServerConfig <- ZIO.attemptBlocking(LobbyServerConfig.loadOrThrow(config))

    _ <- ZIO.logInfo(s"Starting server at ${lobbyServerConfig.host}:${lobbyServerConfig.port}")
    lobbyServerLayer = Server.defaultWith(
      _.binding(lobbyServerConfig.host.toString, lobbyServerConfig.port.value)
    )
    _ <- Server
      .serve(LobbyHttpApp.app)
      .provide(
        lobbyServerLayer,
        AuthenticatorLive.layer,
        LobbyHolderLive.layer,
        SubscribersHolderLive.layer,
      )
  } yield ()
}
