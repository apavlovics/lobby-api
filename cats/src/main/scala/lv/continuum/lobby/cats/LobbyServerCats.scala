package lv.continuum.lobby.cats

import cats.effect.*
import cats.syntax.all.*
import cats.Parallel
import com.typesafe.config.ConfigFactory
import io.odin.formatter.Formatter
import io.odin.{Logger, consoleLogger}
import lv.continuum.lobby.auth.Authenticator as CommonAuthenticator
import lv.continuum.lobby.config.LobbyServerConfig
import lv.continuum.lobby.model.Lobby
import org.http4s.blaze.server.*

object LobbyServerCats extends IOApp {

  private def runF[F[_]: Async: Logger: Parallel]: F[ExitCode] =
    for {
      config            <- Sync[F].blocking(ConfigFactory.load())
      lobbyServerConfig <- Sync[F].blocking(LobbyServerConfig.loadOrThrow(config))

      authenticator = Authenticator[F](CommonAuthenticator.InMemory())
      lobbyRef       <- Ref.of[F, Lobby](Lobby())
      subscribersRef <- Ref.of[F, Subscribers[F]](Set.empty)

      _ <-
        BlazeServerBuilder[F]
          .bindHttp(lobbyServerConfig.port, lobbyServerConfig.host)
          .withHttpWebSocketApp(LobbyHttpApp[F](_, authenticator, lobbyRef, subscribersRef).app)
          .serve
          .compile
          .drain
    } yield ExitCode.Success

  override def run(args: List[String]): IO[ExitCode] = {
    given Logger[IO] = consoleLogger(formatter = Formatter.colorful)
    runF[IO]
  }
}
