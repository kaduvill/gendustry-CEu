/*
 * Copyright (c) bdew, 2013 - 2017
 * https://github.com/bdew/gendustry
 *
 * This mod is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://bdew.net/minecraft-mod-public-license/
 */

package net.bdew.gendustry.power

import net.bdew.gendustry.config.Tuning
import net.bdew.lib.power.ItemPoweredBase
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

/** Shared item balance. The legacy `charge` tag remains whole MJ; only its remainder is new. */
object ItemCharge {
  private final val FractionTag = "gendustry:chargeFraction"

  lazy val forge = new Units(Tuning.getSection("Power").getDouble("Forge_MJ_Ratio"))
  lazy val rf = new Units(Tuning.getSection("Power").getDouble("RF_MJ_Ratio"))
  lazy val tesla = new Units(Tuning.getSection("Power").getDouble("T_MJ_Ratio"))
  lazy val eu = new Units(Tuning.getSection("Power").getDouble("EU_MJ_Ratio"))

  private def lcm(a: BigInt, b: BigInt): BigInt = a / a.gcd(b) * b
  private lazy val scale = Seq(forge, rf, tesla, eu).filter(_.valid).foldLeft(BigInt(1))((s, u) => lcm(s, u.numerator))

  private def fractionTag(stack: ItemStack): NBTTagCompound =
    if (stack.hasTagCompound && stack.getTagCompound.hasKey(FractionTag, 10)) stack.getTagCompound.getCompoundTag(FractionTag)
    else null

  private[power] def whole(item: ItemPoweredBase, stack: ItemStack): Int =
    if (stack.hasTagCompound) math.max(0, math.min(item.maxCharge, stack.getTagCompound.getInteger("charge"))) else 0

  private def readInteger(tag: NBTTagCompound, key: String): BigInt =
    if (tag.hasKey(key, 7)) {
      val bytes = tag.getByteArray(key)
      if (bytes.isEmpty) BigInt(0) else BigInt(bytes)
    } else BigInt(tag.getLong(key))

  private def fraction(item: ItemPoweredBase, stack: ItemStack): (BigInt, BigInt) = {
    val tag = fractionTag(stack)
    if (tag == null || whole(item, stack) >= item.maxCharge) return (BigInt(0), BigInt(1))
    val n = readInteger(tag, "n")
    val d = readInteger(tag, "d")
    if (d > 0 && n > 0 && n < d) (n, d) else (BigInt(0), BigInt(1))
  }

  // A negative result selects the exact wide-integer path. Normal configurations use only Long arithmetic.
  private def ticksLong(item: ItemPoweredBase, stack: ItemStack, s: Long): Long = {
    if (s <= 0 || item.maxCharge < 0 || item.maxCharge.toLong > Long.MaxValue / s) return -1L
    val w = whole(item, stack)
    val tag = fractionTag(stack)
    if (tag == null || w >= item.maxCharge) return w.toLong * s
    if (tag.hasKey("n", 7) || tag.hasKey("d", 7)) return -1L
    val n = tag.getLong("n")
    val d = tag.getLong("d")
    if (d <= 0 || n <= 0 || n >= d) return w.toLong * s
    if (s % d != 0) return -1L
    w.toLong * s + n * (s / d)
  }

  private def state(item: ItemPoweredBase, stack: ItemStack, unitScale: BigInt): (BigInt, BigInt) = {
    val (n, d) = fraction(item, stack)
    val s = lcm(unitScale, d)
    (BigInt(whole(item, stack)) * s + n * (s / d), s)
  }

  private def gcd(a: Long, b: Long): Long = if (b == 0) a else gcd(b, a % b)

  private def setLong(item: ItemPoweredBase, stack: ItemStack, ticks: Long, s: Long): Unit = {
    item.setCharge(stack, (ticks / s).toInt)
    val remainder = ticks % s
    if (remainder > 0) {
      val divisor = gcd(remainder, s)
      val tag = new NBTTagCompound
      tag.setLong("n", remainder / divisor)
      tag.setLong("d", s / divisor)
      stack.getTagCompound.setTag(FractionTag, tag)
    } else clearFraction(stack)
  }

  private def set(item: ItemPoweredBase, stack: ItemStack, ticks: BigInt, s: BigInt): Unit = {
    item.setCharge(stack, (ticks / s).toInt)
    val remainder = ticks % s
    if (remainder > 0) {
      val divisor = remainder.gcd(s)
      val tag = new NBTTagCompound
      for ((key, value) <- Seq("n" -> (remainder / divisor), "d" -> (s / divisor))) {
        if (value.isValidLong) tag.setLong(key, value.toLong)
        else tag.setByteArray(key, value.toByteArray)
      }
      stack.getTagCompound.setTag(FractionTag, tag)
    } else clearFraction(stack)
  }

  def clearFraction(stack: ItemStack): Unit =
    if (stack.hasTagCompound) stack.getTagCompound.removeTag(FractionTag)

  /** Whole-MJ consumption must retain the fraction; explicit setCharge still replaces the entire balance. */
  def consume(item: ItemPoweredBase, stack: ItemStack, amount: Long): Unit = {
    val charge = item.getCharge(stack) // Retain BDLib's initialization and damage-bar behavior.
    val tag = fractionTag(stack)
    val remaining = charge.toLong - amount
    item.setCharge(stack, math.max(0L, math.min(item.maxCharge.toLong, remaining)).toInt)
    if (tag != null && charge < item.maxCharge && remaining >= 0 && remaining < item.maxCharge) stack.getTagCompound.setTag(FractionTag, tag)
  }

  def craftingResult(item: ItemPoweredBase, stack: ItemStack, added: Long): ItemStack = {
    val charge = item.getCharge(stack)
    val (n, _) = fraction(item, stack)
    val room = item.maxCharge.toLong - charge
    if (added > room || (added == room && n > 0)) return ItemStack.EMPTY
    val result = item.stackWithCharge(math.max(0L, charge.toLong + added).toInt)
    val tag = fractionTag(stack)
    if (tag != null && charge.toLong + added >= 0 && charge.toLong + added < item.maxCharge)
      result.getTagCompound.setTag(FractionTag, tag.copy())
    result
  }

  def storedMJ(item: ItemPoweredBase, stack: ItemStack): Double = {
    val (n, d) = fraction(item, stack)
    whole(item, stack).toDouble + quotient(n, d)
  }

  private def quotient(n: BigInt, d: BigInt): Double =
    new java.math.BigDecimal(n.bigInteger).divide(new java.math.BigDecimal(d.bigInteger), java.math.MathContext.DECIMAL128).doubleValue()

  /** Rates come from the original decimal tuning value, never a widened Float. */
  final class Units(val ratio: Double) {
    val valid: Boolean = ratio > 0 && !ratio.isInfinity && !ratio.isNaN
    private val decimal = java.math.BigDecimal.valueOf(if (valid) ratio else 1D).stripTrailingZeros()
    private val unscaled = BigInt(decimal.unscaledValue())
    private val decimalScale = decimal.scale()
    private val rawN = if (decimalScale < 0) unscaled * BigInt(10).pow(-decimalScale) else unscaled
    private val rawD = BigInt(10).pow(math.max(0, decimalScale))
    private val divisor = rawN.gcd(rawD)
    private[ItemCharge] val numerator = rawN / divisor
    private val denominator = rawD / divisor
    private lazy val unitScale = lcm(scale, numerator)
    private lazy val step = unitScale / numerator * denominator
    private lazy val smallScale = if (unitScale.isValidLong) unitScale.toLong else 0L
    private lazy val smallStep = if (step.isValidLong) step.toLong else 0L

    def receive(item: ItemPoweredBase, stack: ItemStack, offered: Long, simulate: Boolean): Long = {
      if (!valid || offered <= 0) return 0L
      val current = ticksLong(item, stack, smallScale)
      if (current >= 0 && smallStep > 0) {
        val accepted = math.min(offered, (item.maxCharge.toLong * smallScale - current) / smallStep)
        if (accepted > 0 && !simulate) setLong(item, stack, current + accepted * smallStep, smallScale)
        accepted
      } else {
        val (ticks, s) = state(item, stack, unitScale)
        val quantum = s / numerator * denominator
        val accepted = BigInt(offered).min((BigInt(item.maxCharge) * s - ticks).max(BigInt(0)) / quantum).toLong
        if (accepted > 0 && !simulate) set(item, stack, ticks + BigInt(accepted) * quantum, s)
        accepted
      }
    }

    def stored(item: ItemPoweredBase, stack: ItemStack): Long = {
      if (!valid) return 0L
      val current = ticksLong(item, stack, smallScale)
      if (current >= 0 && smallStep > 0) current / smallStep
      else {
        val (ticks, s) = state(item, stack, unitScale)
        (ticks / (s / numerator * denominator)).min(BigInt(Long.MaxValue)).toLong
      }
    }

    def capacity(item: ItemPoweredBase): Long = {
      if (!valid || item.maxCharge <= 0) return 0L
      if (smallScale > 0 && smallStep > 0 && item.maxCharge.toLong <= Long.MaxValue / smallScale)
        item.maxCharge.toLong * smallScale / smallStep
      else (BigInt(item.maxCharge) * numerator / denominator).min(BigInt(Long.MaxValue)).toLong
    }

    def storedDouble(item: ItemPoweredBase, stack: ItemStack): Double = {
      if (!valid) return 0D
      val (ticks, s) = state(item, stack, unitScale)
      quotient(ticks * numerator, s * denominator)
    }

    def capacityDouble(item: ItemPoweredBase): Double =
      if (!valid) 0D else quotient(BigInt(item.maxCharge).max(BigInt(0)) * numerator, denominator)

    def remainingDouble(item: ItemPoweredBase, stack: ItemStack): Double = {
      if (!valid) return 0D
      val (ticks, s) = state(item, stack, unitScale)
      quotient((BigInt(item.maxCharge) * s - ticks).max(BigInt(0)) * numerator, s * denominator)
    }

    // IC2 can discharge arbitrary fractional EU, including less than one configured tick.
    // Store that actual amount exactly rather than rounding away energy already taken from armor.
    def receiveDouble(item: ItemPoweredBase, stack: ItemStack, offered: Double, simulate: Boolean): Double = {
      if (!valid || offered <= 0 || offered.isNaN) return 0D
      val (ticks, s) = state(item, stack, unitScale)
      val room = (BigInt(item.maxCharge) * s - ticks).max(BigInt(0))
      val roomUnits = quotient(room * numerator, s * denominator)
      val accepted = math.min(offered, roomUnits)
      if (accepted > 0 && !simulate) {
        if (accepted == roomUnits) set(item, stack, BigInt(item.maxCharge) * s, s)
        else {
          val amount = java.math.BigDecimal.valueOf(accepted).stripTrailingZeros()
          val amountN = BigInt(amount.unscaledValue()) * BigInt(10).pow(math.max(0, -amount.scale())) * denominator
          val amountD = BigInt(10).pow(math.max(0, amount.scale())) * numerator
          val nextScale = lcm(s, amountD)
          set(item, stack, ticks * (nextScale / s) + amountN * (nextScale / amountD), nextScale)
        }
      }
      accepted
    }
  }
}

trait ItemPoweredCharge extends ItemPoweredBase {
  override def setCharge(stack: ItemStack, charge: Int): Unit = {
    ItemCharge.clearFraction(stack)
    super.setCharge(stack, charge)
  }

  override def useCharge(stack: ItemStack, uses: Int, player: EntityLivingBase): Unit =
    ItemCharge.consume(this, stack, uses.toLong * mjPerCharge)
}
