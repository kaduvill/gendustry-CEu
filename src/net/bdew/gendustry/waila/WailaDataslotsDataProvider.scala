/*
 * Copyright (c) bdew, 2013 - 2016
 * https://github.com/bdew/gendustry
 *
 * This mod is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://bdew.net/minecraft-mod-public-license/
 */

package net.bdew.gendustry.waila

import mcp.mobius.waila.api.{IWailaConfigHandler, IWailaDataAccessor}
import net.bdew.gendustry.compat.TooltipText
import net.bdew.lib.data.base.{TileDataSlots, UpdateKind}
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

object WailaDataSlotsDataProvider extends BaseDataProvider(classOf[TileDataSlots]) {
  override def getNBTTag(player: EntityPlayerMP, te: TileDataSlots, tag: NBTTagCompound, world: World, pos: BlockPos) = {
    tag.setTag("gendustry_dataslots", TooltipText.guiData(te))
    tag
  }

  override def getBodyStrings(target: TileDataSlots, stack: ItemStack, acc: IWailaDataAccessor, cfg: IWailaConfigHandler) = {
    if (acc.getNBTData.hasKey("gendustry_dataslots")) {
      target.doLoad(UpdateKind.GUI, acc.getNBTData.getCompoundTag("gendustry_dataslots"))
      TooltipText.dataSlots(target)
    } else List.empty
  }
}
