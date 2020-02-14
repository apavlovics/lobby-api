package lv.continuum.evolution

import fs2.concurrent.Queue
import lv.continuum.evolution.protocol.Protocol.{Out, Table}

package object cats {
  type Tables = Vector[Table]
  type Subscribers[F[_]] = Set[Queue[F, Out]]
}
