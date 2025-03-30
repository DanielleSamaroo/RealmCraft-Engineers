
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Assertions;

import static org.junit.jupiter.api.Assertions.*;
import com.tabslab.tabsmod.exp.Timer;
import com.tabslab.tabsmod.exp.ExpHud;
import com.tabslab.tabsmod.data.Data;
import com.tabslab.tabsmod.data.BlockBroken;
import net.minecraft.core.BlockPos;
import java.util.List;

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
        Timer.startTimer(); // Should not reset
        long secondStart = Timer.timeElapsed();
        assertTrue(secondStart >= firstStart, "Timer should not reset if started twice");
    }

    @Test
    void testTimerPauseWithoutStart() {
        Timer.pauseTimer(); // Should handle gracefully
        long elapsed = Timer.timeElapsed();
        System.out.println("Elapsed without start: " + elapsed);
        assertTrue(elapsed <= 0, "Time should be zero or negative before timer starts");
    }

    @Test
    void testPhaseTransitionToTwo() {
        Timer.startTimer();            // Make sure timer is initialized
        Timer.setPhase(2);             // Now set phase
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
        Timer.resumeTimer(); // Should be no-op
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
    void testViTimerCountsDown() throws InterruptedException {
        Timer.startViTimer();
        long initial = Timer.viTimeRemaining();
        Thread.sleep(50);
        long later = Timer.viTimeRemaining();
        Assertions.assertTrue(later < initial, "VI Timer should be counting down");
    }

    @Test
    void testPointsIncrementCorrectly() {
        int initialPts = ExpHud.getPts();
        ExpHud.incrementPts(1);
        assertEquals(initialPts + 1, ExpHud.getPts(), "Points should increment by 1");
    }

    @Test
    void testPointsAccumulateProperly() {
        ExpHud.endSession();
        ExpHud.incrementPts(3);
        ExpHud.incrementPts(2);
        assertEquals(5, ExpHud.getPts(), "Points should accumulate correctly");
    }

    @Test
    void testExpHudCoinIncrementOnlyDuringPhases() {
        double before = Double.parseDouble(ExpHud.getFormattedCoins());
        Timer.setPhase(0); // Invalid phase
        ExpHud.incrementCoins(1.0);
        double after = Double.parseDouble(ExpHud.getFormattedCoins());
        assertEquals(before, after, 0.0001, "No coins should be added outside valid phases (1-3)");
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
}
