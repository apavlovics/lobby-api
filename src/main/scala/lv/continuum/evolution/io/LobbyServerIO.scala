package lv.continuum.evolution.io

import cats.effect.concurrent.Ref
import cats.effect.{ExitCode, IO, IOApp}
import lv.continuum.evolution.protocol.SampleData._
import org.http4s.server.blaze._

object LobbyServerIO extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = for {
    tablesRef <- Ref.of[IO, Tables](tables)
    subscribersRef <- Ref.of[IO, Subscribers[IO]](Set.empty)
    _ <- BlazeServerBuilder[IO]

      // TODO Load port and host from configuration
      .bindHttp(9000, "0.0.0.0")
      .withHttpApp(LobbyHttpApp[IO](tablesRef, subscribersRef).app)
      .serve
      .compile
      .drain
  } yield ExitCode.Success
}
