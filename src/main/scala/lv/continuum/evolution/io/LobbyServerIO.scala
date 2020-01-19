package lv.continuum.evolution.io

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import org.http4s.server.blaze._

object LobbyServerIO extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]

      // TODO Load port and host from configuration
      .bindHttp(9000, "0.0.0.0")
      .withHttpApp(LobbyHttpApp[IO](LobbyProcessor()).app)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
}
