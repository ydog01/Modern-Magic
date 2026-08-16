package top.ydog01.mmagic.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.PacketDistributor;
import top.ydog01.mmagic.menu.AltarMenu;
import top.ydog01.mmagic.network.ModNetwork;
import top.ydog01.mmagic.spell.NodeParameter;
import top.ydog01.mmagic.spell.SpellGraph;
import top.ydog01.mmagic.spell.SpellNode;
import top.ydog01.mmagic.spell.SpellNodeType;
import top.ydog01.mmagic.spell.SpellRegistry;
import top.ydog01.mmagic.util.SpellCost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class AltarScreen extends Screen implements MenuAccess<AltarMenu> {
    private static final int TAB_PRAY = 0;
    private static final int TAB_ASSEMBLE = 1;

    private static final int PANEL_W = 420;
    private static final int PANEL_H = 232;

    private static final int NODE_W = 100;
    private static final int NODE_H = 40;
    private static final int PARAM_ROW_H = 14;

    private final AltarMenu menu;

    private int editMaxMana = 20;
    private int editRegenX100 = 100;
    private int editCooldown = 40;
    private int currentMaxMana = 20;
    private int currentRegenX100 = 100;
    private int currentCooldown = 40;
    private int crystals;
    private boolean hasWand = true;

    private SpellGraph graph = SpellGraph.createDefault();
    private ResourceLocation selectedType;
    private UUID pendingSource;
    private int pendingOutPort = -1;
    private UUID dragNode;
    private int dragOffsetX;
    private int dragOffsetY;
    private float viewX;
    private float viewY;
    private float viewScale = 1.0f;
    private boolean dirty;
    private int noticeTicks;
    private String noticeText;
    private int noticeColor;
    private PendingPlace pendingPlace;

    private EditBox nameField;
    private EditBox fileField;
    private String pendingImportName;
    private int tab = TAB_PRAY;
    private boolean synced;

    private EditBox valueInput;
    private AttributeKind inputAttr;
    private SpellNode inputNode;
    private NodeParameter inputParam;
    private boolean inputDecimal;

    private final Map<Item, Integer> knownCounts = new HashMap<>();
    private boolean countsInit;

    private PaletteNode paletteRoot;
    private final Set<String> expandedDirs = new HashSet<>();
    private float paletteScroll;

    public AltarScreen(AltarMenu menu, Inventory inventory, Component title) {
        super(title);
        this.menu = menu;
    }

    @Override
    protected void init() {
        super.init();
        this.nameField = new EditBox(font, guiLeft() + 6, guiTop() + 52, 112, 14,
                Component.translatable("screen.modern_magic.wand_name"));
        this.nameField.setMaxLength(50);
        this.nameField.setCanLoseFocus(true);
        this.addRenderableWidget(this.nameField);

        this.fileField = new EditBox(font, guiLeft() + 4, guiTop() + 84, 116, 14,
                Component.translatable("screen.modern_magic.file_name"));
        this.fileField.setMaxLength(40);
        this.fileField.setCanLoseFocus(true);
        this.addRenderableWidget(this.fileField);

        this.paletteRoot = buildPaletteTree();
        this.expandedDirs.clear();
        this.expandedDirs.add("attack");
        this.expandedDirs.add("control");
        this.expandedDirs.add("support");
        this.paletteScroll = 0;
        if (!synced) {
            PacketDistributor.sendToServer(new ModNetwork.RequestAltarSyncPacket());
            synced = true;
        }
    }

    @Override
    public void tick() {
        if (noticeTicks > 0) {
            noticeTicks--;
        }
        if (minecraft != null && minecraft.player != null && minecraft.level != null
                && minecraft.player.tickCount % 20 == 0) {
            PacketDistributor.sendToServer(new ModNetwork.RequestAltarSyncPacket());
        }
    }

    @Override
    public AltarMenu getMenu() {
        return menu;
    }

    public void receiveSync(ModNetwork.SyncAltarPacket packet) {
        boolean attrsChanged = packet.attrs().maxMana() != currentMaxMana
                || packet.attrs().regenX100() != currentRegenX100
                || packet.attrs().cooldown() != currentCooldown;
        this.currentMaxMana = packet.attrs().maxMana();
        this.currentRegenX100 = packet.attrs().regenX100();
        this.currentCooldown = packet.attrs().cooldown();
        this.crystals = packet.crystals();
        this.hasWand = packet.hasWand();
        if (attrsChanged) {
            this.editMaxMana = currentMaxMana;
            this.editRegenX100 = currentRegenX100;
            this.editCooldown = currentCooldown;
        }
        if (!dirty) {
            this.graph = SpellGraph.fromTag(packet.spell());
            if (nameField != null && !nameField.isFocused()) {
                nameField.setValue(packet.name() == null ? "" : packet.name());
            }
        }
        if (!countsInit && minecraft != null && minecraft.player != null) {
            for (ResourceLocation id : SpellRegistry.ids()) {
                SpellNodeType type = SpellRegistry.get(id);
                if (type == null) {
                    continue;
                }
                Item item = type.icon().getItem();
                int total = 0;
                for (var stack : minecraft.player.getInventory().items) {
                    if (stack.getItem() == item) {
                        total += stack.getCount();
                    }
                }
                knownCounts.put(item, total);
            }
            countsInit = true;
        }
    }

    public void receiveNotice(String key) {
        denyNotice(key);
    }

    private int guiLeft() {
        return (width - PANEL_W) / 2;
    }

    private int guiTop() {
        return (height - PANEL_H) / 2;
    }

    private int canvasLeft() {
        return guiLeft() + 124;
    }

    private int canvasTop() {
        return guiTop() + 36;
    }

    private int canvasRight() {
        return guiLeft() + PANEL_W - 4;
    }

    private int canvasBottom() {
        return guiTop() + PANEL_H - 34;
    }

    private int statusBarTop() {
        return canvasBottom();
    }

    private int paletteTop() {
        return guiTop() + 100;
    }

    private int statusBarBottom() {
        return guiTop() + PANEL_H - 10;
    }

    private int scaled(float v) {
        return (int) (v * viewScale);
    }

    private void drawScaledText(GuiGraphics g, String text, float x, float y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0);
        g.pose().scale(viewScale, viewScale, 1.0f);
        g.drawString(font, text, 0, 0, color);
        g.pose().popPose();
    }

    private void drawScaledCenteredText(GuiGraphics g, String text, float cx, float y, int color) {
        float w = font.width(text) * viewScale;
        drawScaledText(g, text, cx - w / 2.0f, y, color);
    }

    private int screenX(float wx) {
        return canvasLeft() + (int) ((wx - viewX) * viewScale);
    }

    private int screenY(float wy) {
        return canvasTop() + (int) ((wy - viewY) * viewScale);
    }

    private float worldX(double mx) {
        return (float) ((mx - canvasLeft()) / viewScale) + viewX;
    }

    private float worldY(double my) {
        return (float) ((my - canvasTop()) / viewScale) + viewY;
    }

    private int countNodeItems(SpellNodeType type) {
        if (minecraft == null || minecraft.player == null) {
            return 0;
        }
        if (minecraft.player.getAbilities().instabuild) {
            return -1;
        }
        return knownCounts.getOrDefault(type.icon().getItem(), 0);
    }

    private void denyNotice(String key) {
        setNotice(Component.translatable(key).getString(), 0xFFFF5555);
    }

    private void setNotice(String text, int color) {
        noticeTicks = 90;
        noticeText = text;
        noticeColor = color;
    }

    public void receivePlaceAck(ModNetwork.PlaceNodeAckPacket packet) {
        if (pendingPlace == null) {
            return;
        }
        PendingPlace place = pendingPlace;
        pendingPlace = null;
        if (packet.ok()) {
            SpellNodeType type = SpellRegistry.get(place.type);
            if (type != null) {
                graph.addNode(type, place.id, place.x, place.y);
                dirty = true;
                if (minecraft != null && minecraft.player != null && !minecraft.player.getAbilities().instabuild) {
                    knownCounts.merge(type.icon().getItem(), -1, Integer::sum);
                }
            }
        } else {
            denyNotice("screen.modern_magic.need_node");
        }
    }

    private record PendingPlace(ResourceLocation type, UUID id, float x, float y) {
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {

        renderBackground(g, mouseX, mouseY, partialTick);

        int left = guiLeft();
        int top = guiTop();
        g.fill(left, top, left + PANEL_W, top + PANEL_H, 0xC0101010);
        g.drawCenteredString(font, title, left + PANEL_W / 2, top + 5, 0xFFFFFFFF);

        renderTab(g, left + 8, top + 16, 110, TAB_PRAY, "tab.modern_magic.pray");
        renderTab(g, left + 122, top + 16, 110, TAB_ASSEMBLE, "tab.modern_magic.assemble");

        if (tab == TAB_PRAY) {
            renderPray(g, left, top);
        } else {
            renderAssemble(g, left, top, mouseX, mouseY);
        }

        if (nameField != null) {
            nameField.setVisible(tab == TAB_ASSEMBLE);
            if (nameField.isVisible()) {
                nameField.render(g, mouseX, mouseY, partialTick);
                if (nameField.getValue().isEmpty() && !nameField.isFocused()) {
                    g.drawString(font, Component.translatable("screen.modern_magic.wand_name"),
                            nameField.getX() + 2, nameField.getY() + 3, 0xFF888888);
                }
            }
        }
        if (fileField != null) {
            fileField.setVisible(tab == TAB_ASSEMBLE);
            if (fileField.isVisible()) {
                fileField.render(g, mouseX, mouseY, partialTick);
                if (fileField.getValue().isEmpty() && !fileField.isFocused()) {
                    g.drawString(font, Component.translatable("screen.modern_magic.file_name"),
                            fileField.getX() + 2, fileField.getY() + 3, 0xFF888888);
                }
            }
        }
        if (valueInput != null) {
            valueInput.render(g, mouseX, mouseY, partialTick);
        }
    }

    private void renderTab(GuiGraphics g, int x, int y, int w, int which, String key) {
        int color = tab == which ? 0xFF707070 : 0xFF383838;
        g.fill(x, y, x + w, y + 14, color);
        g.drawCenteredString(font, Component.translatable(key), x + w / 2, y + 3, 0xFFFFFFFF);
    }

    private void renderFileButton(GuiGraphics g, int x, int y, int w, String key, int color) {
        g.fill(x, y, x + w, y + 14, color);
        g.drawCenteredString(font, Component.translatable(key), x + w / 2, y + 3, 0xFFFFFFFF);
    }

    private void renderPray(GuiGraphics g, int left, int top) {
        int y = top + 44;
        renderAttributeRow(g, left, y, "attr.modern_magic.max_mana", editMaxMana, currentMaxMana, AttributeKind.MANA);
        y += 34;
        renderAttributeRow(g, left, y, "attr.modern_magic.regen", editRegenX100, currentRegenX100, AttributeKind.REGEN);
        y += 34;
        renderAttributeRow(g, left, y, "attr.modern_magic.cooldown", editCooldown, currentCooldown, AttributeKind.COOLDOWN);

        int cost = SpellCost.charge(currentMaxMana, currentRegenX100 / 100.0, currentCooldown,
                editMaxMana, editRegenX100 / 100.0, editCooldown);
        boolean affordable = hasWand && (crystals < 0 || cost <= crystals);
        g.drawCenteredString(font, Component.translatable("screen.modern_magic.cost", cost),
                left + 210, top + 148, affordable ? 0xFF55FF55 : 0xFFFF5555);
        g.drawCenteredString(font, Component.translatable("screen.modern_magic.crystals",
                        crystals < 0 ? "∞" : crystals),
                left + 210, top + 164, 0xFFAAAAAA);

        g.fill(left + 150, top + 180, left + 270, top + 198, 0xFF446622);
        g.drawCenteredString(font, Component.translatable("screen.modern_magic.pray"), left + 210, top + 184, 0xFFFFFFFF);

        if (!hasWand) {
            g.drawCenteredString(font, Component.translatable("screen.modern_magic.need_wand"),
                    left + 210, top + 212, 0xFFFF5555);
        }
    }

    private void renderAttributeRow(GuiGraphics g, int left, int y, String key, int editRaw, int currentRaw, AttributeKind kind) {
        g.drawString(font, Component.translatable(key), left + 12, y + 2, 0xFFFFFFFF);
        g.drawString(font, Component.translatable("screen.modern_magic.current", formatValue(currentRaw, kind)),
                left + 12, y + 16, 0xFF888888);
        g.fill(left + 272, y, left + 286, y + 14, 0xFF444444);
        g.fill(left + 336, y, left + 350, y + 14, 0xFF444444);
        g.drawCenteredString(font, Component.literal("-"), left + 279, y + 3, 0xFFFFFFFF);
        g.drawCenteredString(font, Component.literal("+"), left + 343, y + 3, 0xFFFFFFFF);
        if (!(valueInput != null && inputAttr == kind)) {
            g.drawCenteredString(font, Component.literal(formatValue(editRaw, kind)), left + 311, y + 4, 0xFFFFFF00);
        }
    }

    private String formatValue(int rawValue, AttributeKind kind) {
        return switch (kind) {
            case MANA -> String.valueOf(rawValue);
            case REGEN -> String.format("%.1f/s", rawValue / 100.0);
            case COOLDOWN -> String.format("%.2fs", rawValue / 20.0);
        };
    }

    private void renderAssemble(GuiGraphics g, int left, int top, int mx, int my) {
        int cLeft = canvasLeft();
        int cTop = canvasTop();
        int cRight = canvasRight();
        int cBottom = canvasBottom();

        g.fill(left + 8, top + 36, left + 118, top + 50, 0xFF446622);
        g.drawCenteredString(font, Component.translatable("screen.modern_magic.save"), left + 63, top + 39, 0xFFFFFFFF);

        renderFileButton(g, left + 4, top + 68, 36, "screen.modern_magic.export", 0xFF2A3A55);
        renderFileButton(g, left + 42, top + 68, 36, "screen.modern_magic.import", 0xFF554422);
        renderFileButton(g, left + 80, top + 68, 36, "screen.modern_magic.clear", 0xFF552222);

        g.fill(cLeft - 1, cTop - 1, cRight + 1, cTop, 0xFF777777);
        g.fill(cLeft - 1, cBottom, cRight + 1, cBottom + 1, 0xFF777777);
        g.fill(cLeft - 1, cTop, cLeft, cBottom, 0xFF777777);
        g.fill(cRight, cTop, cRight + 1, cBottom, 0xFF777777);
        g.fill(cLeft, cTop, cRight, cBottom, 0xFF1E1E1E);

        g.enableScissor(cLeft, cTop, cRight, cBottom);
        renderNodeBodies(g, cLeft, cTop);
        renderConnections(g, cLeft, cTop, mx, my);
        renderPorts(g, cLeft, cTop);
        g.disableScissor();

        renderPalette(g, left, top, mx, my);
        renderStatusBar(g, left, top);
        renderHoverTooltips(g, left, top, mx, my);
    }

    private void renderStatusBar(GuiGraphics g, int left, int top) {
        int cLeft = canvasLeft();
        int cRight = canvasRight();
        int y = statusBarTop();
        int b = statusBarBottom();
        g.fill(cLeft, y, cRight, b, 0xFF151515);
        g.fill(cLeft, y, cRight, y + 1, 0xFF777777);
        String line1;
        String line2;
        int line1Color = 0xFFDDDDDD;
        if (noticeTicks > 0 && noticeText != null) {
            line1 = noticeText;
            line1Color = noticeColor;
            line2 = "screen.modern_magic.hint_default_2";
        } else if (selectedType != null) {
            line1 = "screen.modern_magic.hint_place_1";
            line2 = "screen.modern_magic.hint_place_2";
        } else if (pendingSource != null) {
            line1 = "screen.modern_magic.hint_connect_1";
            line2 = "screen.modern_magic.hint_connect_2";
        } else {
            line1 = "screen.modern_magic.hint_default_1";
            line2 = "screen.modern_magic.hint_default_2";
        }
        g.drawString(font, Component.translatable(line1), cLeft + 6, y + 3, line1Color);
        g.drawString(font, Component.translatable(line2), cLeft + 6, y + 13, 0xFF888888);
    }

    private void renderPalette(GuiGraphics g, int left, int top, int mx, int my) {
        int x = left + 4;
        int pTop = paletteTop();
        int pBottom = canvasBottom();
        g.fill(x, pTop, x + 116, pBottom, 0xFF2A2A2A);
        g.enableScissor(x, pTop, x + 116, pBottom);
        for (PaletteRow row : paletteRows()) {
            int y = row.y;
            if (y + 18 < pTop || y > pBottom) {
                continue;
            }
            PaletteNode node = row.node;
            if (node.dir) {
                boolean open = expandedDirs.contains(node.path);
                boolean hover = inRect(mx, my, x + 2, y, 112, 18);
                g.fill(x + 2, y, x + 114, y + 18, hover ? 0xFF3A3A3A : 0xFF2E2E2E);
                g.drawString(font, (open ? "▼ " : "▶ ") + categoryName(node.path),
                        x + 4 + row.depth * 10, y + 5, 0xFFFFCC66);
            } else {
                SpellNodeType type = SpellRegistry.get(node.typeId);
                if (type == null) {
                    continue;
                }
                boolean selected = node.typeId.equals(selectedType);
                boolean hover = inRect(mx, my, x + 2, y, 112, 18);
                g.fill(x + 2, y, x + 114, y + 18, selected ? 0xFF666644 : (hover ? 0xFF444444 : 0xFF303030));
                g.renderItem(type.icon(), x + 4 + row.depth * 10, y + 1);
                g.drawString(font, type.displayName(), x + 22 + row.depth * 10, y + 5, 0xFFFFFFFF);
                int count = countNodeItems(type);
                String countText = count < 0 ? "∞" : "x" + count;
                g.drawString(font, Component.literal(countText), x + 112 - font.width(countText), y + 5, 0xFFFFDD66);
            }
        }
        g.disableScissor();
    }

    private static final class PaletteNode {
        final String path;
        final ResourceLocation typeId;
        final boolean dir;
        final Map<String, PaletteNode> children = new LinkedHashMap<>();

        PaletteNode(String path, ResourceLocation typeId, boolean dir) {
            this.path = path;
            this.typeId = typeId;
            this.dir = dir;
        }
    }

    private static final class PaletteRow {
        final PaletteNode node;
        final int depth;
        final int y;

        PaletteRow(PaletteNode node, int depth, int y) {
            this.node = node;
            this.depth = depth;
            this.y = y;
        }
    }

    private PaletteNode buildPaletteTree() {
        PaletteNode root = new PaletteNode("", null, true);
        for (ResourceLocation id : SpellRegistry.ids()) {
            if (id.equals(SpellRegistry.START_ID)) {
                continue;
            }
            SpellNodeType type = SpellRegistry.get(id);
            if (type == null) {
                continue;
            }
            PaletteNode cur = root;
            StringBuilder full = new StringBuilder();
            for (String seg : type.categories()) {
                if (full.length() > 0) {
                    full.append('.');
                }
                full.append(seg);
                String segPath = full.toString();
                PaletteNode child = cur.children.get(seg);
                if (child == null) {
                    child = new PaletteNode(segPath, null, true);
                    cur.children.put(seg, child);
                }
                cur = child;
            }
            cur.children.put(id.toString(), new PaletteNode(id.toString(), id, false));
        }
        return root;
    }

    private String categoryName(String path) {
        return Component.translatable("category.modern_magic." + path).getString();
    }

    private void collectRows(PaletteNode node, int depth, List<PaletteRow> out, int top) {
        for (PaletteNode child : node.children.values()) {
            int y = top + out.size() * 18;
            out.add(new PaletteRow(child, depth, y));
            if (child.dir && expandedDirs.contains(child.path)) {
                collectRows(child, depth + 1, out, top);
            }
        }
    }

    private List<PaletteRow> paletteRows() {
        List<PaletteRow> rows = new ArrayList<>();
        if (paletteRoot != null) {
            collectRows(paletteRoot, 0, rows, paletteTop() + 4 - (int) paletteScroll);
        }

        float maxScroll = Math.max(0, rows.size() * 18f - (canvasBottom() - paletteTop()));
        if (paletteScroll > maxScroll) {
            paletteScroll = maxScroll;
        }
        return rows;
    }

    private void renderConnections(GuiGraphics g, int cLeft, int cTop, int mx, int my) {
        for (SpellNode node : graph.nodes()) {
            int nx = screenX(node.x());
            int ny = screenY(node.y());
            int nw = scaled(NODE_W);
            for (int p = 0; p < node.outputCount(); p++) {
                for (SpellNode.Connection c : node.outputs(p)) {
                    SpellNode target = graph.node(c.targetId);
                    if (target == null) {
                        continue;
                    }
                    int sx = nx + nw + 1;
                    int sy = ny + scaled(portY(node, p, false));
                    int tx = screenX(target.x()) - 1;
                    int ty = screenY(target.y()) + scaled(portY(target, c.targetInputPort, true));
                    drawLine(g, sx, sy, tx, ty, 0xFF7FA9E8);
                }
            }
        }
        if (pendingSource != null) {
            SpellNode source = graph.node(pendingSource);
            if (source != null) {
                int sx = screenX(source.x()) + scaled(NODE_W) + 1;
                int sy = screenY(source.y()) + scaled(portY(source, pendingOutPort, false));
                drawLine(g, sx, sy, mx, my, 0xFFFFE08A);
            }
        }
    }

    private void renderNodeBodies(GuiGraphics g, int cLeft, int cTop) {
        for (SpellNode node : graph.nodes()) {
            int nx = screenX(node.x());
            int ny = screenY(node.y());
            int nw = scaled(NODE_W);
            int nh = scaled(nodeH(node));
            boolean isStart = graph.isStart(node.id());
            boolean selected = node.id().equals(pendingSource) || node.id().equals(dragNode);
            int bg = isStart ? 0xFF335533 : 0xFF444444;
            g.fill(nx, ny, nx + nw, ny + nh, bg);
            int border = selected ? 0xFFFFDD66 : 0xFF999999;
            g.fill(nx, ny, nx + nw, ny + 1, border);
            g.fill(nx, ny, nx + 1, ny + nh, border);
            g.fill(nx, ny + nh - 1, nx + nw, ny + nh, border);
            g.fill(nx + nw - 1, ny, nx + nw, ny + nh, border);

            drawScaledCenteredText(g, node.type().displayName().getString(), nx + nw / 2, ny + scaled(3), 0xFFFFFFFF);
            drawScaledCenteredText(g, Component.translatable("screen.modern_magic.mana_cost", node.manaCost()).getString(),
                    nx + nw / 2, ny + scaled(14), 0xFFAAAAAA);

            renderParams(g, nx, ny, nw, node);
        }
    }

    private void renderParams(GuiGraphics g, int nx, int ny, int nw, SpellNode node) {
        if (node.type().id().getPath().equals("condition")) {
            renderConditionParams(g, nx, ny, nw, node);
            return;
        }
        List<NodeParameter> ps = node.type().parameters();
        for (int i = 0; i < ps.size(); i++) {
            NodeParameter p = ps.get(i);
            int py = ny + scaled(headerH(node) + i * PARAM_ROW_H);
            int ph = scaled(PARAM_ROW_H);
            g.fill(nx, py, nx + nw, py + ph, 0xFF353535);
            if (p.kind() == NodeParameter.Kind.BOOL) {
                boolean on = node.paramBool(p.key());
                g.fill(nx + scaled(5), py + scaled(3), nx + scaled(13), py + scaled(11), 0xFF1A1A1A);
                if (on) {
                    g.fill(nx + scaled(7), py + scaled(5), nx + scaled(11), py + scaled(9), 0xFF44FF44);
                }
                drawScaledText(g, Component.translatable(p.labelKey()).getString(), nx + scaled(17), py + scaled(3),
                        on ? 0xFFFFFFFF : 0xFFAAAAAA);
            } else {
                drawScaledText(g, Component.translatable(p.labelKey()).getString(), nx + scaled(4), py + scaled(3), 0xFFCCCCCC);
                g.fill(nx + scaled(46), py + scaled(2), nx + scaled(57), py + scaled(12), 0xFF1A1A1A);
                g.fill(nx + scaled(85), py + scaled(2), nx + scaled(96), py + scaled(12), 0xFF1A1A1A);
                drawScaledCenteredText(g, "-", nx + scaled(51), py + scaled(3), 0xFFFFFFFF);
                drawScaledCenteredText(g, "+", nx + scaled(90), py + scaled(3), 0xFFFFFFFF);
                if (!(valueInput != null && inputNode == node && inputParam == p)) {
                    String value = p.kind() == NodeParameter.Kind.INT
                            ? String.valueOf(node.paramInt(p.key()))
                            : trimFloat(node.paramFloat(p.key()));
                    drawScaledCenteredText(g, value, nx + scaled(71), py + scaled(3), 0xFFFFFF66);
                }
            }
        }
    }

    private void renderConditionParams(GuiGraphics g, int nx, int ny, int nw, SpellNode node) {
        List<NodeParameter> ps = node.type().parameters();
        int ph = scaled(PARAM_ROW_H);
        int py0 = ny + scaled(headerH(node));
        g.fill(nx, py0, nx + nw, py0 + ph, 0xFF353535);
        drawScaledText(g, Component.translatable("param.modern_magic.range").getString(),
                nx + scaled(4), py0 + scaled(3), 0xFFCCCCCC);
        String minS = String.valueOf(node.paramInt("min"));
        String maxS = String.valueOf(node.paramInt("max"));
        boolean editingMin = valueInput != null && inputNode == node && inputParam == ps.get(0);
        boolean editingMax = valueInput != null && inputNode == node && inputParam == ps.get(1);
        int bx = nx + scaled(46);
        drawScaledText(g, "[", bx, py0 + scaled(3), 0xFFCCCCCC);
        if (!editingMin) {
            drawScaledCenteredText(g, minS, bx + scaled(13), py0 + scaled(3), 0xFFFFFF66);
        }
        drawScaledText(g, ",", bx + scaled(26), py0 + scaled(3), 0xFFCCCCCC);
        if (!editingMax) {
            drawScaledCenteredText(g, maxS, bx + scaled(39), py0 + scaled(3), 0xFFFFFF66);
        }
        drawScaledText(g, "]", bx + scaled(52), py0 + scaled(3), 0xFFCCCCCC);
        int py1 = ny + scaled(headerH(node) + PARAM_ROW_H);
        g.fill(nx, py1, nx + nw, py1 + ph, 0xFF353535);
        drawScaledText(g, Component.translatable("param.modern_magic.else").getString(),
                nx + scaled(4), py1 + scaled(3), 0xFFAAAAAA);
        drawScaledCenteredText(g, "→ 2", nx + scaled(88), py1 + scaled(3), 0xFFAAAAAA);
    }

    private void renderPorts(GuiGraphics g, int cLeft, int cTop) {
        for (SpellNode node : graph.nodes()) {
            int nx = screenX(node.x());
            int ny = screenY(node.y());
            int nw = scaled(NODE_W);
            for (int i = 0; i < node.inputCount(); i++) {
                int py = ny + scaled(portY(node, i, true));
                int color = inputConnected(node, i) ? 0xFF66FF66 : 0xFF33BB33;
                g.fill(nx - 5, py - 1, nx + 3, py + 7, 0xFF0A0A0A);
                g.fill(nx - 4, py, nx + 2, py + 6, color);
            }
            for (int o = 0; o < node.outputCount(); o++) {
                int py = ny + scaled(portY(node, o, false));
                g.fill(nx + nw - 3, py - 1, nx + nw + 5, py + 7, 0xFF0A0A0A);
                g.fill(nx + nw - 2, py, nx + nw + 4, py + 6, 0xFFCC4444);
            }
        }
    }

    private int headerH(SpellNode node) {
        return Math.max(NODE_H, 24 + Math.max(node.inputCount(), node.outputCount()) * 14);
    }

    private int nodeH(SpellNode node) {
        return headerH(node) + node.type().parameters().size() * PARAM_ROW_H;
    }

    private boolean inputConnected(SpellNode node, int port) {
        for (SpellNode n : graph.nodes()) {
            for (List<SpellNode.Connection> list : n.allOutputs()) {
                for (SpellNode.Connection c : list) {
                    if (c.targetId.equals(node.id()) && c.targetInputPort == port) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private int portY(SpellNode node, int port, boolean input) {
        return 25 + port * 14;
    }

    private List<Component> nodeTooltip(SpellNodeType type, int manaCost) {
        List<Component> lines = new ArrayList<>();
        lines.add(type.displayName());
        lines.add(type.description().copy().withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("screen.modern_magic.mana_cost", manaCost).withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("screen.modern_magic.io_counts", type.inputCount(), type.outputCount())
                .withStyle(ChatFormatting.GREEN));
        return lines;
    }

    private void renderHoverTooltips(GuiGraphics g, int left, int top, int mx, int my) {
        if (dragNode != null) {
            return;
        }
        int cLeft = canvasLeft();
        int cTop = canvasTop();
        int cRight = canvasRight();
        int cBottom = canvasBottom();

        if (mx >= cLeft && mx <= cRight && my >= cTop && my <= cBottom) {
            int wx = (int) worldX(mx);
            int wy = (int) worldY(my);
            UUID id = nodeAt(wx, wy);
            if (id != null) {
                SpellNode node = graph.node(id);
                if (node != null) {
                    g.renderTooltip(font, nodeTooltip(node.type(), node.manaCost()), Optional.empty(), mx, my);
                    return;
                }
            }
        }

        int x = left + 4;
        if (mx >= x + 2 && mx <= x + 114 && my >= paletteTop() && my <= canvasBottom()) {
            for (PaletteRow row : paletteRows()) {
                if (inRect(mx, my, x + 2, row.y, 112, 18)) {
                    PaletteNode node = row.node;
                    if (!node.dir) {
                        SpellNodeType type = SpellRegistry.get(node.typeId);
                        if (type != null) {
                            g.renderTooltip(font, nodeTooltip(type, type.manaCost()), Optional.empty(), mx, my);
                        }
                    }
                    return;
                }
            }
        }
    }

    private static void drawLine(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0;
        int y = y0;
        while (true) {
            g.fill(x, y, x + 1, y + 1, color);
            if (x == x1 && y == y1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int left = guiLeft();
        int top = guiTop();

        if (valueInput != null) {
            if (!valueInput.isMouseOver(mx, my)) {
                commitValueInput();
            } else {
                valueInput.setFocused(true);
                return true;
            }
        }
        if (tab == TAB_ASSEMBLE && nameField != null && nameField.isVisible() && nameField.isMouseOver(mx, my)) {
            boolean result = nameField.mouseClicked(mx, my, button);

            if (button == 0) {
                nameField.setFocused(true);
            }
            return result;
        }
        if (nameField != null && nameField.isFocused() && !nameField.isMouseOver(mx, my)) {
            nameField.setFocused(false);
        }
        if (tab == TAB_ASSEMBLE && fileField != null && fileField.isVisible() && fileField.isMouseOver(mx, my)) {
            boolean result = fileField.mouseClicked(mx, my, button);
            if (button == 0) {
                fileField.setFocused(true);
            }
            return result;
        }
        if (fileField != null && fileField.isFocused() && !fileField.isMouseOver(mx, my)) {
            fileField.setFocused(false);
        }

        if (inRect(mx, my, left + 8, top + 16, 110, 14)) {
            tab = TAB_PRAY;
            return true;
        }
        if (inRect(mx, my, left + 122, top + 16, 110, 14)) {
            tab = TAB_ASSEMBLE;
            return true;
        }

        if (tab == TAB_PRAY) {
            return mouseClickedPray(mx, my, left, top);
        }
        return mouseClickedAssemble(mx, my, button, left, top);
    }

    private boolean mouseClickedPray(double mx, double my, int left, int top) {
        int[] ys = {top + 44, top + 78, top + 112};
        AttributeKind[] kinds = {AttributeKind.MANA, AttributeKind.REGEN, AttributeKind.COOLDOWN};
        for (int i = 0; i < 3; i++) {
            if (inRect(mx, my, left + 272, ys[i], 14, 14)) {
                editMinus(kinds[i]);
                return true;
            }
            if (inRect(mx, my, left + 336, ys[i], 14, 14)) {
                editPlus(kinds[i]);
                return true;
            }
            if (inRect(mx, my, left + 286, ys[i], 50, 14)) {
                openAttrInput(kinds[i], left + 286, ys[i]);
                return true;
            }
        }
        if (inRect(mx, my, left + 150, top + 180, 120, 18)) {
            doPray();
            return true;
        }
        return false;
    }

    private void editMinus(AttributeKind kind) {
        switch (kind) {
            case MANA -> editMaxMana = Math.max(10, editMaxMana - 10);
            case REGEN -> editRegenX100 = Math.max(50, editRegenX100 - 50);
            case COOLDOWN -> editCooldown = Math.max(1, editCooldown - 5);
        }
    }

    private void editPlus(AttributeKind kind) {
        switch (kind) {
            case MANA -> editMaxMana = Math.min(10000, editMaxMana + 10);
            case REGEN -> editRegenX100 = Math.min(10000, editRegenX100 + 50);
            case COOLDOWN -> editCooldown = Math.min(200, editCooldown + 5);
        }
    }

    private void doPray() {
        PacketDistributor.sendToServer(new ModNetwork.PrayPacket(editMaxMana, editRegenX100, editCooldown));
    }

    private boolean mouseClickedAssemble(double mx, double my, int button, int left, int top) {
        int cLeft = canvasLeft();
        int cTop = canvasTop();
        int cRight = canvasRight();
        int cBottom = canvasBottom();

        if (inRect(mx, my, left + 8, top + 36, 110, 14)) {
            doSave();
            return true;
        }
        if (inRect(mx, my, left + 4, top + 68, 36, 14)) {
            doExport();
            return true;
        }
        if (inRect(mx, my, left + 42, top + 68, 36, 14)) {
            doImport();
            return true;
        }
        if (inRect(mx, my, left + 80, top + 68, 36, 14)) {
            doClear();
            return true;
        }
        if (mouseClickedPalette(mx, my, left, top)) {
            return true;
        }
        if (mx < cLeft || mx > cRight || my < cTop || my > cBottom) {
            return false;
        }

        int wx = (int) worldX(mx);
        int wy = (int) worldY(my);

        if (button == 2) {
            return true; 
        }
        if (button == 1) {
            return rightClickCanvas(mx, my, cLeft, cTop, wx, wy);
        }

        UUID outNode = outputPortAt(mx, my, cLeft, cTop);
        if (outNode != null) {
            SpellNode node = graph.node(outNode);
            pendingSource = outNode;
            pendingOutPort = findOutputPort(mx, my, cLeft, cTop, node);
            return true;
        }
        UUID inNode = inputPortAt(mx, my, cLeft, cTop);
        if (inNode != null) {
            if (pendingSource != null) {
                SpellNode node = graph.node(inNode);
                int port = findInputPort(mx, my, cLeft, cTop, node);
                if (!graph.connect(pendingSource, pendingOutPort, inNode, port)) {

                    denyNotice("screen.modern_magic.port_busy");
                } else {
                    dirty = true;
                }
                clearPending();
            }
            return true;
        }
        UUID paramNode = paramAreaNode(wx, wy);
        if (paramNode != null) {
            SpellNode node = graph.node(paramNode);
            if (node != null) {
                clickParam(node, wx - (int) node.x(), wy - (int) node.y());
            }
            return true;
        }
        UUID body = nodeAt(wx, wy);
        if (body != null) {
            dragNode = body;
            SpellNode node = graph.node(body);
            dragOffsetX = wx - (int) node.x();
            dragOffsetY = wy - (int) node.y();
            return true;
        }
        if (selectedType != null) {

            if (pendingSource != null) {
                pendingSource = null;
                pendingOutPort = -1;
                return true;
            }
            SpellNodeType type = SpellRegistry.get(selectedType);
            if (type != null) {

                if (countNodeItems(type) == 0) {
                    denyNotice("screen.modern_magic.need_node");
                    return true;
                }
                pendingPlace = new PendingPlace(selectedType, UUID.randomUUID(),
                        wx - NODE_W / 2f, wy - NODE_H / 2f);
                PacketDistributor.sendToServer(new ModNetwork.PlaceNodePacket(
                        selectedType, pendingPlace.id, pendingPlace.x, pendingPlace.y));
            }
            return true;
        }
        clearPending();
        return true;
    }

    private boolean rightClickCanvas(double mx, double my, int cLeft, int cTop, int wx, int wy) {

        if (paramAreaNode(wx, wy) != null) {
            return true;
        }

        UUID inNode = inputPortAt(mx, my, cLeft, cTop);
        if (inNode != null) {
            SpellNode node = graph.node(inNode);
            int port = findInputPort(mx, my, cLeft, cTop, node);
            graph.disconnectInput(inNode, port);
            clearPending();
            dirty = true;
            return true;
        }

        UUID outNode = outputPortAt(mx, my, cLeft, cTop);
        if (outNode != null) {
            SpellNode node = graph.node(outNode);
            int port = findOutputPort(mx, my, cLeft, cTop, node);
            graph.disconnectOutput(outNode, port);
            clearPending();
            dirty = true;
            return true;
        }

        UUID body = nodeAt(wx, wy);
        if (body != null) {
            if (!graph.isStart(body)) {
                SpellNode removed = graph.node(body);
                if (removed != null) {
                    graph.removeNode(body);
                    dirty = true;
                    if (minecraft != null && minecraft.player != null && !minecraft.player.getAbilities().instabuild) {
                        knownCounts.merge(removed.type().icon().getItem(), 1, Integer::sum);
                    }
                    PacketDistributor.sendToServer(new ModNetwork.RemoveNodePacket(removed.type().id()));
                }
            }
            clearPending();
            return true;
        }
        clearPending();
        return true;
    }

    private UUID paramAreaNode(int wx, int wy) {
        for (SpellNode node : topDownNodes()) {
            if (node.type().parameters().isEmpty()) {
                continue;
            }
            if (wx >= node.x() && wx <= node.x() + NODE_W
                    && wy >= node.y() + headerH(node) && wy <= node.y() + nodeH(node)) {
                return node.id();
            }
        }
        return null;
    }

    private void clickParam(SpellNode node, int relX, int relY) {
        if (node.type().id().getPath().equals("condition")) {
            clickConditionParam(node, relX, relY);
            return;
        }
        int idx = (relY - headerH(node)) / PARAM_ROW_H;
        List<NodeParameter> ps = node.type().parameters();
        if (idx < 0 || idx >= ps.size()) {
            return;
        }
        NodeParameter p = ps.get(idx);
        switch (p.kind()) {
            case BOOL -> node.setParam(p.key(), !node.paramBool(p.key()));
            case INT -> {
                int step = (int) p.step();
                int cur = node.paramInt(p.key());
                if (relX <= 57) {
                    cur = cur - step;
                } else if (relX >= 85) {
                    cur = cur + step;
                } else {
                    int nx = screenX(node.x());
                    int ny = screenY(node.y());
                    int py = ny + scaled(headerH(node) + idx * PARAM_ROW_H);
                    openParamInput(node, p, nx + scaled(57), py, scaled(28), scaled(PARAM_ROW_H));
                    return;
                }
                cur = clampInt(cur, (int) p.min(), (int) p.max());
                node.setParam(p.key(), cur);
            }
            case FLOAT -> {
                float step = p.step();
                float cur = node.paramFloat(p.key());
                if (relX <= 57) {
                    cur = cur - step;
                } else if (relX >= 85) {
                    cur = cur + step;
                } else {
                    int nx = screenX(node.x());
                    int ny = screenY(node.y());
                    int py = ny + scaled(headerH(node) + idx * PARAM_ROW_H);
                    openParamInput(node, p, nx + scaled(57), py, scaled(28), scaled(PARAM_ROW_H));
                    return;
                }
                cur = Math.max(p.min(), Math.min(p.max(), cur));
                node.setParam(p.key(), cur);
            }
        }
        dirty = true;
    }

    private void openAttrInput(AttributeKind kind, int x, int y) {
        commitValueInput();
        this.valueInput = new EditBox(font, x, y, 48, 14, Component.literal(""));
        this.valueInput.setMaxLength(12);
        this.valueInput.setValue(attrText(kind));
        this.valueInput.setFocused(true);
        this.inputAttr = kind;
        this.inputNode = null;
        this.inputParam = null;
        this.inputDecimal = kind != AttributeKind.MANA;
    }

    private void openParamInput(SpellNode node, NodeParameter p, int x, int y, int w, int h) {
        commitValueInput();
        int boxW = Math.max(28, w);
        this.valueInput = new EditBox(font, x, y, boxW, Math.max(12, h), Component.literal(""));
        this.valueInput.setMaxLength(12);
        this.valueInput.setValue(p.kind() == NodeParameter.Kind.INT
                ? String.valueOf(node.paramInt(p.key()))
                : trimFloat(node.paramFloat(p.key())));
        this.valueInput.setFocused(true);
        this.inputNode = node;
        this.inputParam = p;
        this.inputAttr = null;
        this.inputDecimal = p.kind() != NodeParameter.Kind.INT;
    }

    private void commitValueInput() {
        if (valueInput == null) {
            return;
        }
        String text = valueInput.getValue().trim();
        if (inputAttr != null) {
            try {
                switch (inputAttr) {
                    case MANA -> editMaxMana = clampInt(Integer.parseInt(text), 10, 10000);
                    case REGEN -> editRegenX100 = clampInt(Math.round(Float.parseFloat(text) * 100f), 50, 10000);
                    case COOLDOWN -> editCooldown = clampInt(Math.round(Float.parseFloat(text) * 20f), 1, 200);
                }
            } catch (NumberFormatException ignored) {
            }
        } else if (inputNode != null && inputParam != null) {
            try {
                if (inputParam.kind() == NodeParameter.Kind.INT) {
                    int v = clampInt(Integer.parseInt(text), (int) inputParam.min(), (int) inputParam.max());
                    if (inputNode.type().id().getPath().equals("condition")) {
                        if (inputParam.key().equals("min")) {
                            v = Math.min(v, inputNode.paramInt("max"));
                        } else if (inputParam.key().equals("max")) {
                            v = Math.max(v, inputNode.paramInt("min"));
                        }
                    }
                    inputNode.setParam(inputParam.key(), v);
                } else {
                    float v = Math.max(inputParam.min(), Math.min(inputParam.max(), Float.parseFloat(text)));
                    inputNode.setParam(inputParam.key(), v);
                }
                dirty = true;
            } catch (NumberFormatException ignored) {
            }
        }
        valueInput = null;
        inputAttr = null;
        inputNode = null;
        inputParam = null;
        inputDecimal = false;
    }

    private String attrText(AttributeKind kind) {
        return switch (kind) {
            case MANA -> String.valueOf(editMaxMana);
            case REGEN -> trimFloat(editRegenX100 / 100.0f);
            case COOLDOWN -> String.format("%.1f", editCooldown / 20.0f);
        };
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void clickConditionParam(SpellNode node, int relX, int relY) {
        int idx = (relY - headerH(node)) / PARAM_ROW_H;
        if (idx != 0) {
            return;
        }
        List<NodeParameter> ps = node.type().parameters();
        int paramIndex;
        int inputX;
        if (relX >= 48 && relX <= 71) {
            paramIndex = 0;
            inputX = screenX(node.x()) + scaled(48);
        } else if (relX >= 72 && relX <= 96) {
            paramIndex = 1;
            inputX = screenX(node.x()) + scaled(72);
        } else {
            return;
        }
        int py = screenY(node.y()) + scaled(headerH(node));
        openParamInput(node, ps.get(paramIndex), inputX, py,
                scaled(24), scaled(PARAM_ROW_H));
    }

    private void clearPending() {
        pendingSource = null;
        pendingOutPort = -1;
        selectedType = null;
        dragNode = null;
    }

    private void doSave() {
        String name = nameField == null ? "" : nameField.getValue();
        PacketDistributor.sendToServer(new ModNetwork.SaveSpellPacket(graph.toTag(), name));
        if (minecraft != null && minecraft.getSoundManager() != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    public void receiveSaveAck(boolean ok) {
        if (ok) {
            dirty = false;
            setNotice(Component.translatable("screen.modern_magic.saved").getString(), 0xFF55FF55);
        } else {
            denyNotice("screen.modern_magic.save_failed");
        }
    }

    private String fileName() {
        return fileField == null ? "" : fileField.getValue().trim();
    }

    private void doExport() {
        String name = fileName();
        if (name.isEmpty()) {
            denyNotice("screen.modern_magic.name_empty");
            return;
        }
        clearPending();
        PacketDistributor.sendToServer(new ModNetwork.ExportSpellPacket(graph.toTag(), name));
    }

    private void doImport() {
        String name = fileName();
        if (name.isEmpty()) {
            denyNotice("screen.modern_magic.name_empty");
            return;
        }
        pendingImportName = name;
        openConfirm("screen.modern_magic.import_confirm_title",
                "screen.modern_magic.import_confirm_message", this::confirmImport);
    }

    private void doClear() {
        openConfirm("screen.modern_magic.clear_confirm_title",
                "screen.modern_magic.clear_confirm_message", this::confirmClear);
    }

    private void openConfirm(String titleKey, String messageKey, java.util.function.Consumer<Boolean> callback) {
        if (minecraft == null) {
            return;
        }
        minecraft.setScreen(new ConfirmScreen(callback::accept,
                Component.translatable(titleKey),
                Component.translatable(messageKey)));
    }

    private void confirmImport(boolean confirmed) {
        String name = pendingImportName;
        pendingImportName = null;
        if (minecraft != null) {
            minecraft.setScreen(this);
        }
        if (confirmed && name != null) {
            clearPending();
            PacketDistributor.sendToServer(new ModNetwork.ImportSpellPacket(graph.toTag(), name));
        }
    }

    private void confirmClear(boolean confirmed) {
        if (minecraft != null) {
            minecraft.setScreen(this);
        }
        if (confirmed) {
            clearPending();
            PacketDistributor.sendToServer(new ModNetwork.ClearSpellPacket(graph.toTag()));
        }
    }

    public void receiveFileAck(ModNetwork.SpellFileAckPacket packet) {
        if (packet.ok()) {
            switch (packet.action()) {
                case ModNetwork.FILE_ACTION_EXPORT -> setNotice(
                        Component.translatable(packet.msg().key(), packet.msg().arg()).getString(), 0xFF55FF55);
                case ModNetwork.FILE_ACTION_IMPORT -> {
                    graph = SpellGraph.fromTag(packet.spell());
                    applyDeltas(packet.deltas());
                    dirty = true;
                    clearPending();
                    setNotice(Component.translatable(packet.msg().key(), packet.msg().arg()).getString(), 0xFF55FF55);
                }
                case ModNetwork.FILE_ACTION_CLEAR -> {
                    graph = SpellGraph.createDefault();
                    applyDeltas(packet.deltas());
                    dirty = true;
                    clearPending();
                    setNotice(Component.translatable(packet.msg().key()).getString(), 0xFF55FF55);
                }
                default -> {
                }
            }
            return;
        }
        String key = packet.msg().key();
        String arg = packet.msg().arg();
        if (!packet.msg().nodes().isEmpty()) {
            setNotice(Component.translatable(key).getString() + " " + nodeNames(packet.msg().nodes()), 0xFFFF5555);
        } else if (arg != null && !arg.isEmpty()) {
            setNotice(Component.translatable(key, arg).getString(), 0xFFFF5555);
        } else {
            setNotice(Component.translatable(key).getString(), 0xFFFF5555);
        }
    }

    private String nodeNames(List<ResourceLocation> ids) {
        List<String> names = new ArrayList<>();
        for (ResourceLocation id : ids) {
            SpellNodeType type = SpellRegistry.get(id);
            names.add(type == null ? id.toString() : type.displayName().getString());
        }
        return String.join(", ", names);
    }

    private void applyDeltas(Map<ResourceLocation, Integer> deltas) {
        if (deltas == null || deltas.isEmpty()) {
            return;
        }
        for (Map.Entry<ResourceLocation, Integer> e : deltas.entrySet()) {
            Item item = BuiltInRegistries.ITEM.get(e.getKey());
            if (item != null) {
                knownCounts.compute(item, (k, v) -> Math.max(0, (v == null ? 0 : v) + e.getValue()));
            }
        }
    }

    private boolean mouseClickedPalette(double mx, double my, int left, int top) {
        if (!inRect(mx, my, left + 4, paletteTop(), 116, canvasBottom() - paletteTop())) {
            return false;
        }
        for (PaletteRow row : paletteRows()) {
            if (inRect(mx, my, left + 6, row.y, 110, 18)) {
                PaletteNode node = row.node;
                if (node.dir) {

                    if (expandedDirs.contains(node.path)) {
                        expandedDirs.remove(node.path);
                    } else {
                        expandedDirs.add(node.path);
                    }
                } else {
                    selectedType = node.typeId;
                }
                return true;
            }
        }
        return false;
    }

    private List<SpellNode> topDownNodes() {
        List<SpellNode> list = new ArrayList<>(graph.nodes());
        Collections.reverse(list);
        return list;
    }

    private UUID nodeAt(int wx, int wy) {
        for (SpellNode node : topDownNodes()) {
            if (wx >= node.x() && wx <= node.x() + NODE_W && wy >= node.y() && wy <= node.y() + nodeH(node)) {
                return node.id();
            }
        }
        return null;
    }

    private UUID outputPortAt(double mx, double my, int cLeft, int cTop) {
        for (SpellNode node : topDownNodes()) {
            int nx = screenX(node.x());
            int ny = screenY(node.y());
            int nw = scaled(NODE_W);
            for (int o = 0; o < node.outputCount(); o++) {
                int py = ny + scaled(portY(node, o, false));
                if (inRect(mx, my, nx + nw - 5, py - 2, 10, 10)) {
                    return node.id();
                }
            }
        }
        return null;
    }

    private UUID inputPortAt(double mx, double my, int cLeft, int cTop) {
        for (SpellNode node : topDownNodes()) {
            int nx = screenX(node.x());
            int ny = screenY(node.y());
            for (int i = 0; i < node.inputCount(); i++) {
                int py = ny + scaled(portY(node, i, true));
                if (inRect(mx, my, nx - 5, py - 2, 10, 10)) {
                    return node.id();
                }
            }
        }
        return null;
    }

    private int findOutputPort(double mx, double my, int cLeft, int cTop, SpellNode node) {
        int nx = screenX(node.x());
        int ny = screenY(node.y());
        int nw = scaled(NODE_W);
        for (int o = 0; o < node.outputCount(); o++) {
            int py = ny + scaled(portY(node, o, false));
            if (inRect(mx, my, nx + nw - 5, py - 2, 10, 10)) {
                return o;
            }
        }
        return 0;
    }

    private int findInputPort(double mx, double my, int cLeft, int cTop, SpellNode node) {
        int nx = screenX(node.x());
        int ny = screenY(node.y());
        for (int i = 0; i < node.inputCount(); i++) {
            int py = ny + scaled(portY(node, i, true));
            if (inRect(mx, my, nx - 5, py - 2, 10, 10)) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
        if (tab == TAB_ASSEMBLE) {
            if (button == 0 && dragNode != null) {
                float wx = worldX(mx);
                float wy = worldY(my);
                SpellNode node = graph.node(dragNode);
                if (node != null) {
                    node.setPosition(wx - dragOffsetX, wy - dragOffsetY);
                    dirty = true;
                }
                return true;
            }
            if (button == 2) {
                viewX -= (float) dragX / viewScale;
                viewY -= (float) dragY / viewScale;
                return true;
            }
        }
        return super.mouseDragged(mx, my, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (tab == TAB_ASSEMBLE && scrollY != 0) {
            int left = guiLeft();
            int top = guiTop();
            int cLeft = canvasLeft();
            int cTop = canvasTop();
            int cRight = canvasRight();
            int cBottom = canvasBottom();
            if (mx >= left + 4 && mx <= left + 120 && my >= paletteTop() && my <= canvasBottom()) {

                float maxScroll = Math.max(0, paletteRows().size() * 18f - (canvasBottom() - paletteTop()));
                paletteScroll = Math.max(0, Math.min(maxScroll, paletteScroll - (float) scrollY * 12f));
                return true;
            }
            if (mx >= cLeft && mx <= cRight && my >= cTop && my <= cBottom) {
                float old = viewScale;
                viewScale = Math.max(0.5f, Math.min(2.5f, viewScale * (scrollY > 0 ? 1.1f : 1.0f / 1.1f)));
                if (viewScale != old) {

                    float wx = (float) ((mx - cLeft) / old) + viewX;
                    float wy = (float) ((my - cTop) / old) + viewY;
                    viewX = wx - (float) ((mx - cLeft) / viewScale);
                    viewY = wy - (float) ((my - cTop) / viewScale);
                }
                return true;
            }
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragNode != null) {
            dragNode = null;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (valueInput != null && valueInput.isFocused()) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
                commitValueInput();
                return true;
            }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                valueInput = null;
                inputAttr = null;
                inputNode = null;
                inputParam = null;
                inputDecimal = false;
                return true;
            }
            if (valueInput.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        if (nameField != null && nameField.isVisible() && nameField.isFocused()
                && nameField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (fileField != null && fileField.isVisible() && fileField.isFocused()
                && fileField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (valueInput != null && valueInput.isFocused()) {
            if (isValidInputChar(codePoint) && valueInput.charTyped(codePoint, modifiers)) {
                return true;
            }
            return true;
        }
        if (nameField != null && nameField.isVisible() && nameField.isFocused()
                && nameField.charTyped(codePoint, modifiers)) {
            return true;
        }
        if (fileField != null && fileField.isVisible() && fileField.isFocused()
                && fileField.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private boolean isValidInputChar(char c) {
        if (Character.isDigit(c)) {
            return true;
        }
        if (inputDecimal && c == '.') {
            return !valueInput.getValue().contains(".");
        }
        if (c == '-') {
            return valueInput.getCursorPosition() == 0 && !valueInput.getValue().startsWith("-");
        }
        return false;
    }

    @Override
    public void onClose() {
        if (dirty && minecraft != null) {

            minecraft.setScreen(new ConfirmScreen(this::confirmClose,
                    Component.translatable("screen.modern_magic.unsaved_title"),
                    Component.translatable("screen.modern_magic.unsaved_message")));
        } else {
            super.onClose();
        }
    }

    private void confirmClose(boolean confirmed) {
        if (minecraft == null) {
            return;
        }
        if (confirmed) {
            minecraft.setScreen(null);
            if (minecraft.player != null) {
                minecraft.player.closeContainer();
            }
        } else {
            minecraft.setScreen(this);
        }
    }

    private static boolean inRect(double mx, double my, double x, double y, double w, double h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static String trimFloat(float f) {
        if (f == Math.round(f)) {
            return String.valueOf(Math.round(f));
        }
        return String.format("%.1f", f);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum AttributeKind {
        MANA, REGEN, COOLDOWN
    }
}
