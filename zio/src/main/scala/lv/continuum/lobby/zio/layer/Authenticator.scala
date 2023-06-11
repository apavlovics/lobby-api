package lv.continuum.lobby.zio.layer

import lv.continuum.lobby.auth.Authenticator as CommonAuthenticator
import lv.continuum.lobby.protocol.Protocol.{Password, UserType, Username}
import zio.*

trait Authenticator {

  def authenticate(
    username: Username,
    password: Password,
  ): UIO[Option[UserType]]
}

class AuthenticatorLive private (
  commonAuthenticator: CommonAuthenticator,
) extends Authenticator {

  def authenticate(
    username: Username,
    password: Password,
  ): UIO[Option[UserType]] = ZIO.succeed(
    commonAuthenticator.authenticate(username, password)
  )
}

object AuthenticatorLive {

  val layer: ULayer[AuthenticatorLive] =
    ZLayer.succeed(AuthenticatorLive(CommonAuthenticator.InMemory()))
}
