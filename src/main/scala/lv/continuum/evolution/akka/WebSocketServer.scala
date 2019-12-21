package lv.continuum.evolution.akka

import java.util.UUID

import akka.actor._
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging

import scala.io.StdIn
import scala.util.{Failure, Success}

class WebSocketServer(implicit
  system: ActorSystem,
) extends LazyLogging {

  import system.dispatcher

  // Create one TableActor per server
  private val tableActor = system.spawn(TableActor(), s"TableActor")

  val route: Route =
    Route.seal {
      path("lobby_api") {

        // Create sources and flows per WebSocket connection
        val (pushActor, pushSource) = FlowCreator.createPushSource
        val sessionActor = system.spawn(SessionActor(tableActor, pushActor), s"SessionActor-${ UUID.randomUUID() }")
        handleWebSocketMessages(FlowCreator.createLobbyFlow(
          pushSource = pushSource,
          sessionActor = sessionActor,
        ))
      }
    }

  def start(address: String, port: Int): Unit = {
    Http().bindAndHandle(route, address, port).onComplete {
      case Success(serverBinding) =>
        val localAddress = serverBinding.localAddress
        logger.info(s"Server started at ${ localAddress.getHostName }:${ localAddress.getPort }, press enter to terminate")

      case Failure(_) =>
        logger.error(s"Server failed to start, press enter to terminate")
    }
  }
}

object WebSocketServer extends LazyLogging {

  private val config: Config = ConfigFactory.load().getConfig("web-socket-server")

  def main(args: Array[String]): Unit = {

    implicit val system: ActorSystem = ActorSystem("web-socket-server")

    // Start server
    val address = config.getString("address")
    val port = config.getInt("port")
    new WebSocketServer().start(address, port)

    // Terminate server
    StdIn.readLine()
    system.terminate()
  }
}
