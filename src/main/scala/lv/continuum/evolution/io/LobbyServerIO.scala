package lv.continuum.evolution.io

import cats.effect.concurrent.Ref
import cats.effect.{ExitCode, IO, IOApp}
import com.typesafe.config.ConfigFactory
import lv.continuum.evolution.config.LobbyServerConfig
import lv.continuum.evolution.protocol.SampleData._
import org.http4s.server.blaze._

object LobbyServerIO extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = for {
    config <- IO(ConfigFactory.load())
    lobbyServerConfig <- LobbyServerConfig.load[IO](config)

    tablesRef <- Ref.of[IO, Tables](tables)
    subscribersRef <- Ref.of[IO, Subscribers[IO]](Set.empty)

    _ <- BlazeServerBuilder[IO]
      .bindHttp(lobbyServerConfig.port, lobbyServerConfig.host)
      .withHttpApp(LobbyHttpApp[IO](tablesRef, subscribersRef).app)
      .serve
      .compile
      .drain
  } yield ExitCode.Success
}
