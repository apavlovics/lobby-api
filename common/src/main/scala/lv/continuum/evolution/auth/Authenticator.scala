package lv.continuum.evolution.auth

import lv.continuum.evolution.protocol.Protocol._

trait Authenticator {
  def authenticate(username: Username, password: Password): Option[UserType]
}
