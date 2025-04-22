package com.tabslab.tabsmod.exp;

import com.tabslab.tabsmod.data.Data;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.sql.Time;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Timer {
    private static long startTime = 0;
    private static long phaseLength = 300000;
    private static int totalPhases = 3;
    private static boolean timerStarted = false;
    private static long[] intervals = new long[0];
    private static int currentInterval = 0;
    private static long pauseTime = 0;
    private static boolean timerPaused = false;
    private static int lastPhase = -1;
    private static long viElapsedBeforePause = 0;
    private static List<Long> viIntervals;
    public static int viIndex = -1;
    private static long viStartTime = 0;
    private static boolean viTimerRunning = false;
    private static boolean viTimerPaused = false;

    public static void pauseTimer() {
        if (!timerPaused) {
            pauseTime = System.currentTimeMillis();
            timerPaused = true;
        }
        if (viTimerRunning && !viTimerPaused) {
            viElapsedBeforePause = System.currentTimeMillis() - viStartTime;
            viTimerPaused = true;
        }
    }

    public static void resumeTimer() {
        if (timerPaused) {
            long pausedDuration = System.currentTimeMillis() - pauseTime;
            startTime += pausedDuration;
            timerPaused = false;
        }

        if (viTimerPaused) {
            long pausedDuration = System.currentTimeMillis() - pauseTime;
            viStartTime = System.currentTimeMillis() - viElapsedBeforePause;
            viTimerPaused = false;
        }
    }

    public static long timeElapsed() {
        if (!timerStarted || timerPaused) {
            return pauseTime - startTime;
        }
        return System.currentTimeMillis() - startTime;
    }

    public static int getTotalPhases() {
        return totalPhases;
    }

    public static void startTimer() {
        if (!timerStarted) {
            startTime = System.currentTimeMillis();
            timerStarted = true;
        }
    }

    public static boolean timerStarted() {
        return timerStarted;
    }

    public static void scheduleDelayedTask(Runnable task, long delayMillis) {
        new Thread(() -> {
            try {
                Thread.sleep(delayMillis);
                Minecraft.getInstance().execute(task);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
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
        long elapsed = timeElapsed();
        return (int) Math.floorDiv(elapsed, phaseLength) + 1;
    }

    public static String currentPhaseString() {
        return String.valueOf(currentPhase());
    }

    public static void endSession() {
        setPhase(totalPhases + 1);
    }

    public static void startViTimer(int index) {
        if (viIntervals == null || viIntervals.isEmpty()) {
            viIntervals = Data.generateIntervals();
        }
        viIndex = index;
        viStartTime = System.currentTimeMillis();
        viTimerRunning = true;
    }

    public static List<Long> getViIntervals() {
        return viIntervals;
    }

    public static long viTimeRemaining() {
        if (!viTimerRunning || viIndex < 0 || viIndex >= viIntervals.size()) {
            return 0;
        }

        long elapsed;
        if (viTimerPaused) {
            elapsed = viElapsedBeforePause;
        } else {
            elapsed = System.currentTimeMillis() - viStartTime;
        }

        long remaining = viIntervals.get(viIndex) - elapsed;
        return Math.max(remaining, 0);
    }

    public static void nextViInterval() {
        if (viIntervals == null || viIntervals.isEmpty()) {
            viIntervals = Data.generateIntervals();
            viIndex = 0;
        } else if (viIndex >= viIntervals.size() - 1) {
            viIntervals = Data.generateIntervals();
            viIndex = 0;
        } else {
            viIndex++;
        }

        viStartTime = System.currentTimeMillis();
        viElapsedBeforePause = 0;
    }

    public static boolean isViRunning() {
        return viTimerRunning && viTimeRemaining() > 0;
    }

    public static boolean hasPhaseChanged() {
        int currentPhase = currentPhase();
        if (currentPhase != lastPhase) {
            lastPhase = currentPhase;
            return true;
        }
        return false;
    }
    public static void resetViState() {
        viIntervals = Data.generateIntervals();
        viIndex = -1;
        viStartTime = 0;
        viElapsedBeforePause = 0;
        viTimerRunning = false;
        viTimerPaused = false;
    }
}