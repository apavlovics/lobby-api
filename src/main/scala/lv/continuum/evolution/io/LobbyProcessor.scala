package lv.continuum.evolution.io

import cats.Monad
import cats.effect.concurrent.Ref
import cats.syntax.option._
import io.circe.Error
import lv.continuum.evolution.protocol.Protocol.In._
import lv.continuum.evolution.protocol.Protocol.Out._
import lv.continuum.evolution.protocol.Protocol._

class LobbyProcessor[F[_] : Monad](
  tableState: Ref[F, TableState],
) {

  def process(
    in: Either[Error, In],
    sessionParams: Ref[F, SessionParams],
  ): F[Option[Out]] = {
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
  def apply[F[_] : Monad](
    tableState: Ref[F, TableState],
  ): LobbyProcessor[F] = new LobbyProcessor(tableState)
}
