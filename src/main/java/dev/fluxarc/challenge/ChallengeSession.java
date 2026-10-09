package dev.fluxarc.challenge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import dev.fluxarc.FluxConfig;
import dev.fluxarc.tile.TileMachine;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;

/** One authoritative finite challenge; rewards are delegated to the machine's output transaction. */
public final class ChallengeSession {
    private final TileMachine controller;
    private final Set<UUID> pending = new HashSet<UUID>();
    private UUID session, owner;
    private boolean active;
    private int total, completed, remaining;
    private String lastFailure = "";
    public ChallengeSession(TileMachine controller) { this.controller = controller; }
    public boolean isActive() { return active; }
    public UUID sessionId() { return session; }
    public int total() { return total; }
    public int completed() { return completed; }
    public int ticksRemaining() { return remaining; }
    public String lastFailure() { return lastFailure; }

    public EntityPlayer player() {
        if (owner == null || controller.getWorldObj() == null) return null;
        for (Object value : controller.getWorldObj().playerEntities) {
            EntityPlayer player = (EntityPlayer) value;
            if (owner.equals(player.getUniqueID())) return player;
        }
        return null;
    }
    public boolean inside(Entity entity) {
        return entity.worldObj == controller.getWorldObj()
                && Math.abs(entity.posX - (controller.xCoord + 0.5D)) <= 3.5D
                && Math.abs(entity.posZ - (controller.zCoord + 0.5D)) <= 3.5D
                && entity.posY >= controller.yCoord && entity.posY <= controller.yCoord + 4D;
    }
    public boolean owns(UUID id, UUID entity) { return active && session != null && session.equals(id) && pending.contains(entity); }

    public boolean start(EntityPlayer player, String recipeId, int duration) {
        World world = controller.getWorldObj();
        if (!FluxConfig.challenges || world == null || world.isRemote || active || !controller.formed || player == null
                || !player.isEntityAlive() || !inside(player) || world.difficultySetting == EnumDifficulty.PEACEFUL) return false;
        boolean guardian = "guardian_trial".equals(recipeId);
        if (!guardian && !"sentinel_trial".equals(recipeId)) return false;
        // Require a solid, clear 7x7 arena. Never alter blocks or load an absent chunk.
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            int x = controller.xCoord + dx, z = controller.zCoord + dz, y = controller.yCoord;
            if (!world.blockExists(x, y, z) || !World.doesBlockHaveSolidTopSurface(world, x, y, z)
                    || !world.isAirBlock(x, y + 1, z) || !world.isAirBlock(x, y + 2, z)) return false;
        }
        UUID next = UUID.randomUUID();
        ArrayList<ArcSentinel> spawned = new ArrayList<ArcSentinel>();
        int[][] points = { {-2, -2}, {2, 2}, {-2, 2}, {2, -2} };
        for (int i = 0; i < (guardian ? 4 : 2); i++) {
            ArcSentinel enemy = new ArcSentinel(world);
            enemy.bind(controller, next, player.getUniqueID(), guardian);
            enemy.setPosition(controller.xCoord + points[i][0] + 0.5D, controller.yCoord + 1D,
                    controller.zCoord + points[i][1] + 0.5D);
            if (!world.getCollidingBoundingBoxes(enemy, enemy.boundingBox).isEmpty()) return false;
            spawned.add(enemy);
        }
        session = next; owner = player.getUniqueID(); active = true;
        remaining = Math.max(1, duration); total = spawned.size(); completed = 0;
        pending.clear(); lastFailure = "";
        for (ArcSentinel enemy : spawned) pending.add(enemy.getUniqueID());
        for (ArcSentinel enemy : spawned) {
            if (!world.spawnEntityInWorld(enemy)) { fail("challenge.spawn_failed"); return false; }
        }
        controller.markDirty();
        return true;
    }

    public void tick() {
        if (!active || controller.getWorldObj().isRemote) return;
        EntityPlayer player = player();
        if (!FluxConfig.challenges || !controller.formed || !controller.isRunning()) { fail("challenge.stopped"); return; }
        if (player == null || !player.isEntityAlive() || !inside(player)) { fail("challenge.owner_absent"); return; }
        if (--remaining <= 0) { fail("challenge.timeout"); return; }
        // Removal/unload is failure, never a kill. Includes externally removed entities.
        Set<UUID> present = new HashSet<UUID>();
        for (Object object : controller.getWorldObj().loadedEntityList) {
            if (object instanceof ArcSentinel && !((Entity) object).isDead) present.add(((Entity) object).getUniqueID());
        }
        if (!present.containsAll(pending)) { fail("challenge.combatant_missing"); return; }
        controller.markDirty();
    }

    public void defeated(UUID entity) {
        if (!active || !pending.remove(entity)) return;
        completed++;
        if (pending.isEmpty()) {
            active = false; // Close before callback: duplicate death events cannot pay again.
            cleanup(); controller.markDirty(); controller.challengeWon();
        }
    }
    public void cancel() { fail("challenge.cancelled"); }
    public void fail(String reason) {
        if (!active) return;
        active = false; lastFailure = reason;
        cleanup(); controller.markDirty(); controller.challengeFailed(reason);
    }
    private void cleanup() {
        World world = controller.getWorldObj();
        if (world != null) for (Object value : new ArrayList<Object>(world.loadedEntityList)) {
            Entity entity = (Entity) value;
            if (entity instanceof ArcSentinel && pending.contains(entity.getUniqueID())) ((ArcSentinel) entity).beginDissolve();
        }
        pending.clear();
    }
    public void writeNBT(NBTTagCompound root) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("Active", active); tag.setInteger("Completed", completed);
        tag.setInteger("Total", total); tag.setInteger("Remaining", remaining);
        if (session != null) tag.setString("Session", session.toString());
        if (owner != null) tag.setString("Owner", owner.toString());
        tag.setString("Failure", lastFailure); root.setTag("ArcChallenge", tag);
    }
    public void readNBT(NBTTagCompound root) {
        NBTTagCompound tag = root.getCompoundTag("ArcChallenge");
        // Sessions deliberately cannot resume: their transient entity roster may be in another chunk.
        active = false; pending.clear(); session = null; owner = null;
        total = tag.getInteger("Total"); completed = tag.getInteger("Completed"); remaining = 0;
        lastFailure = tag.getBoolean("Active") ? "challenge.reload_aborted" : tag.getString("Failure");
    }
}
