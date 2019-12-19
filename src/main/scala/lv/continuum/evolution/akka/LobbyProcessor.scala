package lv.continuum.evolution.akka

import java.util.concurrent.CopyOnWriteArrayList

import akka.stream.scaladsl._
import com.typesafe.scalalogging.LazyLogging
import lv.continuum.evolution.protocol.Protocol._
import lv.continuum.evolution.protocol.Protocol.In._
import lv.continuum.evolution.protocol.Protocol.Out._

import scala.collection._
import scala.jdk.CollectionConverters._

/** Processes [[lv.continuum.evolution.protocol.Protocol.In In]] instances
  * into [[lv.continuum.evolution.protocol.Protocol.Out Out]] instances.
  */
object LobbyProcessor extends LazyLogging {

  // TODO: In-memory data source with mock tables, should be replaced with real data source.
  private val tables: mutable.Buffer[Table] = new CopyOnWriteArrayList[Table]().asScala
  tables.append(Table(1, "table - James Bond", 7))
  tables.append(Table(2, "table - Mission Impossible", 4))

  def apply(pushQueue: SourceQueue[Out], clientContext: ClientContext, in: In): Option[Out] = {
    logger.debug(s"User type is ${ clientContext.userType }, subscribed is ${ clientContext.subscribed }")
    logger.debug(s"In is $in")

    in match {

      // Login
      case LoginIn(username, password) =>
        (username, password) match {
          case ("admin", "admin") =>
            val userType = UserType.Admin
            clientContext.userType = Some(userType)
            Some(LoginSuccessfulOut(userType = userType))
          case ("user", "user")   =>
            val userType = UserType.User
            clientContext.userType = Some(userType)
            Some(LoginSuccessfulOut(userType = userType))
          case _                  =>
            clientContext.userType = None
            Some(ErrorOut(OutType.LoginFailed))
        }

      // Ping
      case PingIn(seq) if clientContext.isAuthenticated => Some(PongOut(seq = seq))

      // Subscribe tables
      case SubscribeTablesIn if clientContext.isAuthenticated =>
        clientContext.subscribed = true
        Some(TableListOut(tables = tables.toList))

      // Unsubscribe tables
      case UnsubscribeTablesIn if clientContext.isAuthenticated =>
        clientContext.subscribed = false
        None

      // Remove table
      case RemoveTableIn(id) if clientContext.isAuthenticated =>
        if (clientContext.isAdmin) {
          val tablesToRemove = tables.filter(_.id == id)
          tablesToRemove.size match {
            case 0 => Some(TableErrorOut(OutType.RemovalFailed, id))
            case _ =>
              tables --= tablesToRemove
              pushQueue.offer(TableRemovedOut(id = id))
              None
          }
        } else {
          Some(ErrorOut(OutType.NotAuthorized))
        }

      // Error
      case _ if clientContext.isAuthenticated => Some(ErrorOut(OutType.UnknownError))
      case _                                  => Some(ErrorOut(OutType.NotAuthenticated))
    }
  }
}
