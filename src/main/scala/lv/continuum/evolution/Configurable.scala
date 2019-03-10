package lv.continuum.evolution

import com.typesafe.config.{Config, ConfigFactory}

trait Configurable {

  protected val config: Config = ConfigFactory.load()
}
