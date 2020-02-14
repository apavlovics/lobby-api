package lv.continuum.evolution.protocol

import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._
import io.circe.{Codec, Decoder, Encoder}
import lv.continuum.evolution.protocol.Protocol.In._
import lv.continuum.evolution.protocol.Protocol.Out._
import lv.continuum.evolution.protocol.Protocol._

trait ProtocolFormat {

  implicit val configuration: Configuration =
    Configuration.default
      .withDiscriminator("$type")
      .withSnakeCaseConstructorNames
      .withSnakeCaseMemberNames

  implicit val usernameCodec: Codec[Username] = deriveUnwrappedCodec
  implicit val passwordCodec: Codec[Password] = deriveUnwrappedCodec
  implicit val seqCodec: Codec[Seq] = deriveUnwrappedCodec
  implicit val tableIdCodec: Codec[TableId] = deriveUnwrappedCodec
  implicit val tableNameCodec: Codec[TableName] = deriveUnwrappedCodec

  implicit val tableCodec: Codec[Table] = deriveConfiguredCodec
  implicit val tableToAddCodec: Codec[TableToAdd] = deriveConfiguredCodec

  implicit val inDecoder: Decoder[In] = deriveConfiguredDecoder
  implicit val loginDecoder: Decoder[Login] = deriveConfiguredDecoder
  implicit val pingDecoder: Decoder[Ping] = deriveConfiguredDecoder
  implicit val addTableDecoder: Decoder[AddTable] = deriveConfiguredDecoder
  implicit val updateTableDecoder: Decoder[UpdateTable] = deriveConfiguredDecoder
  implicit val removeTableDecoder: Decoder[RemoveTable] = deriveConfiguredDecoder

  implicit val userTypeEncoder: Encoder[UserType] = Encoder.encodeString.contramap(_.entryName)

  implicit val outEncoder: Encoder[Out] = deriveConfiguredEncoder
  implicit val loginSuccessfulEncoder: Encoder[LoginSuccessful] = deriveConfiguredEncoder
  implicit val pongEncoder: Encoder[Pong] = deriveConfiguredEncoder
  implicit val tableListEncoder: Encoder[TableList] = deriveConfiguredEncoder
  implicit val tableAddedEncoder: Encoder[TableAdded] = deriveConfiguredEncoder
  implicit val tableUpdatedEncoder: Encoder[TableUpdated] = deriveConfiguredEncoder
  implicit val tableRemovedEncoder: Encoder[TableRemoved] = deriveConfiguredEncoder
  implicit val tableUpdateFailedEncoder: Encoder[TableUpdateFailed] = deriveConfiguredEncoder
  implicit val tableRemoveFailedEncoder: Encoder[TableRemoveFailed] = deriveConfiguredEncoder
}
