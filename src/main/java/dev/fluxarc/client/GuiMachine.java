package dev.fluxarc.client;
import dev.fluxarc.gui.ContainerMachine;import dev.fluxarc.tile.TileMachine;import dev.fluxarc.recipe.Recipe;
import net.minecraft.client.gui.inventory.GuiContainer;import net.minecraft.client.gui.GuiButton;import net.minecraft.entity.player.InventoryPlayer;import net.minecraft.util.StatCollector;import org.lwjgl.opengl.GL11;
public class GuiMachine extends GuiContainer {
 private final TileMachine tile;
 public GuiMachine(InventoryPlayer p,TileMachine t){super(new ContainerMachine(p,t));tile=t;xSize=240;ySize=238;}
 public void initGui(){super.initGui();buttonList.clear();String[] labels={"启动","配方","停止","材料/结构"};for(int i=0;i<4;i++)buttonList.add(new GuiButton(i,guiLeft+8+i*57,guiTop+124,55,20,labels[i]));}
 public void updateScreen(){super.updateScreen();for(Object o:buttonList){GuiButton b=(GuiButton)o;if(b.id==0||b.id==1)b.enabled=!tile.isRunning();if(b.id==2)b.enabled=tile.isRunning();}}
 protected void actionPerformed(GuiButton b){mc.playerController.sendEnchantPacket(inventorySlots.windowId,b.id);}
 protected void drawGuiContainerBackgroundLayer(float p,int mx,int my){GL11.glColor4f(1,1,1,1);drawRect(guiLeft,guiTop,guiLeft+xSize,guiTop+ySize,0xff13232e);drawRect(guiLeft+2,guiTop+2,guiLeft+xSize-2,guiTop+25,0xff274653);
 for(Object ob:inventorySlots.inventorySlots){net.minecraft.inventory.Slot s=(net.minecraft.inventory.Slot)ob;drawRect(guiLeft+s.xDisplayPosition-1,guiTop+s.yDisplayPosition-1,guiLeft+s.xDisplayPosition+17,guiTop+s.yDisplayPosition+17,0xff08141c);}
 drawRect(guiLeft+10,guiTop+114,guiLeft+230,guiTop+118,0xff30434a);drawRect(guiLeft+10,guiTop+114,guiLeft+10+(int)(220*tile.animationProgress()),guiTop+118,0xff42ccd9);}
 protected void drawGuiContainerForegroundLayer(int x,int y){String name=StatCollector.translateToLocal("tile.fluxarc.controller."+tile.machineType()+".name");fontRendererObj.drawString(name,8,9,0xdef6f6);
 Recipe r=tile.isRunning()?tile.currentRecipe():tile.selectedRecipe();fontRendererObj.drawString(fontRendererObj.trimStringToWidth(r==null?"暂无可用配方":r.name,224),8,30,0xebebdd);
 String[] states={"待机","启动","加工","缺料 / 已停止","完成","结构不完整","断电暂停","输出已满"};fontRendererObj.drawString(states[Math.max(0,Math.min(7,tile.state))]+" | EU "+tile.energy(),8,43,0x8cdee7);
 fontRendererObj.drawString("蓝图       原料          产物      液桶",8,56,0x9fb5b8);
 String in=tile.inputTank.getFluid()==null?"空":tile.inputTank.getFluid().getLocalizedName();String out=tile.outputTank.getFluid()==null?"空":tile.outputTank.getFluid().getLocalizedName();fontRendererObj.drawString(fontRendererObj.trimStringToWidth("输入 "+in+" "+tile.inputTank.getFluidAmount()+" | 输出 "+out+" "+tile.outputTank.getFluidAmount(),225),8,101,0xa7b9b8);
 if(r!=null){String detail=tile.isRunning()?r.steps[Math.max(0,Math.min(r.steps.length-1,tile.stage))].name:(r.requiredCard==null?"无需蓝图":dev.fluxarc.FluxArc.stack(r.requiredCard,1).getDisplayName());if(!tile.isRunning()&&tile.statusDetail().length()>0)detail=StatCollector.translateToLocal("fluxarc.error."+tile.statusDetail());fontRendererObj.drawString(fontRendererObj.trimStringToWidth(detail,222),8,145,0x9bbcc2);}
 }
}
