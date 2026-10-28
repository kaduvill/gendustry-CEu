package net.bdew.gendustry.top

import java.util.function.Function

import mcjty.theoneprobe.api.ITheOneProbe

object TopRegistration {
  private[top] var elementId = -1
}

/** Instantiated by TOP through the optional IMC callback on both physical sides. */
class TopRegistration extends Function[ITheOneProbe, java.lang.Void] {
  override def apply(probe: ITheOneProbe): java.lang.Void = {
    TopRegistration.elementId = probe.registerElementFactory(TopElementFactory)
    probe.registerProvider(TopProvider)
    null
  }
}
