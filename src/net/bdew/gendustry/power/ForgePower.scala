/*
 * Copyright (c) bdew, 2013 - 2017
 * https://github.com/bdew/gendustry
 *
 * This mod is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://bdew.net/minecraft-mod-public-license/
 */

package net.bdew.gendustry.power

import net.bdew.gendustry.compat.PowerProxy
import net.bdew.gendustry.config.Tuning
import net.bdew.lib.capabilities.{CapabilityProvider, CapabilityProviderItem}
import net.bdew.lib.power.{ItemPoweredBase, TilePoweredBase}
import net.minecraft.item.ItemStack
import net.minecraftforge.common.capabilities.{Capability, CapabilityInject}
import net.minecraftforge.energy.{CapabilityEnergy, IEnergyStorage}

import scala.annotation.meta.setter

object ForgePower {
  @(CapabilityInject@setter)(classOf[IEnergyStorage])
  var CAP: Capability[IEnergyStorage] = null

  lazy val ratio = Tuning.getSection("Power").getFloat("Forge_MJ_Ratio")
}

class EnergyHandlerTile(tile: TilePoweredBase) extends IEnergyStorage {
  override def getEnergyStored: Int = (tile.power.stored * ForgePower.ratio).toInt
  override def getMaxEnergyStored: Int = (tile.power.capacity * ForgePower.ratio).toInt

  override def canReceive: Boolean = true
  override def receiveEnergy(maxReceive: Int, simulate: Boolean): Int =
    (tile.power.inject(maxReceive / ForgePower.ratio, simulate) * ForgePower.ratio).floor.toInt

  override def canExtract: Boolean = false
  override def extractEnergy(maxExtract: Int, simulate: Boolean): Int = 0
}

class EnergyHandlerItem(item: ItemPoweredBase, stack: ItemStack) extends IEnergyStorage {
  override def getEnergyStored: Int = math.min(Int.MaxValue.toLong, ItemCharge.forge.stored(item, stack)).toInt
  override def getMaxEnergyStored: Int = math.min(Int.MaxValue.toLong, ItemCharge.forge.capacity(item)).toInt

  override def canReceive: Boolean = true
  override def receiveEnergy(maxReceive: Int, simulate: Boolean): Int =
    ItemCharge.forge.receive(item, stack, maxReceive, simulate).toInt

  override def canExtract: Boolean = false
  override def extractEnergy(maxExtract: Int, simulate: Boolean): Int = 0
}

trait TilePoweredForge extends TilePoweredBase with CapabilityProvider {
  if (PowerProxy.ForgeEnabled)
    addCapability(CapabilityEnergy.ENERGY, new EnergyHandlerTile(this))
}

trait ItemPoweredForge extends ItemPoweredBase with CapabilityProviderItem {
  if (PowerProxy.ForgeEnabled)
    addCapability(CapabilityEnergy.ENERGY, new EnergyHandlerItem(this, _))
}
