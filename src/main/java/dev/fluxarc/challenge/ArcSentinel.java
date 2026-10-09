package dev.fluxarc.challenge;

import java.util.UUID;
import dev.fluxarc.tile.TileMachine;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

/** A session-bound combatant: no natural spawn, block grief, loot, XP or foreign targets. */
public class ArcSentinel extends EntityMob {
    private UUID session;
    private UUID owner;
    private int controllerX, controllerY, controllerZ;
    private int dissolveTicks;
    @Override protected void entityInit() { super.entityInit(); dataWatcher.addObject(20, Integer.valueOf(0)); }
    public float visualScale() { int phase=dataWatcher.getWatchableObjectInt(20);return Math.max(.01F,Math.min(1F,phase<0?-phase/12F:phase/20F)); }
    public void beginDissolve() { if(dissolveTicks==0){dissolveTicks=12;dataWatcher.updateObject(20,Integer.valueOf(-12));setAttackTarget(null);} }

    public ArcSentinel(World world) {
        super(world);
        setSize(0.65F, 1.8F);
        experienceValue = 0;
        isImmuneToFire = true;
        tasks.addTask(1, new EntityAIAttackOnCollide(this, EntityPlayer.class, 1.0D, false));
        setCustomNameTag("FluxArc Sentinel");
        func_110163_bv();
    }

    public void bind(TileMachine controller, UUID sessionId, UUID playerId, boolean guardian) {
        controllerX = controller.xCoord; controllerY = controller.yCoord; controllerZ = controller.zCoord;
        session = sessionId; owner = playerId;
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(guardian ? 50D : 24D);
        getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(guardian ? 6D : 3D);
        setHealth(getMaxHealth());
        setCustomNameTag(guardian ? "FluxArc Guardian" : "FluxArc Sentinel");
    }

    @Override protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.23D);
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(12D);
    }
    @Override protected boolean isAIEnabled() { return true; }
    @Override protected boolean canDespawn() { return false; }
    @Override protected void dropFewItems(boolean hit, int looting) {}
    @Override protected void dropRareDrop(int looting) {}
    @Override protected int getExperiencePoints(EntityPlayer player) { return 0; }
    @Override protected void dropEquipment(boolean hit, int looting) {}

    private ChallengeSession currentSession() {
        if (session == null || !worldObj.blockExists(controllerX, controllerY, controllerZ)) return null;
        TileEntity tile = worldObj.getTileEntity(controllerX, controllerY, controllerZ);
        if (!(tile instanceof TileMachine)) return null;
        ChallengeSession candidate = ((TileMachine) tile).challenge;
        return candidate.owns(session, getUniqueID()) ? candidate : null;
    }

    @Override public void onLivingUpdate() {
        if (!worldObj.isRemote) {
            if(dissolveTicks>0){dataWatcher.updateObject(20,Integer.valueOf(-dissolveTicks));if(--dissolveTicks==0)setDead();return;}
            ChallengeSession state = currentSession();
            if (state == null) { beginDissolve(); return; }
            dataWatcher.updateObject(20,Integer.valueOf(Math.min(20,ticksExisted)));
            if(ticksExisted<20)return;
            EntityPlayer target = state.player();
            if (target == null || !target.isEntityAlive() || !state.inside(this)) {
                state.fail("challenge.boundary"); beginDissolve(); return;
            }
            setAttackTarget(target);
        }
        super.onLivingUpdate();
    }

    @Override public boolean attackEntityFrom(DamageSource source, float amount) {
        if(dissolveTicks>0||ticksExisted<20)return false;
        Entity attacker = source.getEntity();
        if (!(attacker instanceof EntityPlayer) || owner == null
                || !owner.equals(attacker.getUniqueID())) return false;
        ChallengeSession state = currentSession();
        if (state == null || !state.inside(attacker)) return false;
        return super.attackEntityFrom(source, amount);
    }

    @Override public boolean attackEntityAsMob(Entity target) {
        if(dissolveTicks>0||ticksExisted<20)return false;
        if (!(target instanceof EntityPlayer) || owner == null || !owner.equals(target.getUniqueID())) return false;
        ChallengeSession state = currentSession();
        if (state == null || !state.inside(target) || !state.inside(this)) return false;
        return super.attackEntityAsMob(target);
    }

    @Override public void onDeath(DamageSource source) {
        ChallengeSession state = worldObj.isRemote ? null : currentSession();
        super.onDeath(source);
        if (state != null && source.getEntity() instanceof EntityPlayer
                && owner.equals(source.getEntity().getUniqueID())) state.defeated(getUniqueID());
    }

    @Override public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (session != null) tag.setString("ArcSession", session.toString());
        if (owner != null) tag.setString("ArcOwner", owner.toString());
        tag.setInteger("ArcX", controllerX); tag.setInteger("ArcY", controllerY); tag.setInteger("ArcZ", controllerZ);
    }
    @Override public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        try { session = UUID.fromString(tag.getString("ArcSession")); owner = UUID.fromString(tag.getString("ArcOwner")); }
        catch (IllegalArgumentException bad) { session = null; owner = null; }
        controllerX = tag.getInteger("ArcX"); controllerY = tag.getInteger("ArcY"); controllerZ = tag.getInteger("ArcZ");
    }
}
