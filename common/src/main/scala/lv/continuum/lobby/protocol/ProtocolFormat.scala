package lv.continuum.lobby.protocol

import cats.syntax.either.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}
import lv.continuum.lobby.model.ParsingError
import lv.continuum.lobby.protocol.Protocol.*
import lv.continuum.lobby.protocol.ProtocolFormat.*

import scala.util.Try

trait ProtocolFormat {

  given JsonValueCodec[UserType] = new JsonValueCodec {

    override def decodeValue(reader: JsonReader, default: UserType): UserType = {
      val value = reader.readString(default = "")
      UserType.valuesMap.getOrElse(value, reader.decodeError(msg = "Invalid user type"))
    }

    override def encodeValue(userType: UserType, out: JsonWriter): Unit = out.writeVal(userType.value)

    override val nullValue: UserType = null
  }

  given JsonValueCodec[In] = JsonCodecMaker.make(codecMakerConfig)

  given JsonValueCodec[Out] = JsonCodecMaker.make(codecMakerConfig)

  /** Deserializes JSON string into `A`. */
  def fromJson[A](json: String)(using JsonValueCodec[A]): Either[ParsingError, A] =
    Try(readFromString[A](json)).toEither.leftMap(ParsingError.apply)

  /** Serializes `A` into JSON string. */
  def toJson[A](a: A)(using JsonValueCodec[A]): String = writeToString[A](a)
}

private object ProtocolFormat {

  inline def codecMakerConfig: CodecMakerConfig =
    CodecMakerConfig
      .withDiscriminatorFieldName(Some("$type"))
      .withAdtLeafClassNameMapper(JsonCodecMaker.simpleClassName.andThen(JsonCodecMaker.enforce_snake_case))
      .withFieldNameMapper(JsonCodecMaker.enforce_snake_case)
}
