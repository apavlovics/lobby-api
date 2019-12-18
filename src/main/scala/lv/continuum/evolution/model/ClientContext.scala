package lv.continuum.evolution.model

import lv.continuum.evolution.model.Protocol.UserType

class ClientContext {

  @volatile var userType: Option[UserType] = None
  @volatile var subscribed = false

  def isAuthenticated: Boolean = userType.isDefined

  def isAdmin: Boolean = userType.contains(UserType.Admin)
}
