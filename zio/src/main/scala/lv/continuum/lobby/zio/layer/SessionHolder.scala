package lv.continuum.lobby.zio.layer

import lv.continuum.lobby.protocol.Protocol.UserType
import lv.continuum.lobby.session.SessionParams
import zio.*

trait SessionHolder {

  def params(): UIO[SessionParams]

  def updateUserType(userType: Option[UserType]): UIO[Unit]
}

class SessionHolderLive private (
  sessionParamsRef: Ref[SessionParams],
) extends SessionHolder {

  override def params(): UIO[SessionParams] = sessionParamsRef.get

  override def updateUserType(userType: Option[UserType]): UIO[Unit] =
    sessionParamsRef.update(_.copy(userType = userType))
}

object SessionHolderLive {

  val layer: ULayer[SessionHolder] =
    ZLayer {
      for {
        _                <- ZIO.logDebug("Creating new session holder")
        sessionParamsRef <- Ref.make(SessionParams())
      } yield SessionHolderLive(sessionParamsRef)
    }
}
