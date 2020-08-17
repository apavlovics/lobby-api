package lv.continuum.lobby.cats

import lv.continuum.lobby.protocol.Protocol.UserType

case class SessionParams(
  userType: Option[UserType] = None,
)
