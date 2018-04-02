package lv.continuum.evolution.model

class ClientContext {

  @volatile var userType: Option[String] = None
  @volatile var subscribed = false

  def isAuthenticated = userType.isDefined

  def isAdmin = userType.contains("admin")
}
