package dev.fluxarc.item;
import dev.fluxarc.FluxArc;
import net.minecraft.item.Item;import net.minecraft.item.ItemStack;import net.minecraft.entity.player.EntityPlayer;import net.minecraft.util.IIcon;
import net.minecraft.client.renderer.texture.IIconRegister;import cpw.mods.fml.relauncher.Side;import cpw.mods.fml.relauncher.SideOnly;import java.util.List;
public class ArcItems extends Item {
 public static final String[] IDS={"blueprint_basic","blueprint_precision","blueprint_quantum","process_steel","process_copper","process_recycle","probe","sensor_payload","sample_payload","deep_payload","research_data","orbital_sample","deep_sample","relay","precision_core","quantum_core","tactical_data","field_key","ceramic_plate","gravity_coil"};
 @SideOnly(Side.CLIENT) private IIcon[] icons;
 public ArcItems(){setHasSubtypes(true);setMaxDamage(0);setCreativeTab(FluxArc.TAB);setUnlocalizedName("fluxarc.parts");}
 public static int index(String name){for(int i=0;i<IDS.length;i++)if(IDS[i].equals(name))return i;throw new IllegalArgumentException("Unknown FluxArc item: "+name);}
 public String getUnlocalizedName(ItemStack s){return "item.fluxarc."+IDS[Math.max(0,Math.min(IDS.length-1,s.getItemDamage()))];}
 @SideOnly(Side.CLIENT) public void registerIcons(IIconRegister r){icons=new IIcon[IDS.length];for(int i=0;i<icons.length;i++)icons[i]=r.registerIcon("fluxarc:"+IDS[i]);}
 @SideOnly(Side.CLIENT) public IIcon getIconFromDamage(int i){return icons[Math.max(0,Math.min(icons.length-1,i))];}
 @SideOnly(Side.CLIENT) public void getSubItems(Item i,net.minecraft.creativetab.CreativeTabs t,List l){for(int k=0;k<IDS.length;k++)l.add(new ItemStack(i,1,k));}
 @SideOnly(Side.CLIENT) public void addInformation(ItemStack s,EntityPlayer p,List l,boolean a){l.add(net.minecraft.util.StatCollector.translateToLocal(getUnlocalizedName(s)+".desc"));}
}
