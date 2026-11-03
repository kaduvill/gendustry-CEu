/*
 * Copyright (c) bdew, 2013 - 2016
 * https://github.com/bdew/gendustry
 *
 * This mod is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://bdew.net/minecraft-mod-public-license/
 */

package net.bdew.gendustry.power

import ic2.api.item.{ElectricItem, IElectricItemManager}
import net.bdew.gendustry.config.Config
import net.bdew.lib.{DecFormat, Misc}
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

class ItemPoweredEUManager(item: ItemPoweredEU) extends IElectricItemManager {
  private def units = ItemCharge.eu

  override def charge(itemStack: ItemStack, amount: Double, tier: Int, ignoreTransferLimit: Boolean, simulate: Boolean): Double =
    units.receiveDouble(item, itemStack, amount, simulate)

  override def discharge(itemStack: ItemStack, amount: Double, tier: Int, ignoreTransferLimit: Boolean, externally: Boolean, simulate: Boolean) = 0
  override def getCharge(itemStack: ItemStack): Double = units.storedDouble(item, itemStack)

  override def getTier(itemStack: ItemStack): Int = 2

  override def getMaxCharge(itemStack: ItemStack): Double =
    units.capacityDouble(item)

  override def use(itemStack: ItemStack, amount: Double, entity: EntityLivingBase) = false
  override def canUse(itemStack: ItemStack, amount: Double) = false

  override def chargeFromArmor(itemStack: ItemStack, entity: EntityLivingBase): Unit = {
    import scala.collection.JavaConversions._
    if (!entity.isInstanceOf[EntityPlayer]) return
    val player = entity.asInstanceOf[EntityPlayer]
    var charged = false
    for (stack <- player.getArmorInventoryList if !stack.isEmpty) {
      val tier = ElectricItem.manager.getTier(stack)
      val needCharge = units.remainingDouble(item, itemStack)
      val hasCharge = Misc.clamp(ElectricItem.manager.discharge(stack, needCharge, tier, true, true, true), 0D, needCharge)
      if (hasCharge > 0) {
        val paid = ElectricItem.manager.discharge(stack, hasCharge, tier, true, true, false)
        charged |= charge(itemStack, paid, tier, true, false) > 0
      }
    }
    if (charged)
      entity.asInstanceOf[EntityPlayer].inventoryContainer.detectAndSendChanges()
  }

  override def getToolTip(itemStack: ItemStack) = {
    item.getCharge(itemStack) // Preserve normal empty-tool initialization.
    DecFormat.round(ItemCharge.storedMJ(item, itemStack) * Config.powerShowMultiplier) + " / " +
      DecFormat.round(item.maxCharge.toDouble * Config.powerShowMultiplier) + " " + Config.powerShowUnits
  }
}
