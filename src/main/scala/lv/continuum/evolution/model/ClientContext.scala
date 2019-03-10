package lv.continuum.evolution.model

class ClientContext {

  @volatile var userType: Option[String] = None
  @volatile var subscribed = false

  def isAuthenticated: Boolean = userType.isDefined

  def isAdmin: Boolean = userType.contains("admin")
}
