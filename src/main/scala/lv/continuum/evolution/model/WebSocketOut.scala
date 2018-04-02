package lv.continuum.evolution.model

import io.circe._
import io.circe.generic.extras._
import io.circe.syntax._

trait WebSocketOut

object WebSocketOut {

  implicit val encoder: Encoder[WebSocketOut] = {
    case loginOut: LoginOut ⇒ loginOut.asJson
    case pingOut: PingOut   ⇒ pingOut.asJson
    case errorOut: ErrorOut ⇒ errorOut.asJson
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
case class ErrorOut(
  $type: String = "error") extends WebSocketOut

@ConfiguredJsonCodec
case class Table(
  id:           Long,
  name:         String,
  participants: Long)

object LoginOut extends CirceConfiguration
object PingOut extends CirceConfiguration
object ErrorOut extends CirceConfiguration
object Table extends CirceConfiguration
