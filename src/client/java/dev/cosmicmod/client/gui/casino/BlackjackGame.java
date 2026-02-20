package dev.cosmicmod.client.gui.casino;

import dev.cosmicmod.client.CosmicConfig;
import dev.cosmicmod.client.gui.CasinoScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

public class BlackjackGame implements ICasinoGame {
    private final CasinoScreen screen;
    
    // Blackjack State
    private int currentChip = 10;
    private final List<Card> deck = new ArrayList<>();
    private final List<Card> playerHand = new ArrayList<>();
    private final List<Card> dealerHand = new ArrayList<>();
    private final List<BlackjackBet> blackjackBets = new ArrayList<>();
    private final List<FlyingCard> flyingCards = new ArrayList<>();
    private int blackjackState = 0; // 0: Betting, 1: Dealing, 2: Player Turn, 3: Dealer Turn, 4: Result
    private boolean dealerHidden = true;
    private long lastBlackjackWin = 0;
    private float blackjackWinAnimScale = 0;

    private static final ResourceLocation CARD_BACK_BLUE = ResourceLocation.fromNamespaceAndPath("cosmicmod", "textures/gui/casino/cards/back_blue.png");

    public static class Card {
        int rank; // 1-13 (A, 2-10, J, Q, K)
        int suit; // 0-3 (Hearts, Diamonds, Clubs, Spades)
        final ResourceLocation texture;

        Card(int rank, int suit) {
            this.rank = rank;
            this.suit = suit;
            String suitName = switch (suit) {
                case 0 -> "hearts";
                case 1 -> "diamonds";
                case 2 -> "clubs";
                case 3 -> "spades";
                default -> "hearts";
            };
            char suitChar = switch (suit) {
                case 0 -> 'h';
                case 1 -> 'd';
                case 2 -> 'c';
                case 3 -> 's';
                default -> 'h';
            };
            this.texture = ResourceLocation.fromNamespaceAndPath("cosmicmod", "textures/gui/casino/cards/" + suitName + "/" + rank + suitChar + ".png");
        }

        int getValue() {
            if (rank > 10) return 10;
            if (rank == 1) return 11;
            return rank;
        }
    }

    public static class FlyingCard {
        Card card;
        float startX, startY;
        float targetX, targetY;
        float progress = 0;
        boolean toDealer;
        int index;

        public FlyingCard(Card card, float startX, float startY, float targetX, float targetY, boolean toDealer, int index) {
            this.card = card;
            this.startX = startX;
            this.startY = startY;
            this.targetX = targetX;
            this.targetY = targetY;
            this.toDealer = toDealer;
            this.index = index;
        }
    }

    private static class BlackjackBet {
        int type; // 0: Main, 1: Pairs, 2: 21+3
        long amount;
        BlackjackBet(int type, long amount) { this.type = type; this.amount = amount; }
    }

    public BlackjackGame(CasinoScreen screen) {
        this.screen = screen;
    }

    @Override
    public void init() {
    }

    @Override
    public void tick() {
        if (!flyingCards.isEmpty()) {
            boolean anyMoving = false;
            for (int i = 0; i < flyingCards.size(); i++) {
                FlyingCard fc = flyingCards.get(i);
                if (fc.progress < 1.0f) {
                    fc.progress += 0.05f;
                    if (fc.progress >= 1.0f) {
                        fc.progress = 1.0f;
                        if (fc.toDealer) dealerHand.add(fc.card);
                        else playerHand.add(fc.card);
                        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ITEM_PICKUP, 1.0f));
                    }
                    anyMoving = true;
                    break;
                }
            }
            if (!anyMoving) {
                flyingCards.clear();
                if (blackjackState == 1) {
                    blackjackState = 2;
                    checkBlackjackInitial();
                } else if (blackjackState == 3) {
                    processDealerTurn();
                }
            }
        }

        if (blackjackWinAnimScale > 0 && blackjackWinAnimScale < 10.0f) {
            blackjackWinAnimScale += 0.05f;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int areaX = screen.getLeft() + 115;
        int areaY = screen.getTop() + 45 + 15;
        int areaWidth = screen.getGuiWidth() - 130;
        int areaHeight = screen.getGuiHeight() - 60 - 15;

        screen.renderRoundedRectPublic(guiGraphics, areaX + 10, areaY + 10, areaWidth - 20, areaHeight - 20, 0xFF0A4A1A);

        int deckX = areaX + areaWidth - 50;
        int deckY = areaY + 30;
        renderCardBack(guiGraphics, deckX, deckY);
        guiGraphics.drawString(Minecraft.getInstance().font, "DECK", deckX - 5, deckY + 48, 0xFFAAAAAA, false);

        int dealerX = areaX + areaWidth / 2 - 20;
        int dealerY = areaY + 40;
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, "DEALER", areaX + areaWidth / 2, dealerY - 15, 0xFFFFFFFF);
        renderHand(guiGraphics, dealerHand, dealerX, dealerY, dealerHidden && blackjackState < 4);

        int playerX = areaX + areaWidth / 2 - 20;
        int playerY = areaY + areaHeight - 85;
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, "PLAYER (" + getHandValue(playerHand) + ")", areaX + areaWidth / 2, playerY + 50, 0xFFFFFFFF);
        renderHand(guiGraphics, playerHand, playerX, playerY, false);

        if (blackjackState == 0) {
            renderBettingArea(guiGraphics, areaX, areaY, areaWidth, areaHeight, mouseX, mouseY);
        } else if (blackjackState == 2 && flyingCards.isEmpty()) {
            int btnW = 40; int btnH = 15;
            int btnX = areaX + areaWidth / 2 - 45; int btnY = areaY + areaHeight / 2;
            boolean hitHover = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
            screen.renderRoundedRectPublic(guiGraphics, btnX, btnY, btnW, btnH, hitHover ? 0x66FFFFFF : 0x22FFFFFF);
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, "HIT", btnX + btnW / 2, btnY + 4, 0xFFFFFFFF);
            boolean standHover = mouseX >= btnX + 50 && mouseX <= btnX + 50 + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
            screen.renderRoundedRectPublic(guiGraphics, btnX + 50, btnY, btnW, btnH, standHover ? 0x66FFFFFF : 0x22FFFFFF);
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, "STAND", btnX + 50 + btnW / 2, btnY + 4, 0xFFFFFFFF);
        }

        for (FlyingCard fc : flyingCards) {
            if (fc.progress < 1.0f) {
                float curX = fc.startX + (fc.targetX - fc.startX) * fc.progress;
                float curY = fc.startY + (fc.targetY - fc.startY) * fc.progress;
                renderCardBack(guiGraphics, (int)curX, (int)curY);
            }
        }

        if (blackjackState == 4) {
            renderBlackjackResult(guiGraphics, areaX, areaY, areaWidth, areaHeight);
        }

        renderChips(guiGraphics, areaX + areaWidth - 40, areaY + 80, mouseX, mouseY);
        renderInfoMenu(guiGraphics);
    }

    private void renderInfoMenu(GuiGraphics guiGraphics) {
        int infoX = screen.getLeft() - 155;
        int infoY = screen.getTop() + 10;
        int infoWidth = 150;
        int infoHeight = screen.getGuiHeight() - 20;

        screen.renderRoundedRectPublic(guiGraphics, infoX, infoY, infoWidth, infoHeight, 0xEE101015);
        
        int drawY = infoY + 10;
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, "BLACKJACK INFO", infoX + infoWidth / 2, drawY, 0xFFFFCC00);
        drawY += 15;
        
        String[] infoLines = {
            "Goal: Get closer to 21",
            "than the dealer without",
            "going over.",
            "",
            "PAYOUTS:",
            "Win: 1:1",
            "Blackjack: 3:2",
            "Push: Bet returned",
            "",
            "SIDE BETS:",
            "Pairs: 12:1",
            "(Player first 2 cards)",
            "21+3: 10:1 (Flush)",
            "(Player 2 + Dealer 1)"
        };

        for (String line : infoLines) {
            guiGraphics.drawString(Minecraft.getInstance().font, line, infoX + 10, drawY, 0xFFCCCCCC, false);
            drawY += 10;
        }
    }

    private void renderCardBack(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(RenderType::guiTextured, CARD_BACK_BLUE, x, y, 0, 0, 30, 45, 30, 45);
    }

    private void renderHand(GuiGraphics guiGraphics, List<Card> hand, int x, int y, boolean hidden) {
        for (int i = 0; i < hand.size(); i++) {
            int cx = x + i * 20;
            if (hidden && i == 1) {
                renderCardBack(guiGraphics, cx, y);
            } else {
                guiGraphics.blit(RenderType::guiTextured, hand.get(i).texture, cx, y, 0, 0, 30, 45, 30, 45);
            }
        }
    }

    private void renderBettingArea(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY) {
        int cx = x + width / 2;
        int cy = y + height / 2;
        renderBetCircle(guiGraphics, cx, cy + 40, 20, 0, "BET", mouseX, mouseY);
        renderBetCircle(guiGraphics, cx - 60, cy + 20, 15, 1, "PAIRS", mouseX, mouseY);
        renderBetCircle(guiGraphics, cx + 60, cy + 20, 15, 2, "21+3", mouseX, mouseY);

        int dealX = x + 20; int dealY = y + height - 40;
        boolean dealHover = mouseX >= dealX && mouseX <= dealX + 50 && mouseY >= dealY && mouseY <= dealY + 20;
        screen.renderRoundedRectPublic(guiGraphics, dealX, dealY, 50, 20, dealHover ? 0x66FFFFFF : 0x22FFFFFF);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, "DEAL", dealX + 25, dealY + 6, 0xFFFFFFFF);

        int clearX = dealX + 60;
        boolean clearHover = mouseX >= clearX && mouseX <= clearX + 50 && mouseY >= dealY && mouseY <= dealY + 20;
        screen.renderRoundedRectPublic(guiGraphics, clearX, dealY, 50, 20, clearHover ? 0x66FFFFFF : 0x22FFFFFF);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, "CLEAR", clearX + 25, dealY + 6, 0xFFFFFFFF);
    }

    private void renderBetCircle(GuiGraphics guiGraphics, int cx, int cy, int radius, int type, String label, int mouseX, int mouseY) {
        boolean hovered = Math.sqrt(Math.pow(mouseX - cx, 2) + Math.pow(mouseY - cy, 2)) < radius;
        int color = hovered ? 0x66FFFFFF : 0x22FFFFFF;
        for (float a = 0; a < 360; a += 10) {
            double rad = Math.toRadians(a);
            int rx = cx + (int)(Math.cos(rad) * radius);
            int ry = cy + (int)(Math.sin(rad) * radius);
            guiGraphics.fill(rx - 1, ry - 1, rx + 1, ry + 1, color);
        }
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, label, cx, cy - radius - 10, 0xFFFFFFFF);
        long amount = 0;
        for (BlackjackBet bet : blackjackBets) if (bet.type == type) amount += bet.amount;
        if (amount > 0) {
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, screen.formatNumberPublic(amount), cx, cy - 4, 0xFFFFFF00);
        }
    }

    private void renderBlackjackResult(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        String res;
        int pVal = getHandValue(playerHand);
        int dVal = getHandValue(dealerHand);
        if (pVal > 21) res = "BUST!";
        else if (dVal > 21) res = "DEALER BUSTS!";
        else if (pVal > dVal) res = "YOU WIN!";
        else if (dVal > pVal) res = "DEALER WINS!";
        else res = "PUSH";

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x + width / 2f, y + height / 2f, 100);
        guiGraphics.pose().scale(2.0f, 2.0f, 1.0f);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, res, 0, -10, 0xFFFFFFFF);
        if (lastBlackjackWin > 0) {
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, "+" + screen.formatNumberPublic(lastBlackjackWin), 0, 10, 0xFF55FF55);
        }
        guiGraphics.pose().popPose();
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, "Click anywhere to continue", x + width / 2, y + height - 20, 0xFFAAAAAA);
    }

    private void renderChips(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        int[] chipValues = {10, 50, 100, 500, 1000};
        for (int i = 0; i < chipValues.length; i++) {
            int cy = y + i * 22;
            boolean hovered = mouseX >= x && mouseX <= x + 30 && mouseY >= cy && mouseY <= cy + 20;
            boolean selected = currentChip == chipValues[i];
            int color = selected ? 0xFFFFFF00 : (hovered ? 0x66FFFFFF : 0x22FFFFFF);
            screen.renderRoundedRectPublic(guiGraphics, x, cy, 30, 20, color);
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, screen.formatNumberPublic(chipValues[i]), x + 15, cy + 6, selected ? 0xFF000000 : 0xFFFFFFFF);
        }
    }

    private int getHandValue(List<Card> hand) {
        int val = 0; int aces = 0;
        for (Card c : hand) { val += c.getValue(); if (c.rank == 1) aces++; }
        while (val > 21 && aces > 0) { val -= 10; aces--; }
        return val;
    }

    private void dealBlackjack() {
        if (blackjackState != 0) return;
        long mainBet = 0;
        for (BlackjackBet bet : blackjackBets) if (bet.type == 0) mainBet += bet.amount;
        if (mainBet == 0) return;
        blackjackState = 1; playerHand.clear(); dealerHand.clear(); createDeck();
        int areaX = screen.getLeft() + 115; int areaY = screen.getTop() + 45 + 15;
        int areaWidth = screen.getGuiWidth() - 130; int areaHeight = screen.getGuiHeight() - 60 - 15;
        int deckX = areaX + areaWidth - 50; int deckY = areaY + 30;
        int centerX = areaX + areaWidth / 2; int playerY = areaY + areaHeight - 85; int dealerY = areaY + 40;
        flyingCards.add(new FlyingCard(drawCard(), deckX, deckY, centerX - 20, playerY, false, 0));
        flyingCards.add(new FlyingCard(drawCard(), deckX, deckY, centerX - 20, dealerY, true, 0));
        flyingCards.add(new FlyingCard(drawCard(), deckX, deckY, centerX - 5, playerY, false, 1));
        flyingCards.add(new FlyingCard(drawCard(), deckX, deckY, centerX - 5, dealerY, true, 1));
    }

    private void createDeck() {
        deck.clear();
        for (int s = 0; s < 4; s++) for (int r = 1; r <= 13; r++) deck.add(new Card(r, s));
        java.util.Collections.shuffle(deck);
    }

    private Card drawCard() {
        if (deck.isEmpty()) createDeck();
        return deck.remove(0);
    }

    private void checkBlackjackInitial() { if (getHandValue(playerHand) == 21) standBlackjack(); }

    private void hitBlackjack() {
        if (blackjackState != 2 || !flyingCards.isEmpty()) return;
        int areaX = screen.getLeft() + 115; int areaY = screen.getTop() + 45 + 15;
        int areaWidth = screen.getGuiWidth() - 130; int areaHeight = screen.getGuiHeight() - 60 - 15;
        int deckX = areaX + areaWidth - 50; int deckY = areaY + 30;
        int centerX = areaX + areaWidth / 2; int playerY = areaY + areaHeight - 85;
        flyingCards.add(new FlyingCard(drawCard(), deckX, deckY, centerX - 20 + playerHand.size() * 15, playerY, false, playerHand.size()));
    }

    private void standBlackjack() { if (blackjackState != 2) return; blackjackState = 3; processDealerTurn(); }

    private void processDealerTurn() {
        int dVal = getHandValue(dealerHand); int pVal = getHandValue(playerHand);
        if (pVal > 21 || (dVal >= 17)) { blackjackState = 4; calculateBlackjackWins(); } else {
            int areaX = screen.getLeft() + 115; int areaY = screen.getTop() + 45 + 15;
            int areaWidth = screen.getGuiWidth() - 130; int deckX = areaX + areaWidth - 50; int deckY = areaY + 30;
            int centerX = areaX + areaWidth / 2; int dealerY = areaY + 40;
            flyingCards.add(new FlyingCard(drawCard(), deckX, deckY, centerX - 20 + dealerHand.size() * 15, dealerY, true, dealerHand.size()));
        }
    }

    private void calculateBlackjackWins() {
        int pVal = getHandValue(playerHand); int dVal = getHandValue(dealerHand);
        long mainBet = 0; for (BlackjackBet b : blackjackBets) if (b.type == 0) mainBet += b.amount;
        long totalWin = 0;
        if (pVal <= 21) {
            if (dVal > 21 || pVal > dVal) {
                totalWin = mainBet * 2; if (pVal == 21 && playerHand.size() == 2) totalWin = (long)(mainBet * 2.5);
            } else if (pVal == dVal) { totalWin = mainBet; }
        }
        long pairsBet = 0; for (BlackjackBet b : blackjackBets) if (b.type == 1) pairsBet += b.amount;
        if (pairsBet > 0 && playerHand.size() >= 2) if (playerHand.get(0).rank == playerHand.get(1).rank) totalWin += pairsBet * 12;
        long t213Bet = 0; for (BlackjackBet b : blackjackBets) if (b.type == 2) t213Bet += b.amount;
        if (t213Bet > 0 && playerHand.size() >= 2 && dealerHand.size() >= 1) {
            boolean flush = playerHand.get(0).suit == playerHand.get(1).suit && playerHand.get(1).suit == dealerHand.get(0).suit;
            if (flush) totalWin += t213Bet * 10;
        }
        lastBlackjackWin = totalWin;
        if (totalWin > 0) {
            CosmicConfig.getInstance().casinoCoins += totalWin; CosmicConfig.save();
            blackjackWinAnimScale = 0.1f; Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.0f));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        int areaX = screen.getLeft() + 115; int areaY = screen.getTop() + 45 + 15;
        int areaWidth = screen.getGuiWidth() - 130; int areaHeight = screen.getGuiHeight() - 60 - 15;
        if (blackjackState == 0) {
            int cx = areaX + areaWidth / 2; int cy = areaY + areaHeight / 2;
            if (Math.sqrt(Math.pow(mouseX - cx, 2) + Math.pow(mouseY - (cy + 40), 2)) < 20) { placeBlackjackBet(0); return true; }
            if (Math.sqrt(Math.pow(mouseX - (cx - 60), 2) + Math.pow(mouseY - (cy + 20), 2)) < 15) { placeBlackjackBet(1); return true; }
            if (Math.sqrt(Math.pow(mouseX - (cx + 60), 2) + Math.pow(mouseY - (cy + 20), 2)) < 15) { placeBlackjackBet(2); return true; }
            int dealX = areaX + 20; int dealY = areaY + areaHeight - 40;
            if (mouseX >= dealX && mouseX <= dealX + 50 && mouseY >= dealY && mouseY <= dealY + 20) { dealBlackjack(); return true; }
            int clearX = dealX + 60;
            if (mouseX >= clearX && mouseX <= clearX + 50 && mouseY >= dealY && mouseY <= dealY + 20) {
                for (BlackjackBet b : blackjackBets) CosmicConfig.getInstance().casinoCoins += b.amount;
                blackjackBets.clear(); CosmicConfig.save(); return true;
            }
        } else if (blackjackState == 2 && flyingCards.isEmpty()) {
            int btnW = 40; int btnH = 15; int btnX = areaX + areaWidth / 2 - 45; int btnY = areaY + areaHeight / 2;
            if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) { hitBlackjack(); return true; }
            if (mouseX >= btnX + 50 && mouseX <= btnX + 50 + btnW && mouseY >= btnY && mouseY <= btnY + btnH) { standBlackjack(); return true; }
        } else if (blackjackState == 4) {
            blackjackState = 0; blackjackBets.clear(); playerHand.clear(); dealerHand.clear(); lastBlackjackWin = 0; return true;
        }
        int chipX = areaX + areaWidth - 40; int[] chipValues = {10, 50, 100, 500, 1000};
        for (int i = 0; i < chipValues.length; i++) {
            int cy = areaY + 80 + i * 22; if (mouseX >= chipX && mouseX <= chipX + 30 && mouseY >= cy && mouseY <= cy + 20) { currentChip = chipValues[i]; return true; }
        }
        return false;
    }

    private void placeBlackjackBet(int type) {
        CosmicConfig config = CosmicConfig.getInstance();
        if (config.casinoCoins >= currentChip) {
            config.casinoCoins -= currentChip; blackjackBets.add(new BlackjackBet(type, currentChip));
            CosmicConfig.save(); Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }

    @Override
    public String getName() { return "Blackjack"; }
}
