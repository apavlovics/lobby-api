package lv.continuum.evolution.cats

import lv.continuum.evolution.protocol.Protocol.UserType

case class SessionParams(
  userType: Option[UserType] = None,
)
