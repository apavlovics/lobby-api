package lv.continuum.lobby

import fs2.concurrent.Queue
import lv.continuum.lobby.protocol.Protocol.PushOut

package object cats {
  type Subscriber[F[_]] = Queue[F, PushOut]
  type Subscribers[F[_]] = Set[Subscriber[F]]
}
