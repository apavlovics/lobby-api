package lv.continuum.evolution.auth

import cats.syntax.option._
import lv.continuum.evolution.protocol.Protocol.UserType._
import lv.continuum.evolution.protocol.Protocol._

class SimpleAuthenticator extends Authenticator {
  override def authenticate(username: Username, password: Password): Option[UserType] =
    (username, password) match {
      case (Username("admin"), Password("admin")) => Admin.some
      case (Username("user"), Password("user"))   => User.some
      case _                                      => None
    }
}
