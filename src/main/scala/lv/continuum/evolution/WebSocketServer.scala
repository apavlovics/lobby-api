package lv.continuum.evolution

import akka.actor._
import akka.event.Logging
import akka.http.scaladsl._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives._
import akka.stream._
import akka.stream.Supervision._

import lv.continuum.evolution.model._

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.io.StdIn
import scala.util.{ Success, Failure }

class WebSocketServer(implicit val system: ActorSystem, implicit val materializer: ActorMaterializer) extends Loggable {
  import system.dispatcher

  // Create push flow
  val (pushQueue, pushSource) = FlowCreator.createPushFlow

  // Define route
  val route =
    Route.seal {
      path("ws_api") {

        // Create new lobby flow for each connection
        handleWebSocketMessages(FlowCreator.createLobbyFlow(pushQueue, pushSource, new ClientContext()))
      }
    }

  def start(address: String, port: Int) = {
    Http().bindAndHandle(route, address, port).onComplete {
      case Success(serverBinding) =>
        val localAddress = serverBinding.localAddress
        log.info(s"Server started at ${localAddress.getHostName}:${localAddress.getPort}, press enter to terminate")
      case Failure(e) =>
        log.error(s"Server failed to start, press enter to terminate")
    }
  }
}

object WebSocketServer extends Configurable with Loggable {

  private val address = config.getString("web-socket-server.address")
  private val port = config.getInt("web-socket-server.port")

  def main(args: Array[String]): Unit = {

    // Setup actor system
    implicit val system = ActorSystem()

    // Decider can be configured to restart, resume or stop streams upon certain exceptions
    val decider: Decider = {
      case e ⇒ {
        log.error("Issue while processing stream", e)
        Supervision.Stop
      }
    }

    // Setup actor materializer
    implicit val materializer = ActorMaterializer(
      ActorMaterializerSettings(system).withSupervisionStrategy(decider))

    // Start server
    new WebSocketServer().start(address, port)

    // Terminate server
    StdIn.readLine()
    system.terminate()
  }
}
