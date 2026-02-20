package dev.cosmicmod.client.gui.casino;

import dev.cosmicmod.client.CosmicConfig;
import dev.cosmicmod.client.gui.CasinoScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SlotsGame implements ICasinoGame {
    private static final int ROWS = 7;
    private static final int COLS = 8;
    private static final Random RANDOM = new Random();

    private static final Item[] SLOT_ITEMS = {
            Items.COAL,
            Items.IRON_INGOT,
            Items.GOLD_INGOT,
            Items.REDSTONE,
            Items.LAPIS_LAZULI,
            Items.DIAMOND,
            Items.EMERALD,
            Items.ENDER_EYE, // MULTIPLIER
            Items.TRIAL_KEY, // FREE SPIN
            Items.OMINOUS_TRIAL_KEY // SUPER FREE SPIN
    };

    private static final int SYMBOL_COUNT = 7;
    private static final int MULTIPLIER_INDEX = 7;
    private static final int FREE_SPIN_INDEX = 8;
    private static final int SUPER_FREE_SPIN_INDEX = 9;

    private final CasinoScreen screen;
    
    // Slots State
    private final int[][] slotsGrid = new int[ROWS][COLS];
    private final int[][] gridMultipliers = new int[ROWS][COLS];
    private final float[] columnProgress = new float[COLS];
    private final boolean[] columnFinished = new boolean[COLS];
    private boolean isSpinning = false;
    private boolean autoSpin = false;
    private int autoSpinsLeft = 0;
    private boolean showAutoSpinMenu = false;
    private boolean showSettingsMenu = false;
    private boolean showInfoMenu = false;
    private int currentBet = 10;
    private long lastWinAmount = 0;
    private float winAnimScale = 0.0f;
    private boolean boostedSpins = false;
    private boolean showBonusMenu = false;
    
    private final List<CasinoScreen.SlotButton> slotButtons = new ArrayList<>();
    private final List<CasinoScreen.AutoSpinButton> autoSpinButtons = new ArrayList<>();
    private final List<CasinoScreen.SlotButton> settingsButtons = new ArrayList<>();
    private final List<CasinoScreen.SlotButton> bonusButtons = new ArrayList<>();
    private final List<List<int[]>> winningClusters = new ArrayList<>();

    private int freeSpinsLeft = 0;
    private int totalFreeSpins = 0;
    private long totalBonusWin = 0;
    private boolean isBonusRound = false;
    private boolean isBonusEntrance = false;
    private int bonusEntranceTick = 0;
    private boolean isBonusSummary = false;
    private final int[][] stickyMultipliers = new int[ROWS][COLS];
    private boolean isSuperBonus = false;
    private boolean isTumbling = false;
    private boolean isFallingOut = false;
    private final boolean[][] winningCells = new boolean[ROWS][COLS];
    private final float[][] tumbleOffset = new float[ROWS][COLS]; // Offset for falling symbols during tumble
    private final float[][] fallOutOffset = new float[ROWS][COLS]; // Offset for winning symbols falling out
    private final int[][] fallingOutGrid = new int[ROWS][COLS]; // Store symbols that are falling out

    private int spinTickCounter = 0;
    private long baseWinAmount = 0;
    private long animatedDisplayWin = 0;
    private final List<FlyingMultiplier> flyingMultipliers = new ArrayList<>();
    private int winAnimStage = 0; // 0: None, 1: Base Win Expanding, 2: Flying Multipliers, 3: Final Total Show, 4: SENSATIONAL
    private int multiAnimTick = 0;
    private float sensationalScale = 0;
    private float bannerScale = 0;

    public static class FlyingMultiplier {
        int value;
        float startX, startY;
        float progress = 0;
        public FlyingMultiplier(int value, float startX, float startY) {
            this.value = value;
            this.startX = startX;
            this.startY = startY;
        }
    }

    public SlotsGame(CasinoScreen screen) {
        this.screen = screen;
        initializeSlots();
        for (int i = 0; i < COLS; i++) {
            columnProgress[i] = 1.0f;
            columnFinished[i] = true;
        }
    }

    private void initializeSlots() {
        if (!isBonusRound) {
            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    gridMultipliers[r][c] = 1;
                }
            }
        }

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (isBonusRound && slotsGrid[r][c] == MULTIPLIER_INDEX) {
                    continue; // Keep sticky multipliers in bonus rounds
                }
                slotsGrid[r][c] = generateRandomSymbol();
            }
        }
    }

    private int generateRandomSymbol() {
        int roll = RANDOM.nextInt(1000);
        int fsChance = 10;
        int sfsChance = 2; // 0.2% chance for super free spin
        int multiChance = 30; // 3% chance for a multiplier bottle
        if (boostedSpins) {
            fsChance = 50;
            sfsChance = 10;
            multiChance = 60;
        }

        if (roll < sfsChance) {
            return SUPER_FREE_SPIN_INDEX;
        } else if (roll < sfsChance + fsChance) {
            return FREE_SPIN_INDEX;
        } else if (roll < sfsChance + fsChance + multiChance) {
            return MULTIPLIER_INDEX;
        } else {
            return RANDOM.nextInt(SYMBOL_COUNT);
        }
    }

    @Override
    public void init() {
        slotButtons.clear();
        settingsButtons.clear();
        autoSpinButtons.clear();
        bonusButtons.clear();

        slotButtons.add(new CasinoScreen.SlotButton(screen.getLeft() + screen.getGuiWidth() - 65, screen.getTop() + screen.getGuiHeight() - 35, 50, 20, "SPIN", () -> {
            int cost = currentBet;
            if (boostedSpins) cost *= 2.0;
            spin(cost);
        }, () -> boostedSpins));
        slotButtons.add(new CasinoScreen.SlotButton(screen.getLeft() + screen.getGuiWidth() - 115, screen.getTop() + screen.getGuiHeight() - 35, 45, 20, "AUTO", () -> showAutoSpinMenu = !showAutoSpinMenu));
        slotButtons.add(new CasinoScreen.SlotButton(screen.getLeft() + screen.getGuiWidth() - 165, screen.getTop() + screen.getGuiHeight() - 35, 45, 20, "SET", () -> showSettingsMenu = !showSettingsMenu));
        slotButtons.add(new CasinoScreen.SlotButton(screen.getLeft() + screen.getGuiWidth() - 215, screen.getTop() + screen.getGuiHeight() - 35, 45, 20, "BONUS", () -> showBonusMenu = !showBonusMenu));

        int bonusMenuX = screen.getLeft() + screen.getGuiWidth() + 5;
        int bonusMenuY = screen.getTop() + 65;
        bonusButtons.add(new CasinoScreen.SlotButton(bonusMenuX, bonusMenuY, 90, 20, "BUY FS (" + screen.formatNumberPublic(currentBet * 100) + ")", () -> startBonusBuy(100, false)));
        bonusButtons.add(new CasinoScreen.SlotButton(bonusMenuX, bonusMenuY + 25, 90, 20, "SUPER FS (" + screen.formatNumberPublic(currentBet * 500) + ")", () -> startBonusBuy(500, true)));
        bonusButtons.add(new CasinoScreen.SlotButton(bonusMenuX, bonusMenuY + 50, 90, 20, "BOOSTED: " + (boostedSpins ? "ON" : "OFF"), () -> {
            boostedSpins = !boostedSpins;
            this.init();
        }));
        
        int settingsX = screen.getLeft() + screen.getGuiWidth() + 5;
        int settingsY = screen.getTop() + 65;
        settingsButtons.add(new CasinoScreen.SlotButton(settingsX, settingsY + 35, 20, 20, "-", () -> changeBet(-10)));
        settingsButtons.add(new CasinoScreen.SlotButton(settingsX + 25, settingsY + 35, 20, 20, "+", () -> changeBet(10)));
        settingsButtons.add(new CasinoScreen.SlotButton(settingsX + 50, settingsY + 35, 30, 20, "MAX", () -> {
            currentBet = 1000;
            this.init();
        }));

        int menuX = screen.getLeft() + screen.getGuiWidth() + 5;
        int menuY = screen.getTop() + 65;
        int[] counts = {10, 25, 50, 100, 500, 1000};
        for (int count : counts) {
            autoSpinButtons.add(new CasinoScreen.AutoSpinButton(menuX, menuY, 60, 20, String.valueOf(count), () -> startAutoSpin(count)));
            menuY += 22;
        }
    }

    private void startBonusBuy(int multiplier, boolean superBonus) {
        if (isSpinning) return;
        int cost = currentBet * multiplier;
        CosmicConfig config = CosmicConfig.getInstance();
        if (config.casinoCoins >= cost) {
            config.casinoCoins -= cost;
            triggerBonus(superBonus);
            CosmicConfig.save();
        }
    }

    private void triggerBonus(boolean superBonus) {
        isBonusEntrance = true;
        bonusEntranceTick = 100;
        totalBonusWin = 0;
        totalFreeSpins = 10;
        freeSpinsLeft = 10;
        isSuperBonus = superBonus;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                gridMultipliers[r][c] = isSuperBonus ? 2 : 1;
            }
        }
    }

    private void startBonusInternal() {
        isBonusRound = true;
        spin(0);
    }

    private void changeBet(int amount) {
        if (isSpinning) return;
        currentBet = Math.max(10, Math.min(1000, currentBet + amount));
        this.init();
    }

    private void startAutoSpin(int count) {
        autoSpin = true;
        autoSpinsLeft = count;
        showAutoSpinMenu = false;
        if (!isSpinning) {
            spin(currentBet);
        }
    }

    private void spin(int cost) {
        if (isSpinning) return;
        CosmicConfig config = CosmicConfig.getInstance();
        if (cost == 0 || config.casinoCoins >= cost) {
            if (cost > 0) config.casinoCoins -= cost;
            isSpinning = true;
            lastWinAmount = 0;
            winningClusters.clear();
            for (int i = 0; i < COLS; i++) {
                columnProgress[i] = -0.05f * i;
                columnFinished[i] = false;
            }
            initializeSlots();
            CosmicConfig.save();
            if (autoSpin && cost != 0) {
                autoSpinsLeft--;
            }
        } else if (autoSpin) {
            autoSpin = false;
            autoSpinsLeft = 0;
        }
    }

    private void onSpinFinished() {
        isSpinning = false;
        checkWins();
    }

    private void checkWins() {
        if (winAnimStage != 0) return;
        
        lastWinAmount = 0;
        winAnimScale = 0.0f;
        baseWinAmount = 0;
        animatedDisplayWin = 0;
        flyingMultipliers.clear();
        winningClusters.clear();
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                winningCells[r][c] = false;
            }
        }

        boolean[][] visited = new boolean[ROWS][COLS];
        long totalWin = 0;
        int totalMultiplier = 0;

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (slotsGrid[r][c] == MULTIPLIER_INDEX) {
                    if (isBonusRound) {
                        totalMultiplier += gridMultipliers[r][c];
                        // Do not mark as winningCell in bonus round to prevent falling out
                    } else {
                        totalMultiplier += 2; // Multiplier bottles add 2x each in normal spins
                        winningCells[r][c] = true;
                    }
                }
            }
        }

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (visited[r][c]) continue;
                int symbol = slotsGrid[r][c];
                if (symbol >= FREE_SPIN_INDEX || symbol == MULTIPLIER_INDEX) continue; // FREE_SPIN_INDEX or beyond

                List<int[]> cluster = new ArrayList<>();
                findCluster(r, c, symbol, visited, cluster);

                if (cluster.size() >= 4) {
                    winningClusters.add(cluster);
                    // Adjust cluster win calculation to match new indices and SYMBOL_COUNT
                    long symbolValue = symbol + 1; 
                    long clusterWin = (long) cluster.size() * symbolValue * (currentBet / 2);
                    
                    for (int[] pos : cluster) {
                        winningCells[pos[0]][pos[1]] = true;
                    }
                    totalWin += clusterWin;
                }
            }
        }

        if (totalMultiplier > 0) {
            totalWin *= totalMultiplier;
        } else {
            // Apply base multipliers from the grid if any (for bonus rounds or special states)
            // In the old system, we'll just use a multiplier of 1 if no bottles are present
            // unless we want to keep the grid-based ones as well? 
            // Reverting to "old" usually means removing the grid-based doubling.
        }

        int fsCount = 0;
        int sfsCount = 0;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (slotsGrid[r][c] == FREE_SPIN_INDEX) fsCount++;
                if (slotsGrid[r][c] == SUPER_FREE_SPIN_INDEX) sfsCount++;
            }
        }

        if (sfsCount >= 4 && !isBonusRound) {
            triggerBonus(true);
            return;
        }

        if (fsCount >= 4 && !isBonusRound) {
            triggerBonus(false);
            return;
        }

        if (totalWin > 0) {
            lastWinAmount = totalWin;
            isTumbling = false;
            isFallingOut = false;
            if (isBonusRound) {
                totalBonusWin += lastWinAmount;
            }

            CosmicConfig.getInstance().casinoCoins += lastWinAmount;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.0f));
            winAnimScale = 0.1f;
            winAnimStage = 1;
        } else if (isTumbling) {
            isTumbling = false;
            onTumbleFinished();
        } else if (isFallingOut) {
            isFallingOut = false;
            startTumble();
        } else {
            // No wins, check if we need to continue bonus or auto-spin
            onTumbleFinished();
        }
    }

    private void onTumbleFinished() {
        if (isBonusRound && freeSpinsLeft > 0) {
            freeSpinsLeft--;
            if (freeSpinsLeft > 0) {
                spinTickCounter = 20;
            } else {
                isBonusRound = false;
                isBonusSummary = true;
                // Clear sticky multipliers after bonus summary
                for (int r = 0; r < ROWS; r++) {
                    for (int c = 0; c < COLS; c++) {
                        gridMultipliers[r][c] = 1;
                    }
                }
            }
        } else if (autoSpin && autoSpinsLeft > 0) {
            int cost = currentBet;
            if (boostedSpins) cost *= 2.0;
            if (CosmicConfig.getInstance().casinoCoins >= cost) {
                spinTickCounter = 20;
            } else {
                autoSpin = false;
                autoSpinsLeft = 0;
            }
        } else if (autoSpin) {
            autoSpin = false;
            autoSpinsLeft = 0;
        }
    }

    private void findCluster(int r, int c, int symbol, boolean[][] visited, List<int[]> cluster) {
        if (r < 0 || r >= ROWS || c < 0 || c >= COLS || visited[r][c]) return;
        int currentSymbol = slotsGrid[r][c];
        
        // FREE_SPIN and MULTIPLIER should not form clusters
        if (currentSymbol >= FREE_SPIN_INDEX || currentSymbol == MULTIPLIER_INDEX || currentSymbol == -1) return;
        if (symbol >= FREE_SPIN_INDEX || symbol == MULTIPLIER_INDEX || symbol == -1) return;

        if (currentSymbol != symbol) return;
        
        visited[r][c] = true;
        cluster.add(new int[]{r, c});
        findCluster(r + 1, c, symbol, visited, cluster);
        findCluster(r - 1, c, symbol, visited, cluster);
        findCluster(r, c + 1, symbol, visited, cluster);
        findCluster(r, c - 1, symbol, visited, cluster);
    }

    private void startFallingOut() {
        isFallingOut = true;
        winAnimScale = 0;
        winningClusters.clear();
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (winningCells[r][c]) {
                    fallingOutGrid[r][c] = slotsGrid[r][c];
                    slotsGrid[r][c] = -1;
                    fallOutOffset[r][c] = 0;
                } else {
                    fallingOutGrid[r][c] = -1;
                }
            }
        }
    }

    @Override
    public void tick() {
        if (isBonusEntrance) {
            bonusEntranceTick--;
            if (bonusEntranceTick <= 0) {
                isBonusEntrance = false;
                startBonusInternal();
            }
        }

        if (isSpinning) {
            boolean allFinished = true;
            for (int i = 0; i < COLS; i++) {
                if (!columnFinished[i]) {
                    columnProgress[i] += 0.04f;
                    if (columnProgress[i] >= 1.0f) {
                        columnProgress[i] = 1.0f;
                        columnFinished[i] = true;
                        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ITEM_PICKUP, 1.0f));
                    }
                    allFinished = false;
                }
            }
            if (allFinished) {
                onSpinFinished();
            }
        } else if (isFallingOut) {
            boolean anyFalling = false;
            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    if (fallingOutGrid[r][c] != -1) {
                        fallOutOffset[r][c] += 0.5f;
                        if (fallOutOffset[r][c] < ROWS + 1) {
                            anyFalling = true;
                        }
                    }
                }
            }
            if (!anyFalling) {
                isFallingOut = false;
                startTumble();
            }
        } else if (isTumbling) {
            boolean anyMoving = false;
            for (int c = 0; c < COLS; c++) {
                for (int r = ROWS - 1; r >= 0; r--) {
                    if (tumbleOffset[r][c] < 0) {
                        tumbleOffset[r][c] += 0.35f;
                        if (tumbleOffset[r][c] >= 0) tumbleOffset[r][c] = 0;
                        anyMoving = true;
                    }
                }
            }
            if (!anyMoving) {
                checkWins();
            }
        } else if ((autoSpin || isBonusRound) && spinTickCounter > 0 && winAnimStage == 0 && winningClusters.isEmpty()) {
            spinTickCounter--;
            if (spinTickCounter == 0) {
                spin(isBonusRound ? 0 : currentBet);
            }
        }

        if (winAnimStage == 0 && winAnimScale > 0) {
            if (winAnimScale < 20.0f) {
                winAnimScale += 0.005f;
            }
            if (!winningClusters.isEmpty()) {
                startFallingOut();
            }
        }
    }

    private void startTumble() {
        isTumbling = true;
        winningClusters.clear();
        
        // Mark cells that are empty
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (winningCells[r][c]) {
                    slotsGrid[r][c] = -1;
                }
            }
        }

        // In Bonus, Ender Eye adds to grid multipliers permanently
        // We do this BEFORE shifting symbols so we know where they were
        if (isBonusRound) {
            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    // If a winning cluster touched a multiplier, it should probably increment?
                    // Actually, usually multipliers increment when THEY land or when a win happens on them.
                    // Previous logic: if (winningCells[r][c] && fallingOutGrid[r][c] == MULTIPLIER_INDEX)
                    // But now MULTIPLIER_INDEX doesn't fall out in bonus.
                    // Let's say if it's on the grid during a win, it increments.
                    if (slotsGrid[r][c] == MULTIPLIER_INDEX && hasAdjacentWin(r, c)) {
                        gridMultipliers[r][c] += 2;
                    }
                }
            }
        }
        
        for (int c = 0; c < COLS; c++) {
            int writePos = ROWS - 1;
            for (int r = ROWS - 1; r >= 0; r--) {
                int symbol = slotsGrid[r][c];
                if (symbol != -1) {
                    // Sticky Multipliers in Bonus Round do not move
                    if (isBonusRound && symbol == MULTIPLIER_INDEX) {
                        // Keep it here, but we need to make sure writePos doesn't skip it or overwrite it
                        continue; 
                    }

                    slotsGrid[r][c] = -1;
                    
                    // Find a free write position (not -1 and not a sticky multiplier)
                    while (writePos >= 0 && (slotsGrid[writePos][c] != -1)) {
                        writePos--;
                    }
                    
                    if (writePos < 0) break;
                    
                    slotsGrid[writePos][c] = symbol;
                    if (writePos != r) {
                        tumbleOffset[writePos][c] = -(writePos - r);
                    }
                    writePos--;
                }
            }
        }

        // Fill new symbols in the empty spaces (excluding sticky multipliers)
        boolean forcedMulti = false;
        if (isBonusRound && isSuperBonus) {
            // Check if any symbols are being filled
            for (int c = 0; c < COLS; c++) {
                for (int r = 0; r < ROWS; r++) {
                    if (slotsGrid[r][c] == -1) {
                        forcedMulti = true;
                        break;
                    }
                }
                if (forcedMulti) break;
            }
        }

        for (int c = 0; c < COLS; c++) {
            for (int r = ROWS - 1; r >= 0; r--) {
                if (slotsGrid[r][c] == -1) {
                    int symbol = generateRandomSymbol();
                    if (forcedMulti) {
                        symbol = MULTIPLIER_INDEX;
                        forcedMulti = false;
                    }
                    slotsGrid[r][c] = symbol;
                    tumbleOffset[r][c] = -ROWS;
                }
            }
        }
        
        // Reset multipliers if not in bonus
        if (!isBonusRound) {
            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    gridMultipliers[r][c] = 1;
                }
            }
        }
    }

    private boolean hasAdjacentWin(int r, int c) {
        int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        for (int[] d : dirs) {
            int nr = r + d[0];
            int nc = c + d[1];
            if (nr >= 0 && nr < ROWS && nc >= 0 && nc < COLS && winningCells[nr][nc]) {
                return true;
            }
        }
        // Also if the cell itself is part of a win (though multipliers aren't in bonus)
        return winningCells[r][c];
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int areaX = screen.getLeft() + 115;
        int areaY = screen.getTop() + 45;
        int areaWidth = screen.getGuiWidth() - 130;
        int areaHeight = screen.getGuiHeight() - 60;

        if (isBonusRound) {
            String bonusText = "SPINS LEFT: " + freeSpinsLeft;
            String bonusWinText = "TOTAL WIN: " + screen.formatNumberPublic(totalBonusWin);
            int bonusTextWidth = Minecraft.getInstance().font.width(bonusText);
            guiGraphics.drawString(Minecraft.getInstance().font, bonusText, areaX + areaWidth - bonusTextWidth - 10, areaY, 0xFFFF55FF, false);
            guiGraphics.drawString(Minecraft.getInstance().font, bonusWinText, areaX + 100, areaY, 0xFF55FF55, false);
        }

        renderSlots(guiGraphics, areaX, areaY + 15, areaWidth, areaHeight - 40);
        String betText = "BET: " + screen.formatNumberPublic(currentBet);
        guiGraphics.drawString(Minecraft.getInstance().font, betText, areaX + 10, screen.getTop() + screen.getGuiHeight() - 30, 0xFFFFFFFF, false);

        for (CasinoScreen.SlotButton btn : slotButtons) {
            btn.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        if (showSettingsMenu) {
            int settingsX = screen.getLeft() + screen.getGuiWidth() + 5;
            int settingsY = screen.getTop() + 65;
            screen.renderRoundedRectPublic(guiGraphics, settingsX - 5, settingsY - 5, 85, 70, 0xEE101015);
            String betLabel = "BET: " + screen.formatNumberPublic(currentBet);
            int labelW = Minecraft.getInstance().font.width(betLabel);
            guiGraphics.drawString(Minecraft.getInstance().font, betLabel, settingsX + 30 - labelW / 2, settingsY + 10, 0xFFFFFFFF, false);
            for (CasinoScreen.SlotButton btn : settingsButtons) {
                btn.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }

        if (showAutoSpinMenu) {
            int menuX = screen.getLeft() + screen.getGuiWidth() + 5;
            int menuY = screen.getTop() + 65;
            screen.renderRoundedRectPublic(guiGraphics, menuX - 5, menuY - 5, 70, 145, 0xEE101015);
            for (CasinoScreen.AutoSpinButton btn : autoSpinButtons) {
                btn.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }

        if (showBonusMenu) {
            int menuX = screen.getLeft() + screen.getGuiWidth() + 5;
            int menuY = screen.getTop() + 65;
            screen.renderRoundedRectPublic(guiGraphics, menuX - 5, menuY - 5, 100, 85, 0xEE101015);
            for (CasinoScreen.SlotButton btn : bonusButtons) {
                btn.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }

        renderInfoMenu(guiGraphics);

        if (winAnimStage > 0 && !isSpinning) {
            renderWinAnimation(guiGraphics);
        }
        if (isBonusEntrance) {
            renderBonusEntrance(guiGraphics);
        }
        if (isBonusSummary) {
            renderBonusSummary(guiGraphics);
        }
    }

    private void renderWinAnimation(GuiGraphics guiGraphics) {
        if (winAnimStage == 1) {
            winAnimScale += 0.015f;
            if (winAnimScale >= 4.0f) {
                winAnimScale = 4.0f;
                winAnimStage = lastWinAmount >= currentBet * 50 ? 4 : 3;
            }
        } else if (winAnimStage == 3) {
            winAnimScale += 0.01f;
            if (winAnimScale >= 10.0f) {
                winAnimStage = 0;
            }
        } else if (winAnimStage == 4) {
            sensationalScale = Math.min(2.0f, sensationalScale + 0.05f);
            bannerScale = Math.min(1.0f, bannerScale + 0.05f);
            multiAnimTick++;
            if (multiAnimTick > 150) {
                winAnimStage = 0;
                sensationalScale = 0;
                bannerScale = 0;
            }
        }

        if (winAnimStage == 4) {
            renderSensationalWin(guiGraphics);
            return;
        }

        float scale = winAnimScale;
        if (winAnimStage == 3 && scale > 5.0f) scale = 10.0f - scale;
        if (scale < 0) scale = 0;

        if (scale > 0) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(screen.width / 2f, screen.height / 2f, 500);
            guiGraphics.pose().scale(scale * 2, scale * 2, 1.0f);
            String winMsg = "+" + screen.formatNumberPublic(lastWinAmount);
            int msgWidth = Minecraft.getInstance().font.width(winMsg);
            guiGraphics.drawString(Minecraft.getInstance().font, winMsg, -msgWidth / 2, -4, 0xFF55FF55, true);
            guiGraphics.pose().popPose();
        }
    }

    private void renderSensationalWin(GuiGraphics guiGraphics) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(screen.width / 2f, screen.height / 2f, 700);
        float starScale = sensationalScale * 0.5f;
        for (int i = -1; i <= 1; i++) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(i * 40, -60, 0);
            guiGraphics.pose().scale(starScale, starScale, 1.0f);
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, "★", 0, -4, 0xFFFFFF00);
            guiGraphics.pose().popPose();
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(sensationalScale, sensationalScale, 1.0f);
        String text = "SENSATIONAL!";
        int[] colors = {0xFFFF5555, 0xFFFFFF55, 0xFF55FF55, 0xFF55FFFF, 0xFF5555FF, 0xFFFF55FF};
        int totalW = 0;
        for (int i = 0; i < text.length(); i++) {
            String charStr = String.valueOf(text.charAt(i));
            int charW = Minecraft.getInstance().font.width(charStr);
            guiGraphics.drawString(Minecraft.getInstance().font, charStr, -Minecraft.getInstance().font.width(text)/2 + totalW, -20, colors[i % colors.length], true);
            totalW += charW;
        }
        guiGraphics.pose().popPose();
        if (bannerScale > 0) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 30, 0);
            guiGraphics.pose().scale(bannerScale * 1.5f, bannerScale * 1.5f, 1.0f);
            String amountStr = "$" + screen.formatNumberPublic(lastWinAmount) + ".00";
            int amountW = Minecraft.getInstance().font.width(amountStr);
            screen.renderRoundedRectPublic(guiGraphics, -amountW / 2 - 10, -10, amountW + 20, 20, 0xFFFF55FF);
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, amountStr, 0, -4, 0xFFFFFFFF);
            guiGraphics.pose().popPose();
        }
        guiGraphics.pose().popPose();
    }

    private void renderBonusEntrance(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, screen.width, screen.height, 0xAA000000);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(screen.width / 2f, screen.height / 2f, 600);
        guiGraphics.pose().scale(2.0f, 2.0f, 1.0f);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, "BONUS ROUND!", 0, -20, 0xFFFF55FF);
        guiGraphics.pose().popPose();
        for (int i = 0; i < 4; i++) {
            float progress = (100 - bonusEntranceTick) / 100f;
            progress = Math.min(1.0f, progress * 1.5f - (i * 0.1f));
            if (progress < 0) continue;
            int fsX = screen.width / 2 - 60 + i * 40;
            int fsY = (int) (-50 + (screen.height / 2 + 20 + 50) * progress);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(fsX, fsY, 610);
            guiGraphics.pose().scale(1.5f, 1.5f, 1.0f);
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, "FS", 0, 0, 0xFF55FFFF);
            guiGraphics.pose().popPose();
        }
    }

    private void renderBonusSummary(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, screen.width, screen.height, 0xAA000000);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(screen.width / 2f, screen.height / 2f, 600);
        guiGraphics.pose().scale(2.0f, 2.0f, 1.0f);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, "BONUS COMPLETE", 0, -30, 0xFFFF55FF);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, "TOTAL WIN: " + screen.formatNumberPublic(totalBonusWin), 0, 0, 0xFFFFFF00);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, "IN " + totalFreeSpins + " SPINS", 0, 20, 0xFFFFFFFF);
        guiGraphics.pose().popPose();
    }

    private void renderInfoMenu(GuiGraphics guiGraphics) {
        int infoX = screen.getLeft() - 155;
        int infoY = screen.getTop() + 10;
        int infoWidth = 150;
        int infoHeight = screen.getGuiHeight() - 20;

        screen.renderRoundedRectPublic(guiGraphics, infoX, infoY, infoWidth, infoHeight, 0xEE101015);
        
        int drawY = infoY + 10;
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, "GAME INFO", infoX + infoWidth / 2, drawY, 0xFFFFCC00);
        drawY += 15;
        
        String[] infoLines = {
            "Cluster Pays: 4+ items",
            "connected horiz/vert.",
            "",
            "SYMBOL VALUES:",
            "Coal: 1x",
            "Iron: 2x",
            "Gold: 3x",
            "Redstone: 4x",
            "Lapis: 5x",
            "Diamond: 6x",
            "Emerald: 7x",
            "",
            "SPECIALS:",
            "Ender Eye: Multiplier",
            "  * Sticks in Bonus Rounds",
            "  * Normal: 2x boost",
            "  * Bonus: Adds to position",
            "Trial Key: 4+ = Bonus",
            "Ominous Key: 4+ = Super Bonus",
            "",
            "Super Bonus starts with",
            "2x grid multipliers."
        };

        for (String line : infoLines) {
            guiGraphics.drawString(Minecraft.getInstance().font, line, infoX + 10, drawY, 0xFFCCCCCC, false);
            drawY += 10;
        }
    }

    private void renderSlots(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        int cellSize = Math.min(width / COLS, height / ROWS);
        int gridWidth = COLS * cellSize;
        int gridHeight = ROWS * cellSize;
        int gridX = x + (width - gridWidth) / 2;
        int gridY = y + (height - gridHeight) / 2;

        int hoveredCol = -1;
        int hoveredRow = -1;
        double mx = Minecraft.getInstance().mouseHandler.xpos() * (double) Minecraft.getInstance().getWindow().getGuiScaledWidth() / (double) Minecraft.getInstance().getWindow().getScreenWidth();
        double my = Minecraft.getInstance().mouseHandler.ypos() * (double) Minecraft.getInstance().getWindow().getGuiScaledHeight() / (double) Minecraft.getInstance().getWindow().getScreenHeight();

        if (mx >= gridX && mx < gridX + gridWidth && my >= gridY && my < gridY + gridHeight) {
            hoveredCol = (int) ((mx - gridX) / cellSize);
            hoveredRow = (int) ((my - gridY) / cellSize);
        }

        screen.renderRoundedRectPublic(guiGraphics, gridX - 5, gridY - 5, gridWidth + 10, gridHeight + 10, 0xFF000000); 

        // Draw hover highlight
        if (hoveredCol != -1 && hoveredRow != -1) {
            // Highlight 1x1 area
            guiGraphics.fill(gridX + hoveredCol * cellSize, gridY + hoveredRow * cellSize, gridX + (hoveredCol + 1) * cellSize, gridY + (hoveredRow + 1) * cellSize, 0x33FFFFFF);
        }

        if (!isSpinning && !winningClusters.isEmpty() && !isFallingOut) {
            for (List<int[]> cluster : winningClusters) {
                for (int[] pos : cluster) {
                    int cellX = gridX + pos[1] * cellSize;
                    int cellY = gridY + pos[0] * cellSize;
                    guiGraphics.fill(cellX, cellY, cellX + cellSize, cellY + cellSize, 0x44FFFF00);
                }
            }
            for (List<int[]> cluster : winningClusters) {
                for (int i = 0; i < cluster.size(); i++) {
                    for (int j = i + 1; j < cluster.size(); j++) {
                        int[] p1 = cluster.get(i);
                        int[] p2 = cluster.get(j);
                        if (Math.abs(p1[0] - p2[0]) + Math.abs(p1[1] - p2[1]) == 1) {
                            int x1 = gridX + p1[1] * cellSize + cellSize / 2;
                            int y1 = gridY + p1[0] * cellSize + cellSize / 2;
                            int x2 = gridX + p2[1] * cellSize + cellSize / 2;
                            int y2 = gridY + p2[0] * cellSize + cellSize / 2;
                            int minX = Math.min(x1, x2);
                            int minY = Math.min(y1, y2);
                            int maxX = Math.max(x1, x2);
                            int maxY = Math.max(y1, y2);
                            if (x1 == x2) {
                                guiGraphics.fill(x1 - 2, minY, x1 + 2, maxY, 0xAAFFFFFF);
                            } else {
                                guiGraphics.fill(minX, y1 - 2, maxX, y1 + 2, 0xAAFFFFFF);
                            }
                        }
                    }
                }
            }
        }

        for (int c = 0; c < COLS; c++) {
            float progress = columnProgress[c];
            if (progress < 0) continue;
            for (int r = 0; r < ROWS; r++) {
                int cellX = gridX + c * cellSize;
                int targetY = gridY + r * cellSize;
                int startY = targetY - 100;
                int currentY = (int) (startY + (targetY - startY) * progress);

                // Add tumble offset
                if (isTumbling) {
                    currentY += (int) (tumbleOffset[r][c] * cellSize);
                }

                int symbol = slotsGrid[r][c];
                renderSymbol(guiGraphics, symbol, cellX, currentY, cellSize);

                // Render falling out symbols
                if (isFallingOut) {
                    int foSymbol = fallingOutGrid[r][c];
                    if (foSymbol != -1) {
                        int foY = targetY + (int) (fallOutOffset[r][c] * cellSize);
                        renderSymbol(guiGraphics, foSymbol, cellX, foY, cellSize);
                    }
                }
            }
        }
        renderTextLayer(guiGraphics, gridX, gridY, cellSize);
    }

    private void renderSymbol(GuiGraphics guiGraphics, int symbol, int cellX, int currentY, int cellSize) {
        if (symbol != -1) {
            if (symbol < 0 || symbol >= SLOT_ITEMS.length) return;
            ItemStack stack = new ItemStack(SLOT_ITEMS[symbol]);
            int itemX = cellX + (cellSize - 20) / 2;
            int itemY = currentY + (cellSize - 20) / 2;
            
            if (symbol == FREE_SPIN_INDEX || symbol == SUPER_FREE_SPIN_INDEX) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(itemX, itemY, 100);
                guiGraphics.pose().scale(1.5f, 1.5f, 1.0f);
                guiGraphics.renderItem(stack, 0, 0);
                guiGraphics.pose().popPose();
            } else if (symbol < SYMBOL_COUNT || symbol == MULTIPLIER_INDEX) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(itemX, itemY, 100);
                guiGraphics.pose().scale(1.5f, 1.5f, 1.0f);
                guiGraphics.renderItem(stack, 0, 0);
                guiGraphics.pose().popPose();
            }
        }
    }

    private void renderTextLayer(GuiGraphics guiGraphics, int gridX, int gridY, int cellSize) {
        for (int c = 0; c < COLS; c++) {
            float progress = columnProgress[c];
            if (progress < 0) continue;
            for (int r = 0; r < ROWS; r++) {
                int cellX = gridX + c * cellSize;
                int targetY = gridY + r * cellSize;
                int startY = targetY - 100;
                int currentY = (int) (startY + (targetY - startY) * progress);

                if (isTumbling) {
                    currentY += (int) (tumbleOffset[r][c] * cellSize);
                }

                int symbol = slotsGrid[r][c];
                if (symbol != -1) {
                    if (symbol == FREE_SPIN_INDEX || symbol == SUPER_FREE_SPIN_INDEX) {
                        guiGraphics.pose().pushPose();
                        guiGraphics.pose().translate(cellX + cellSize / 2f, currentY + cellSize / 2f, 300);
                        String fText = (symbol == SUPER_FREE_SPIN_INDEX) ? "SFS" : "FS";
                        int fWidth = Minecraft.getInstance().font.width(fText);
                        guiGraphics.drawString(Minecraft.getInstance().font, fText, -fWidth / 2, -4, 0xFF55FFFF, true);
                        guiGraphics.pose().popPose();
                    } else if (symbol == MULTIPLIER_INDEX) {
                        guiGraphics.pose().pushPose();
                        guiGraphics.pose().translate(cellX + cellSize / 2f, currentY + cellSize / 2f + 5, 300);
                        guiGraphics.pose().scale(0.8f, 0.8f, 1.0f);
                        String mText = (isBonusRound && gridMultipliers[r][c] > 1) ? gridMultipliers[r][c] + "x" : "2x";
                        guiGraphics.drawCenteredString(Minecraft.getInstance().font, mText, 0, 0, 0xFFFFCC00);
                        guiGraphics.pose().popPose();
                    }
                }

                if (isFallingOut) {
                    int foSymbol = fallingOutGrid[r][c];
                    if (foSymbol != -1) {
                        int foY = targetY + (int) (fallOutOffset[r][c] * cellSize);
                        if (foSymbol == FREE_SPIN_INDEX || foSymbol == SUPER_FREE_SPIN_INDEX) {
                            guiGraphics.pose().pushPose();
                            guiGraphics.pose().translate(cellX + cellSize / 2f, foY + cellSize / 2f, 300);
                            String fText = (foSymbol == SUPER_FREE_SPIN_INDEX) ? "SFS" : "FS";
                            int fWidth = Minecraft.getInstance().font.width(fText);
                            guiGraphics.drawString(Minecraft.getInstance().font, fText, -fWidth / 2, -4, 0xFF55FFFF, true);
                            guiGraphics.pose().popPose();
                        } else if (foSymbol == MULTIPLIER_INDEX) {
                            guiGraphics.pose().pushPose();
                            guiGraphics.pose().translate(cellX + cellSize / 2f, foY + cellSize / 2f + 5, 300);
                            guiGraphics.pose().scale(0.8f, 0.8f, 1.0f);
                            String mText = (isBonusRound && gridMultipliers[r][c] > 1) ? gridMultipliers[r][c] + "x" : "2x";
                            guiGraphics.drawCenteredString(Minecraft.getInstance().font, mText, 0, 0, 0xFFFFCC00);
                            guiGraphics.pose().popPose();
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 32) { // SPACE bar
            double mx = Minecraft.getInstance().mouseHandler.xpos() * (double) Minecraft.getInstance().getWindow().getGuiScaledWidth() / (double) Minecraft.getInstance().getWindow().getScreenWidth();
            double my = Minecraft.getInstance().mouseHandler.ypos() * (double) Minecraft.getInstance().getWindow().getGuiScaledHeight() / (double) Minecraft.getInstance().getWindow().getScreenHeight();
            
            int areaX = screen.getLeft() + 115;
            int areaY = screen.getTop() + 45;
            int areaWidth = screen.getGuiWidth() - 130;
            int areaHeight = screen.getGuiHeight() - 60;
            int cellSize = Math.min(areaWidth / COLS, (areaHeight - 40) / ROWS);
            int gridWidth = COLS * cellSize;
            int gridHeight = ROWS * cellSize;
            int gridX = areaX + (areaWidth - gridWidth) / 2;
            int gridY = areaY + 15 + ((areaHeight - 40) - gridHeight) / 2;

            if (mx >= gridX && mx < gridX + gridWidth && my >= gridY && my < gridY + gridHeight) {
                int col = (int) ((mx - gridX) / cellSize);
                int row = (int) ((my - gridY) / cellSize);

                if (row >= 0 && row < ROWS && col >= 0 && col < COLS) {
                    int symbol = slotsGrid[row][col];

                    if (symbol >= 0 && symbol < SLOT_ITEMS.length) {
                        String itemName;
                        if (symbol == MULTIPLIER_INDEX) {
                            int multi = (isBonusRound) ? gridMultipliers[row][col] : 2;
                            itemName = "Multiplier (" + multi + "x)";
                        } else if (symbol == FREE_SPIN_INDEX) {
                            itemName = "Free Spin";
                        } else if (symbol == SUPER_FREE_SPIN_INDEX) {
                            itemName = "Super Free Spin";
                        } else {
                            itemName = SLOT_ITEMS[symbol].getName(SLOT_ITEMS[symbol].getDefaultInstance()).getString();
                        }
                        
                        net.minecraft.network.chat.Component message = net.minecraft.network.chat.Component.literal("§6Slot Info: §f" + itemName);
                        Minecraft.getInstance().player.displayClientMessage(message, false);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isSpinning) {
                for (int i = 0; i < COLS; i++) {
                    columnProgress[i] = 1.0f;
                    columnFinished[i] = true;
                }
                onSpinFinished();
                return true;
            }
            if (isTumbling) {
                for (int c = 0; c < COLS; c++) {
                    for (int r = 0; r < ROWS; r++) {
                        tumbleOffset[r][c] = 0;
                    }
                }
                checkWins();
                return true;
            }
            if (isFallingOut) {
                for (int c = 0; c < COLS; c++) {
                    for (int r = 0; r < ROWS; r++) {
                        fallOutOffset[r][c] = ROWS + 1;
                    }
                }
                isFallingOut = false;
                startTumble();
                return true;
            }

            if (winAnimStage > 0) {
                winAnimStage = 0;
                sensationalScale = 0;
                bannerScale = 0;
                multiAnimTick = 0;
                return true;
            }

            if (isBonusEntrance) {
                isBonusEntrance = false;
                startBonusInternal();
                return true;
            }
            if (isBonusSummary) {
                isBonusSummary = false;
                // Also reset symbols when closing summary if they were still showing multipliers
                initializeSlots();
                return true;
            }

            if (showBonusMenu) {
                for (CasinoScreen.SlotButton btn : bonusButtons) {
                    if (btn.isHovered((int) mouseX, (int) mouseY)) {
                        btn.action.run();
                        return true;
                    }
                }
                showBonusMenu = false;
                return true;
            }

            if (showAutoSpinMenu) {
                for (CasinoScreen.AutoSpinButton btn : autoSpinButtons) {
                    if (btn.isHovered((int) mouseX, (int) mouseY)) {
                        btn.action.run();
                        return true;
                    }
                }
                showAutoSpinMenu = false;
                return true;
            }

            if (showSettingsMenu) {
                for (CasinoScreen.SlotButton btn : settingsButtons) {
                    if (btn.isHovered((int) mouseX, (int) mouseY)) {
                        btn.action.run();
                        return true;
                    }
                }
                showSettingsMenu = false;
                return true;
            }

            for (CasinoScreen.SlotButton btn : slotButtons) {
                if (btn.isHovered((int) mouseX, (int) mouseY)) {
                    btn.action.run();
                    return true;
                }
            }
        } else if (button == 1) { // Right Click
            int areaX = screen.getLeft() + 115;
            int areaY = screen.getTop() + 45;
            int areaWidth = screen.getGuiWidth() - 130;
            int areaHeight = screen.getGuiHeight() - 60;
            int cellSize = Math.min(areaWidth / COLS, (areaHeight - 40) / ROWS);
            int gridWidth = COLS * cellSize;
            int gridHeight = ROWS * cellSize;
            int gridX = areaX + (areaWidth - gridWidth) / 2;
            int gridY = areaY + 15 + ((areaHeight - 40) - gridHeight) / 2;

            if (mouseX >= gridX && mouseX < gridX + gridWidth && mouseY >= gridY && mouseY < gridY + gridHeight) {
                int col = (int) ((mouseX - gridX) / cellSize);
                int row = (int) ((mouseY - gridY) / cellSize);

                if (row >= 0 && row < ROWS && col >= 0 && col < COLS) {
                    int symbol = slotsGrid[row][col];

                    if (symbol >= 0 && symbol < SLOT_ITEMS.length) {
                        String itemName;
                        if (symbol == MULTIPLIER_INDEX) {
                            int multi = (isBonusRound) ? gridMultipliers[row][col] : 2;
                            itemName = "Multiplier (" + multi + "x)";
                        } else if (symbol == FREE_SPIN_INDEX) {
                            itemName = "Free Spin";
                        } else if (symbol == SUPER_FREE_SPIN_INDEX) {
                            itemName = "Super Free Spin";
                        } else {
                            itemName = SLOT_ITEMS[symbol].getName(SLOT_ITEMS[symbol].getDefaultInstance()).getString();
                        }
                        
                        net.minecraft.network.chat.Component message = net.minecraft.network.chat.Component.literal("§6Slot Info: §f" + itemName);
                        Minecraft.getInstance().player.displayClientMessage(message, false);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public String getName() {
        return "Slots";
    }
}
