package lv.continuum.evolution

import org.slf4j.LoggerFactory

trait Loggable {

  protected val log = LoggerFactory.getLogger(getClass)
}
