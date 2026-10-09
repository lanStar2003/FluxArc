package dev.fluxarc.block;
import dev.fluxarc.FluxArc;
import dev.fluxarc.tile.TileMachine;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.client.renderer.texture.IIconRegister;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.List;
public class ArcBlock extends BlockContainer {
 private final boolean controller;@SideOnly(Side.CLIENT) private IIcon[] icons;
 public ArcBlock(boolean control){super(Material.iron);controller=control;setBlockName(control?"fluxarc.controller":"fluxarc.component");setHardness(5);setResistance(15);setCreativeTab(FluxArc.TAB);setHarvestLevel("pickaxe",2);}
 @Override public TileEntity createNewTileEntity(World w,int meta){return controller?new TileMachine():null;}
 @Override public int damageDropped(int meta){return meta;}
 @Override public int getRenderType(){return 0;}
 @Override public boolean isOpaqueCube(){return controller;}
 @Override public void setBlockBoundsBasedOnState(net.minecraft.world.IBlockAccess w,int x,int y,int z){int m=w.getBlockMetadata(x,y,z);if(controller||m==1)setBlockBounds(0,0,0,1,1,1);else if(m==4)setBlockBounds(0,.35f,0,1,.65f,1);else if(m==0)setBlockBounds(.2f,0,.2f,.8f,1,.8f);else setBlockBounds(.15f,0,.15f,.85f,1,.85f);}
 @Override public net.minecraft.util.AxisAlignedBB getCollisionBoundingBoxFromPool(World w,int x,int y,int z){setBlockBoundsBasedOnState(w,x,y,z);return super.getCollisionBoundingBoxFromPool(w,x,y,z);}
 @Override public void setBlockBoundsForItemRender(){setBlockBounds(0,0,0,1,1,1);}
 @Override public boolean onBlockActivated(World w,int x,int y,int z,EntityPlayer p,int side,float a,float b,float c){if(!controller)return false;if(!w.isRemote)p.openGui(FluxArc.instance,0,w,x,y,z);return true;}
 @Override public void breakBlock(World w,int x,int y,int z,net.minecraft.block.Block old,int meta){TileEntity t=w.getTileEntity(x,y,z);if(!w.isRemote&&t instanceof TileMachine){TileMachine m=(TileMachine)t;m.shutdown();for(int i=0;i<m.getSizeInventory();i++){ItemStack s=m.getStackInSlot(i);if(s!=null){w.spawnEntityInWorld(new EntityItem(w,x+.5,y+.5,z+.5,s.copy()));m.setInventorySlotContents(i,null);}}}super.breakBlock(w,x,y,z,old,meta);}
 @Override @SideOnly(Side.CLIENT) public void registerBlockIcons(IIconRegister r){icons=new IIcon[controller?6:5];for(int i=0;i<icons.length;i++)icons[i]=r.registerIcon("fluxarc:"+(controller?"controller_":"component_")+i);}
 @Override @SideOnly(Side.CLIENT) public IIcon getIcon(int side,int meta){return icons[Math.max(0,Math.min(icons.length-1,meta))];}
 @Override @SideOnly(Side.CLIENT) public void getSubBlocks(Item item,net.minecraft.creativetab.CreativeTabs tab,List list){for(int i=0;i<(controller?6:5);i++)list.add(new ItemStack(item,1,i));}
}
