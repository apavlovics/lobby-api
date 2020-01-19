package lv.continuum.evolution.io

import cats.Monad
import cats.syntax.option._
import io.circe.Error
import lv.continuum.evolution.protocol.Protocol.In._
import lv.continuum.evolution.protocol.Protocol.Out._
import lv.continuum.evolution.protocol.Protocol._

class LobbyProcessor[F[_] : Monad] {

  def process(in: Either[Error, In]): F[Option[Out]] = {
    in match {
      case Right(PingIn(seq)) => F.pure(PongOut(seq = seq).some)

      // TODO Add logging framework
      case Left(_) => F.pure(ErrorOut(OutType.InvalidMessage).some)

      // TODO Complete implementation
      case _ => F.pure(None)
    }
  }
}

object LobbyProcessor {
  def apply[F[_] : Monad](): LobbyProcessor[F] = new LobbyProcessor()
}
