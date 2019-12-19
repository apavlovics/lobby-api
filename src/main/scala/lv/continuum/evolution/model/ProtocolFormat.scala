package lv.continuum.evolution.model

import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import lv.continuum.evolution.model.Protocol._
import lv.continuum.evolution.model.Protocol.In._
import lv.continuum.evolution.model.Protocol.Out._

import scala.util.Try

trait ProtocolFormat {

  implicit val configuration: Configuration =
    Configuration.default
      .withSnakeCaseMemberNames

  implicit val inTypeDecoder: Decoder[InType] = Decoder.decodeString.emapTry { s => Try(InType.withName(s)) }
  implicit val loginInDecoder: Decoder[LoginIn] = deriveConfiguredDecoder
  implicit val pingInDecoder: Decoder[PingIn] = deriveConfiguredDecoder
  implicit val removeTableInDecoder: Decoder[RemoveTableIn] = deriveConfiguredDecoder

  implicit val inDecoder: Decoder[In] =
    c => for {
      inType <- c.downField("$type").as[InType]
      in <- inType match {
        case InType.Login             => loginInDecoder.decodeJson(c.value)
        case InType.Ping              => pingInDecoder.decodeJson(c.value)
        case InType.RemoveTable       => removeTableInDecoder.decodeJson(c.value)
        case InType.SubscribeTables   => Right(SubscribeTablesIn)
        case InType.UnsubscribeTables => Right(UnsubscribeTablesIn)
      }
    } yield in

  implicit val outTypeEncoder: Encoder[OutType] = Encoder.encodeString.contramap(_.entryName)
  implicit val userTypeEncoder: Encoder[UserType] = Encoder.encodeString.contramap(_.entryName)
  implicit val tableEncoder: Encoder[Table] = deriveConfiguredEncoder
  implicit val loginSuccessfulOutEncoder: Encoder[LoginSuccessfulOut] = deriveConfiguredEncoder
  implicit val pongOutEncoder: Encoder[PongOut] = deriveConfiguredEncoder
  implicit val tableRemovedOutEncoder: Encoder[TableRemovedOut] = deriveConfiguredEncoder
  implicit val tableListOutEncoder: Encoder[TableListOut] = deriveConfiguredEncoder
  implicit val tableErrorOutEncoder: Encoder[TableErrorOut] = deriveConfiguredEncoder
  implicit val errorOutEncoder: Encoder[ErrorOut] = deriveConfiguredEncoder

  implicit val outEncoder: Encoder[Out] = Encoder.instance {
    case out: LoginSuccessfulOut => out.asJson
    case out: PongOut            => out.asJson
    case out: TableRemovedOut    => out.asJson
    case out: TableListOut       => out.asJson
    case out: TableErrorOut      => out.asJson
    case out: ErrorOut           => out.asJson
  }
}
