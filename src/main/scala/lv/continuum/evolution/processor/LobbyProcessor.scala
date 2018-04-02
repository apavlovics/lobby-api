package lv.continuum.evolution.processor

import java.util.concurrent.ConcurrentHashMap

import lv.continuum.evolution.model._

import org.slf4j.LoggerFactory

import scala.collection.concurrent._
import scala.collection.convert.decorateAsScala._

/**
 * Processes [[WebSocketIn]] instances into [[WebSocketOut]] instances.
 */
object LobbyProcessor {

  private val log = LoggerFactory.getLogger(LobbyProcessor.getClass)

  // TODO: In-memory data source with mock tables, should be replaced with real data source.
  private val tables: Map[Long, Table] = new ConcurrentHashMap().asScala
  tables.put(1, Table(1, "table - James Bond", 7))
  tables.put(2, Table(1, "table - Mission Impossible", 4))

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
      case PingIn("ping", seq) if clientContext.isAuthenticated => PingOut(seq = seq)

      // Subscribe tables
      case TableListIn("subscribe_tables") if clientContext.isAuthenticated => TableListOut(tables = tables.values.toList)

      // Error
      case _ if clientContext.isAuthenticated => ErrorOut()
      case _ => ErrorOut("unauthenticated")
    }
  }
}
