package com.tabslab.tabsmod.data;

import com.tabslab.tabsmod.TabsMod;
import com.tabslab.tabsmod.exp.Timer;
import com.tabslab.tabsmod.init.BlockInit;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class Data {
    public static List<Long> storedViIntervals = new ArrayList<>();
    private static final ArrayList<Event> evts = new ArrayList<>();
    private static String playerName;
    public static final Map<String, BlockPos> blockPositions = new HashMap<>();
    private static Entity playerEntity;
    public static long sessionStartTime = 0;
    private static long sessionEndTime = 0;
    private static double meanIntervalValue = 2000.0;
    private static int numberOfSteps = 10;
    private static double probability = .5;
    private static final Map<UUID, Vec3> playerOriginPositions = new HashMap<>();
    public static boolean allowPhaseTeleport = true; // false = disabled teleportation
    public static Map<Integer, Double> phaseDistanceMap = new HashMap<>();
    public static Vec3 lastRecordedPosition = null;


    public static void setParameters(double sec, int steps, double prob) {
        Data.meanIntervalValue = sec;
        Data.numberOfSteps = steps;
        Data.probability = prob;
    }

    public static List<Long> generateIntervals() {
        // get the current time
        long baseTime = Timer.timeElapsed();

        // list to store end times of each interval
        List<Long> intervalDurations = new ArrayList<>();

        // constant factor
        double factor = -1.0 / Math.log(1 - probability);

        // random generator for variability
        Random random = new Random();

        System.out.println("Generated Intervals:");

        // iterate to generate interval
        for (int n = 1; n <= numberOfSteps; n++) {
            double t_n;

            // the last interval
            if (n == numberOfSteps) {
                // calculate the last interval to ensure it is valid
                t_n = factor * (1 + Math.log(numberOfSteps));
            } else {
                // Fleshler-Hoffman
                t_n = factor * (
                        1 + Math.log(numberOfSteps) +
                                (numberOfSteps - n) * Math.log(numberOfSteps - n) -
                                (numberOfSteps - n + 1) * Math.log(numberOfSteps - n + 1)
                );
            }

            // introduce variability: random multiplier between 0.7 and 1.0
            double randomFactor = 0.7 + (0.3 * random.nextDouble());
            long intervalDuration = (long) (t_n * meanIntervalValue * randomFactor);

            // store the end time in the list
            intervalDurations.add(intervalDuration);

            // calculate end time of the interval
            long endTime = baseTime + intervalDuration;

            // update baseTime to the end of the current interval for the next iteration
            baseTime = endTime;
        }

        // shuffle list
        Collections.shuffle(intervalDurations);

        storedViIntervals = new ArrayList<>(intervalDurations);
        for (int i = 0; i < storedViIntervals.size(); i++) {
            Map<String, Object> data = new HashMap<>();
            data.put("interval_index", i);
            data.put("interval_duration_ms", storedViIntervals.get(i));
            Data.addEvent("interval_generated", Timer.timeElapsed(), data);
        }

        // return the list of total interval times
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

    public static void setAllowPhaseTeleport(boolean allow) {
        allowPhaseTeleport = allow;
        System.out.println("DEBUG: allowPhaseTeleport set to " + allow);
    }

    public static void teleportPlayerToDimension(ServerPlayer player, ResourceKey<Level> destinationDimension, Vec3 targetPos) {
        MinecraftServer server = player.getServer();
        if (server != null && destinationDimension != null) {
            ServerLevel destinationLevel = server.getLevel(destinationDimension);
            if (destinationLevel != null) {
                if (targetPos != null) {
                    playerOriginPositions.put(player.getUUID(), targetPos);
                    player.teleportTo(destinationLevel, targetPos.x, targetPos.y, targetPos.z, player.getYRot(), player.getXRot());
                } else {
                    player.changeDimension(destinationLevel); // fallback if target position is null
                }
            } else {
                System.err.println("Error: Destination dimension is null!");
            }
        } else {
            if (server == null) System.err.println("Error: MinecraftServer instance is null!");
            if (player == null) System.err.println("Error: ServerPlayer instance is null!");
            if (destinationDimension == null) System.err.println("Error: Destination dimension key is null!");
        }
    }

    public static Vec3 getOriginPosition(ServerPlayer player) {
        return playerOriginPositions.get(player.getUUID());
    }

    public static void clearOriginPosition(ServerPlayer player) {
        playerOriginPositions.remove(player.getUUID());
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
            // remove old blocks
            BlockPos block_a_pos = blockPositions.get("block_a");
            BlockPos block_b_pos = blockPositions.get("block_b");

            if (block_a_pos == null || block_b_pos == null) {
                block_a_pos = playerEntity.blockPosition();
                block_b_pos = playerEntity.blockPosition();
            } else {
                // Give coins
                int phase = Timer.currentPhase();
                boolean canDestroyA = phase == 1 && blockBroken == BlockBroken.BlockA && Timer.viTimeRemaining() == 0;
                boolean canDestroyB = phase == 2 && blockBroken == BlockBroken.BlockB && Timer.viTimeRemaining() == 0;

                if (blockBroken != BlockBroken.Neither) {
                    lvl.destroyBlock(block_a_pos, phase == 1 && blockBroken == BlockBroken.BlockA);
                    lvl.destroyBlock(block_b_pos, phase == 2 && blockBroken == BlockBroken.BlockB);
                }

                lvl.destroyBlock(block_a_pos, canDestroyA);
                lvl.destroyBlock(block_b_pos, canDestroyB);
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

            // Get direction the player is facing
            Vec3 lookVec = playerEntity.getLookAngle().normalize();

            double sideOffset = 3.0;      // *** adjust distance to the left and right ***
            double distanceForward = 6.0; // *** adjust distance in front of player ***
            double verticalOffset = 1.0;  // how high it is off the ground (should stay at 1)

            // Forward offset from player
            double fx = lookVec.x * distanceForward;
            double fz = lookVec.z * distanceForward;

            // Get right vector (90° rotated horizontal vector)
            Vec3 rightVec = new Vec3(-lookVec.z, 0, lookVec.x).normalize();

            // Block A respawn pos
            BlockPos updated_block_a_pos_new = new BlockPos(
                    xPos + fx - rightVec.x * sideOffset,
                    yPos + verticalOffset,
                    zPos + fz - rightVec.z * sideOffset
            );

            // Block B respawn pos
            BlockPos updated_block_b_pos_new = new BlockPos(
                    xPos + fx + rightVec.x * sideOffset,
                    yPos + verticalOffset,
                    zPos + fz + rightVec.z * sideOffset
            );

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
                lvl.destroyBlock(pos, true);
            }
        }
    }

    public static void addEvent(String type, long time, Map<String, Object> data) {
        boolean dev = TabsMod.getDev();
        if (!dev) {
            int currentPhase = Timer.currentPhase();
            Event evt = new Event(type, time, currentPhase, data);

            // Print to log
            System.out.println("-----------------------------------------");
            System.out.println("Event Type: " + evt.getType());
            System.out.println("Time: " + evt.getTime());
            System.out.println("Data: " + evt.getDataString());
            System.out.println("Current Phase: " + currentPhase);
            System.out.println("-----------------------------------------");

            evts.add(evt);
        }
    }

    public static void addEvent(String type, long time) {
        boolean dev = TabsMod.getDev();
        if (!dev) {
            int currentPhase = Timer.currentPhase();
            Event evt = new Event(type, time, currentPhase);

            System.out.println("-----------------------------------------");
            System.out.println("Event Type: " + evt.getType());
            System.out.println("Time: " + evt.getTime());
            System.out.println("Current Phase: " + currentPhase);
            System.out.println("-----------------------------------------");

            evts.add(new Event(type, time, currentPhase));
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

            pw.println("Start: " + startTimeStr);
            pw.println("End: " + endTimeStr);
            pw.println(timeZoneStr);
            pw.println();

            pw.println("Player Name: " + playerName);
            pw.println();

            if (storedViIntervals != null && !storedViIntervals.isEmpty()) {
                pw.println("Generated Intervals (ms):");
                for (int i = 0; i < storedViIntervals.size(); i++) {
                    pw.println("Interval " + (i + 1) + ": " + storedViIntervals.get(i));
                }
                pw.println();
            }

            pw.println("Distance Traveled:");
            for (Map.Entry<Integer, Double> entry : Data.phaseDistanceMap.entrySet()) {
                pw.println("Phase " + entry.getKey() + ": " + entry.getValue() + " units");
            }
            pw.println();

            String[] cols = { "Time", "Type", "Current Phase", "Other Data" };
            pw.println(String.join(",", cols));

            for (Event evt : evts) {
                pw.println(evt.toCSV());
            }

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
