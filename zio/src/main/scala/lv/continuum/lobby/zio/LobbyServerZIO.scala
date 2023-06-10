package lv.continuum.lobby.zio

import com.typesafe.config.ConfigFactory
import lv.continuum.lobby.config.LobbyServerConfig
import zio.*
import zio.http.*

object LobbyServerZIO extends ZIOAppDefault {

  private val app: App[Any] =
    Http.collect[Request] { case Method.GET -> Root / "text" =>
      Response.text("Hello World!")
    }

  override def run: ZIO[Any, Throwable, Unit] = for {
    config            <- ZIO.attemptBlocking(ConfigFactory.load())
    lobbyServerConfig <- ZIO.attemptBlocking(LobbyServerConfig.loadOrThrow(config))

    _ <- ZIO.logInfo(message = s"Starting server at ${lobbyServerConfig.host}:${lobbyServerConfig.port}...")
    lobbyServer = Server.defaultWith(_.binding(lobbyServerConfig.host, lobbyServerConfig.port))
    _ <- Server.serve(app).provide(lobbyServer)
  } yield ()
}
