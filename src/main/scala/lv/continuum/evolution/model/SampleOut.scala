package lv.continuum.evolution.model

import io.circe.generic.JsonCodec
import io.circe.java8.time._
import io.circe.syntax._

import java.time.LocalDateTime

@JsonCodec
case class SampleOut(createdAt: LocalDateTime = LocalDateTime.now(), content: String)
