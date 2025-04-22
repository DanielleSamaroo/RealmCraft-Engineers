package com.tabslab.tabsmod.blocks;

import com.tabslab.tabsmod.data.Data;
import com.tabslab.tabsmod.exp.ExpHud;
import com.tabslab.tabsmod.exp.Timer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.level.BlockEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BlockB extends Block {
    public BlockB(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public static void broken(BlockEvent.BreakEvent event) {
      // Add to event list
        long time = Timer.timeElapsed();

        Map<String, Object> data = new HashMap<>();
        data.put("position", event.getPos());
        data.put("cumulative_points", ExpHud.getPts());
        Data.addEvent("block_a_break", time, data);

    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState state1, boolean myBool) {
        Data.blockPositions.put("block_b", pos);
    }

    // This is triggered when block is punched (left mouse click)
    @Override
    public void attack(BlockState state, Level world, BlockPos pos, Player player) {
        if (false) {
            if (world.isClientSide) {

                // Get interaction data
                long time = Timer.timeElapsed();
                /* Note: Other event data is available via method parameters */

                // Add to event list
                Map<String, Object> data = new HashMap<>();
                data.put("position", pos);
                Data.addEvent("Block B Punch", time, data);

                // Add (or don't add) to points depending on phase
                int phase = Timer.currentPhase();

                switch (phase) {
                    case 1, 3: ExpHud.incrementPts(0);
                        break;
                    case 2: ExpHud.incrementPts(1);
                        break;
                }
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult res) {

        if (level.isClientSide) {

            // Get interaction data
            long time = Timer.timeElapsed();
            /* Note: Other event data is available via method parameters */

            // Add to event list
            Map<String, Object> data = new HashMap<>();
            data.put("position", pos);
            data.put("hand", hand);
            data.put("result", res);
            Data.addEvent("block_b_use", time, data);
        }

        return super.use(state, level, pos, player, hand, res);
    }
}
