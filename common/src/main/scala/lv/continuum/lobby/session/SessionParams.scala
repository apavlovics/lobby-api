package lv.continuum.lobby.session

import lv.continuum.lobby.protocol.Protocol.UserType

case class SessionParams(
  userType: Option[UserType] = None,
)
