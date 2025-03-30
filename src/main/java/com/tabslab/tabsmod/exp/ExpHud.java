package com.tabslab.tabsmod.exp;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tabslab.tabsmod.data.Data;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.awt.*;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ExpHud {
    private static int numPts = 0;
    private static int currentPhase = 0;
    private static double totalCoins = 0.0;
    private static boolean coinAvailable = false;

    private static boolean showPickupPrompt = false;

    public static void setShowPickupPrompt(boolean show) {
        showPickupPrompt = show;
    }

    public static boolean isCoinAvailable() {
        return coinAvailable;
    }

    public static void setCoinAvailable(boolean available) {
        coinAvailable = available;
    }


    // Method to increment coins based on the current interval and stimulus
    public static void incrementCoins(double x) {
        int phase = Timer.currentPhase();
        if (phase >= 1 && phase <= Timer.getTotalPhases()) {
            totalCoins += x;
        }
    }

    // Method to get the total coins as a formatted string
    public static String getFormattedCoins() {
        return String.format("%.4f", totalCoins);
    }

    public static final IGuiOverlay HUD = (((gui, poseStack, partialTick, screenWidth, screenHeight) -> {

        Font font = Minecraft.getInstance().font;
        int textColor = 0xFFFFFF;
        int padding = 20;
        int linePadding = 5;
        int lineHeight = font.lineHeight;

        if (showPickupPrompt) {
            String pickupMessage = "Please pick up the coin to proceed";
            int messageWidth = Minecraft.getInstance().font.width(pickupMessage);
            int x = (screenWidth - messageWidth) / 2;
            int y = screenHeight / 2; // Center vertically
            GuiComponent.drawString(poseStack, Minecraft.getInstance().font, pickupMessage, x, y, 0xFFFFFF); // white color
        }

        // display vi Timer
        if (Timer.isViRunning() || Timer.viTimeRemaining() == 0) {
            long viRemaining = Math.max(Timer.viTimeRemaining(), 0); // ensures it doesnt go negative
            String viTime = "Vi Timer: " + viRemaining + "ms";
            int viWidth = font.width(viTime);
            int viX = 10;
            int viY = 10;
            GuiComponent.drawString(poseStack, font, viTime, viX, viY, textColor);

            List<Long> intervals = Timer.getViIntervals(); // Get the interval list
            if (intervals != null && !intervals.isEmpty()) {
                int yOffset = 20;
                for (int i = 0; i < intervals.size(); i++) {
                    String intervalText = intervals.get(i) + "ms";
                    GuiComponent.drawString(poseStack, font, intervalText, viX, viY + yOffset, textColor);
                    yOffset += 12;
                }
            }
        }

        // Is session over?
        int phase = Timer.currentPhase();

        if (phase == Timer.getTotalPhases() + 1) {

            String sessionOver = "Session Over!";
            String thankYou = "Thank you for participating!";
            int width = screenWidth - font.width(thankYou) - padding;
            int height = (screenHeight / 2) - lineHeight;


            GuiComponent.drawString(poseStack, font, sessionOver, width, height, textColor);

        } else {
            int[] widths = new int[4];
            int[] heights = new int[4];
            String[] strings = new String[4];
            Arrays.fill(widths, 0);

            // Time Elapsed
            String timeElapsed = "Time: " + Timer.timeString();
            strings[0] = timeElapsed;
            int timeWidth = font.width(timeElapsed);
            widths[0] = timeWidth;
            heights[0] = (screenHeight / 2) - ((heights.length * lineHeight) / 2) - ((linePadding * (heights.length - 1)));

            // Current Phase
            String newPhase = "Phase: " + Timer.currentPhaseString();
            strings[1] = newPhase;
            int phaseWidth = font.width(newPhase);
            widths[1] = phaseWidth;
            heights[1] = heights[0] + lineHeight + linePadding;

            // Current Points
            String pts = "Points: " + numPts;
            strings[2] = pts;
            int ptsWidth = font.width(pts);
            widths[2] = ptsWidth;
            heights[2] = heights[1] + lineHeight + linePadding;

            // Add Money Display
            String coins = "Money: " + getFormattedCoins();
            strings[3] = coins;
            widths[3] = font.width(coins);
            heights[3] = heights[2] + lineHeight + linePadding;

            int maxWidth = Arrays.stream(widths).max().getAsInt();

            for (int i = 0; i < strings.length; i++) {
                GuiComponent.drawString(poseStack, font, strings[i], screenWidth - maxWidth - padding, heights[i], textColor);
            }
        }
    }));

    public static int getPts() {
        return numPts;
    }

    public static void incrementPts(int x) {
        System.out.println("-----------------------------------------");
        System.out.println("Increment points was called with x = " + x);
        System.out.println("-----------------------------------------");
        numPts += x;
    }

    public static void endSession() {
        numPts = 0;
        currentPhase = 0;
    }

}
