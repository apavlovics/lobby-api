package lv.continuum.evolution.processor

import lv.continuum.evolution.model._

/**
 * Processes [[WebSocketIn]] instances into [[WebSocketOut]] instances.
 */
object LobbyProcessor {

  def apply(webSocketIn: WebSocketIn): WebSocketOut = {
    webSocketIn.$type match {

      // Login
      case "login" => {
        (webSocketIn.username, webSocketIn.password) match {
          case (Some("admin"), Some("admin")) => WebSocketOut("login_successful", Some("admin"))
          case (Some("user"), Some("user"))   => WebSocketOut("login_successful", Some("user"))
          case _                              => WebSocketOut("login_failed")
        }
      }

      // Ping
      case "ping" => WebSocketOut("pong", seq = webSocketIn.seq)

      // Error
      case _      => WebSocketOut("error")
    }
  }
}
