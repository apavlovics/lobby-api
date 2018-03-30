package lv.continuum.evolution.model

import io.circe.generic.JsonCodec
import io.circe.syntax._

@JsonCodec
case class SampleIn(id: Long = 0, content: String)
