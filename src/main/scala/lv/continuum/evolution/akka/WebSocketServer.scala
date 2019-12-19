package lv.continuum.evolution.akka

import java.util.UUID

import akka.actor._
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.LazyLogging

import scala.io.StdIn
import scala.util.{Failure, Success}

class WebSocketServer(implicit
  system: ActorSystem,
) extends LazyLogging {

  import system.dispatcher

  // Create push flow
  private val (pushQueue, pushSource) = FlowCreator.createPushFlow

  // Define route
  val route: Route =
    Route.seal {
      path("lobby_api") {

        // Create new lobby flow for each connection
        handleWebSocketMessages(FlowCreator.createLobbyFlow(
          pushQueue = pushQueue,
          pushSource = pushSource,
          sessionActor = system.spawn(SessionActor(), s"SessionActor-${ UUID.randomUUID() }"),
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

object WebSocketServer extends Configurable with LazyLogging {

  private val address = config.getString("web-socket-server.address")
  private val port = config.getInt("web-socket-server.port")

  def main(args: Array[String]): Unit = {

    // Setup actor system
    implicit val system: ActorSystem = ActorSystem("web-socket-server")

    // Start server
    new WebSocketServer().start(address, port)

    // Terminate server
    StdIn.readLine()
    system.terminate()
  }
}
