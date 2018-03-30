package lv.continuum.evolution.processor

import lv.continuum.evolution.model._

/**
 * Processes [[SampleIn]] instances into [[SampleOut]] instances.
 */
object SampleProcessor {

  def apply(sampleIn: SampleIn, flowName: String): SampleOut = {
    SampleOut(content = s"$flowName: ${sampleIn.content}")
  }
}
