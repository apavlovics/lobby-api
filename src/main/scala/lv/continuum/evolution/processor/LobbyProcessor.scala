package lv.continuum.evolution.processor

import lv.continuum.evolution.model._

import org.slf4j.LoggerFactory

/**
 * Processes [[WebSocketIn]] instances into [[WebSocketOut]] instances.
 */
object LobbyProcessor {

  private val log = LoggerFactory.getLogger(LobbyProcessor.getClass)

  def apply(clientContext: ClientContext, webSocketIn: WebSocketIn): WebSocketOut = {
    log.debug(s"Client context user type is ${clientContext.userType}")

    webSocketIn match {

      // Login
      case LoginIn("login", username, password) => {
        (username, password) match {
          case ("admin", "admin") => {
            val userType = "admin"
            clientContext.userType = Some(userType)
            LoginOut("login_successful", userType)
          }
          case ("user", "user") => {
            val userType = "user"
            clientContext.userType = Some(userType)
            LoginOut("login_successful", userType)
          }
          case _ => {
            clientContext.userType = None
            ErrorOut("login_failed")
          }
        }
      }

      // Ping
      case PingIn("ping", seq) => PingOut(seq = seq)

      // Error
      case _                   => ErrorOut()
    }
  }
}
