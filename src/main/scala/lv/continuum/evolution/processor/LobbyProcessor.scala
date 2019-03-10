package lv.continuum.evolution.processor

import akka.stream.scaladsl._
import java.util.concurrent.CopyOnWriteArrayList

import com.typesafe.scalalogging.LazyLogging
import lv.continuum.evolution.model._

import scala.collection._
import scala.collection.JavaConverters._

/**
  * Processes [[lv.continuum.evolution.model.WebSocketIn WebSocketIn]] instances
  * into [[lv.continuum.evolution.model.WebSocketOut WebSocketOut]] instances.
  */
object LobbyProcessor extends LazyLogging {

  // TODO: In-memory data source with mock tables, should be replaced with real data source.
  private val tables: mutable.Buffer[Table] = new CopyOnWriteArrayList[Table]().asScala
  tables.append(Table(1, "table - James Bond", 7))
  tables.append(Table(2, "table - Mission Impossible", 4))

  def apply(pushQueue: SourceQueue[WebSocketOut], clientContext: ClientContext, webSocketIn: WebSocketIn): Option[WebSocketOut] = {
    logger.debug(s"User type is ${clientContext.userType}, subscribed is ${clientContext.subscribed}")
    logger.debug(s"Web socket in is $webSocketIn")

    webSocketIn match {

      // Login
      case LoginIn("login", username, password) =>
        (username, password) match {
          case ("admin", "admin") =>
            val userType = "admin"
            clientContext.userType = Some(userType)
            Some(LoginOut("login_successful", userType))
          case ("user", "user") =>
            val userType = "user"
            clientContext.userType = Some(userType)
            Some(LoginOut("login_successful", userType))
          case _ =>
            clientContext.userType = None
            Some(ErrorOut("login_failed"))
        }

      // Ping
      case PingIn("ping", seq) if clientContext.isAuthenticated => Some(PingOut(seq = seq))

      // Subscribe tables
      case TableListIn("subscribe_tables") if clientContext.isAuthenticated =>
        clientContext.subscribed = true
        Some(TableListOut(tables = tables))

      // Unsubscribe tables
      case TableListIn("unsubscribe_tables") if clientContext.isAuthenticated =>
        clientContext.subscribed = false
        None

      // Remove table
      case RemoveTableIn("remove_table", id) if clientContext.isAuthenticated =>
        if (clientContext.isAdmin) {
          val tablesToRemove = tables.filter(_.id == id)
          tablesToRemove.size match {
            case 0 => Some(ErrorTableOut("removal_failed", id))
            case _ =>
              tables --= tablesToRemove
              pushQueue.offer(RemoveTableOut(id = id))
              None
          }
        } else {
          Some(ErrorOut("not_authorized"))
        }

      // Error
      case _ if clientContext.isAuthenticated => Some(ErrorOut())
      case _ => Some(ErrorOut("not_authenticated"))
    }
  }
}
