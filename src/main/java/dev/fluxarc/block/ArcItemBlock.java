package dev.fluxarc.block;
import net.minecraft.block.Block;import net.minecraft.item.ItemBlock;import net.minecraft.item.ItemStack;
public class ArcItemBlock extends ItemBlock {public ArcItemBlock(Block b){super(b);setHasSubtypes(true);setMaxDamage(0);}public int getMetadata(int m){return m;}public String getUnlocalizedName(ItemStack s){return super.getUnlocalizedName()+"."+s.getItemDamage();}}
