package lv.continuum.evolution.model

import io.circe._
import io.circe.generic.extras._
import io.circe.syntax._

trait CirceConfiguration {

  implicit val configuration = Configuration.default.withSnakeCaseMemberNames
}
