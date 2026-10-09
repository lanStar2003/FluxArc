package dev.fluxarc;

import dev.fluxarc.tile.TileMachine;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/** Actual NBT serializers and tanks, without pretending to run a Minecraft world. */
public class TilePersistenceTest {
    private static final Item MATERIAL = new Item();
    private static final Fluid COOLANT = new Fluid("fluxarc_test_coolant");
    private static final Fluid WASTE = new Fluid("fluxarc_test_waste");

    @BeforeClass public static void registerFixtures() throws Exception {
        // Tiny isolated registries are enough for real ItemStack/FluidStack NBT IDs.
        Item.itemRegistry.addObject(31000, "fluxarc_test:material", MATERIAL);
        FluidRegistry.registerFluid(COOLANT); FluidRegistry.registerFluid(WASTE);
        Method mapping = TileEntity.class.getDeclaredMethod("addMapping", Class.class, String.class);
        mapping.setAccessible(true); mapping.invoke(null, TileMachine.class, "fluxarc_test_machine");
    }

    @Test public void midJobNbtPreservesInventoryFluidsEscrowAndAccounting() throws Exception {
        TileMachine original = new TileMachine();
        original.xCoord = 10; original.yCoord = 64; original.zCoord = -12;
        original.operator = UUID.randomUUID();
        ItemStack material = new ItemStack(MATERIAL, 7, 2);
        material.setTagCompound(new NBTTagCompound()); material.getTagCompound().setString("lot", "unique");
        original.setInventorySlotContents(2, material);
        original.inputTank.fill(new FluidStack(COOLANT, 2500), true);
        original.outputTank.fill(new FluidStack(WASTE, 1000), true);
        set(original, "escrow", new ItemStack[]{new ItemStack(MATERIAL, 3, 5)});
        set(original, "escrowFluid", new FluidStack(WASTE, 600));
        set(original, "recipeId", "test_snapshot");
        original.ledger.inject(128, 20, 128); original.ledger.begin(10);
        original.ledger.advance(true, 32); original.ledger.advance(true, 128);
        TileMachine copy = reload(original);
        assertEquals(original.operator, copy.operator);
        assertEquals(10, copy.xCoord); assertEquals(64, copy.yCoord); assertEquals(-12, copy.zCoord);
        assertEquals(7, copy.getStackInSlot(2).stackSize);
        assertEquals("unique", copy.getStackInSlot(2).getTagCompound().getString("lot"));
        assertEquals(2500, copy.inputTank.getFluidAmount()); assertSame(COOLANT, copy.inputTank.getFluid().getFluid());
        assertEquals(1000, copy.outputTank.getFluidAmount()); assertSame(WASTE, copy.outputTank.getFluid().getFluid());
        assertEquals(2400, copy.energy()); assertEquals(2, copy.ledger.progress()); assertEquals(10, copy.ledger.duration());
        assertEquals(1, copy.ledger.serial()); assertTrue(copy.ledger.active());
        ItemStack[] escrow = (ItemStack[]) get(copy, "escrow");
        assertEquals(1, escrow.length); assertEquals(3, escrow[0].stackSize); assertEquals(5, escrow[0].getItemDamage());
        assertEquals(600, ((FluidStack)get(copy, "escrowFluid")).amount);
    }

    @Test public void fullFluidTankBlocksItemsAndCompletedSaveCannotRepeatReward() throws Exception {
        TileMachine tile = new TileMachine();
        tile.outputTank.fill(new FluidStack(WASTE, 32000), true);
        set(tile, "escrow", new ItemStack[]{new ItemStack(MATERIAL, 2)});
        set(tile, "escrowFluid", new FluidStack(WASTE, 250));
        tile.ledger.inject(32, 1, 32); tile.ledger.begin(1); tile.ledger.advance(true, 32);
        assertFalse((Boolean) call(tile, "commitOutput")); assertNull(tile.getStackInSlot(9));
        TileMachine ready = reload(tile);
        assertTrue(ready.ledger.ready()); assertEquals(0, ready.energy());
        ready.outputTank.drain(250, true);
        assertTrue((Boolean)call(ready, "commitOutput"));
        assertTrue(ready.ledger.finish()); call(ready, "clearEscrow");
        TileMachine done = reload(ready);
        assertFalse(done.ledger.active()); assertFalse(done.ledger.finish());
        assertEquals(2, done.getStackInSlot(9).stackSize); assertEquals(32000, done.outputTank.getFluidAmount());
        // Even accidental planner invocation after clearing cannot emit a second reward.
        call(done, "commitOutput");
        assertEquals(2, done.getStackInSlot(9).stackSize); assertEquals(32000, done.outputTank.getFluidAmount());
        assertEquals(0, ((ItemStack[]) get(done, "escrow")).length); assertNull(get(done, "escrowFluid"));
    }

    @Test public void challengeRosterIsNeverResurrectedFromNbt() throws Exception {
        TileMachine tile = new TileMachine(); NBTTagCompound saved = new NBTTagCompound(); tile.writeToNBT(saved);
        NBTTagCompound session = saved.getCompoundTag("ArcChallenge"); session.setBoolean("Active", true);
        session.setString("Session", UUID.randomUUID().toString()); session.setString("Owner", UUID.randomUUID().toString());
        session.setInteger("Total", 4); session.setInteger("Completed", 3);
        TileMachine loaded = new TileMachine(); loaded.readFromNBT(saved);
        assertFalse(loaded.challenge.isActive()); assertNull(loaded.challenge.sessionId());
        assertEquals("challenge.reload_aborted", loaded.challenge.lastFailure());
        assertFalse(loaded.ledger.ready());
    }

    private static TileMachine reload(TileMachine tile) {
        NBTTagCompound nbt = new NBTTagCompound(); tile.writeToNBT(nbt);
        TileMachine copy = new TileMachine(); copy.readFromNBT(nbt); return copy;
    }
    private static Object get(TileMachine tile, String name) throws Exception {
        Field f = TileMachine.class.getDeclaredField(name); f.setAccessible(true); return f.get(tile);
    }
    private static void set(TileMachine tile, String name, Object value) throws Exception {
        Field f = TileMachine.class.getDeclaredField(name); f.setAccessible(true); f.set(tile, value);
    }
    private static Object call(TileMachine tile, String name) throws Exception {
        Method m = TileMachine.class.getDeclaredMethod(name); m.setAccessible(true); return m.invoke(tile);
    }
}
