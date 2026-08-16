package top.ydog01.mmagic.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;
import top.ydog01.mmagic.network.ModNetwork;
import top.ydog01.mmagic.quest.QuestDef;
import top.ydog01.mmagic.quest.QuestProgress;
import top.ydog01.mmagic.quest.Quests;

import java.util.List;

public class QuestBookScreen extends Screen {
    private static final int PANEL_W = 250;
    private static final int PANEL_H = 178;
    private static final int LIST_W = 104;
    private static final int ROW_H = 20;
    private static final int DETAIL_X = 8 + LIST_W + 8;
    private static final int DETAIL_W = PANEL_W - DETAIL_X - 8;

    private QuestProgress progress;
    private QuestDef selected;
    private double detailScroll;

    public QuestBookScreen(QuestProgress progress) {
        super(Component.translatable("quest.modern_magic.title"));
        this.progress = progress;
        if (!Quests.all().isEmpty()) {
            this.selected = Quests.all().get(0);
        }
    }

    public static void receive(QuestProgress progress) {
        if (Minecraft.getInstance().screen instanceof QuestBookScreen screen) {
            screen.progress = progress;
        }
    }

    private int left() {
        return (width - PANEL_W) / 2;
    }

    private int top() {
        return (height - PANEL_H) / 2;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        int l = left();
        int t = top();
        g.fill(l, t, l + PANEL_W, t + PANEL_H, 0xE8101010);
        g.fill(l, t, l + PANEL_W, t + 1, 0xFFC8A04A);
        g.fill(l, t + PANEL_H - 1, l + PANEL_W, t + PANEL_H, 0xFFC8A04A);
        g.fill(l, t, l + 1, t + PANEL_H, 0xFFC8A04A);
        g.fill(l + PANEL_W - 1, t, l + PANEL_W, t + PANEL_H, 0xFFC8A04A);
        g.drawCenteredString(font, title, l + PANEL_W / 2, t + 4, 0xFFFFFF);
        renderList(g, mouseX, mouseY, l, t);
        renderDetail(g, mouseX, mouseY, l, t);
    }

    private void renderList(GuiGraphics g, int mouseX, int mouseY, int l, int t) {
        int x = l + 8;
        for (int i = 0; i < Quests.all().size(); i++) {
            QuestDef def = Quests.all().get(i);
            int ry = t + 14 + i * ROW_H;
            if (def == selected) {
                g.fill(x, ry, x + LIST_W, ry + ROW_H - 2, 0x33FFFFFF);
            }
            g.renderItem(def.icon(), x + 1, ry + 1);
            int color = statusColor(def);
            String title = font.plainSubstrByWidth(Component.translatable("quest.modern_magic." + def.id().getPath() + ".title").getString(),
                    LIST_W - 42);
            g.drawString(font, title, x + 20, ry + 5, color);
            if (isClaimable(def)) {
                g.drawString(font, "!", x + LIST_W - 8, ry + 5, 0xFFD700);
            }
        }
    }

    private void renderDetail(GuiGraphics g, int mouseX, int mouseY, int l, int t) {
        if (selected == null) {
            return;
        }
        int x = l + DETAIL_X;
        int y = t + 14;
        g.renderItem(selected.icon(), x, y);
        g.drawString(font, Component.translatable("quest.modern_magic." + selected.id().getPath() + ".title"),
                x + 20, y + 4, 0xFFFFFF);
        int contentTop = y + 22;
        int contentBottom = t + PANEL_H - 30;
        int right = l + PANEL_W - 8;
        g.enableScissor(x, contentTop, right, contentBottom);
        int shiftedStart = contentTop - (int) detailScroll;
        int textY = shiftedStart;
        if (selected.recipeId() != null) {
            textY = renderRecipe(g, mouseX, mouseY, x, textY) + 4;
        }
        for (FormattedCharSequence line : font.split(Component.translatable("quest.modern_magic." + selected.id().getPath() + ".desc"),
                DETAIL_W)) {
            g.drawString(font, line, x, textY, 0xB0B0B0);
            textY += 9;
        }
        textY += 2;
        QuestProgress.Entry entry = progress.entries().get(selected.id());
        int prog = entry == null ? 0 : entry.progress();
        g.drawString(font, Component.translatable("quest.modern_magic.objective", prog, selected.target()),
                x, textY, prog >= selected.target() ? 0xFFD700 : 0xFFFFFF);
        textY += 12;
        g.drawString(font, Component.translatable("quest.modern_magic.reward"), x, textY, 0xFFFFFF);
        textY += 12;
        int itemX = x;
        for (ItemStack reward : selected.rewards()) {
            g.renderItem(reward, itemX, textY);
            g.renderItemDecorations(font, reward, itemX, textY, reward.getCount() > 1 ? String.valueOf(reward.getCount()) : null);
            if (mouseX >= itemX && mouseX < itemX + 16 && mouseY >= textY && mouseY < textY + 16) {
                g.renderTooltip(font, reward, mouseX, mouseY);
            }
            itemX += 18;
        }
        textY += 18;
        g.drawString(font, Component.translatable("quest.modern_magic.xp", selected.xp()), x, textY, 0x55FF55);
        g.disableScissor();
        int contentHeight = textY - shiftedStart;
        double maxScroll = Math.max(0, contentHeight - (contentBottom - contentTop));
        detailScroll = Math.max(0, Math.min(detailScroll, maxScroll));
        int bx = l + DETAIL_X + DETAIL_W - 64;
        int by = t + PANEL_H - 24;
        boolean claimable = isClaimable(selected);
        boolean claimed = isClaimed(selected);
        int fillColor = claimable ? 0xFFB8860B : 0xFF3A3A3A;
        g.fill(bx, by, bx + 64, by + 16, fillColor);
        Component label = claimed
                ? Component.translatable("quest.modern_magic.claimed")
                : Component.translatable("quest.modern_magic.claim");
        g.drawCenteredString(font, label, bx + 32, by + 4, claimed ? 0x808080 : (claimable ? 0xFFFFFF : 0x808080));
    }

    private int renderRecipe(GuiGraphics g, int mouseX, int mouseY, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || selected.recipeId() == null) {
            return y;
        }
        Recipe<?> recipe = mc.level.getRecipeManager().byKey(selected.recipeId())
                .map(RecipeHolder::value).orElse(null);
        if (recipe == null) {
            return y;
        }
        List<Ingredient> ingredients = recipe.getIngredients();
        int width = 3;
        int height = 3;
        if (recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped) {
            width = shaped.getWidth();
            height = shaped.getHeight();
        }
        int offsetX = (3 - width) / 2;
        int offsetY = (3 - height) / 2;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int cx = x + col * 18;
                int cy = y + row * 18;
                g.fill(cx, cy, cx + 16, cy + 16, 0x33FFFFFF);
                if (col < width && row < height) {
                    int idx = row * width + col;
                    if (idx < ingredients.size()) {
                        Ingredient ing = ingredients.get(idx);
                        ItemStack[] items = ing.getItems();
                        if (items.length > 0) {
                            int ix = x + (col + offsetX) * 18;
                            int iy = y + (row + offsetY) * 18;
                            g.renderItem(items[0], ix, iy);
                            if (mouseX >= ix && mouseX < ix + 16 && mouseY >= iy && mouseY < iy + 16) {
                                g.renderTooltip(font, items[0], mouseX, mouseY);
                            }
                        }
                    }
                }
            }
        }
        int ax = x + 3 * 18 + 6;
        g.drawString(font, "▶", ax, y + 18, 0xFFFFFF);
        int rx = ax + 12;
        int ry = y + 18;
        g.fill(rx, ry, rx + 16, ry + 16, 0x33FFFFFF);
        ItemStack result = recipe.getResultItem(mc.level.registryAccess());
        g.renderItem(result, rx, ry);
        g.renderItemDecorations(font, result, rx, ry, result.getCount() > 1 ? String.valueOf(result.getCount()) : null);
        if (mouseX >= rx && mouseX < rx + 16 && mouseY >= ry && mouseY < ry + 16) {
            g.renderTooltip(font, result, mouseX, mouseY);
        }
        return y + 54;
    }

    private QuestProgress.Entry entryOf(QuestDef def) {
        return progress.entries().get(def.id());
    }

    private boolean isClaimable(QuestDef def) {
        QuestProgress.Entry e = entryOf(def);
        return e != null && !e.claimed() && e.progress() >= def.target();
    }

    private boolean isClaimed(QuestDef def) {
        QuestProgress.Entry e = entryOf(def);
        return e != null && e.claimed();
    }

    private int statusColor(QuestDef def) {
        if (isClaimed(def)) {
            return 0x55FF55;
        }
        if (isClaimable(def)) {
            return 0xFFD700;
        }
        QuestProgress.Entry e = entryOf(def);
        if (e != null && e.progress() > 0) {
            return 0xFFFFFF;
        }
        return 0x9A9A9A;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int l = left();
        int t = top();
        if (mouseX >= l + DETAIL_X && mouseX <= l + PANEL_W - 8 && mouseY >= t + 14 && mouseY <= t + PANEL_H) {
            detailScroll = Math.max(0, detailScroll - scrollY * 12);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int l = left();
            int t = top();
            for (int i = 0; i < Quests.all().size(); i++) {
                int ry = t + 14 + i * ROW_H;
                if (mouseX >= l + 8 && mouseX <= l + 8 + LIST_W && mouseY >= ry && mouseY <= ry + ROW_H - 2) {
                    selected = Quests.all().get(i);
                    detailScroll = 0;
                    return true;
                }
            }
            if (selected != null && isClaimable(selected)) {
                int bx = l + DETAIL_X + DETAIL_W - 64;
                int by = t + PANEL_H - 24;
                if (mouseX >= bx && mouseX <= bx + 64 && mouseY >= by && mouseY <= by + 16) {
                    PacketDistributor.sendToServer(new ModNetwork.ClaimQuestPacket(selected.id()));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
