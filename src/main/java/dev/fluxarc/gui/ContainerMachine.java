package dev.fluxarc.gui;
import dev.fluxarc.tile.TileMachine;
import net.minecraft.inventory.Container;import net.minecraft.inventory.Slot;import net.minecraft.entity.player.EntityPlayer;import net.minecraft.entity.player.InventoryPlayer;import net.minecraft.item.ItemStack;
public class ContainerMachine extends Container {
 public final TileMachine tile;
 public ContainerMachine(InventoryPlayer player,TileMachine t){tile=t;
 addSlotToContainer(new Slot(t,0,12,76));
 for(int i=0;i<8;i++)addSlotToContainer(new Slot(t,i+1,42+(i%4)*18,67+(i/4)*18));
 for(int i=0;i<6;i++)addSlotToContainer(new Slot(t,i+9,137+(i%3)*18,67+(i/3)*18){public boolean isItemValid(ItemStack s){return false;}});
 addSlotToContainer(new Slot(t,15,204,76));
 for(int r=0;r<3;r++)for(int c=0;c<9;c++)addSlotToContainer(new Slot(player,c+r*9+9,34+c*18,154+r*18));
 for(int c=0;c<9;c++)addSlotToContainer(new Slot(player,c,34+c*18,212));}
 @Override public boolean canInteractWith(EntityPlayer p){return tile.isUseableByPlayer(p);}
 @Override public boolean enchantItem(EntityPlayer p,int id){if(!canInteractWith(p)||id<0||id>3)return false;tile.action(p,id);return true;}
 @Override public ItemStack transferStackInSlot(EntityPlayer p,int index){Slot slot=(Slot)inventorySlots.get(index);if(!slot.getHasStack())return null;ItemStack stack=slot.getStack(),copy=stack.copy();if(index<16){if(!mergeItemStack(stack,16,52,true))return null;}else if(!mergeItemStack(stack,1,9,false))return null;if(stack.stackSize==0)slot.putStack(null);else slot.onSlotChanged();slot.onPickupFromSlot(p,stack);return copy;}
}
