package lv.continuum.evolution.io

import cats.effect.concurrent.Ref
import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.server.blaze._

object LobbyServerIO extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = for {
    tableState <- Ref.of[IO, TableState](TableState())
    _ <- BlazeServerBuilder[IO]

      // TODO Load port and host from configuration
      .bindHttp(9000, "0.0.0.0")
      .withHttpApp(LobbyHttpApp[IO](LobbyProcessor(tableState)).app)
      .serve
      .compile
      .drain
  } yield ExitCode.Success
}
