package lv.continuum.evolution.auth

import cats.syntax.option._
import lv.continuum.evolution.protocol.Protocol.UserType._
import lv.continuum.evolution.protocol.Protocol._

/** A simple in-memory `Authenticator` service. While it is enough for the purposes of this sample
  * application, a real-world alternative would probably need to work asynchronously. */
class Authenticator {

  def authenticate(username: Username, password: Password): Option[UserType] =
    (username, password) match {
      case (Username("admin"), Password("admin")) => Admin.some
      case (Username("user"), Password("user"))   => User.some
      case _                                      => None
    }
}
