package lv.continuum.lobby.cats

import cats.effect.concurrent.Ref
import cats.effect.{Blocker, ConcurrentEffect, ContextShift, ExitCode, IO, IOApp, Timer}
import cats.implicits._
import cats.{Applicative, Parallel}
import com.typesafe.config.ConfigFactory
import io.odin.formatter.Formatter
import io.odin.{Logger, consoleLogger}
import lv.continuum.lobby.auth.{Authenticator => CommonAuthenticator}
import lv.continuum.lobby.config.LobbyServerConfig
import lv.continuum.lobby.model.Lobby
import org.http4s.server.blaze._

import scala.concurrent.ExecutionContext.global

object LobbyServerCats extends IOApp {

  private def runF[F[_]: ConcurrentEffect: ContextShift: Logger: Parallel: Timer]: F[ExitCode] =
    Blocker[F].use { blocker =>
      for {
        config            <- Applicative[F].pure(ConfigFactory.load())
        lobbyServerConfig <- LobbyServerConfig.load[F](config, blocker)

        authenticator = Authenticator[F](new CommonAuthenticator)
        lobbyRef       <- Ref.of[F, Lobby](Lobby())
        subscribersRef <- Ref.of[F, Subscribers[F]](Set.empty)

        _ <-
          BlazeServerBuilder[F](global)
            .bindHttp(lobbyServerConfig.port, lobbyServerConfig.host)
            .withHttpApp(LobbyHttpApp[F](authenticator, lobbyRef, subscribersRef).app)
            .serve
            .compile
            .drain
      } yield ExitCode.Success
    }

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val logger: Logger[IO] = consoleLogger(formatter = Formatter.colorful)
    runF[IO]
  }
}
