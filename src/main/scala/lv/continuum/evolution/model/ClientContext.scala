package lv.continuum.evolution.model

class ClientContext {

  @volatile var userType: Option[String] = None

  def isAuthenticated = userType.isDefined
}
