package org.slf4j.impl

import cats.effect.{ConcurrentEffect, ContextShift, IO, Timer}
import io.odin._
import io.odin.formatter.Formatter
import io.odin.slf4j.OdinLoggerBinder

import scala.concurrent.ExecutionContext

/** @see [[https://github.com/valskalla/odin#slf4j-bridge Odin SLF4J Bridge]] */
class StaticLoggerBinder extends OdinLoggerBinder[IO] {

  private val ec = ExecutionContext.global

  implicit val timer: Timer[IO] = IO.timer(ec)
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)
  implicit val F: ConcurrentEffect[IO] = IO.ioConcurrentEffect

  val loggers: PartialFunction[String, Logger[IO]] = {
    case _ =>
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
