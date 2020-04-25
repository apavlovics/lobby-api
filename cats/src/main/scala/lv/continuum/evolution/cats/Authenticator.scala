package lv.continuum.evolution.cats

import cats.effect.Sync
import lv.continuum.evolution.auth.{Authenticator => CommonAuthenticator}
import lv.continuum.evolution.protocol.Protocol.{Password, UserType, Username}

/** A purely functional wrapper for [[lv.continuum.evolution.auth.Authenticator Authenticator]] from `common` module. */
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
