/*
 * Copyright (c) bdew, 2013 - 2017
 * https://github.com/bdew/gendustry
 *
 * This mod is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://bdew.net/minecraft-mod-public-license/
 */

package net.bdew.gendustry.power

import cofh.redstoneflux.api.IEnergyContainerItem
import net.bdew.gendustry.compat.PowerProxy
import net.bdew.lib.power.ItemPoweredBase
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.Optional

@Optional.Interface(modid = PowerProxy.TE_MOD_ID, iface = "cofh.redstoneflux.api.IEnergyContainerItem")
trait ItemPoweredRF extends ItemPoweredBase with IEnergyContainerItem {
  def receiveEnergy(container: ItemStack, maxReceive: Int, simulate: Boolean): Int = {
    if (!PowerProxy.RFEnabled) return 0
    ItemCharge.rf.receive(this, container, maxReceive, simulate).toInt
  }

  def extractEnergy(container: ItemStack, maxExtract: Int, simulate: Boolean): Int = 0
  def getEnergyStored(container: ItemStack): Int = math.min(Int.MaxValue.toLong, ItemCharge.rf.stored(this, container)).toInt
  def getMaxEnergyStored(container: ItemStack): Int = math.min(Int.MaxValue.toLong, ItemCharge.rf.capacity(this)).toInt
}
