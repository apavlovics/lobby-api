package lv.continuum.evolution.processor

import lv.continuum.evolution.model._

/**
 * Processes [[SampleIn]] instances into [[SampleOut]] instances.
 */
object SampleProcessor {

  def apply(sampleIn: SampleIn): SampleOut = {
    sampleIn match {
      case SampleIn("login", _) => SampleOut("error")
      case _ => SampleOut("error")
    }
  }
}
