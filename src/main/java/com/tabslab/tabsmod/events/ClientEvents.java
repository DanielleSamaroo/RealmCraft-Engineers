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
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
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
import java.util.UUID;

public class ClientEvents {
    private static boolean correctBlockHit = false;
    private static boolean viWasRunningLastTick = false;
    private static boolean initialBlockBreak;
    private static Vec3 lastPosition = new Vec3(0, 0, 0);
    private static ItemEntity currentCoin = null;
    private static boolean waitingForCoinPickup = false;
    private static boolean pickupPromptShown = false;
    private static long coinSpawnTime = 0;
    public static boolean phaseThreeReinforcement = false; // false = no reinforcement phase 3, true = reinforcement phase 3
    private static boolean reinforcementStarted = false;
    private static boolean waitingForCorrectClickPostVi = false;
    private static boolean waitingForCoinPickupAfterVi = false;
    private static final Map<UUID, Boolean> hasTeleportedPhase2 = new HashMap<>();
    private static final Map<UUID, Boolean> hasTeleportedPhase3 = new HashMap<>();

    private static void spawnCoin(Level level, Player player) {
        if (level.isClientSide) return;

        Vec3 lookVec = player.getLookAngle().normalize();
        double spawnDistance = 2.5;

        double spawnX = player.getX() + lookVec.x * spawnDistance;
        double spawnY = player.getY() + 0.5;
        double spawnZ = player.getZ() + lookVec.z * spawnDistance;

        ItemStack coinStack = new ItemStack(ItemInit.COIN.get(), 1);
        currentCoin = new ItemEntity(level, spawnX, spawnY, spawnZ, coinStack);
        currentCoin.setPickUpDelay(20);

        level.addFreshEntity(currentCoin);

        Timer.pauseTimer();
        waitingForCoinPickup = true;
        waitingForCoinPickupAfterVi = true;
        pickupPromptShown = false;
        coinSpawnTime = System.currentTimeMillis();
        ExpHud.setCoinAvailable(true);
    }

    private static void handleCoinCollected() {
        ExpHud.incrementPts(.05);
        ExpHud.setCoinAvailable(false);
        ExpHud.setShowPickupPrompt(false);

        if (currentCoin != null && currentCoin.isAlive()) {
            currentCoin.discard();
        }

        if (Minecraft.getInstance().player != null) {
            Player player = Minecraft.getInstance().player;
            player.getInventory().add(new ItemStack(ItemInit.COIN.get(), 1));
        }

        currentCoin = null;
        correctBlockHit = false;
        waitingForCoinPickup = false;
        pickupPromptShown = false;
        coinSpawnTime = 0;

        Timer.resumeTimer();
        Timer.nextViInterval();
        waitingForCoinPickupAfterVi = false;

    }

    private static void handleCoinMissed() {
        ExpHud.setCoinAvailable(false);
        ExpHud.setShowPickupPrompt(false);

        if (currentCoin != null && currentCoin.isAlive()) {
            currentCoin.discard();
        }

        currentCoin = null;
        correctBlockHit = false;
        waitingForCoinPickup = false;
        pickupPromptShown = false;
        coinSpawnTime = 0;

        Timer.resumeTimer();
        Timer.nextViInterval();
        waitingForCoinPickupAfterVi = false;

        Map<String, Object> data = new HashMap<>();
        data.put("reason", "coin_missed_despawned");
        Data.addEvent("coin_missed", Timer.timeElapsed(), data);
    }

    private static void forceCollectCoin() {
        handleCoinMissed();
    }

    @Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD, modid=TabsMod.MODID, value=Dist.CLIENT)
    public static class ClientModBusEvents {
        @SubscribeEvent
        public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("points", ExpHud.HUD);
        }
    }

    @Mod.EventBusSubscriber(bus= Mod.EventBusSubscriber.Bus.FORGE, modid=TabsMod.MODID, value=Dist.CLIENT)
    public static class ForgeEvents {

        @SubscribeEvent
        public static void registerCommands(RegisterCommandsEvent event) {
            Session.register(event.getDispatcher());
        }

        @SubscribeEvent
        public static void onTicks(TickEvent.PlayerTickEvent event) {
            if (Timer.hasPhaseChanged() && initialBlockBreak) {
                Timer.resetViState();
                initialBlockBreak = false;
                reinforcementStarted = false;
                waitingForCorrectClickPostVi = false;
            }

            boolean viRunningNow = Timer.isViRunning();

            if (correctBlockHit && Timer.viTimeRemaining() == 0 && !waitingForCoinPickup && !waitingForCorrectClickPostVi) {
                waitingForCorrectClickPostVi = true;
                correctBlockHit = false;
                Timer.pauseTimer();
            }


            if (waitingForCoinPickup) {
                long now = System.currentTimeMillis();
                long elapsedSinceSpawn = now - coinSpawnTime;

                if (elapsedSinceSpawn >= 5000 && !pickupPromptShown) {
                    ExpHud.setShowPickupPrompt(true);
                    pickupPromptShown = true;
                }

                if (elapsedSinceSpawn >= 10000) {
                    forceCollectCoin();
                }
            }

            if (viWasRunningLastTick && !viRunningNow && !correctBlockHit && !waitingForCoinPickup && !waitingForCorrectClickPostVi && !waitingForCoinPickupAfterVi) {
                Timer.nextViInterval();
            }


            viWasRunningLastTick = viRunningNow;

            // teleportation
            if (!event.player.level.isClientSide()) {
                ServerPlayer player = (ServerPlayer) event.player;
                UUID playerId = player.getUUID();
                MinecraftServer server = player.getServer();
                Vec3 originPos = player.position();
                int currentPhase = Timer.currentPhase();

                // Phase 2: Desert
                if (currentPhase == 2 && !hasTeleportedPhase2.getOrDefault(playerId, false) && Data.allowPhaseTeleport) {

                    ResourceKey<Level> desertDimensionKey = ResourceKey.create(
                            Registries.DIMENSION,
                            new ResourceLocation(TabsMod.MODID, "desert_dimension")
                    );
                    ServerLevel targetLevel = server.getLevel(desertDimensionKey);

                    if (targetLevel != null) {
                        int x = (int) originPos.x;
                        int z = (int) originPos.z;

                        int surfaceY = targetLevel.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                        BlockPos.MutableBlockPos groundCheck = new BlockPos.MutableBlockPos(x, surfaceY, z);

                        while (groundCheck.getY() > targetLevel.getMinBuildHeight() && targetLevel.getBlockState(groundCheck).isAir()) {
                            groundCheck.move(0, -1, 0);
                        }

                        int safeY = groundCheck.getY() + 4;

                        if (safeY <= targetLevel.getMinBuildHeight()) {
                            safeY = 64;
                        }

                        Vec3 targetVec = new Vec3(x + 0.5, safeY, z + 0.5);
                        Data.teleportPlayerToDimension(player, desertDimensionKey, targetVec);
                        hasTeleportedPhase2.put(playerId, true);

                        Data.respawnBlocks(targetLevel, false, BlockBroken.Neither);
                    }

                    // Phase 3: Frozen
                } else if (currentPhase == 3 && !hasTeleportedPhase3.getOrDefault(playerId, false) && Data.allowPhaseTeleport) {

                    ResourceKey<Level> frozenDimensionKey = ResourceKey.create(
                            Registries.DIMENSION,
                            new ResourceLocation(TabsMod.MODID, "frozen_dimension")
                    );
                    ServerLevel targetLevel = server.getLevel(frozenDimensionKey);

                    if (targetLevel != null) {
                        int x = (int) originPos.x;
                        int z = (int) originPos.z;

                        int surfaceY = targetLevel.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                        BlockPos.MutableBlockPos groundCheck = new BlockPos.MutableBlockPos(x, surfaceY, z);

                        while (groundCheck.getY() > targetLevel.getMinBuildHeight() && targetLevel.getBlockState(groundCheck).isAir()) {
                            groundCheck.move(0, -1, 0);
                        }

                        int safeY = groundCheck.getY() + 4;

                        if (safeY <= targetLevel.getMinBuildHeight()) {
                            safeY = 64;
                        }

                        Vec3 targetVec = new Vec3(x + 0.5, safeY, z + 0.5);
                        Data.teleportPlayerToDimension(player, frozenDimensionKey, targetVec);
                        hasTeleportedPhase3.put(playerId, true);

                        Data.respawnBlocks(targetLevel, false, BlockBroken.Neither);
                    }

                    if (currentPhase < 2) {
                        hasTeleportedPhase2.remove(playerId);
                    }
                    if (currentPhase < 3) {
                        hasTeleportedPhase3.remove(playerId);
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onItemPickup(PlayerEvent.ItemPickupEvent event) {
            ItemStack itemStack = event.getStack();

            if (itemStack.getItem() == ItemInit.COIN.get() && waitingForCoinPickup) {
                handleCoinCollected();
            }
        }

        @SubscribeEvent
        public static void onBlockBreak(BreakEvent event) {
            if (waitingForCoinPickup) {
                event.setCanceled(true);
                return;
            }

            Block block = event.getState().getBlock();

            if (block.equals(BlockInit.BLOCK_A.get()) || block.equals(BlockInit.BLOCK_B.get())) {
                Map<String, Object> data = new HashMap<>();
                data.put("block_type", block.getDescriptionId());
                data.put("position", event.getPos());
                data.put("phase", Timer.currentPhase());
                data.put("vi_time_remaining", Timer.viTimeRemaining());
                Data.addEvent("block_broken", Timer.timeElapsed(), data);

                if (!Timer.timerStarted()) {
                    Timer.startTimer();
                }

                if (!initialBlockBreak) {
                    initialBlockBreak = true;
                }

                if (waitingForCorrectClickPostVi) {
                    int currentPhase = Timer.currentPhase();

                    if ((currentPhase == 1 && block.equals(BlockInit.BLOCK_A.get())) ||
                            (currentPhase == 2 && block.equals(BlockInit.BLOCK_B.get())) ||
                            (currentPhase == 3 && phaseThreeReinforcement && block.equals(BlockInit.BLOCK_B.get()))) {
                        spawnCoin(event.getPlayer().level, event.getPlayer());
                    } else {
                        ExpHud.incrementPts(-0.05);
                        Timer.resumeTimer();
                        Timer.nextViInterval();
                    }

                    waitingForCorrectClickPostVi = false;
                    event.setCanceled(true);
                    return;
                }


                int currentPhase = Timer.currentPhase();

                if (!reinforcementStarted) {
                    if ((currentPhase == 1 && block.equals(BlockInit.BLOCK_A.get())) ||
                            (currentPhase == 2 && block.equals(BlockInit.BLOCK_B.get())) ||
                            (currentPhase == 3 && phaseThreeReinforcement && block.equals(BlockInit.BLOCK_B.get()))) {

                        reinforcementStarted = true;
                        Timer.startViTimer(0);
                        correctBlockHit = true;
                    }
                }


                if (reinforcementStarted) {
                    if (currentPhase == 1) {
                        if (Timer.viTimeRemaining() == 0) {
                            if (block.equals(BlockInit.BLOCK_A.get())) {
                                ExpHud.incrementPts(.05);
                            } else if (block.equals(BlockInit.BLOCK_B.get())) {
                                ExpHud.incrementPts(-.05);
                            }
                        } else {
                            if (block.equals(BlockInit.BLOCK_A.get())) {
                                correctBlockHit = true;
                            } else if (block.equals(BlockInit.BLOCK_B.get())) {
                                ExpHud.incrementPts(-.05);
                            }
                        }
                    }
                    else if (currentPhase == 2) {
                        if (Timer.viTimeRemaining() == 0) {
                            if (block.equals(BlockInit.BLOCK_B.get())) {
                                ExpHud.incrementPts(.05);
                            } else if (block.equals(BlockInit.BLOCK_A.get())) {
                                ExpHud.incrementPts(-.05);
                            }
                        } else {
                            if (block.equals(BlockInit.BLOCK_B.get())) {
                                correctBlockHit = true;
                            } else if (block.equals(BlockInit.BLOCK_A.get())) {
                                ExpHud.incrementPts(-.05);
                            }
                        }
                    }
                    else if (currentPhase == 3) {
                        if (phaseThreeReinforcement) {
                            if (Timer.viTimeRemaining() == 0) {
                                if (block.equals(BlockInit.BLOCK_B.get())) {
                                    ExpHud.incrementPts(.05);
                                } else if (block.equals(BlockInit.BLOCK_A.get())) {
                                    ExpHud.incrementPts(-.05);
                                }
                            } else {
                                if (block.equals(BlockInit.BLOCK_B.get())) {
                                    correctBlockHit = true;
                                } else if (block.equals(BlockInit.BLOCK_A.get())) {
                                    ExpHud.incrementPts(-.05);
                                }
                            }
                        }
                    }

                }

                if (block.equals(BlockInit.BLOCK_A.get())) {
                    BlockA.broken(event);
                    Data.respawnBlocks(event.getPlayer().getLevel(), false, BlockBroken.BlockA);
                } else if (block.equals(BlockInit.BLOCK_B.get())) {
                    BlockB.broken(event);
                    Data.respawnBlocks(event.getPlayer().getLevel(), false, BlockBroken.BlockB);
                }

            } else {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
            long time = Timer.timeElapsed();

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
            Data.sessionStartTime = System.currentTimeMillis();

            Data.setPlayerEntity(event.getEntity());

            Data.respawnBlocks(event.getEntity().getLevel(), true, BlockBroken.Neither);

            initialBlockBreak = false;

            long time = Timer.timeElapsed();

            Map<String, Object> data = new HashMap<>();
            String name = event.getEntity().getName().getString();

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
            lvl.setDayTime(1000);

            data.put("is_raining", lvl.isRaining());
            data.put("is_thundering", lvl.isThundering());

            Data.setName(name);

            Data.addEvent("phase_1_start", time);

            Data.addEvent("player_join_level", time, data);
        }

        @SubscribeEvent
        public static void playerEntityInteract(PlayerInteractEvent.EntityInteract event) {
            if (event.getLevel().isClientSide) {

                long time = Timer.timeElapsed();
                BlockPos pos = event.getPos();
                InteractionHand hand = event.getHand();
                Event.Result res = event.getResult();

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

                long time = Timer.timeElapsed();
                BlockPos pos = event.getPos();
                InteractionHand hand = event.getHand();
                Event.Result res = event.getResult();

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

                long time = Timer.timeElapsed();
                BlockPos pos = event.getPos();
                InteractionHand hand = event.getHand();
                Event.Result res = event.getResult();
                Event.Result useBlock = event.getUseBlock();

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

                long time = Timer.timeElapsed();
                BlockPos pos = event.getPos();
                Event.Result res = event.getResult();
                Event.Result useBlock = event.getUseBlock();

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

                long time = Timer.timeElapsed();
                BlockPos pos = event.getPos();
                InteractionHand hand = event.getHand();
                Event.Result res = event.getResult();

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

                long time = Timer.timeElapsed();
                BlockPos pos = event.getPos();
                InteractionHand hand = event.getHand();
                Event.Result res = event.getResult();

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
                String keyName = GLFW.glfwGetKeyName(key, 0);

                if (keyName != null) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("key", keyName);
                    Data.addEvent("key_press", time, data);
                } else {
                    Map<String, Object> data = new HashMap<>();
                    data.put("key", key);
                    Data.addEvent("key_press", time, data);
                }
            }

            // FOR TESTING
            if (key == GLFW.GLFW_KEY_H) { // 'H' key
                Timer.setPhase(1);
                Minecraft.getInstance().player.sendSystemMessage(Component.literal("Jumped to Phase 2!"));
            }

            if (key == GLFW.GLFW_KEY_J) { // 'J' key
                Timer.setPhase(2);
                Minecraft.getInstance().player.sendSystemMessage(Component.literal("Jumped to Phase 2!"));
            }

            if (key == GLFW.GLFW_KEY_K) { // 'K' key
                Timer.setPhase(3);
                Minecraft.getInstance().player.sendSystemMessage(Component.literal("Jumped to Phase 3!"));
            }
        }

    }
}
