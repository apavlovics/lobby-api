package org.slf4j.impl

import cats.effect.std.Dispatcher
import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Sync}
import io.odin._
import io.odin.formatter.Formatter
import io.odin.slf4j.OdinLoggerBinder

/** @see [[https://github.com/valskalla/odin#slf4j-bridge Odin SLF4J Bridge]] */
class StaticLoggerBinder extends OdinLoggerBinder[IO] {

  implicit val F: Sync[IO] = IO.asyncForIO
  implicit val dispatcher: Dispatcher[IO] = Dispatcher[IO].allocated.unsafeRunSync()._1

  val loggers: PartialFunction[String, Logger[IO]] = { case _ =>
    consoleLogger[IO](
      formatter = Formatter.colorful,
      minLevel = Level.Info,
    )
  }
}

object StaticLoggerBinder extends StaticLoggerBinder {

  var REQUESTED_API_VERSION: String = "1.7"

  def getSingleton: StaticLoggerBinder = this
}
