package com.tabslab.tabsmod.data;

import com.tabslab.tabsmod.TabsMod;
import com.tabslab.tabsmod.exp.ExpHud;
import com.tabslab.tabsmod.exp.Timer;
import com.tabslab.tabsmod.init.BlockInit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerLevel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class Data {

    private static final ArrayList<Event> evts = new ArrayList<>();
    private static String playerName;
    public static final Map<String, BlockPos> blockPositions = new HashMap<>();
    private static Entity playerEntity;
    private static double meanIntervalValue = 2000.0; // Mean interval value in ms (2s)
    private static int numberOfSteps = 10; // Total number of intervals (steps)
    private static double probability = .5; // Probability of reinforcement

    //private static List<long> intervals;

    public static long sessionStartTime = 0;
    private static long sessionEndTime = 0;

    public static void setParameters(double sec, int steps, double prob) {
        Data.meanIntervalValue = sec;
        Data.numberOfSteps = steps;
        Data.probability = prob;
    }

    public static long[] generateIntervals() {
        long totalDuration = 600_000; // Total duration of 10 minutes in milliseconds
        long firstHalfDuration = 300_000; // First 5 minutes in milliseconds
        long secondHalfDuration = 300_000; // Second 5 minutes in milliseconds
        long baseTime = Timer.timeElapsed();
        long[] intervalDurations = new long[numberOfSteps * 2];
        double factor = -1.0 / Math.log(1 - probability);
        Random random = new Random();

        long[][] rawDurations = new long[2][numberOfSteps];

        // Generate raw intervals for each half
        for (int half = 0; half < 2; half++) {
            long duration = (half == 0) ? firstHalfDuration : secondHalfDuration;
            long rawTotalDuration = 0;

            for (int n = 1; n <= numberOfSteps; n++) {
                double t_n;
                if (n == numberOfSteps) {
                    t_n = factor * (1 + Math.log(numberOfSteps));
                } else {
                    t_n = factor * (
                            1 + Math.log(numberOfSteps) +
                                    (numberOfSteps - n) * Math.log(numberOfSteps - n) -
                                    (numberOfSteps - n + 1) * Math.log(numberOfSteps - n + 1)
                    );
                }
                double randomFactor = 0.7 + (0.3 * random.nextDouble());
                long intervalDuration = (long) (t_n * meanIntervalValue * randomFactor);
                rawDurations[half][n - 1] = intervalDuration;
                rawTotalDuration += intervalDuration;
            }

            // Scale intervals to fit exactly within the half duration
            double scalingFactor = (double) duration / rawTotalDuration;
            for (int i = 0; i < numberOfSteps; i++) {
                rawDurations[half][i] = (long) (rawDurations[half][i] * scalingFactor);
            }
        }

        // Combine intervals and adjust base time
        int index = 0;
        for (int half = 0; half < 2; half++) {
            for (int i = 0; i < numberOfSteps; i++) {
                intervalDurations[index] = rawDurations[half][i] + baseTime;
                baseTime = intervalDurations[index];
                index++;
            }
        }

        // To log intervals
        System.out.println("Generated Intervals:");
        long startTime = Timer.timeElapsed();
        for (int i = 0; i < intervalDurations.length; i++) {
            long endTime = startTime + intervalDurations[i];
            System.out.printf("Interval %d: Start = %d ms, End = %d ms, Interval Duration = %d ms%n",
                    i + 1, startTime, endTime, intervalDurations[i]);
            startTime = endTime; // Updates startTime for the next interval
        }

        return intervalDurations;
    }



    public static void setPlayerEntity(Entity entity) {
        playerEntity = entity;
        System.out.println("Entity Set");
        System.out.println("Position:");
        System.out.println(entity.position());

        System.out.println("Player Chunk: ");
        System.out.println(entity.chunkPosition());
    }

    public static void teleportPlayer(double x, double y, double z) {
        playerEntity.moveTo(x, y, z);
        //playerEntity.setPositionAndUpdate(x, y, z);
    }

    public static void setBlockPositions(Map<String, BlockPos> positions) {
        blockPositions.put("block_a", positions.get("block_a"));
        blockPositions.put("block_b", positions.get("block_b"));
    }

    public static BlockPos getBlockAPos() {
        return blockPositions.get("block_a");
    }

    public static BlockPos getBlockBPos() {
        return blockPositions.get("block_b");
    }

    public static void respawnBlocks(Level lvl, boolean initialSpawn, BlockBroken blockBroken) {

        boolean dev = TabsMod.getDev();
        if (!dev) {
            // First, remove old blocks if it isn't the initial level

            BlockPos block_a_pos = blockPositions.get("block_a");
            BlockPos block_b_pos = blockPositions.get("block_b");

            if (block_a_pos == null || block_b_pos == null) {
                block_a_pos = playerEntity.blockPosition();
                block_b_pos = playerEntity.blockPosition();
            } else {
                // Give coins
                int phase = Timer.currentPhase();
                lvl.destroyBlock(block_a_pos, phase == 1 && blockBroken == BlockBroken.BlockA);
                lvl.destroyBlock(block_b_pos, phase == 2 && blockBroken == BlockBroken.BlockB);
            }

            // Get the chunks where block_a and block_b are located
            LevelChunk chunk_a = lvl.getChunkAt(block_a_pos);
            LevelChunk chunk_b = lvl.getChunkAt(block_b_pos);

            // Generate two random positions within the chunks
            Random random = new Random();
            int chunkX_a = chunk_a.getPos().x;
            int chunkZ_a = chunk_a.getPos().z;
            int chunkX_b = chunk_b.getPos().x;
            int chunkZ_b = chunk_b.getPos().z;

            // Gets position of player
            BlockPos playerPos = playerEntity.getOnPos();
            int xPos = playerPos.getX();
            int yPos = playerPos.getY();
            int zPos = playerPos.getZ();
            // Respawns blocks to be at an equidistant position from player
            BlockPos updated_block_a_pos_new = new BlockPos(xPos + 3, yPos + 1, zPos + 3);
            BlockPos updated_block_b_pos_new = new BlockPos(xPos - 3, yPos + 1, zPos + 3);


            // Place the blocks at the new random positions
            BlockState blockStateA = BlockInit.BLOCK_A.get().defaultBlockState();
            BlockState blockStateB = BlockInit.BLOCK_B.get().defaultBlockState();
            boolean set_a = lvl.setBlockAndUpdate(updated_block_a_pos_new, blockStateA);
            boolean set_b = lvl.setBlockAndUpdate(updated_block_b_pos_new, blockStateB);

            // Log as event
            Map<String, Object> data = new HashMap<>();
            data.put("block_a_spawn", updated_block_a_pos_new);
            data.put("block_a_set", set_a);
            data.put("block_b_spawn", updated_block_b_pos_new);
            data.put("block_b_set", set_b);
            if (initialSpawn) {
                addEvent("blocks_spawn_initial", 0, data);
            } else {
                long time = Timer.timeElapsed();
                addEvent("blocks_spawn", time, data);
            }

            // Update new block positions
            blockPositions.clear();

            blockPositions.put("block_a", updated_block_a_pos_new);
            blockPositions.put("block_b", updated_block_b_pos_new);

        }
    }

    private static void removeAllBlocks(Level lvl, Block targetBlock) {
        for (BlockPos pos : BlockPos.betweenClosed(lvl.getMinBuildHeight(), 0, lvl.getMinBuildHeight(), lvl.getMaxBuildHeight(), 255, lvl.getMaxBuildHeight())) {
            if (lvl.getBlockState(pos).getBlock() == targetBlock) {
                lvl.destroyBlock(pos, true); // Drop the block as an item
            }
        }
    }

    public static void removeAnimals(Level level) {
        if (!(level instanceof ServerLevel)) {
            System.out.println("Not a ServerLevel instance.");
            return;
        }

        System.out.println("Removing animals...");
        ServerLevel serverLevel = (ServerLevel) level;

        AABB worldBounds = new AABB(
                level.getMinBuildHeight(), level.getMinBuildHeight(), level.getMinBuildHeight(),
                level.getMaxBuildHeight(), level.getMaxBuildHeight(), level.getMaxBuildHeight()
        );

        List<Animal> landAnimals = serverLevel.getEntitiesOfClass(Animal.class, worldBounds);
        for (Animal animal : landAnimals) {
            animal.discard();
            serverLevel.broadcastEntityEvent(animal, (byte) 60);
            System.out.println("Removed land animal: " + animal.getType().toString());
        }

        List<WaterAnimal> waterAnimals = serverLevel.getEntitiesOfClass(WaterAnimal.class, worldBounds);
        for (WaterAnimal waterAnimal : waterAnimals) {
            waterAnimal.discard();
            serverLevel.broadcastEntityEvent(waterAnimal, (byte) 60);
            System.out.println("Removed water animal: " + waterAnimal.getType().toString());
        }
    }

    public static void addEvent(String type, long time, Map<String, Object> data) {
        boolean dev = TabsMod.getDev();
        if (!dev) {
            Event evt = new Event(type, time, data);

            // Print to log
            System.out.println("-----------------------------------------");
            System.out.println("Event Type: " + evt.getType());
            System.out.println("Time: " + evt.getTime());
            System.out.println("Data: " + evt.getDataString());
            System.out.println("-----------------------------------------");

            evts.add(evt);
        }
    }

    public static void addEvent(String type, long time) {
        boolean dev = TabsMod.getDev();
        if (!dev) {
            Event evt = new Event(type, time);

            // Print to log
            System.out.println("-----------------------------------------");
            System.out.println("Event Type: " + evt.getType());
            System.out.println("Time: " + evt.getTime());
            System.out.println("-----------------------------------------");

            evts.add(new Event(type, time));
        }
    }

    public static void setName(String name) {
        playerName = name;
    }

    public static void printSummary() {
        boolean dev = TabsMod.getDev();
        if (!dev) {
            System.out.println("-----------------------------------------");
            System.out.println("Event Summary");
            System.out.println(evts);
            System.out.println("-----------------------------------------");
        }
    }

    public static void endSession() {
        boolean dev = TabsMod.getDev();
        sessionEndTime = System.currentTimeMillis();
        if (!dev) {
            writeToCSV();
        }
        playerName = null;
        blockPositions.clear();
        evts.clear();
    }

    public static void writeToCSV() {

        System.out.println("-----------------------------------------");
        System.out.println("Creating csv file...");
        System.out.println("-----------------------------------------");

        File file = new File(playerName + ".csv");

        try(PrintWriter pw = new PrintWriter(file)) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getDefault());

            String startTimeStr = dateFormat.format(new Date(sessionStartTime));
            String endTimeStr = dateFormat.format(new Date(sessionEndTime));
            String timeZoneStr = new SimpleDateFormat("HH:mm:ss z").format(new Date(sessionEndTime));

            // Write session timestamps at the top of the file
            pw.println("Start: " + startTimeStr);
            pw.println("End: " + endTimeStr);
            pw.println(timeZoneStr);
            pw.println();

            // Add headers
            pw.println("Player Name: " + playerName);

            // Write events
            String[] cols = { "Time", "Type", "Other Data" };
            pw.println(String.join(",", cols));

            for (Event evt : evts) {
                pw.println(evt.toCSV());
            }

            // Close connection
            pw.close();

            System.out.println("-----------------------------------------");
            System.out.println("Data File Created!");
            System.out.println("Absolute Path: " + file.getAbsolutePath());
            System.out.println("-----------------------------------------");

        } catch (IOException e) {

            System.out.println("-----------------------------------------");
            System.out.println("Exception during file creation:");
            System.out.println(e.getMessage());
            System.out.println("-----------------------------------------");
        }

    }
}
