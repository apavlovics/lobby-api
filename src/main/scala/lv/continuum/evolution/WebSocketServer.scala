package lv.continuum.evolution

import akka.actor._
import akka.event.Logging
import akka.http.scaladsl._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives._
import akka.stream._
import akka.stream.Supervision._

import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.io.StdIn

class WebSocketServer(implicit val system: ActorSystem, implicit val materializer: ActorMaterializer) {

  // Create flows
  val echoFlow = FlowCreator.createEchoFlow
  val broadcastFlow = FlowCreator.createBroadcastFlow
  val pushFlow = FlowCreator.createPushFlow

  // Define route
  val route =
    Route.seal {
      authenticateBasicAsync(realm = "secure-realm", new AsyncBasicAuthenticator) { user =>
        pathPrefix("ws_api") {
          path("echo") {
            handleWebSocketMessages(echoFlow)
          } ~ path("broadcast") {
            handleWebSocketMessages(broadcastFlow)
          } ~ path("push") {
            handleWebSocketMessages(pushFlow)
          }
        }
      }
    }

  def start(address: String, port: Int) = {
    Http().bindAndHandle(route, address, port)
  }
}

object WebSocketServer {

  private val log = LoggerFactory.getLogger(WebSocketServer.getClass)

  private val port = 9000;
  log.info(s"Port is $port");

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
    new WebSocketServer().start("localhost", port)
    log.info(s"Server started at localhost:$port, press enter to terminate")

    // Terminate server
    StdIn.readLine()
    system.terminate()
  }
}
