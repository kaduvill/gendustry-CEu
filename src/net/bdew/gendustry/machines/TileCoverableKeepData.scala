package net.bdew.gendustry.machines

import net.bdew.lib.block.TileKeepData
import net.bdew.lib.covers.TileCoverable
import net.minecraft.nbt.NBTTagCompound

trait TileCoverableKeepData extends TileCoverable with TileKeepData {
  override def afterTileBreakSave(t: NBTTagCompound): NBTTagCompound = {
    val saved = super.afterTileBreakSave(t)
    // BlockCoverable drops covers separately when the block is removed.
    covers.valuesIterator.foreach(slot => saved.removeTag(slot.name))
    saved
  }
}
