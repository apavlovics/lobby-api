package lv.continuum.evolution

import fs2.concurrent.Queue
import lv.continuum.evolution.protocol.Protocol.{PushOut, Table}

package object cats {
  type Tables = Vector[Table]
  type Subscriber[F[_]] = Queue[F, PushOut]
  type Subscribers[F[_]] = Set[Subscriber[F]]
}
