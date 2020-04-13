package lv.continuum.evolution.cats

import cats.effect.concurrent.Ref
import cats.effect.{Blocker, ExitCode, IO, IOApp}
import com.typesafe.config.ConfigFactory
import io.odin.formatter.Formatter
import io.odin.{Logger, consoleLogger}
import lv.continuum.evolution.config.LobbyServerConfig
import lv.continuum.evolution.model.Lobby
import org.http4s.server.blaze._

object LobbyServerCats extends IOApp {

  private implicit val logger: Logger[IO] = consoleLogger(formatter = Formatter.colorful)

  override def run(args: List[String]): IO[ExitCode] =
    Blocker[IO].use { blocker =>
      for {
        config <- IO(ConfigFactory.load())
        lobbyServerConfig <- LobbyServerConfig.load[IO](config, blocker)

        lobbyRef <- Ref.of[IO, Lobby](Lobby())
        subscribersRef <- Ref.of[IO, Subscribers[IO]](Set.empty)

        _ <- BlazeServerBuilder[IO]
          .bindHttp(lobbyServerConfig.port, lobbyServerConfig.host)
          .withHttpApp(LobbyHttpApp[IO](lobbyRef, subscribersRef).app)
          .serve
          .compile
          .drain
      } yield ExitCode.Success
    }
}
