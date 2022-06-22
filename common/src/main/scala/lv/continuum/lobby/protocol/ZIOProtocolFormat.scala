package lv.continuum.lobby.protocol

import zio.json.{DeriveJsonCodec, JsonCodec, JsonDecoder, JsonEncoder}
import lv.continuum.lobby.protocol.Protocol._

trait ZIOProtocolFormat {

  implicit val usernameCodec: JsonCodec[Username] = {
    val encoder: JsonEncoder[Username] = JsonEncoder[String].contramap(_.value)
    val decoder: JsonDecoder[Username] = JsonDecoder[String].map(Username)
    JsonCodec(encoder, decoder)
  }

  implicit val passwordCodec: JsonCodec[Password] = {
    val encoder: JsonEncoder[Password] = JsonEncoder[String].contramap(_.value)
    val decoder: JsonDecoder[Password] = JsonDecoder[String].map(Password)
    JsonCodec(encoder, decoder)
  }

  implicit val seqCodec: JsonCodec[Seq] = {
    val encoder: JsonEncoder[Seq] = JsonEncoder[Long].contramap(_.value)
    val decoder: JsonDecoder[Seq] = JsonDecoder[Long].map(Seq)
    JsonCodec(encoder, decoder)
  }

  implicit val tableIdCodec: JsonCodec[TableId] = {
    val encoder: JsonEncoder[TableId] = JsonEncoder[Long].contramap(_.value)
    val decoder: JsonDecoder[TableId] = JsonDecoder[Long].map(TableId(_))
    JsonCodec(encoder, decoder)
  }

  implicit val tableNameCodec: JsonCodec[TableName] = {
    val encoder: JsonEncoder[TableName] = JsonEncoder[String].contramap(_.value)
    val decoder: JsonDecoder[TableName] = JsonDecoder[String].map(TableName)
    JsonCodec(encoder, decoder)
  }

  implicit val userTypeCodec: JsonCodec[UserType] = {
    val encoder: JsonEncoder[UserType] = JsonEncoder[String].contramap(_.entryName)
    val decoder: JsonDecoder[UserType] = JsonDecoder[String].map(UserType.namesToValuesMap)
    JsonCodec(encoder, decoder)
  }

  implicit val tableCodec: JsonCodec[Table] = DeriveJsonCodec.gen
  implicit val tableToAddCodec: JsonCodec[TableToAdd] = DeriveJsonCodec.gen

  implicit val inCodec: JsonCodec[In] = DeriveJsonCodec.gen
  implicit val outCodec: JsonCodec[Out] = DeriveJsonCodec.gen
}
