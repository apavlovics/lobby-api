package lv.continuum.evolution.io

import lv.continuum.evolution.protocol.Protocol.UserType

case class SessionParams(userType: Option[UserType] = None)
