package net.bdew.gendustry.top

import mcjty.theoneprobe.api.{IProbeHitData, IProbeInfo, IProbeInfoProvider, ProbeMode}
import net.bdew.gendustry.compat.TooltipText
import net.bdew.gendustry.power.TilePowered
import net.bdew.lib.data.base.TileDataSlots
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World

object TopProvider extends IProbeInfoProvider {
  override def getID: String = "gendustry:machines"

  override def addProbeInfo(mode: ProbeMode, info: IProbeInfo, player: EntityPlayer, world: World,
                            state: IBlockState, hit: IProbeHitData): Unit = {
    if (world.isBlockLoaded(hit.getPos)) {
      world.getTileEntity(hit.getPos) match {
        case tile: TilePowered with TileDataSlots =>
          info.element(new TopElement(world.provider.getDimension, hit.getPos, state.getBlock.getRegistryName,
            hit.getSideHit, TooltipText.guiData(tile)))
        case _ =>
      }
    }
  }
}
