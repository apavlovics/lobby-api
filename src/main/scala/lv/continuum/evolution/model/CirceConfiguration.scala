package lv.continuum.evolution.model

import io.circe.generic.extras._

trait CirceConfiguration {

  protected implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames
}
