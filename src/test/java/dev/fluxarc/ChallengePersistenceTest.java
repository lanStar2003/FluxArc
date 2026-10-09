package dev.fluxarc;

import dev.fluxarc.tile.TileMachine;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;
import static org.junit.Assert.*;

/** Tests the real session serializer without bootstrapping Forge's global item/fluid registries. */
public class ChallengePersistenceTest {
    @Test public void activeSavedChallengeFailsClosedInsteadOfResumingOrRewarding() {
        NBTTagCompound saved = new NBTTagCompound();
        NBTTagCompound session = new NBTTagCompound();
        session.setBoolean("Active", true); session.setInteger("Completed", 3); session.setInteger("Total", 4);
        session.setString("Session", "malformed-id"); session.setString("Owner", "malformed-id");
        saved.setTag("ArcChallenge", session);
        TileMachine restored = new TileMachine(); restored.challenge.readNBT(saved);
        assertFalse(restored.challenge.isActive()); assertNull(restored.challenge.sessionId());
        assertEquals("challenge.reload_aborted", restored.challenge.lastFailure());
        assertFalse(restored.ledger.ready()); assertEquals(0, restored.challenge.ticksRemaining());
        NBTTagCompound nextSave = new NBTTagCompound(); restored.challenge.writeNBT(nextSave);
        assertFalse(nextSave.getCompoundTag("ArcChallenge").getBoolean("Active"));
        TileMachine reloadedAgain = new TileMachine(); reloadedAgain.challenge.readNBT(nextSave);
        assertFalse(reloadedAgain.challenge.isActive()); assertFalse(reloadedAgain.ledger.ready());
    }
}
