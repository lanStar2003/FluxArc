package dev.fluxarc.gui;
import cpw.mods.fml.common.network.IGuiHandler;import net.minecraft.entity.player.EntityPlayer;import net.minecraft.world.World;import net.minecraft.tileentity.TileEntity;import dev.fluxarc.tile.TileMachine;
public class GuiHandler implements IGuiHandler {
 public Object getServerGuiElement(int id,EntityPlayer p,World w,int x,int y,int z){TileEntity t=w.getTileEntity(x,y,z);return t instanceof TileMachine?new ContainerMachine(p.inventory,(TileMachine)t):null;}
 @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
 public Object getClientGuiElement(int id,EntityPlayer p,World w,int x,int y,int z){TileEntity t=w.getTileEntity(x,y,z);return t instanceof TileMachine?new dev.fluxarc.client.GuiMachine(p.inventory,(TileMachine)t):null;}
}
