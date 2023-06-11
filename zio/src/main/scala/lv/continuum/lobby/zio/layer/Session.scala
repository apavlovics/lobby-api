package lv.continuum.lobby.zio.layer

import lv.continuum.lobby.protocol.Protocol.UserType
import lv.continuum.lobby.session.SessionParams
import zio.*

trait Session {

  def params(): UIO[SessionParams]

  def updateUserType(userType: Option[UserType]): UIO[Unit]
}

class SessionLive private (
  sessionParamsRef: Ref[SessionParams],
) extends Session {

  def params(): UIO[SessionParams] = sessionParamsRef.get

  def updateUserType(userType: Option[UserType]): UIO[Unit] =
    sessionParamsRef.update(_.copy(userType = userType))
}

object SessionLive {

  val layer: ULayer[Session] =
    ZLayer {
      for {
        _                <- ZIO.logDebug("Creating new session")
        sessionParamsRef <- Ref.make(SessionParams())
      } yield SessionLive(sessionParamsRef)
    }
}
