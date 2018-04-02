package lv.continuum.evolution.processor

import lv.continuum.evolution.model._

import org.slf4j.LoggerFactory

/**
 * Processes [[WebSocketIn]] instances into [[WebSocketOut]] instances.
 */
object LobbyProcessor {

  private val log = LoggerFactory.getLogger(LobbyProcessor.getClass)

  def apply(clientContext: ClientContext, webSocketIn: WebSocketIn): WebSocketOut = {
    log.debug(s"Client context username is ${clientContext.username}")

    webSocketIn.$type match {

      // Login
      case "login" => {
        (webSocketIn.username, webSocketIn.password) match {
          case (Some("admin"), Some("admin")) => {
            clientContext.username = webSocketIn.username
            WebSocketOut("login_successful", Some("admin"))
          }
          case (Some("user"), Some("user")) => {
            clientContext.username = webSocketIn.username
            WebSocketOut("login_successful", Some("user"))
          }
          case _ => {
            clientContext.username = None
            WebSocketOut("login_failed")
          }
        }
      }

      // Ping
      case "ping" => WebSocketOut("pong", seq = webSocketIn.seq)

      // Error
      case _      => WebSocketOut("error")
    }
  }
}
