package lv.continuum.lobby.zio.layer

import lv.continuum.lobby.session.SessionParams
import zio.*

trait Session {

  def params(): UIO[SessionParams]
}

class SessionLive(
  sessionParamsRef: Ref[SessionParams],
) extends Session {

  def params(): UIO[SessionParams] = sessionParamsRef.get
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
