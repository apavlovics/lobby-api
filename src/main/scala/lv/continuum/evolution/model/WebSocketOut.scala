package lv.continuum.evolution.model

import io.circe.generic.extras._
import io.circe.syntax._

@ConfiguredJsonCodec
case class WebSocketOut(
  $type:    String,
  userType: Option[String]      = None,
  seq:      Option[Long]        = None,
  tables:   Option[List[Table]] = None)

object WebSocketOut {

  implicit val configuration = Configuration.default.withSnakeCaseMemberNames
}

@ConfiguredJsonCodec
case class Table(
  id:           Long,
  name:         String,
  participants: Long)

object Table {

  implicit val configuration = Configuration.default.withSnakeCaseMemberNames
}
