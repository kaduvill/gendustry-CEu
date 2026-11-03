/*
 * Copyright (c) bdew, 2013 - 2017
 * https://github.com/bdew/gendustry
 *
 * This mod is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://bdew.net/minecraft-mod-public-license/
 */

package net.bdew.gendustry.power

import net.bdew.lib.power.ItemPoweredBase
import net.minecraft.item.ItemStack

trait ItemPowered extends ItemPoweredBase with ItemPoweredCharge with ItemPoweredRF with ItemPoweredEU with ItemPoweredTesla with ItemPoweredForge {
  setNoRepair()

  override def showDurabilityBar(stack: ItemStack): Boolean = stack.isItemStackDamageable

  override def getDurabilityForDisplay(stack: ItemStack): Double = {
    if (stack.getItemDamage != 0) return super.getDurabilityForDisplay(stack)
    // Fresh recipe stacks have no cached damage. Match BDLib's bar without initializing NBT.
    val damage = math.max(1, math.min(100, (100F * (1F - ItemCharge.whole(this, stack).toFloat / maxCharge)).round))
    damage.toDouble / stack.getMaxDamage
  }
}
