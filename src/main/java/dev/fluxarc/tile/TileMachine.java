package dev.fluxarc.tile;

import java.util.List;
import java.util.UUID;
import dev.fluxarc.FluxArc;
import dev.fluxarc.FluxConfig;
import dev.fluxarc.core.MachineLedger;
import dev.fluxarc.recipe.*;
import dev.fluxarc.structure.StructureSpec;
import dev.fluxarc.challenge.ChallengeSession;
import gregtech.api.interfaces.tileentity.IEnergyConnected;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.Packet;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.*;

/** Server-owned escrow processing; no client packet can set energy, inventories, progress or rewards. */
public class TileMachine extends TileEntity implements ISidedInventory,IFluidHandler,IEnergyConnected {
 public int state=0,progress=0,totalTicks=0,stage=0,ticks=0;
 public boolean formed;
 public UUID operator;
 public final ChallengeSession challenge=new ChallengeSession(this);
 public final MachineLedger ledger=new MachineLedger(FluxConfig.capacity);
 private ItemStack[] inv=new ItemStack[16];
 public final FluidTank inputTank=new FluidTank(32000),outputTank=new FluidTank(32000);
 private String recipeId="",failure="";
 private int selection=0,euScale=100,timeScale=100,doneFlash;
 private ItemStack[] escrow=new ItemStack[0];
 private FluidStack escrowFluid;
 private boolean challengeVictory=false,loadedCheck=false;
 public int machineType(){return worldObj==null?getBlockMetadata():worldObj.getBlockMetadata(xCoord,yCoord,zCoord);}
 public boolean isRunning(){return ledger.active();}
 public float animationProgress(){return totalTicks>0?Math.min(1f,(float)progress/totalTicks):0;}
 public float phaseProgress(){Recipe r=currentRecipe();if(r==null)return 0;int index=Math.max(0,Math.min(r.steps.length-1,stage)),before=0;for(int i=0;i<index;i++)before+=scaledTicks(r.steps[i].duration);return Math.max(0,Math.min(1,(float)(progress-before)/scaledTicks(r.steps[index].duration)));}
 public Recipe currentRecipe(){return RecipeCatalog.find(recipeId);}
 public Recipe selectedRecipe(){List<Recipe> l=RecipeCatalog.forMachine(machineType());return l.isEmpty()?null:l.get(Math.floorMod(selection,l.size()));}
 public String statusDetail(){return failure;}
 public int selectedIndex(){return selection;}
 public long energy(){return ledger.energy();}
 @Override public void updateEntity(){
  ticks++;if(worldObj.isRemote)return;
  if(ticks%10==1||ledger.active())formed=StructureSpec.validate(this);
  if(loadedCheck){loadedCheck=false;if(machineType()==5&&ledger.active()&&!challengeVictory){ledger.abort();clearEscrow();failure="challenge.reload_aborted";state=3;}}
  consumeContainer();
  if(!ledger.active()){state=!formed?5:(doneFlash>0?4:(failure.length()>0?3:0));if(doneFlash>0)doneFlash--;sync();return;}
  Recipe r=currentRecipe();if(r==null||r.type!=machineType()){ledger.abort();clearEscrow();failure="unknown_recipe";state=3;sync();return;}
  stage=stageFor(r,ledger.progress());
  long cost=Math.max(1,(r.steps[stage].euPerTick*(long)euScale+99)/100);
  int result=ledger.advance(formed,cost);state=result;progress=ledger.progress();totalTicks=ledger.duration();
  if(machineType()==5&&!challengeVictory){
   if(result==5||result==6){challenge.cancel();}
   else {challenge.tick();if(ledger.ready()&&!challengeVictory)challenge.fail("challenge.timeout");}
  }
  if(formed&&ledger.ready()&&(machineType()!=5||challengeVictory)){
   if(commitOutput()){ledger.finish();clearEscrow();state=4;doneFlash=60;failure="";}else state=7;
  } else if(state==2&&progress<20)state=1;
  markDirty();sync();
 }
 private int scaledTicks(int base){return (int)Math.max(1,Math.min(10000000L,((long)base*timeScale+99)/100));}
 private int stageFor(Recipe r,int done){int boundary=0;for(int i=0;i<r.steps.length;i++){boundary+=scaledTicks(r.steps[i].duration);if(done<boundary)return i;}return r.steps.length-1;}
 public void action(EntityPlayer player,int id){
  if(worldObj.isRemote||!isUseableByPlayer(player))return;
  if(id==0){start(player);return;}
  if(id==1&&!ledger.active()){selection++;markDirty();worldObj.markBlockForUpdate(xCoord,yCoord,zCoord);}
  if(id==2&&ledger.active()&&(operator==null||operator.equals(player.getUniqueID()))){shutdown();failure="cancelled_reserved_inputs_lost";player.addChatMessage(new ChatComponentText("FluxArc: 已停止；预留材料不返还。"));}
  if(id==3){player.addChatMessage(new ChatComponentText(StructureSpec.summary(machineType())));Recipe r=selectedRecipe();if(r!=null){player.addChatMessage(new ChatComponentText(r.name+" / "+r.totalTicks()+" ticks / "+r.totalEu()+" EU"));for(ItemIngredient ingredient:r.inputs){ItemStack sample=ingredient.displayStack();player.addChatMessage(new ChatComponentText("- "+(sample==null?"missing ore":sample.getDisplayName())+" x"+ingredient.amount));}if(r.fluidName!=null)player.addChatMessage(new ChatComponentText(r.fluidName+": "+r.fluidAmount+" mB"));}}
 }
 private boolean start(EntityPlayer player){
  if(ledger.active())return false;
  formed=StructureSpec.validate(this);Recipe r=selectedRecipe();
  if(!formed||r==null){state=5;failure="structure_or_recipe";return false;}
  if(machineType()==5&&!FluxConfig.challenges){failure="challenge_disabled";return false;}
  if(r.requiredCard!=null){ItemStack expected=FluxArc.stack(r.requiredCard,1);if(inv[0]==null||!inv[0].isItemEqual(expected)){state=3;failure="missing_card";return false;}}
  int[] consume=new int[16];
  for(ItemIngredient ingredient:r.inputs){int left=ingredient.amount;for(int s=1;s<=8&&left>0;s++){if(inv[s]!=null&&ingredient.matches(inv[s])){int amount=Math.min(left,inv[s].stackSize-consume[s]);consume[s]+=amount;left-=amount;}}if(left>0){state=3;failure="missing_items";return false;}}
  FluidStack fluid=r.fluidName==null?null:FluidRegistry.getFluidStack(r.fluidName,r.fluidAmount);
  if(r.fluidAmount>0&&(fluid==null||inputTank.getFluid()==null||!inputTank.getFluid().isFluidEqual(fluid)||inputTank.getFluidAmount()<r.fluidAmount)){state=3;failure="missing_fluid";return false;}
  FluidStack out=r.outputFluid==null?null:FluidRegistry.getFluidStack(r.outputFluid,r.outputFluidAmount);
  if(r.outputFluidAmount>0&&out==null){failure="missing_output_fluid_registration";return false;}
  euScale=FluxConfig.euPercent;timeScale=FluxConfig.timePercent;
  int duration=0;for(Step s:r.steps)duration+=scaledTicks(s.duration);
  if(ledger.energy()<Math.max(1,((long)r.steps[0].euPerTick*euScale+99)/100)){state=6;failure="no_power";return false;}
  operator=player.getUniqueID();recipeId=r.id;challengeVictory=false;
  // Start the challenge only after all resources are validated, before the irreversible input reservation.
  // It cannot tick or reward between these lines: Minecraft runs this on one server thread.
  if(machineType()==5&&!challenge.start(player,r.id,duration)){failure="challenge_start_rejected";return false;}
  if(!ledger.begin(duration)){challenge.cancel();return false;}
  for(int s=1;s<=8;s++)if(consume[s]>0){inv[s].stackSize-=consume[s];if(inv[s].stackSize==0)inv[s]=null;}
  if(fluid!=null)inputTank.drain(fluid.amount,true);
  escrow=new ItemStack[r.outputs.length];for(int i=0;i<escrow.length;i++)escrow[i]=r.outputs[i].copy();escrowFluid=out;
  progress=0;totalTicks=duration;stage=0;state=1;failure="";markDirty();worldObj.markBlockForUpdate(xCoord,yCoord,zCoord);return true;
 }
 private static boolean same(ItemStack a,ItemStack b){return a.isItemEqual(b)&&ItemStack.areItemStackTagsEqual(a,b);}
 private boolean commitOutput(){
  ItemStack[] result=new ItemStack[6];for(int i=0;i<6;i++)result[i]=inv[i+9]==null?null:inv[i+9].copy();
  for(ItemStack output:escrow){int left=output.stackSize;for(int i=0;i<6&&left>0;i++){if(result[i]!=null&&same(result[i],output)){int n=Math.min(left,Math.max(0,Math.min(64,result[i].getMaxStackSize())-result[i].stackSize));result[i].stackSize+=n;left-=n;}}
   for(int i=0;i<6&&left>0;i++)if(result[i]==null){int n=Math.min(left,Math.min(64,output.getMaxStackSize()));result[i]=output.copy();result[i].stackSize=n;left-=n;}if(left>0)return false;
  }
  if(escrowFluid!=null&&outputTank.fill(escrowFluid,false)<escrowFluid.amount)return false;
  for(int i=0;i<6;i++)inv[i+9]=result[i];if(escrowFluid!=null)outputTank.fill(escrowFluid,true);return true;
 }
 private void consumeContainer(){ItemStack s=inv[15];if(s==null)return;FluidStack f=FluidContainerRegistry.getFluidForFilledItem(s);if(f==null||!canFill(ForgeDirection.UNKNOWN,f.getFluid())||inputTank.fill(f,false)!=f.amount)return;
  ItemStack empty=FluidContainerRegistry.drainFluidContainer(s);if(s.stackSize==1){inputTank.fill(f,true);inv[15]=empty;markDirty();}}
 private void clearEscrow(){escrow=new ItemStack[0];escrowFluid=null;recipeId="";progress=0;totalTicks=0;stage=0;challengeVictory=false;}
 public void challengeWon(){if(!worldObj.isRemote&&ledger.active()&&machineType()==5){challengeVictory=true;markDirty();}}
 public void challengeFailed(String reason){if(worldObj!=null&&!worldObj.isRemote){ledger.abort();clearEscrow();state=3;failure=reason;markDirty();}}
 public void shutdown(){challenge.cancel();ledger.abort();clearEscrow();markDirty();}
 @Override public void invalidate(){if(worldObj!=null&&!worldObj.isRemote)challenge.cancel();super.invalidate();}
 @Override public void onChunkUnload(){if(worldObj!=null&&!worldObj.isRemote)challenge.cancel();super.onChunkUnload();}
 private void sync(){if(ticks%10==0)worldObj.markBlockForUpdate(xCoord,yCoord,zCoord);}
 @Override public AxisAlignedBB getRenderBoundingBox(){return AxisAlignedBB.getBoundingBox(xCoord-5,yCoord,zCoord-8,xCoord+6,yCoord+10,zCoord+6);}
 @Override public double getMaxRenderDistanceSquared(){return 16384;}
 @Override public Packet getDescriptionPacket(){NBTTagCompound n=new NBTTagCompound();writeToNBT(n);return new S35PacketUpdateTileEntity(xCoord,yCoord,zCoord,1,n);}
 @Override public void onDataPacket(NetworkManager manager,S35PacketUpdateTileEntity packet){readFromNBT(packet.func_148857_g());}
 @Override public void writeToNBT(NBTTagCompound n){super.writeToNBT(n);NBTTagList list=new NBTTagList();for(int i=0;i<16;i++)if(inv[i]!=null){NBTTagCompound a=new NBTTagCompound();a.setInteger("slot",i);inv[i].writeToNBT(a);list.appendTag(a);}n.setTag("inventory",list);
 n.setLong("energy",ledger.energy());n.setInteger("progress",ledger.progress());n.setInteger("duration",ledger.duration());n.setBoolean("active",ledger.active());n.setLong("serial",ledger.serial());n.setString("recipe",recipeId);n.setInteger("selection",selection);n.setInteger("euScale",euScale);n.setInteger("timeScale",timeScale);n.setBoolean("victory",challengeVictory);
 n.setInteger("state",state);n.setInteger("stage",stage);n.setBoolean("formed",formed);n.setString("failure",failure);if(operator!=null)n.setString("operator",operator.toString());
 NBTTagList outputs=new NBTTagList();for(ItemStack s:escrow){NBTTagCompound a=new NBTTagCompound();s.writeToNBT(a);outputs.appendTag(a);}n.setTag("escrow",outputs);if(escrowFluid!=null)n.setTag("escrowFluid",escrowFluid.writeToNBT(new NBTTagCompound()));n.setTag("inputTank",inputTank.writeToNBT(new NBTTagCompound()));n.setTag("outputTank",outputTank.writeToNBT(new NBTTagCompound()));challenge.writeNBT(n);}
 @Override public void readFromNBT(NBTTagCompound n){super.readFromNBT(n);inv=new ItemStack[16];NBTTagList list=n.getTagList("inventory",10);for(int i=0;i<list.tagCount();i++){NBTTagCompound a=list.getCompoundTagAt(i);int slot=a.getInteger("slot");if(slot>=0&&slot<16)inv[slot]=ItemStack.loadItemStackFromNBT(a);}
 ledger.restore(n.getLong("energy"),n.getInteger("progress"),n.getInteger("duration"),n.getBoolean("active"),n.getLong("serial"));recipeId=n.getString("recipe");selection=Math.max(0,n.getInteger("selection"));euScale=Math.max(10,Math.min(1000,n.getInteger("euScale")));timeScale=Math.max(10,Math.min(1000,n.getInteger("timeScale")));challengeVictory=n.getBoolean("victory");state=n.getInteger("state");stage=n.getInteger("stage");formed=n.getBoolean("formed");failure=n.getString("failure");progress=ledger.progress();totalTicks=ledger.duration();
 String who=n.getString("operator");try{operator=who.length()==0?null:UUID.fromString(who);}catch(IllegalArgumentException e){operator=null;}
 NBTTagList outputs=n.getTagList("escrow",10);java.util.ArrayList<ItemStack> valid=new java.util.ArrayList<ItemStack>();for(int i=0;i<outputs.tagCount();i++){ItemStack s=ItemStack.loadItemStackFromNBT(outputs.getCompoundTagAt(i));if(s!=null&&s.stackSize>0)valid.add(s);}escrow=valid.toArray(new ItemStack[valid.size()]);escrowFluid=FluidStack.loadFluidStackFromNBT(n.getCompoundTag("escrowFluid"));inputTank.readFromNBT(n.getCompoundTag("inputTank"));outputTank.readFromNBT(n.getCompoundTag("outputTank"));challenge.readNBT(n);loadedCheck=true;
 }
 @Override public long injectEnergyUnits(ForgeDirection side,long voltage,long amperage){if(worldObj==null||worldObj.isRemote||!inputEnergyFrom(side))return 0;long accepted=ledger.inject(voltage,amperage,FluxConfig.maxVoltage);if(accepted>0)markDirty();return accepted;}
 @Override public boolean inputEnergyFrom(ForgeDirection side){return side!=ForgeDirection.UP;}
 @Override public boolean outputsEnergyTo(ForgeDirection side){return false;}
 @Override public byte getColorization(){return -1;}@Override public byte setColorization(byte c){return -1;}
 @Override public int fill(ForgeDirection side,FluidStack f,boolean execute){if(worldObj!=null&&worldObj.isRemote||f==null||!canFill(side,f.getFluid()))return 0;int n=inputTank.fill(f,execute);if(execute&&n>0)markDirty();return n;}
 @Override public FluidStack drain(ForgeDirection side,FluidStack f,boolean execute){return f!=null&&outputTank.getFluid()!=null&&outputTank.getFluid().isFluidEqual(f)?drain(side,f.amount,execute):null;}
 @Override public FluidStack drain(ForgeDirection side,int n,boolean execute){if(worldObj!=null&&worldObj.isRemote)return null;FluidStack f=outputTank.drain(n,execute);if(execute&&f!=null)markDirty();return f;}
 @Override public boolean canFill(ForgeDirection s,Fluid f){if(f==null)return false;for(Recipe r:RecipeCatalog.forMachine(machineType()))if(r.fluidName!=null&&r.fluidName.equals(f.getName()))return true;return false;}@Override public boolean canDrain(ForgeDirection s,Fluid f){return outputTank.getFluid()!=null&&outputTank.getFluid().getFluid()==f;}
 @Override public FluidTankInfo[] getTankInfo(ForgeDirection s){return new FluidTankInfo[]{inputTank.getInfo(),outputTank.getInfo()};}
 @Override public int getSizeInventory(){return 16;}@Override public ItemStack getStackInSlot(int i){return i>=0&&i<16?inv[i]:null;}
 @Override public ItemStack decrStackSize(int i,int n){if(n<=0)return null;ItemStack s=getStackInSlot(i);if(s==null)return null;ItemStack out=s.stackSize<=n?s:s.splitStack(n);if(s==out||s.stackSize==0)inv[i]=null;markDirty();return out;}
 @Override public ItemStack getStackInSlotOnClosing(int i){return null;}
 @Override public void setInventorySlotContents(int i,ItemStack s){if(i<0||i>=16)return;if(s!=null&&s.stackSize<=0)s=null;inv[i]=s;if(s!=null)s.stackSize=Math.min(Math.min(64,s.getMaxStackSize()),s.stackSize);markDirty();}
 @Override public String getInventoryName(){return "container.fluxarc";}@Override public boolean hasCustomInventoryName(){return false;}@Override public int getInventoryStackLimit(){return 64;}
 @Override public boolean isUseableByPlayer(EntityPlayer p){return worldObj.getTileEntity(xCoord,yCoord,zCoord)==this&&p.getDistanceSq(xCoord+.5,yCoord+.5,zCoord+.5)<=64;}
 @Override public void openInventory(){}@Override public void closeInventory(){}
 @Override public boolean isItemValidForSlot(int i,ItemStack s){return i>=0&&(i<=8||i==15);}
 @Override public int[] getAccessibleSlotsFromSide(int s){return s==0?new int[]{9,10,11,12,13,14}:new int[]{1,2,3,4,5,6,7,8,15};}
 @Override public boolean canInsertItem(int i,ItemStack s,int side){return i>=1&&i<=8||i==15&&FluidContainerRegistry.isFilledContainer(s);}
 @Override public boolean canExtractItem(int i,ItemStack s,int side){return i>=9&&i<=14;}
}
