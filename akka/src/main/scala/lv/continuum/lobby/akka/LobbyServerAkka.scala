package lv.continuum.lobby.akka

import java.util.UUID

import akka.actor._
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl._
import akka.http.scaladsl.server.Directives.{Authenticator => _, _}
import akka.http.scaladsl.server.Route
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import lv.continuum.lobby.auth.Authenticator
import lv.continuum.lobby.config.LobbyServerConfig

import scala.io.StdIn
import scala.util.{Failure, Success}

class LobbyServerAkka(implicit
  system: ActorSystem,
) extends LazyLogging {

  import system.dispatcher

  // Create one TableActor and Authenticator per server
  private val tableActor = system.spawn(TableActor(), s"TableActor")
  private val authenticator: Authenticator = new Authenticator

  private[akka] val route: Route =
    Route.seal {
      path("lobby_api") {

        // Initialize actors, sources and flows per WebSocket connection
        val (pushActor, pushSource) = PushSource()
        val sessionActor = system.spawn(
          behavior = SessionActor(authenticator, tableActor, pushActor),
          name = s"SessionActor-${UUID.randomUUID()}",
        )
        handleWebSocketMessages(LobbyFlow(pushSource, sessionActor))
      }
    }

  def start(host: String, port: Int): Unit =
    Http().newServerAt(host, port).bind(route).onComplete {
      case Success(serverBinding) =>
        val localAddress = serverBinding.localAddress
        logger.info(
          s"Server started at ${localAddress.getHostName}:${localAddress.getPort}, press enter to terminate"
        )

      case Failure(_) =>
        logger.error(s"Server failed to start, press enter to terminate")
    }
}

object LobbyServerAkka extends App {

  private def start(): Unit = {

    implicit val system: ActorSystem = ActorSystem("akka-lobby-server")

    // Start server
    val lobbyServerConfig = LobbyServerConfig.loadOrThrow(ConfigFactory.load())
    new LobbyServerAkka().start(lobbyServerConfig.host, lobbyServerConfig.port)

    // Terminate server
    StdIn.readLine()
    system.terminate()
  }

  start()
}
