package lv.continuum.evolution.model

import io.circe.generic.extras._
import io.circe.syntax._

@ConfiguredJsonCodec
case class WebSocketIn(
  $type:    String,
  username: Option[String] = None,
  password: Option[String] = None,
  seq: Option[Long] = None)

object WebSocketIn {

  implicit val configuration = Configuration.default.withSnakeCaseMemberNames
}
