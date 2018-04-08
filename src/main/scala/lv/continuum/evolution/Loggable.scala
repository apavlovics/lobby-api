package lv.continuum.evolution

import org.slf4j.LoggerFactory

trait Loggable {

  val log = LoggerFactory.getLogger(getClass)
}
