package lv.continuum.evolution

import akka.actor._
import akka.http.scaladsl._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Supervision._
import akka.stream._
import com.typesafe.scalalogging.LazyLogging
import lv.continuum.evolution.model._

import scala.io.StdIn
import scala.util.{Failure, Success}

class WebSocketServer(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends LazyLogging {

  import system.dispatcher

  // Create push flow
  private val (pushQueue, pushSource) = FlowCreator.createPushFlow

  // Define route
  val route: Route =
    Route.seal {
      path("ws_api") {

        // Create new lobby flow for each connection
        handleWebSocketMessages(FlowCreator.createLobbyFlow(pushQueue, pushSource, new ClientContext()))
      }
    }

  def start(address: String, port: Int): Unit = {
    Http().bindAndHandle(route, address, port).onComplete {
      case Success(serverBinding) =>
        val localAddress = serverBinding.localAddress
        logger.info(s"Server started at ${ localAddress.getHostName }:${ localAddress.getPort }, press enter to terminate")
      case Failure(e)             =>
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

    // Decider can be configured to restart, resume or stop streams upon certain exceptions
    val decider: Decider = {
      e =>
        logger.error("Issue while processing stream", e)
        Supervision.Stop
    }

    // Setup actor materializer
    implicit val materializer: ActorMaterializer = ActorMaterializer(
      ActorMaterializerSettings(system).withSupervisionStrategy(decider))

    // Start server
    new WebSocketServer().start(address, port)

    // Terminate server
    StdIn.readLine()
    system.terminate()
  }
}
