package dev.fluxarc.client;
import dev.fluxarc.CommonProxy;import dev.fluxarc.tile.TileMachine;import dev.fluxarc.challenge.ArcSentinel;
import cpw.mods.fml.client.registry.ClientRegistry;import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.client.renderer.entity.RenderBiped;import net.minecraft.client.model.ModelBiped;import net.minecraft.entity.Entity;import net.minecraft.util.ResourceLocation;
public class ClientProxy extends CommonProxy {public void init(){ClientRegistry.bindTileEntitySpecialRenderer(TileMachine.class,new ArcRenderer());RenderingRegistry.registerEntityRenderingHandler(ArcSentinel.class,new RenderBiped(new ModelBiped(),.3f){protected ResourceLocation getEntityTexture(Entity e){return new ResourceLocation("fluxarc","textures/entity/sentinel.png");}protected void preRenderCallback(net.minecraft.entity.EntityLivingBase e,float partial){float s=((ArcSentinel)e).visualScale();org.lwjgl.opengl.GL11.glScalef(.5f+.5f*s,s,.5f+.5f*s);}});}}
