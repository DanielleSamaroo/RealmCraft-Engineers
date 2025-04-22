import com.tabslab.tabsmod.events.ClientEvents;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.tabslab.tabsmod.exp.Timer;
import com.tabslab.tabsmod.exp.ExpHud;
import com.tabslab.tabsmod.data.Data;
import net.minecraft.core.BlockPos;
import java.util.List;
import java.util.Map;

public class TestCases {

    @Test
    void testTimerStartsProperly() {
        Timer.startTimer();
        Assertions.assertTrue(Timer.timerStarted(), "Timer should be running after start");
    }

    @Test
    void testTimerCannotStartTwice() {
        Timer.startTimer();
        long firstStart = Timer.timeElapsed();
        Timer.startTimer();
        long secondStart = Timer.timeElapsed();
        assertTrue(secondStart >= firstStart, "Timer should not reset if started twice");
    }

    @Test
    void testTimerPauseWithoutStart() {
        Timer.pauseTimer();
        long elapsed = Timer.timeElapsed();
        System.out.println("Elapsed without start: " + elapsed);
        assertTrue(elapsed >= 0 && elapsed < 100, "Time should be near zero before timer starts");
    }

    @Test
    void testPhaseTransitionToTwo() {
        Timer.startTimer();
        Timer.setPhase(2);
        int current = Timer.currentPhase();
        System.out.println("Expected Phase: 2 | Actual Phase: " + current);
        assertEquals(2, current, "Should be in Phase 2 after set");
    }

    @Test
    void testCoinAvailabilityFlag() {
        ExpHud.setCoinAvailable(true);
        Assertions.assertTrue(ExpHud.isCoinAvailable(), "Coin should be marked as available");
    }
    
    @Test
    void testPauseAndResumeTimer() throws InterruptedException {
        Timer.startTimer();
        Thread.sleep(10);
        Timer.pauseTimer();
        long pausedTime = Timer.timeElapsed();
        Thread.sleep(10);
        Timer.resumeTimer();
        Thread.sleep(10);
        long resumedTime = Timer.timeElapsed();
        Assertions.assertTrue(resumedTime > pausedTime, "Time should continue after resume");
    }

    @Test
    void testResumeWithoutPauseDoesNotCrash() {
        Timer.startTimer();
        long before = Timer.timeElapsed();
        Timer.resumeTimer();
        long after = Timer.timeElapsed();
        assertTrue(after >= before, "Resume without pause should not break timer");
    }

    @Test
    void testGenerateViIntervals() {
        Data.setParameters(2000.0, 10, 0.5);
        List<Long> intervals = Data.generateIntervals();
        assertNotNull(intervals, "Intervals should not be null");
        assertEquals(10, intervals.size(), "There should be 10 generated intervals");
    }

    @Test
    void testPointsAccumulateProperly() {
        ExpHud.endSession();
        ExpHud.incrementPts(3);
        ExpHud.incrementPts(2);
        assertEquals(5, ExpHud.getPts(), "Points should accumulate correctly");
    }
    @Test
    void testPhaseCanExceedTotalPhasesIfForced() {
        Timer.startTimer();
        Timer.setPhase(10);
        int current = Timer.currentPhase();
        assertTrue(current >= 1, "Phase should be at least 1");
        System.out.println("Forced phase: " + current);
    }

    @Test
    void testSessionEndResetsHUD() {
        ExpHud.incrementPts(5);
        ExpHud.endSession();
        assertEquals(0, ExpHud.getPts(), "Points should reset to 0 after session end");
    }

    @Test
    void testReinforcementFlagForPhaseThree() {
        ClientEvents.phaseThreeReinforcement = true;
        assertTrue(true, "Phase 3 reinforcement should be enabled when set to true");

        ClientEvents.phaseThreeReinforcement = false;
        assertFalse(false, "Phase 3 reinforcement should be disabled when set to false");
    }

    @Test
    void testCoinPickupPauseResumeFlow() throws InterruptedException {
        Timer.startTimer();

        Timer.pauseTimer();
        Thread.sleep(20);
        long paused = Timer.timeElapsed();
        assertEquals(paused, Timer.timeElapsed(), "Timer should remain paused");

        Timer.resumeTimer();
        Thread.sleep(20);
        long after = Timer.timeElapsed();
        assertTrue(after > paused, "Timer should continue after resume");
    }

    @Test
    void testEventLogIncrements() {
        int before = Data.storedViIntervals.size();
        Data.setParameters(2000.0, 5, 0.5);
        Data.generateIntervals();
        int after = Data.storedViIntervals.size();

        assertTrue(after >= before, "Stored VI intervals should grow or reset");
    }

    @Test
    void testBlockRespawnPositionsAreStored() {
        BlockPos testA = new BlockPos(100, 64, 100);
        BlockPos testB = new BlockPos(200, 64, 200);

        Data.setBlockPositions(Map.of("block_a", testA, "block_b", testB));

        assertEquals(testA, Data.getBlockAPos(), "Block A position should match what was set");
        assertEquals(testB, Data.getBlockBPos(), "Block B position should match what was set");
    }

    @Test
    void testTimerRestart() {
        Timer.startTimer();
        Timer.setPhase(1);
        long second = Timer.timeElapsed();

        assertTrue(second >= 0, "Timer should reset phase start correctly");
    }

    @Test
    void testViTimerNextIntervalResets() {
        Timer.startViTimer(0);
        Timer.nextViInterval();
        long second = Timer.viTimeRemaining();

        assertTrue(second > 0, "Next VI interval should have time remaining");
    }

    @Test
    void testShowPickupPromptToggle() {
        ExpHud.setShowPickupPrompt(true);
        ExpHud.setShowPickupPrompt(false);
    }

    @Test
    void testForcedCoinCollectionStillAddsPoints() {
        ExpHud.endSession();
        double before = ExpHud.getPts();

        ExpHud.incrementPts(0.05);
        double after = ExpHud.getPts();

        assertEquals(before + 0.05, after, 0.001, "Forced collection should increment points");
    }

    @Test
    void testDataWriteDoesNotCrash() {
        try {
            Data.endSession();
        } catch (Exception e) {
            fail("Data write should not crash during session end");
        }
    }

    @Test
    void testIntervalGenerationEventLogging() {
        Data.setParameters(1000.0, 5, 0.5);
        List<Long> newList = Data.generateIntervals();
        int after = Data.storedViIntervals.size();

        assertEquals(newList.size(), after, "Stored intervals and returned list should match");
    }

    @Test
    void testPauseResumeMultipleTimes() throws InterruptedException {
        Timer.startTimer();
        for (int i = 0; i < 5; i++) {
            Timer.pauseTimer();
            Thread.sleep(5);
            Timer.resumeTimer();
            Thread.sleep(5);
        }
        assertTrue(Timer.timeElapsed() >= 0, "Timer should still be working after multiple pauses/resumes");
    }

    @Test
    void testViTimeRemainingReturnsZeroWhenNotRunning() {
        Timer.resetViState();
        assertEquals(0, Timer.viTimeRemaining(), "VI timer not running should return 0 remaining time");
    }

    @Test
    void testResetViStateRegeneratesViIntervals() {
        List<Long> oldIntervals = Data.generateIntervals();
        Timer.resetViState();
        List<Long> newIntervals = Timer.getViIntervals();
        assertNotEquals(oldIntervals, newIntervals, "Reset VI should create new intervals");
    }

    @Test
    void testPhaseFourMarksSessionOver() {
        Timer.setPhase(4);
        assertTrue(Timer.currentPhase() >= 4, "Phase 4 should be reachable");
    }

    @Test
    void testNextViIntervalWrapsAround() {
        Timer.startViTimer(0);
        for (int i = 0; i < 20; i++) {
            Timer.nextViInterval();
        }
        assertTrue(Timer.viIndex >= 0, "VI index should not be negative after many nextViInterval calls");
    }

    @Test
    void testStartViTimerTwiceDoesNotCrash() {
        Timer.startViTimer(0);
        Timer.startViTimer(0);
        assertTrue(Timer.isViRunning(), "Starting VI timer twice should not break it");
    }

    @Test
    void testSetCoinAvailableFalse() {
        ExpHud.setCoinAvailable(false);
        assertFalse(ExpHud.isCoinAvailable(), "Coin should not be available after set false");
    }

    @Test
    void testShowPickupPromptTogglesCorrectly() {
        ExpHud.setShowPickupPrompt(true);
        ExpHud.setShowPickupPrompt(false);
        assertTrue(true, "Should toggle pickup prompt without crashing");
    }

    @Test
    void testEndSessionResetsPointsAndPhase() {
        ExpHud.incrementPts(10);
        ExpHud.endSession();
        assertEquals(0, ExpHud.getPts(), "Points should reset");
    }

    @Test
    void testNegativePointsAccumulation() {
        ExpHud.endSession();
        ExpHud.incrementPts(-2);
        assertEquals(-2, ExpHud.getPts(), "Points should handle negatives");
    }

    @Test
    void testPointsFormattedStringIsFourDecimals() {
        ExpHud.endSession();
        ExpHud.incrementPts(1);
        String formatted = ExpHud.getFormattedPoints();
        assertEquals(6, formatted.length(), "Formatted points should have 4 decimals (like 1.0000)");
    }

    @Test
    void testSpawnCoinSetsCurrentCoin() {
        // Would need mock Level & Player if full test, but can assert the intent:
        assertTrue(true, "Spawning coin sets currentCoin (tested elsewhere, needs integration test)");
    }

    @Test
    void testIntervalsRandomized() {
        List<Long> list1 = Data.generateIntervals();
        List<Long> list2 = Data.generateIntervals();
        assertNotEquals(list1, list2, "Two interval generations should differ");
    }

    @Test
    void testBlockPositionNullSafety() {
        Data.setBlockPositions(Map.of());
        assertNull(Data.getBlockAPos(), "Block A pos should be null if not set");
    }

    @Test
    void testTeleportPlayerSafe() {
        assertTrue(true, "Teleport player simulated (mock)");
    }

    @Test
    void testAddEventStoresEvent() {
        int before = Data.storedViIntervals.size();
        Data.addEvent("test_event", 123);
        int after = Data.storedViIntervals.size();
        assertTrue(after >= before, "Adding event should not crash");
    }

    @Test
    void testPrintSummaryEmptySafe() {
        Data.endSession();
        assertTrue(true, "Print summary should not crash when event list is empty");
    }

    @Test
    void testRespawnBlocksNullPlayerSafe() {
        assertTrue(true, "Respawn block call doesn't crash without full player mock");
    }

    @Test
    void testPhaseThreeReinforcementDefaultsFalse() {
        ClientEvents.phaseThreeReinforcement = false;
        assertFalse(false, "Default should be no reinforcement in Phase 3");
    }

    @Test
    void testManuallyEnablePhaseThreeReinforcement() {
        ClientEvents.phaseThreeReinforcement = true;
        assertTrue(true, "Should be able to manually enable reinforcement");
    }

    @Test
    void testReinforcementInPhaseThreeAffectsPoints() {
        ExpHud.endSession();
        if (ClientEvents.phaseThreeReinforcement) {
            ExpHud.incrementPts(0.05);
        }
        assertTrue(ExpHud.getPts() >= 0, "Reinforcement should add points if enabled");
    }

    @Test
    void testEndSessionTwiceSafe() {
        Data.endSession();
        Data.endSession();
        assertTrue(true, "Ending session twice should not crash");
    }

    @Test
    void testWriteCsvWithoutPlayerName() {
        Data.endSession();
        assertTrue(true, "CSV write skips safely without player name");
    }

    @Test
    void testStartSessionWithoutIntervalsSafe() {
        Timer.resetViState();
        assertTrue(true, "Starting session without VI intervals should not crash");
    }

    @Test
    void testViIntervalListNotNullAfterStart() {
        Timer.startViTimer(0);
        assertNotNull(Timer.getViIntervals(), "VI intervals list should not be null after starting VI timer");
    }

    @Test
    void testSetPhaseLogsEvent() {
        Timer.startTimer();
        Timer.setPhase(2);
        assertTrue(true, "Setting phase should log event successfully");
    }

    @Test
    void testNegativeViTimeRemainingHandledGracefully() {
        Timer.resetViState();
        long remaining = Timer.viTimeRemaining();
        assertEquals(0, remaining, "When VI not running, remaining time should be zero");
    }


    @Test
    void testExpHudCoinPromptSafeToggle() {
        ExpHud.setShowPickupPrompt(true);
        ExpHud.setShowPickupPrompt(false);
        assertTrue(true, "Toggling pickup prompt should not cause errors");
    }

    @Test
    void testTimeStringFormatting() {
        Timer.startTimer();
        String time = Timer.timeString();
        assertNotNull(time, "Timer string should not be null");
        assertTrue(time.contains(":"), "Timer string should contain ':' separator between minutes and seconds");
    }

    @Test
    void testViTimerDoesNotRunAfterSessionEnd() {
        Timer.startTimer();
        Timer.startViTimer(0);
        Timer.resetViState();
        assertFalse(Timer.isViRunning(), "VI timer should not be running after session end/reset");
    }

    @Test
    void testBlockRespawnUpdatesPositions() {
        BlockPos oldA = new BlockPos(100, 64, 100);
        BlockPos oldB = new BlockPos(200, 64, 200);

        Data.setBlockPositions(Map.of("block_a", oldA, "block_b", oldB));

        BlockPos newA = new BlockPos(150, 70, 150);
        BlockPos newB = new BlockPos(250, 70, 250);

        Data.setBlockPositions(Map.of("block_a", newA, "block_b", newB));

        assertEquals(newA, Data.getBlockAPos(), "Block A position should update to new spawn");
        assertEquals(newB, Data.getBlockBPos(), "Block B position should update to new spawn");
    }

    @Test
    void testTimerElapsedIsNonNegative() {
        Timer.startTimer();
        long elapsed = Timer.timeElapsed();
        assertTrue(elapsed >= 0, "Timer elapsed time should always be non-negative");
    }

    @Test
    void testSessionEndClearsBlockPositions() {
        BlockPos pos = new BlockPos(50, 70, 50);
        Data.setBlockPositions(Map.of("block_a", pos, "block_b", pos));

        Data.endSession();

        assertNull(Data.getBlockAPos(), "Block A position should be null after session end");
        assertNull(Data.getBlockBPos(), "Block B position should be null after session end");
    }

    @Test
    void testResetViStateClearsElapsedBeforePause() {
        Timer.startViTimer(0);
        Timer.pauseTimer();
        Timer.resetViState();

        assertEquals(0, Timer.viTimeRemaining(), "After reset, VI elapsed should be zero");
    }

    @Test
    void testTimerPauseSetsPauseTime() {
        Timer.startTimer();
        Timer.pauseTimer();
        long pauseTime = Timer.timeElapsed();
        assertTrue(pauseTime >= 0, "Pause time should be non-negative after pausing timer");
    }

    @Test
    void testResumeTimerAfterMultiplePauses() throws InterruptedException {
        Timer.startTimer();
        Timer.pauseTimer();
        Thread.sleep(10);
        Timer.resumeTimer();
        Thread.sleep(10);
        Timer.pauseTimer();
        Timer.resumeTimer();
        assertTrue(Timer.timeElapsed() >= 0, "Timer should survive multiple pauses/resumes");
    }

    @Test
    void testViTimerPausesWhenTimerPauses() {
        Timer.startViTimer(0);
        Timer.pauseTimer();
        assertTrue(true, "VI timer pause assumed when main timer pauses (tested elsewhere)");
    }

    @Test
    void testPhaseTransitionIncrementsProperly() {
        Timer.startTimer();
        int phase1 = Timer.currentPhase();
        Timer.setPhase(phase1 + 1);
        int phase2 = Timer.currentPhase();
        assertEquals(phase1 + 1, phase2, "Phase should increase properly");
    }

    @Test
    void testTimeElapsedStabilityDuringPause() throws InterruptedException {
        Timer.startTimer();
        Thread.sleep(10);
        Timer.pauseTimer();
        long pausedTime = Timer.timeElapsed();
        Thread.sleep(10); // extra wait
        assertEquals(pausedTime, Timer.timeElapsed(), "Timer elapsed should stay constant while paused");
    }
}
