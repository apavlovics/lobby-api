package lv.continuum.evolution.io

import cats.effect.IO
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.{HttpApp, HttpRoutes}

object LobbyHttpApp {

  val app: HttpApp[IO] = HttpRoutes.of[IO] {
    case GET -> Root => Ok("Hello, world!")
  }.orNotFound
}
