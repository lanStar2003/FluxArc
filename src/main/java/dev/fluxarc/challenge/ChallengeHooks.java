package dev.fluxarc.challenge;

import cpw.mods.fml.common.registry.EntityRegistry;
import dev.fluxarc.FluxArc;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

/** Server-safe registration; no renderer or client classes are loaded here. */
public final class ChallengeHooks {
    private ChallengeHooks() {}
    public static void init() {
        EntityRegistry.registerModEntity(ArcSentinel.class, "arc_sentinel", 1,
                FluxArc.instance, 64, 3, true);
        MinecraftForge.EVENT_BUS.register(new ChallengeHooks());
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void preventDrops(LivingDropsEvent event) {
        if (event.entityLiving instanceof ArcSentinel) { event.drops.clear(); event.setCanceled(true); }
    }
}
