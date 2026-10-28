package net.bdew.gendustry.top

import net.bdew.gendustry.Gendustry
import net.bdew.gendustry.compat.TooltipText
import net.bdew.gendustry.config.Config
import net.bdew.gendustry.machines.apiary.TileApiary
import net.bdew.gendustry.power.TilePowered
import net.bdew.lib.data.base.{TileDataSlots, UpdateKind}
import net.minecraft.client.Minecraft
import net.minecraft.util.text.TextFormatting

import scala.util.control.NonFatal

/** Called only by TOP's client-side layout/render methods, never by packet decoding. */
object TopClient {
  private def lines(element: TopElement): Vector[String] = {
    val mc = Minecraft.getMinecraft
    if (mc.world == null || mc.player == null || mc.world.provider.getDimension != element.dimension ||
      !mc.world.isBlockLoaded(element.pos) || mc.world.getBlockState(element.pos).getBlock.getRegistryName != element.block)
      return Vector.empty

    mc.world.getTileEntity(element.pos) match {
      case tile: TilePowered with TileDataSlots =>
        val sneaking = mc.player.isSneaking
        if ((element.clientTile ne tile) || element.clientLines == null || element.detailed != sneaking ||
          element.language != mc.gameSettings.language || element.units != Config.powerShowUnits ||
          element.multiplier != Config.powerShowMultiplier) {
          try {
            if ((element.clientTile ne tile) && element.guiData != null)
              tile.doLoad(UpdateKind.GUI, element.guiData)
            var result = if (element.guiData == null) Vector.empty[String] else TooltipText.dataSlots(tile, showPower = false).toVector
            tile match {
              case apiary: TileApiary => result ++= TooltipText.apiary(apiary, element.side, sneaking, showShiftHint = false)
              case _ =>
            }
            element.clientLines = result
          } catch {
            case NonFatal(e) =>
              Gendustry.logWarnException("Error in TOP handler", e)
              element.clientLines = Vector("[%s%s%s]".format(TextFormatting.RED, e.toString, TextFormatting.RESET))
          }
          element.clientTile = tile
          element.detailed = sneaking
          element.language = mc.gameSettings.language
          element.units = Config.powerShowUnits
          element.multiplier = Config.powerShowMultiplier
        }
        element.clientLines
      case _ => Vector.empty
    }
  }

  def width(element: TopElement): Int = {
    val font = Minecraft.getMinecraft.fontRenderer
    val text = lines(element)
    var width = 0
    var index = 0
    while (index < text.size) {
      width = math.max(width, font.getStringWidth(text(index)))
      index += 1
    }
    width
  }

  def height(element: TopElement): Int = {
    val count = lines(element).size
    if (count == 0) 0 else count * 12 - 2
  }

  def render(element: TopElement, x: Int, y: Int): Unit = {
    val font = Minecraft.getMinecraft.fontRenderer
    val text = lines(element)
    var index = 0
    while (index < text.size) {
      font.drawStringWithShadow(text(index), x.toFloat, (y + index * 12).toFloat, 0xFFFFFF)
      index += 1
    }
  }
}
