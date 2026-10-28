package net.bdew.gendustry.top

import io.netty.buffer.ByteBuf
import mcjty.theoneprobe.api.{IElement, IElementFactory}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.{EnumFacing, ResourceLocation}
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.common.network.ByteBufUtils

/** TOP transports the same GUI snapshot that Waila supplies to the client tile. */
class TopElement(val dimension: Int, val pos: BlockPos, val block: ResourceLocation,
                 val side: EnumFacing, val guiData: NBTTagCompound) extends IElement {
  private[top] var clientTile: TileEntity = null
  private[top] var clientLines: Vector[String] = null
  private[top] var detailed = false
  private[top] var language: String = null
  private[top] var units: String = null
  private[top] var multiplier = 0F

  override def getID: Int = TopRegistration.elementId

  override def toBytes(buf: ByteBuf): Unit = {
    buf.writeInt(dimension)
    buf.writeLong(pos.toLong)
    ByteBufUtils.writeUTF8String(buf, block.toString)
    buf.writeByte(if (side == null) -1 else side.getIndex)
    ByteBufUtils.writeTag(buf, guiData)
  }

  override def getWidth: Int = TopClient.width(this)
  override def getHeight: Int = TopClient.height(this)
  override def render(x: Int, y: Int): Unit = TopClient.render(this, x, y)
}

object TopElementFactory extends IElementFactory {
  override def createElement(buf: ByteBuf): IElement = {
    val dimension = buf.readInt()
    val pos = BlockPos.fromLong(buf.readLong())
    val block = new ResourceLocation(ByteBufUtils.readUTF8String(buf))
    val sideIndex = buf.readByte().toInt
    val side = if (sideIndex >= 0 && sideIndex < 6) EnumFacing.getFront(sideIndex) else null
    new TopElement(dimension, pos, block, side, ByteBufUtils.readTag(buf))
  }
}
