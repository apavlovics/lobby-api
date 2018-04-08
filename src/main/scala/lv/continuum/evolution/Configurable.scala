package lv.continuum.evolution

import com.typesafe.config.ConfigFactory

trait Configurable {

  protected val config = ConfigFactory.load()
}
