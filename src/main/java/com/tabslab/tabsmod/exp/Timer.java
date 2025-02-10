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
    public static int getTotalPhases() {
        return totalPhases;
    }

    public static void startTimer() {
        startTime = System.currentTimeMillis();

    }


    public static long timeElapsed() {
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
