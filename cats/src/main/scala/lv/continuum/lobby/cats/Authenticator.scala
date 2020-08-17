package lv.continuum.lobby.cats

import cats.effect.Sync
import lv.continuum.lobby.auth.{Authenticator => CommonAuthenticator}
import lv.continuum.lobby.protocol.Protocol.{Password, UserType, Username}

/** A purely functional wrapper for [[lv.continuum.lobby.auth.Authenticator Authenticator]] from `common` module. */
class Authenticator[F[_]: Sync](
  commonAuthenticator: CommonAuthenticator,
) {

  def authenticate(username: Username, password: Password): F[Option[UserType]] =
    Sync[F].delay(commonAuthenticator.authenticate(username, password))
}

object Authenticator {
  def apply[F[_]: Sync](
    commonAuthenticator: CommonAuthenticator,
  ): Authenticator[F] = new Authenticator(commonAuthenticator)
}
