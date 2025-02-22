package com.tabslab.tabsmod.exp;

import com.tabslab.tabsmod.data.Data;
import net.minecraft.core.BlockPos;

import java.sql.Time;
import java.util.HashMap;
import java.util.Map;

public class Timer {
    private static long startTime = 0;
    private static long phaseLength = 300000; // Milliseconds
    private static int totalPhases = 3;

    private static boolean timerStarted = false;
    private static long[] intervals = new long[0];
    private static int currentInterval = 0; // Track the current interval

    public static void setIntervals(long[] generatedIntervals) {
        intervals = generatedIntervals;
        currentInterval = 0;
    }

    // Check if the stimulus point for the current interval has been reached
    public static boolean isStimulusReached() {
        long elapsedTime = timeElapsed();

        // If all intervals are completed, return false
        if (currentInterval >= intervals.length) return false;

        // Check if elapsed time has passed the stimulus point of the current interval
        if (elapsedTime >= intervals[currentInterval]) {
            currentInterval++; // Move to the next interval
            return true;       // Stimulus point reached
        }
        return false;
    }

    public static int getTotalPhases() {
        return totalPhases;
    }

    public static void startTimer() {
        if (!timerStarted) {
            startTime = System.currentTimeMillis();
            timerStarted = true;  // Set this to true when the timer starts
        }

    }
    public static boolean timerStarted() {
        return timerStarted;  // Getter method to check if the timer has started
    }

    public static long timeElapsed() {
        if (!timerStarted) {
            return 0;  // If the timer hasn't started, return 0
        }
        return System.currentTimeMillis() - startTime;
    }

    public static String timeString() {
        long millis = timeElapsed();
        String mins = String.format("%02d", (millis / 1000) / 60);
        String secs = String.format("%02d", (millis / 1000) % 60);

        return mins + ":" + secs;
    }

    public static void setPhase(int phase) {
        int oldPhase = Timer.currentPhase();
        startTime = System.currentTimeMillis() - (phaseLength * (phase - 1));

        String evt_type = "phase_set_" + phase;
        Map<String, Object> data = new HashMap<>();
        data.put("old_phase", oldPhase);
        data.put("new_phase", phase);
        Data.addEvent(evt_type, Timer.timeElapsed(), data);
    }

    public static int currentPhase() {
        // This is where the package will determine what phase the experiment is in
        // when called.
        long elapsed = timeElapsed();
        return (int) Math.floorDiv(elapsed, phaseLength) + 1;
    }

    public static String currentPhaseString() {
        return String.valueOf(currentPhase());
    }

    public static void endSession() {
        setPhase(totalPhases + 1);
    }

}