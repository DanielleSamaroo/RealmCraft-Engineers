package com.tabslab.tabsmod.events;

import com.google.gson.GsonBuilder;
import com.tabslab.tabsmod.TabsMod;
import com.tabslab.tabsmod.blocks.BlockA;
import com.tabslab.tabsmod.blocks.BlockB;
import com.tabslab.tabsmod.commands.Session;
import com.tabslab.tabsmod.data.BlockBroken;
import com.tabslab.tabsmod.data.Data;
import com.tabslab.tabsmod.exp.ExpHud;
import com.tabslab.tabsmod.exp.Timer;
import com.tabslab.tabsmod.init.BlockInit;
import com.tabslab.tabsmod.init.ItemInit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent.BreakEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import net.minecraft.world.entity.Mob;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientEvents {
    private static boolean initialBlockBreak;
    private static Vec3 lastPosition = new Vec3(0, 0, 0);
    private static List<Long> intervals;

    @Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD, modid=TabsMod.MODID, value=Dist.CLIENT)
    public static class ClientModBusEvents {
        @SubscribeEvent
        public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("points", ExpHud.HUD);
        }
    }

    @Mod.EventBusSubscriber(bus= Mod.EventBusSubscriber.Bus.FORGE, modid=TabsMod.MODID, value=Dist.CLIENT)
    public static class ForgeEvents {

        // Register custom commands
        @SubscribeEvent
        public static void registerCommands(RegisterCommandsEvent event) {
            Session.register(event.getDispatcher());
        }

        // tick is unit of time within game's update cycle
        @SubscribeEvent
        public static void onTicks(TickEvent.PlayerTickEvent event) {
            if (Timer.hasPhaseChanged() && initialBlockBreak) {
                Timer.newIntervals();
                Timer.startViTimer(); // "restart" Vi Timer on phase change
            }
        }

        @SubscribeEvent
        public static void onItemPickup(PlayerEvent.ItemPickupEvent event) {
            ItemStack itemStack = event.getStack();  // This should be the correct method to get the item stack
            Player player = event.getEntity();       // This should correctly reference the player picking up the item

            // Check if the item is indeed the coin and if the coin pickup should trigger a point increment
            if (itemStack.getItem() == ItemInit.COIN.get()) {
                ExpHud.incrementPts(1);  // This should add points when a coin is picked up
                System.out.println("Coin picked up, points incremented");

                // Resume the timer when the coin is picked up
                Timer.resumeTimer();
            }

            // Coin picked up, reset the prompt
            if (itemStack.getItem() == ItemInit.COIN.get()) {
                ExpHud.setCoinAvailable(false);
                ExpHud.setShowPickupPrompt(false);
            }
        }


        // Will be run when a block is broken
        @SubscribeEvent
        public static void onBlockBreak(BreakEvent event) {
            Block block = event.getState().getBlock();

            // Check if either BlockA or BlockB is broken
            if (block.equals(BlockInit.BLOCK_A.get()) || block.equals(BlockInit.BLOCK_B.get())) {
                // TO COLLECT CSV DATA
                Map<String, Object> data = new HashMap<>();
                data.put("block_type", block.getDescriptionId());
                data.put("position", event.getPos());
                data.put("phase", Timer.currentPhase());
                data.put("vi_time_remaining", Timer.viTimeRemaining());
                Data.addEvent("block_broken", Timer.timeElapsed(), data);
                // Pause the timer when a coin is potentially dropped
                //Timer.pauseTimer();

                // Mark the coin as available when it drops
                //ExpHud.setCoinAvailable(true);

                // Check if timer has not been started yet
                if (!Timer.timerStarted()) {
                    Timer.startTimer();  // Start the timer on first block break
                }
                
                //  if the first block is broken, start interval schedule for each phase
                if (!initialBlockBreak) {
                    initialBlockBreak = true;
                    ExpHud.incrementCoins(.0005);
                    Timer.startViTimer();
                }

                // if interval scheudule has started
                if (initialBlockBreak && Timer.currentPhase() == 1) {
                    if (Timer.viTimeRemaining() == 0) {
                        if (block.equals(BlockInit.BLOCK_A.get())) {
                            ExpHud.incrementCoins(.0005);
                        }
                        else { // block b
                            ExpHud.incrementCoins(-.0005); // penalty for breaking the wrong one
                        }
                        Timer.nextViInterval();
                    }
                    else { // penalty for breaking before reinforcement time
                        ExpHud.incrementCoins(-.0005);
                    }
                }
                else if (Timer.currentPhase() == 2) {
                    if (Timer.viTimeRemaining() == 0) {
                        if (block.equals(BlockInit.BLOCK_B.get())) { // reinforcement if block b
                            ExpHud.incrementCoins(.0005);
                        }
                        else { // block b
                            ExpHud.incrementCoins(-.0005); // penalty for breaking the wrong one
                        }
                        Timer.nextViInterval();
                    }
                    else { // penalty for breaking before reinforcement time
                        ExpHud.incrementCoins(-.0005);
                    }
                }
                else if (Timer.currentPhase() == 3) {
                    // do nothing
                }

                //  if the first block is broken, start interval schedule for each phase
                if (!initialBlockBreak) {
                    initialBlockBreak = true;
                    ExpHud.incrementCoins(.0005);
                    Timer.startViTimer();
                }

                if (!ExpHud.isCoinAvailable()) {
                    if (Timer.currentPhase() == 1 && block.equals(BlockInit.BLOCK_A.get())) {
                        Timer.pauseTimer();
                        ExpHud.setCoinAvailable(true);

                        Timer.scheduleDelayedTask(() -> {
                            if (ExpHud.isCoinAvailable()) {
                                ExpHud.setShowPickupPrompt(true);
                            }
                        }, 5000);
                    }
                    else if (Timer.currentPhase() == 2 && block.equals(BlockInit.BLOCK_B.get())) {
                        Timer.pauseTimer();
                        ExpHud.setCoinAvailable(true);

                        Timer.scheduleDelayedTask(() -> {
                            if (ExpHud.isCoinAvailable()) {
                                ExpHud.setShowPickupPrompt(true);
                            }
                        }, 5000);
                    }
                }

                // Allow breaking BlockA and BlockB
                if (block.equals(BlockInit.BLOCK_A.get())) {
                    BlockA.broken(event);
                    Data.respawnBlocks(event.getPlayer().getLevel(), false, BlockBroken.BlockA);
                } else if (block.equals(BlockInit.BLOCK_B.get())) {
                    BlockB.broken(event);
                    Data.respawnBlocks(event.getPlayer().getLevel(), false, BlockBroken.BlockB);
                }
            }
            else {
                // Prevent breaking all other blocks (ground)
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {

            // Get event data
            long time = Timer.timeElapsed();

            // Leave level event
            Map<String, Object> data = new HashMap<>();
            Vec3 pos = event.getEntity().position();
            String name = event.getEntity().getName().getString();
            data.put("position", pos);
            data.put("name", name);

            Data.addEvent("player_leave_level", time, data);

            TabsMod.endSession();
        }

        @SubscribeEvent
        public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
            // Store session start time
            Data.sessionStartTime = System.currentTimeMillis();

            // Set entity
            Data.setPlayerEntity(event.getEntity());

            // First, spawn block_a and block_b equidistant from player
            Data.respawnBlocks(event.getEntity().getLevel(), true, BlockBroken.Neither);

            // Flag for interval
            initialBlockBreak = false;

            // Get event data
            long time = Timer.timeElapsed();

            // Join level event
            Map<String, Object> data = new HashMap<>();
            String name = event.getEntity().getName().getString();

            // Get level data
            ClientLevel.ClientLevelData lvl = Minecraft.getInstance().level.getLevelData();
            data.put("day_time", lvl.getDayTime());
            data.put("game_time", lvl.getGameTime());
            data.put("difficulty", lvl.getDifficulty().getKey());
            data.put("spawn_angle", lvl.getSpawnAngle());
            data.put("spawn_position", String.join("`", new GsonBuilder().create().toJson(Map.of(
                    "x", lvl.getXSpawn(),
                    "y", lvl.getYSpawn(),
                    "z", lvl.getZSpawn()
            ))));
            data.put("is_hardcore", lvl.isHardcore());
            lvl.setRaining(false);
            data.put("is_raining", lvl.isRaining());
            data.put("is_thundering", lvl.isThundering());

            // Set name
            Data.setName(name);

            // Phase 1 start event
            Data.addEvent("phase_1_start", time);

            Data.addEvent("player_join_level", time, data);
        }

        @SubscribeEvent
        public static void playerEntityInteract(PlayerInteractEvent.EntityInteract event) {
            if (event.getLevel().isClientSide) {

                // Get interaction data
                long time = Timer.timeElapsed();
                BlockPos pos = event.getPos();
                InteractionHand hand = event.getHand();
                Event.Result res = event.getResult();

                // Add to event list
                Map<String, Object> data = new HashMap<>();
                data.put("position", pos);
                data.put("hand", hand);
                data.put("result", res);
                Data.addEvent("entity_interact", time, data);
            }
        }

        @SubscribeEvent
        public static void playerEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
            if (event.getLevel().isClientSide) {

                // Get interaction data
                long time = Timer.timeElapsed();
                BlockPos pos = event.getPos();
                InteractionHand hand = event.getHand();
                Event.Result res = event.getResult();

                // Add to event list
                Map<String, Object> data = new HashMap<>();
                data.put("position", pos);
                data.put("hand", hand);
                data.put("result", res);
                Data.addEvent("entity_interact_specific", time, data);
            }
        }

        @SubscribeEvent
        public static void playerLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
            if (event.getLevel().isClientSide) {

                // Get interaction data
                long time = Timer.timeElapsed();
                BlockPos pos = event.getPos();
                InteractionHand hand = event.getHand();
                Event.Result res = event.getResult();
                Event.Result useBlock = event.getUseBlock();

                // Add to event list
                Map<String, Object> data = new HashMap<>();
                data.put("position", pos);
                data.put("hand", hand);
                data.put("result", res);
                data.put("useBlock", useBlock);
                Data.addEvent("left_click_block", time, data);
            }
        }

        @SubscribeEvent
        public static void playerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            InteractionHand hand = event.getHand();
            if (event.getLevel().isClientSide && hand == InteractionHand.OFF_HAND) {

                // Get interaction data
                long time = Timer.timeElapsed();
                BlockPos pos = event.getPos();
                Event.Result res = event.getResult();
                Event.Result useBlock = event.getUseBlock();

                // Add to event list
                Map<String, Object> data = new HashMap<>();
                data.put("position", pos);
                data.put("hand", hand);
                data.put("result", res);
                data.put("useBlock", useBlock);
                Data.addEvent("right_click_block", time, data);
            }
        }

        @SubscribeEvent
        public static void playerRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
            if (event.getLevel().isClientSide) {

                // Get interaction data
                long time = Timer.timeElapsed();
                BlockPos pos = event.getPos();
                InteractionHand hand = event.getHand();
                Event.Result res = event.getResult();

                // Add to event list
                Map<String, Object> data = new HashMap<>();
                data.put("position", pos);
                data.put("hand", hand);
                data.put("result", res);
                Data.addEvent("right_click_empty", time, data);
            }
        }

        @SubscribeEvent
        public static void playerRightClickItem(PlayerInteractEvent.RightClickItem event) {
            if (event.getLevel().isClientSide) {

                // Get interaction data
                long time = Timer.timeElapsed();
                BlockPos pos = event.getPos();
                InteractionHand hand = event.getHand();
                Event.Result res = event.getResult();

                // Add to event list
                Map<String, Object> data = new HashMap<>();
                data.put("position", pos);
                data.put("hand", hand);
                data.put("result", res);
                Data.addEvent("right_click_item", time, data);
            }
        }

        @SubscribeEvent
        public static void onPrintEventSummary(InputEvent.Key event) {
            int key = event.getKey();
            if (key == GLFW.GLFW_KEY_P) {
                Data.printSummary();
            }
        }

        @SubscribeEvent
        public static void onEntityJoin(EntityJoinLevelEvent event) {
            Entity entity = event.getEntity();

            if (entity instanceof Mob) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onPlayerMove(TickEvent.PlayerTickEvent event) {
            Player player = event.player;
            Vec3 currentPosition = player.position();

            if (!currentPosition.equals(lastPosition)) {
                long time = Timer.timeElapsed();
                Map<String, Object> data = new HashMap<>();
                data.put("position", currentPosition);
                Data.addEvent("player_move", time, data);
                lastPosition = currentPosition;
            }
        }



        @SubscribeEvent
        public static void onKeyPress(InputEvent.Key event) {
            long time = Timer.timeElapsed();
            int key = event.getKey();
            int action = event.getAction();

            if (action == GLFW.GLFW_PRESS) {
                Map<String, Object> data = new HashMap<>();
                data.put("key", key);
                Data.addEvent("key_press", time, data);
            }
        }

    }
}
