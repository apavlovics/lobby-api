package lv.continuum.lobby.cats

import cats.effect._
import cats.syntax.all._
import cats.{Applicative, Parallel}
import com.typesafe.config.ConfigFactory
import io.odin.formatter.Formatter
import io.odin.{Logger, consoleLogger}
import lv.continuum.lobby.auth.{Authenticator => CommonAuthenticator}
import lv.continuum.lobby.config.LobbyServerConfig
import lv.continuum.lobby.model.Lobby
import org.http4s.blaze.server._

object LobbyServerCats extends IOApp {

  private def runF[F[_]: Async: Logger: Parallel]: F[ExitCode] =
    for {
      config            <- Applicative[F].pure(ConfigFactory.load())
      lobbyServerConfig <- LobbyServerConfig.load[F](config)

      authenticator = Authenticator[F](new CommonAuthenticator.InMemory)
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
    implicit val logger: Logger[IO] = consoleLogger(formatter = Formatter.colorful)
    runF[IO]
  }
}
