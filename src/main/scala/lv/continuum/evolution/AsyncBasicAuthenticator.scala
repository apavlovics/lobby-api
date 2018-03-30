package lv.continuum.evolution

import akka.http.scaladsl.server.directives._
import akka.http.scaladsl.server.Directives._

import org.slf4j.LoggerFactory

import scala.concurrent._

/**
 * Defines an asynchronous HTTP basic authenticator.
 *
 * Sample implementation accepts user "admin" with password "admin".
 */
class AsyncBasicAuthenticator extends AsyncAuthenticator[String] {

  private val log = LoggerFactory.getLogger(classOf[AsyncBasicAuthenticator])

  override def apply(credentials: Credentials) = {
    credentials match {
      case p @ Credentials.Provided(user) => {
        if ("admin".equalsIgnoreCase(p.identifier) && p.verify("admin")) {
          log.debug(s"Allowed access to $credentials")
          Future.successful(Some(user))
        } else {
          log.debug(s"Denied access to $credentials")
          Future.successful(None)
        }
      }
      case _ => {
        log.debug(s"Denied access to $credentials")
        Future.successful(None)
      }
    }
  }
}
