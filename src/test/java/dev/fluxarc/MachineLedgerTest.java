package dev.fluxarc;

import dev.fluxarc.core.MachineLedger;
import org.junit.Test;
import static org.junit.Assert.*;

/** Contract tests for the production ledger; inventory/world checks require integration tests. */
public class MachineLedgerTest {
    @Test public void failedStartAndDuplicateClickCannotResetPaidProgress() {
        MachineLedger l = new MachineLedger(1000);
        assertFalse(l.begin(0)); assertFalse(l.begin(-1));
        assertTrue(l.begin(3));
        l.inject(100, 4, 100);
        assertEquals(MachineLedger.RUNNING, l.advance(true, 100));
        assertFalse(l.begin(3));
        assertEquals(1, l.progress()); assertEquals(300, l.energy()); assertEquals(1, l.serial());
        assertFalse(l.finish());
    }

    @Test public void powerAndStructurePausesPreserveBothProgressAndEnergy() {
        MachineLedger l = new MachineLedger(1000);
        l.begin(2); l.inject(32, 1, 32);
        assertEquals(MachineLedger.INVALID, l.advance(false, 32));
        assertEquals(0, l.progress()); assertEquals(32, l.energy());
        assertEquals(MachineLedger.NO_POWER, l.advance(true, 33));
        assertEquals(0, l.progress()); assertEquals(32, l.energy());
        assertEquals(MachineLedger.RUNNING, l.advance(true, 32));
        assertEquals(1, l.progress()); assertEquals(0, l.energy());
        for (int i = 0; i < 50; i++) assertEquals(MachineLedger.NO_POWER, l.advance(true, 32));
        assertEquals(1, l.progress());
    }

    @Test public void fullOutputAndRepeatedCompletionCannotConsumeOrRewardTwice() {
        MachineLedger l = new MachineLedger(1000);
        l.begin(2); l.inject(100, 5, 100);
        l.advance(true, 70); l.advance(true, 90);
        assertTrue(l.ready());
        for (int i = 0; i < 1000; i++) assertEquals(MachineLedger.OUTPUT_FULL, l.advance(true, 100));
        assertEquals(340, l.energy()); assertEquals(2, l.progress());
        assertTrue(l.finish()); assertFalse(l.finish()); assertFalse(l.ready());
        assertEquals(MachineLedger.IDLE, l.advance(true, 100));
        assertEquals(340, l.energy());
    }

    @Test public void restoreMidJobAndReadyJobPreserveExactlyRemainingWork() {
        MachineLedger original = new MachineLedger(1000);
        original.inject(100, 6, 100); original.begin(3); original.advance(true, 100);
        MachineLedger restored = reload(original);
        restored.advance(true, 100); restored.advance(true, 100);
        assertTrue(restored.ready()); assertEquals(300, restored.energy());
        MachineLedger ready = reload(restored);
        assertTrue(ready.ready()); assertEquals(MachineLedger.OUTPUT_FULL, ready.advance(true, 100));
        assertTrue(ready.finish());
        MachineLedger complete = reload(ready);
        assertFalse(complete.ready()); assertFalse(complete.finish()); assertEquals(300, complete.energy());
    }

    @Test public void abortNeverRefundsSpentEnergyOrAllowsCompletion() {
        MachineLedger l = new MachineLedger(1000);
        l.inject(100, 5, 100); l.begin(2); l.advance(true, 100); l.abort();
        assertEquals(400, l.energy()); assertFalse(l.finish()); assertFalse(reload(l).active());
        assertTrue(l.begin(2)); assertEquals(2, l.serial());
    }

    @Test public void euPacketsRemainWholeAndBoundedEvenAtLongLimits() {
        MachineLedger l = new MachineLedger(100);
        assertEquals(0, l.inject(101, 1, 100));
        assertEquals(0, l.inject(-1, 100, 100));
        assertEquals(0, l.inject(1, -100, 100));
        assertEquals(3, l.inject(32, Long.MAX_VALUE, 32)); assertEquals(96, l.energy());
        assertEquals(0, l.inject(32, 1, 32));
        assertEquals(4, l.inject(1, Long.MAX_VALUE, 32)); assertEquals(100, l.energy());
        MachineLedger huge = new MachineLedger(Long.MAX_VALUE);
        assertEquals(1, huge.inject(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, huge.energy());
    }

    @Test public void malformedSavedCountersFailClosedAndClampEnergy() {
        int[][] bad = {{-1, 10}, {11, 10}, {0, 0}, {0, -1}};
        for (int[] pair : bad) {
            MachineLedger l = new MachineLedger(100);
            l.restore(Long.MAX_VALUE, pair[0], pair[1], true, -1);
            assertFalse(l.active()); assertFalse(l.finish());
            assertEquals(100, l.energy()); assertEquals(0, l.progress()); assertEquals(0, l.serial());
        }
        MachineLedger l = new MachineLedger(100);
        l.restore(-100, 1, 2, false, 3);
        assertEquals(0, l.energy()); assertFalse(l.active());
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeCostCannotCreateEU() {
        MachineLedger l = new MachineLedger(100); l.begin(1); l.advance(true, -1);
    }

    @Test public void phasedJobConservesEUAcrossFrequentReloadsAndPowerInterruptions() {
        MachineLedger l = new MachineLedger(1000000);
        long supplied = 0, spent = 0; l.begin(180);
        for (int tick = 0; tick < 180; tick++) {
            long cost = tick < 40 ? 16 : tick < 120 ? 128 : 32;
            assertEquals(MachineLedger.INVALID, l.advance(false, cost));
            supplied += l.inject(32, 4, 32) * 32;
            assertEquals(MachineLedger.RUNNING, l.advance(true, cost)); spent += cost;
            if (tick % 7 == 0) l = reload(l);
            assertEquals(supplied - spent, l.energy());
            assertEquals(tick + 1, l.progress());
        }
        assertTrue(l.ready()); assertEquals(12800, spent);
        assertTrue(l.finish()); assertFalse(reload(l).finish());
    }

    private static MachineLedger reload(MachineLedger source) {
        MachineLedger result = new MachineLedger(source.capacity());
        result.restore(source.energy(), source.progress(), source.duration(), source.active(), source.serial());
        return result;
    }
}
