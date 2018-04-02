package lv.continuum.evolution.model

import io.circe._
import io.circe.generic.extras._
import io.circe.syntax._

trait WebSocketIn

object WebSocketIn {

  // Order complex case classes first
  implicit val decoder: Decoder[WebSocketIn] =
    Decoder[LoginIn].map[WebSocketIn](identity)
      .or(Decoder[PingIn].map[WebSocketIn](identity)
        .or(Decoder[RemoveTableIn].map[WebSocketIn](identity)
          .or(Decoder[TableListIn].map[WebSocketIn](identity)
            .or(Decoder[ErrorIn].map[WebSocketIn](identity)))))
}

@ConfiguredJsonCodec
case class LoginIn(
  $type:    String,
  username: String,
  password: String) extends WebSocketIn

@ConfiguredJsonCodec
case class PingIn(
  $type: String,
  seq:   Long) extends WebSocketIn

@ConfiguredJsonCodec
case class RemoveTableIn(
  $type: String,
  id:    Long) extends WebSocketIn

@ConfiguredJsonCodec
case class TableListIn(
  $type: String) extends WebSocketIn

@ConfiguredJsonCodec
case class ErrorIn() extends WebSocketIn

object LoginIn extends CirceConfiguration
object PingIn extends CirceConfiguration
object RemoveTableIn extends CirceConfiguration
object TableListIn extends CirceConfiguration
object ErrorIn extends CirceConfiguration
