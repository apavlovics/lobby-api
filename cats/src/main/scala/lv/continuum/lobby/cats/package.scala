package lv.continuum.lobby

import _root_.cats.effect.std.Queue
import lv.continuum.lobby.protocol.Protocol.PushOut

package object cats {
  type Subscriber[F[_]] = Queue[F, PushOut]
  type Subscribers[F[_]] = Set[Subscriber[F]]
}
