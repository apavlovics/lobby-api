package lv.continuum.evolution.model

import io.circe._
import io.circe.generic.extras._
import io.circe.syntax._

trait WebSocketOut

object WebSocketOut {

  implicit val encoder: Encoder[WebSocketOut] = {
    case wso: LoginOut       => wso.asJson
    case wso: PingOut        => wso.asJson
    case wso: RemoveTableOut => wso.asJson
    case wso: TableListOut   => wso.asJson
    case wso: ErrorTableOut  => wso.asJson
    case wso: ErrorOut       => wso.asJson
  }
}

@ConfiguredJsonCodec
case class LoginOut(
  $type:    String,
  userType: String) extends WebSocketOut

@ConfiguredJsonCodec
case class PingOut(
  $type: String = "pong",
  seq:   Long) extends WebSocketOut

@ConfiguredJsonCodec
case class RemoveTableOut(
  $type: String = "table_removed",
  id:    Long) extends WebSocketOut

@ConfiguredJsonCodec
case class TableListOut(
  $type:  String     = "table_list",
  tables: Seq[Table]) extends WebSocketOut

@ConfiguredJsonCodec
case class ErrorTableOut(
  $type: String,
  id:    Long) extends WebSocketOut

@ConfiguredJsonCodec
case class ErrorOut(
  $type: String = "unknown_error") extends WebSocketOut

@ConfiguredJsonCodec
case class Table(
  id:           Long,
  name:         String,
  participants: Long)

object LoginOut extends CirceConfiguration
object PingOut extends CirceConfiguration
object RemoveTableOut extends CirceConfiguration
object TableListOut extends CirceConfiguration
object ErrorTableOut extends CirceConfiguration
object ErrorOut extends CirceConfiguration
object Table extends CirceConfiguration
