package top.ydog01.mmagic.quest;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import top.ydog01.mmagic.ModernMagic;
import top.ydog01.mmagic.entity.WizardEntity;
import top.ydog01.mmagic.init.ModAttachments;
import top.ydog01.mmagic.init.ModItems;
import top.ydog01.mmagic.item.SpellNodeItem;
import top.ydog01.mmagic.network.ModNetwork;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = ModernMagic.MODID)
public final class Quests {
    private static List<QuestDef> ALL;

    private Quests() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ModernMagic.MODID, path);
    }

    public static List<QuestDef> all() {
        if (ALL == null) {
            ALL = List.of(
                    new QuestDef(id("wand"), QuestDef.Kind.WAND, new ItemStack(ModItems.WAND.get()), 1, 40,
                            List.of(new ItemStack(ModItems.MAGIC_CRYSTAL.get(), 16)), recipe("wand")),
                    new QuestDef(id("altar"), QuestDef.Kind.ALTAR, new ItemStack(ModItems.ALTAR.get()), 1, 20,
                            List.of(new ItemStack(ModItems.MAGIC_CRYSTAL.get(), 8)), recipe("altar")),
                    new QuestDef(id("crystals"), QuestDef.Kind.CRYSTALS, new ItemStack(ModItems.MAGIC_CRYSTAL.get()), 24, 25,
                            List.of(new ItemStack(ModItems.EXTRA_NODES.get("fire").get()),
                                    new ItemStack(ModItems.MAGIC_CRYSTAL.get(), 4)), recipe("magic_crystal")),
                    new QuestDef(id("missile"), QuestDef.Kind.MISSILE,
                            new ItemStack(ModItems.SPELL_NODE_MAGIC_MISSILE.get()), 1, 20,
                            List.of(new ItemStack(ModItems.MAGIC_CRYSTAL.get(), 8)),
                            recipe("spell_node_magic_missile")),
                    new QuestDef(id("nodes"), QuestDef.Kind.NODES, new ItemStack(ModItems.SPELL_NODE_EXPLOSION.get()), 8, 50,
                            List.of(new ItemStack(ModItems.EXTRA_NODES.get("homing").get()),
                                    new ItemStack(ModItems.MAGIC_CRYSTAL.get(), 16)), null),
                    new QuestDef(id("duplicate"), QuestDef.Kind.DUPLICATE,
                            new ItemStack(ModItems.MAGIC_CRYSTAL.get()), 1, 20,
                            List.of(new ItemStack(ModItems.MAGIC_CRYSTAL.get(), 8)),
                            recipe("duplicate_magic_missile")),
                    new QuestDef(id("spell_edit"), QuestDef.Kind.SPELL_EDIT,
                            new ItemStack(ModItems.EXTRA_NODES.get("echo").get()), 1, 30,
                            List.of(new ItemStack(ModItems.EXTRA_NODES.get("multi_cast").get()),
                                    new ItemStack(ModItems.MAGIC_CRYSTAL.get(), 8)), null)
            );
        }
        return ALL;
    }

    private static ResourceLocation recipe(String path) {
        return ResourceLocation.fromNamespaceAndPath(ModernMagic.MODID, path);
    }

    public static QuestDef get(ResourceLocation id) {
        for (QuestDef def : all()) {
            if (def.id().equals(id)) {
                return def;
            }
        }
        return null;
    }

    public static void openBook(ServerPlayer sp) {
        refreshPossession(sp);
        sendSync(sp, true);
    }

    public static void onWizardKill(ServerPlayer sp, boolean legendary) {
        QuestProgress data = sp.getData(ModAttachments.QUEST_PROGRESS);
        Map<ResourceLocation, QuestProgress.Entry> entries = new HashMap<>(data.entries());
        boolean changed = false;
        for (QuestDef def : all()) {
            boolean matches = def.kind() == QuestDef.Kind.WIZARD_KILL
                    || (def.kind() == QuestDef.Kind.LEGENDARY_KILL && legendary);
            if (matches) {
                QuestProgress.Entry old = entries.get(def.id());
                int progress = Math.min((old == null ? 0 : old.progress()) + 1, def.target());
                entries.put(def.id(), new QuestProgress.Entry(progress, old != null && old.claimed()));
                changed = true;
            }
        }
        if (changed) {
            sp.setData(ModAttachments.QUEST_PROGRESS, new QuestProgress(entries));
            sendSync(sp, false);
        }
    }

    public static void onSpellEdited(ServerPlayer sp) {
        QuestProgress data = sp.getData(ModAttachments.QUEST_PROGRESS);
        Map<ResourceLocation, QuestProgress.Entry> entries = new HashMap<>(data.entries());
        boolean changed = false;
        for (QuestDef def : all()) {
            if (def.kind() == QuestDef.Kind.SPELL_EDIT) {
                QuestProgress.Entry old = entries.get(def.id());
                int progress = Math.min((old == null ? 0 : old.progress()) + 1, def.target());
                entries.put(def.id(), new QuestProgress.Entry(progress, old != null && old.claimed()));
                changed = true;
            }
        }
        if (changed) {
            sp.setData(ModAttachments.QUEST_PROGRESS, new QuestProgress(entries));
            sendSync(sp, false);
        }
    }

    public static void refreshPossession(ServerPlayer sp) {
        QuestProgress data = sp.getData(ModAttachments.QUEST_PROGRESS);
        Map<ResourceLocation, QuestProgress.Entry> entries = new HashMap<>(data.entries());
        boolean changed = false;
        for (QuestDef def : all()) {
            int computed = Math.min(computePossession(sp, def.kind()), def.target());
            QuestProgress.Entry old = entries.get(def.id());
            int stored = old == null ? 0 : old.progress();
            if (computed > stored) {
                entries.put(def.id(), new QuestProgress.Entry(computed, old != null && old.claimed()));
                changed = true;
            }
        }
        if (changed) {
            sp.setData(ModAttachments.QUEST_PROGRESS, new QuestProgress(entries));
            sendSync(sp, false);
        }
    }

    private static int computePossession(ServerPlayer sp, QuestDef.Kind kind) {
        return switch (kind) {
            case ALTAR -> countItem(sp, ModItems.ALTAR.get());
            case CRYSTALS -> countItem(sp, ModItems.MAGIC_CRYSTAL.get());
            case WAND -> countItem(sp, ModItems.WAND.get());
            case NODES -> countDistinctNodes(sp);
            case MISSILE -> countItem(sp, ModItems.SPELL_NODE_MAGIC_MISSILE.get());
            case DUPLICATE -> countDuplicatedNodes(sp);
            default -> 0;
        };
    }

    private static int countItem(Player player, Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int countDistinctNodes(Player player) {
        Set<Item> types = new HashSet<>();
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof SpellNodeItem) {
                types.add(stack.getItem());
            }
        }
        return types.size();
    }

    private static int countDuplicatedNodes(Player player) {
        Map<Item, Integer> counts = new HashMap<>();
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof SpellNodeItem) {
                counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        for (int count : counts.values()) {
            if (count >= 2) {
                return 1;
            }
        }
        return 0;
    }

    public static void claim(ServerPlayer sp, ResourceLocation id) {
        QuestDef def = get(id);
        if (def == null) {
            return;
        }
        QuestProgress data = sp.getData(ModAttachments.QUEST_PROGRESS);
        QuestProgress.Entry entry = data.entries().get(id);
        if (entry == null || entry.claimed() || entry.progress() < def.target()) {
            return;
        }
        Map<ResourceLocation, QuestProgress.Entry> entries = new HashMap<>(data.entries());
        entries.put(id, new QuestProgress.Entry(entry.progress(), true));
        sp.setData(ModAttachments.QUEST_PROGRESS, new QuestProgress(entries));
        for (ItemStack reward : def.rewards()) {
            ItemStack copy = reward.copy();
            if (!sp.getInventory().add(copy)) {
                sp.drop(copy, false);
            }
        }
        if (def.xp() > 0) {
            sp.giveExperiencePoints(def.xp());
        }
        sendSync(sp, false);
    }

    public static void sendSync(ServerPlayer sp, boolean open) {
        if (sp.connection == null) {
            return;
        }
        PacketDistributor.sendToPlayer(sp,
                new ModNetwork.QuestSyncPacket(sp.getData(ModAttachments.QUEST_PROGRESS), open));
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof WizardEntity wizard)) {
            return;
        }
        if (event.getSource().getEntity() instanceof ServerPlayer sp) {
            onWizardKill(sp, wizard.isLegendary());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer sp && sp.level().getGameTime() % 20 == 0) {
            refreshPossession(sp);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            if (!sp.getData(ModAttachments.STARTER_BOOK)) {
                ItemStack book = new ItemStack(ModItems.QUEST_BOOK.get());
                if (!sp.getInventory().add(book)) {
                    sp.drop(book, false);
                }
                sp.setData(ModAttachments.STARTER_BOOK, Boolean.TRUE);
                net.minecraft.advancements.CriteriaTriggers.INVENTORY_CHANGED.trigger(sp,
                        sp.getInventory(), new ItemStack(ModItems.QUEST_BOOK.get()));
            }
            sendSync(sp, false);
        }
    }
}
