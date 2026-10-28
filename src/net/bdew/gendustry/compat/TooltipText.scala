/*
 * Copyright (c) bdew, 2013 - 2016
 * https://github.com/bdew/gendustry
 *
 * This mod is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://bdew.net/minecraft-mod-public-license/
 */

package net.bdew.gendustry.compat

import java.util.Locale

import net.bdew.gendustry.config.Config
import net.bdew.gendustry.items.covers.ErrorSensorCover
import net.bdew.gendustry.machines.apiary.TileApiary
import net.bdew.lib.data.base.{TileDataSlots, UpdateKind}
import net.bdew.lib.data.{DataSlotTank, DataSlotTankRestricted}
import net.bdew.lib.power.DataSlotPower
import net.bdew.lib.{DecFormat, Misc}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.text.TextFormatting
import net.minecraftforge.fluids.FluidStack

/** Shared tooltip content; display units and translations are evaluated on the client. */
object TooltipText {
  def guiData(target: TileDataSlots): NBTTagCompound = {
    val tag = new NBTTagCompound
    target.doSave(UpdateKind.GUI, tag)
    tag
  }

  def dataSlots(target: TileDataSlots, showPower: Boolean = true): Iterable[String] = {
    target.dataSlots.values flatMap {
      case slot: DataSlotPower if showPower =>
        Some("%s / %s %s".format(DecFormat.round(slot.stored * Config.powerShowMultiplier), DecFormat.round(slot.capacity * Config.powerShowMultiplier), Config.powerShowUnits))

      case slot: DataSlotTankRestricted =>
        if (slot.getFluid != null && slot.getFluid.getFluid != null) {
          Some("%s / %s mB %s".format(DecFormat.round(slot.getFluidAmount), DecFormat.round(slot.size), slot.getFluid.getLocalizedName))
        } else {
          Some("0 / %s mB %s".format(DecFormat.round(slot.size), new FluidStack(slot.filterFluid, 0).getLocalizedName))
        }

      case slot: DataSlotTank =>
        if (slot.getFluid != null && slot.getFluid.getFluid != null) {
          Some("%s / %s mB %s".format(DecFormat.round(slot.getFluidAmount), DecFormat.round(slot.size), slot.getFluid.getLocalizedName))
        } else {
          None
        }

      case _ => None
    }
  }

  def apiary(target: TileApiary, side: EnumFacing, sneaking: Boolean, showShiftHint: Boolean = true): Iterable[String] = {
    var strings = target.errorConditions.toList.sortBy(_.getID) map { err =>
      TextFormatting.RED + Misc.toLocal(err.getUnlocalizedDescription)
    }
    strings :+= Misc.toLocalF("gendustry.label.control", Misc.toLocal("gendustry.rsmode." + target.rsmode.value.toString.toLowerCase(Locale.US)))

    strings ++= target.queen.filterNot(_.isEmpty).map(_.getDisplayName)

    if (side != null) {
      target.covers(side).value foreach { cover =>
        if (cover.getItem == ErrorSensorCover) {
          ErrorSensorCover.getErrorSensor(cover) foreach { sensor =>
            strings :+= "%s (%s%s%s)".format(
              Misc.toLocalF("gendustry.cover.label", cover.getDisplayName),
              TextFormatting.YELLOW,
              Misc.toLocal(sensor.getUnLocalizedName),
              TextFormatting.RESET
            )
          }
        } else {
          strings :+= Misc.toLocalF("gendustry.cover.label", cover.getDisplayName)
        }
      }
    }

    if (sneaking)
      strings ++= target.getStats
    else if (showShiftHint)
      strings :+= Misc.toLocal("gendustry.label.shift")

    strings
  }
}
