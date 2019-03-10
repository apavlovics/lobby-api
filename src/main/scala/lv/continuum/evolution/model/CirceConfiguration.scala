package lv.continuum.evolution.model

import io.circe.generic.extras._

trait CirceConfiguration {

  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames
}
