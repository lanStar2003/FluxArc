package dev.fluxarc;

import dev.fluxarc.tile.TileMachine;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.junit.Test;
import static org.junit.Assert.*;

/** Exercises the actual controller output planner using real Minecraft ItemStacks. */
public class TileTransactionTest {
    private final Item first = new Item();
    private final Item second = new Item();

    @Test public void failureOnSecondOutputDoesNotPartiallyCommitFirst() throws Exception {
        TileMachine tile = new TileMachine();
        for (int slot = 9; slot <= 14; slot++) tile.setInventorySlotContents(slot, new ItemStack(first, 64));
        tile.setInventorySlotContents(9, new ItemStack(first, 63));
        outputs(tile, new ItemStack(first, 1), new ItemStack(second, 1));
        assertFalse(commit(tile));
        assertEquals(63, tile.getStackInSlot(9).stackSize);
        for (int slot = 10; slot <= 14; slot++) assertEquals(64, tile.getStackInSlot(slot).stackSize);
        tile.setInventorySlotContents(14, null);
        assertTrue(commit(tile));
        assertEquals(64, tile.getStackInSlot(9).stackSize);
        assertSame(second, tile.getStackInSlot(14).getItem());
        assertEquals(1, tile.getStackInSlot(14).stackSize);
    }

    @Test public void outputsRespectItemsWhoseStackLimitIsOne() throws Exception {
        Item singleton = new Item().setMaxStackSize(1);
        TileMachine tile = new TileMachine();
        outputs(tile, new ItemStack(singleton, 3));
        assertTrue(commit(tile));
        for (int slot = 9; slot <= 11; slot++) assertEquals(1, tile.getStackInSlot(slot).stackSize);
        assertNull(tile.getStackInSlot(12));
    }

    @Test public void negativeExtractionCannotManufactureItems() {
        TileMachine tile = new TileMachine();
        tile.setInventorySlotContents(1, new ItemStack(first, 5));
        assertNull(tile.decrStackSize(1, -10));
        assertNull(tile.decrStackSize(1, 0));
        assertEquals(5, tile.getStackInSlot(1).stackSize);
        assertEquals(2, tile.decrStackSize(1, 2).stackSize);
        assertEquals(3, tile.getStackInSlot(1).stackSize);
    }

    private static void outputs(TileMachine tile, ItemStack... stacks) throws Exception {
        Field field = TileMachine.class.getDeclaredField("escrow"); field.setAccessible(true); field.set(tile, stacks);
    }
    private static boolean commit(TileMachine tile) throws Exception {
        Method method = TileMachine.class.getDeclaredMethod("commitOutput"); method.setAccessible(true);
        return (Boolean) method.invoke(tile);
    }
}
