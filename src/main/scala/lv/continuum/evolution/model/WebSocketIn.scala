package lv.continuum.evolution.model

import cats.syntax.functor._

import io.circe._
import io.circe.generic.extras._
import io.circe.syntax._

sealed trait WebSocketIn

object WebSocketIn {

  // Order complex case classes first
  implicit val decode: Decoder[WebSocketIn] = List[Decoder[WebSocketIn]](
    Decoder[LoginIn].widen,
    Decoder[PingIn].widen,
    Decoder[RemoveTableIn].widen,
    Decoder[TableListIn].widen,
    Decoder[ErrorIn].widen).reduceLeft(_ or _)
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
