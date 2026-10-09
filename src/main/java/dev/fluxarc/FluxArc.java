package dev.fluxarc;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.creativetab.CreativeTabs;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.network.NetworkRegistry;
import dev.fluxarc.block.ArcBlock;
import dev.fluxarc.block.ArcItemBlock;
import dev.fluxarc.item.ArcItems;
import dev.fluxarc.tile.TileMachine;
import dev.fluxarc.recipe.RecipeCatalog;
import dev.fluxarc.challenge.ChallengeHooks;
import dev.fluxarc.gui.GuiHandler;
@Mod(modid=FluxArc.ID,name="FluxArc",version="0.1.1",dependencies="required-after:gregtech;required-after:gregtech_nh@[5.09.51.482];required-after:Forge@[10.13.4.1614,)",acceptedMinecraftVersions="[1.7.10]")
public class FluxArc {
 public static final String ID="fluxarc";
 @Mod.Instance(ID) public static FluxArc instance;
 @SidedProxy(clientSide="dev.fluxarc.client.ClientProxy",serverSide="dev.fluxarc.CommonProxy") public static CommonProxy proxy;
 public static Item items;public static Block controller,component;
 public static final CreativeTabs TAB=new CreativeTabs(ID){public Item getTabIconItem(){return Item.getItemFromBlock(controller);}};
 @Mod.EventHandler public void preInit(FMLPreInitializationEvent e){FluxConfig.load(e.getSuggestedConfigurationFile());
 items=new ArcItems();GameRegistry.registerItem(items,"parts");
 component=new ArcBlock(false);controller=new ArcBlock(true);
 GameRegistry.registerBlock(component,ArcItemBlock.class,"component");GameRegistry.registerBlock(controller,ArcItemBlock.class,"controller");
 GameRegistry.registerTileEntity(TileMachine.class,"fluxarc.machine");}
 @Mod.EventHandler public void init(FMLInitializationEvent e){NetworkRegistry.INSTANCE.registerGuiHandler(this,new GuiHandler());ChallengeHooks.init();proxy.init();}
 @Mod.EventHandler public void postInit(FMLPostInitializationEvent e){RecipeCatalog.init();}
 public static ItemStack stack(String id,int count){return new ItemStack(items,count,ArcItems.index(id));}
}
