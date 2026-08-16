package top.ydog01.mmagic.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import top.ydog01.mmagic.ModernMagic;
import top.ydog01.mmagic.client.AltarScreen;
import top.ydog01.mmagic.client.QuestBookClient;
import top.ydog01.mmagic.client.WandHud;
import top.ydog01.mmagic.menu.AltarMenu;
import top.ydog01.mmagic.quest.QuestProgress;
import top.ydog01.mmagic.quest.Quests;
import top.ydog01.mmagic.spell.SpellGraph;
import top.ydog01.mmagic.spell.SpellNode;
import top.ydog01.mmagic.spell.SpellNodeType;
import top.ydog01.mmagic.spell.SpellRegistry;
import top.ydog01.mmagic.spell.SpellRunner;
import top.ydog01.mmagic.util.Crystals;
import top.ydog01.mmagic.util.SpellCost;
import top.ydog01.mmagic.util.SpellFiles;
import top.ydog01.mmagic.util.WandData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ModNetwork {
    private ModNetwork() {
    }

    @EventBusSubscriber(modid = ModernMagic.MODID)
    public static final class NetworkEvents {
        private NetworkEvents() {
        }

        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            PayloadRegistrar registrar = event.registrar("1");
            registrar.playToClient(SyncAltarPacket.TYPE, SyncAltarPacket.STREAM_CODEC, ModNetwork::handleSync);
            registrar.playToClient(AltarNoticePacket.TYPE, AltarNoticePacket.STREAM_CODEC, ModNetwork::handleAltarNotice);
            registrar.playToClient(PlaceNodeAckPacket.TYPE, PlaceNodeAckPacket.STREAM_CODEC, ModNetwork::handlePlaceNodeAck);
            registrar.playToClient(ManaSyncPacket.TYPE, ManaSyncPacket.STREAM_CODEC, ModNetwork::handleManaSync);
            registrar.playToServer(RequestAltarSyncPacket.TYPE, RequestAltarSyncPacket.STREAM_CODEC, ModNetwork::handleRequest);
            registrar.playToServer(PrayPacket.TYPE, PrayPacket.STREAM_CODEC, ModNetwork::handlePray);
            registrar.playToServer(SaveSpellPacket.TYPE, SaveSpellPacket.STREAM_CODEC, ModNetwork::handleSave);
            registrar.playToClient(SaveSpellAckPacket.TYPE, SaveSpellAckPacket.STREAM_CODEC, ModNetwork::handleSaveAck);
            registrar.playToServer(PlaceNodePacket.TYPE, PlaceNodePacket.STREAM_CODEC, ModNetwork::handlePlaceNode);
            registrar.playToServer(RemoveNodePacket.TYPE, RemoveNodePacket.STREAM_CODEC, ModNetwork::handleRemoveNode);
            registrar.playToServer(ExportSpellPacket.TYPE, ExportSpellPacket.STREAM_CODEC, ModNetwork::handleExport);
            registrar.playToServer(ImportSpellPacket.TYPE, ImportSpellPacket.STREAM_CODEC, ModNetwork::handleImport);
            registrar.playToServer(ClearSpellPacket.TYPE, ClearSpellPacket.STREAM_CODEC, ModNetwork::handleClear);
            registrar.playToClient(SpellFileAckPacket.TYPE, SpellFileAckPacket.STREAM_CODEC, ModNetwork::handleFileAck);
            registrar.playToClient(QuestSyncPacket.TYPE, QuestSyncPacket.STREAM_CODEC, ModNetwork::handleQuestSync);
            registrar.playToServer(ClaimQuestPacket.TYPE, ClaimQuestPacket.STREAM_CODEC, ModNetwork::handleClaimQuest);
        }
    }

    public record QuestSyncPacket(QuestProgress progress, boolean open) implements CustomPacketPayload {
        public static final Type<QuestSyncPacket> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(ModernMagic.MODID, "quest_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, QuestSyncPacket> STREAM_CODEC = StreamCodec.composite(
                QuestProgress.STREAM_CODEC, QuestSyncPacket::progress,
                ByteBufCodecs.BOOL, QuestSyncPacket::open,
                QuestSyncPacket::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ClaimQuestPacket(ResourceLocation questId) implements CustomPacketPayload {
        public static final Type<ClaimQuestPacket> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(ModernMagic.MODID, "claim_quest"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClaimQuestPacket> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, ClaimQuestPacket::questId,
                ClaimQuestPacket::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void handleQuestSync(QuestSyncPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> QuestBookClient.receive(packet));
    }

    public static void handleClaimQuest(ClaimQuestPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                Quests.claim(sp, packet.questId());
            }
        });
    }

    public record ManaSyncPacket(double mana, int maxMana, int regenX100, int cooldown, long lastCastTick,
                                 long lastUpdateTick)
            implements CustomPacketPayload {
        public static final Type<ManaSyncPacket> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(ModernMagic.MODID, "mana_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ManaSyncPacket> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.DOUBLE, ManaSyncPacket::mana,
                ByteBufCodecs.INT, ManaSyncPacket::maxMana,
                ByteBufCodecs.INT, ManaSyncPacket::regenX100,
                ByteBufCodecs.INT, ManaSyncPacket::cooldown,
                ByteBufCodecs.VAR_LONG, ManaSyncPacket::lastCastTick,
                ByteBufCodecs.VAR_LONG, ManaSyncPacket::lastUpdateTick,
                ManaSyncPacket::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RemoveNodePacket(ResourceLocation nodeType) implements CustomPacketPayload {
        public static final Type<RemoveNodePacket> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(ModernMagic.MODID, "remove_node"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RemoveNodePacket> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, RemoveNodePacket::nodeType,
                RemoveNodePacket::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record PlaceNodePacket(ResourceLocation nodeType, UUID nodeId, float x, float y) implements CustomPacketPayload {
        public static final Type<PlaceNodePacket> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(ModernMagic.MODID, "place_node"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PlaceNodePacket> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, PlaceNodePacket::nodeType,
                UUIDUtil.STREAM_CODEC, PlaceNodePacket::nodeId,
                ByteBufCodecs.FLOAT, PlaceNodePacket::x,
                ByteBufCodecs.FLOAT, PlaceNodePacket::y,
                PlaceNodePacket::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record PlaceNodeAckPacket(boolean ok) implements CustomPacketPayload {
        public static final Type<PlaceNodeAckPacket> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(ModernMagic.MODID, "place_node_ack"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PlaceNodeAckPacket> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, PlaceNodeAckPacket::ok,
                PlaceNodeAckPacket::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record WandAttrs(int maxMana, int regenX100, int cooldown) {
        public static final StreamCodec<RegistryFriendlyByteBuf, WandAttrs> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, WandAttrs::maxMana,
                ByteBufCodecs.INT, WandAttrs::regenX100,
                ByteBufCodecs.INT, WandAttrs::cooldown,
                WandAttrs::new
        );
    }

    public record SyncAltarPacket(WandAttrs attrs, int crystals, boolean hasWand, String name, CompoundTag spell)
            implements CustomPacketPayload {
        public static final Type<SyncAltarPacket> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(ModernMagic.MODID, "sync_altar"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncAltarPacket> STREAM_CODEC = StreamCodec.composite(
                WandAttrs.STREAM_CODEC, SyncAltarPacket::attrs,
                ByteBufCodecs.INT, SyncAltarPacket::crystals,
                ByteBufCodecs.BOOL, SyncAltarPacket::hasWand,
                ByteBufCodecs.STRING_UTF8, SyncAltarPacket::name,
                ByteBufCodecs.COMPOUND_TAG, SyncAltarPacket::spell,
                SyncAltarPacket::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RequestAltarSyncPacket() implements CustomPacketPayload {
        public static final Type<RequestAltarSyncPacket> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(ModernMagic.MODID, "request_altar_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestAltarSyncPacket> STREAM_CODEC =
                StreamCodec.unit(new RequestAltarSyncPacket());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record AltarNoticePacket(String key) implements CustomPacketPayload {
        public static final Type<AltarNoticePacket> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(ModernMagic.MODID, "altar_notice"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AltarNoticePacket> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, AltarNoticePacket::key,
                AltarNoticePacket::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void handleAltarNotice(AltarNoticePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof AltarScreen screen) {
                screen.receiveNotice(packet.key());
            }
        });
    }

    public static void sendNotice(ServerPlayer sp, String key) {
        if (sp.connection == null) {
            return;
        }
        PacketDistributor.sendToPlayer(sp, new AltarNoticePacket(key));
    }

    public record PrayPacket(int maxMana, int regenX100, int cooldown) implements CustomPacketPayload {
        public static final Type<PrayPacket> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(ModernMagic.MODID, "pray"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PrayPacket> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, PrayPacket::maxMana,
                ByteBufCodecs.INT, PrayPacket::regenX100,
                ByteBufCodecs.INT, PrayPacket::cooldown,
                PrayPacket::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SaveSpellPacket(CompoundTag spell, String name) implements CustomPacketPayload {
        public static final Type<SaveSpellPacket> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(ModernMagic.MODID, "save_spell"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SaveSpellPacket> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, SaveSpellPacket::spell,
                ByteBufCodecs.STRING_UTF8, SaveSpellPacket::name,
                SaveSpellPacket::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SaveSpellAckPacket(boolean ok) implements CustomPacketPayload {
        public static final Type<SaveSpellAckPacket> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(ModernMagic.MODID, "save_spell_ack"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SaveSpellAckPacket> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, SaveSpellAckPacket::ok,
                SaveSpellAckPacket::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void handleSaveAck(SaveSpellAckPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof AltarScreen screen) {
                screen.receiveSaveAck(packet.ok());
            }
        });
    }

    public static final int FILE_ACTION_EXPORT = 0;
    public static final int FILE_ACTION_IMPORT = 1;
    public static final int FILE_ACTION_CLEAR = 2;

    public record ExportSpellPacket(CompoundTag spell, String name) implements CustomPacketPayload {
        public static final Type<ExportSpellPacket> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(ModernMagic.MODID, "export_spell"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ExportSpellPacket> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, ExportSpellPacket::spell,
                ByteBufCodecs.STRING_UTF8, ExportSpellPacket::name,
                ExportSpellPacket::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ImportSpellPacket(CompoundTag current, String name) implements CustomPacketPayload {
        public static final Type<ImportSpellPacket> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(ModernMagic.MODID, "import_spell"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ImportSpellPacket> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, ImportSpellPacket::current,
                ByteBufCodecs.STRING_UTF8, ImportSpellPacket::name,
                ImportSpellPacket::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ClearSpellPacket(CompoundTag current) implements CustomPacketPayload {
        public static final Type<ClearSpellPacket> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(ModernMagic.MODID, "clear_spell"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClearSpellPacket> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, ClearSpellPacket::current,
                ClearSpellPacket::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SpellFileAckPacket(int action, boolean ok, FileMsg msg, CompoundTag spell,
                                     Map<ResourceLocation, Integer> deltas)
            implements CustomPacketPayload {
        public static final Type<SpellFileAckPacket> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(ModernMagic.MODID, "spell_file_ack"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SpellFileAckPacket> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, SpellFileAckPacket::action,
                ByteBufCodecs.BOOL, SpellFileAckPacket::ok,
                FileMsg.STREAM_CODEC, SpellFileAckPacket::msg,
                ByteBufCodecs.COMPOUND_TAG, SpellFileAckPacket::spell,
                ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.INT),
                SpellFileAckPacket::deltas,
                SpellFileAckPacket::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record FileMsg(String key, String arg, List<ResourceLocation> nodes) {
        public static final StreamCodec<RegistryFriendlyByteBuf, FileMsg> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, FileMsg::key,
                ByteBufCodecs.STRING_UTF8, FileMsg::arg,
                ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), FileMsg::nodes,
                FileMsg::new
        );
    }

    public static void handleRequest(RequestAltarSyncPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                sendSync(sp);
            }
        });
    }

    public static void handlePray(PrayPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp) || !(sp.containerMenu instanceof AltarMenu)) {
                return;
            }
            ItemStack wand = SpellRunner.findWand(sp);
            if (wand.isEmpty()) {
                sendNotice(sp, "screen.modern_magic.need_wand");
                return;
            }

            int maxMana = clamp(packet.maxMana(), 10, 10000);
            double regen = clamp(packet.regenX100() / 100.0, 0.5, 100.0);
            int cooldown = clamp(packet.cooldown(), 1, 200);
            int cost = SpellCost.charge(WandData.getMaxMana(wand), WandData.getRegen(wand), WandData.getCooldown(wand),
                    maxMana, regen, cooldown);

            if (!sp.getAbilities().instabuild && !Crystals.consume(sp, cost)) {
                sendNotice(sp, "screen.modern_magic.not_enough_crystals");
                return;
            }
            WandData.setAttributes(wand, maxMana, regen, cooldown);
            sendSync(sp);
        });
    }

    public static void handleSave(SaveSpellPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp) || !(sp.containerMenu instanceof AltarMenu)) {
                return;
            }
            ItemStack wand = SpellRunner.findWand(sp);
            if (wand.isEmpty()) {
                sendNotice(sp, "screen.modern_magic.need_wand");
                return;
            }
            WandData.setGraph(wand, SpellGraph.fromTag(packet.spell()));
            String name = packet.name() == null ? "" : packet.name().trim();
            if (name.length() > 50) {
                name = name.substring(0, 50);
            }
            WandData.setName(wand, name);
            PacketDistributor.sendToPlayer(sp, new SaveSpellAckPacket(true));
            sendSync(sp);
        });
    }

    public static void handlePlaceNode(PlaceNodePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            boolean ok = false;
            if (ctx.player() instanceof ServerPlayer sp && sp.containerMenu instanceof AltarMenu) {
                ItemStack wand = SpellRunner.findWand(sp);
                if (!wand.isEmpty()) {
                    SpellNodeType type = SpellRegistry.get(packet.nodeType());
                    if (type != null) {

                        Item nodeItem = type.icon().getItem();
                        ok = sp.getAbilities().instabuild || Crystals.consumeItem(sp, nodeItem, 1);
                        if (ok) {
                            Quests.onSpellEdited(sp);
                        }
                    }
                }
            }
            if (ctx.player() instanceof ServerPlayer sp) {
                PacketDistributor.sendToPlayer(sp, new PlaceNodeAckPacket(ok));
            }
        });
    }

    public static void handlePlaceNodeAck(PlaceNodeAckPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof AltarScreen screen) {
                screen.receivePlaceAck(packet);
            }
        });
    }

    public static void handleRemoveNode(RemoveNodePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp) || !(sp.containerMenu instanceof AltarMenu)) {
                return;
            }
            if (SpellRunner.findWand(sp).isEmpty()) {
                return;
            }
            SpellNodeType type = SpellRegistry.get(packet.nodeType());
            if (type == null) {
                return;
            }

            if (!sp.getAbilities().instabuild) {
                sp.getInventory().add(new ItemStack(type.icon().getItem()));
            }
        });
    }

    public static void handleExport(ExportSpellPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp) || !(sp.containerMenu instanceof AltarMenu)) {
                return;
            }
            if (SpellRunner.findWand(sp).isEmpty()) {
                sendFileAck(sp, FILE_ACTION_EXPORT, false, "screen.modern_magic.need_wand", "", List.of(),
                        new CompoundTag(), Map.of());
                return;
            }
            String name = SpellFiles.sanitize(packet.name());
            if (name.isEmpty() || !SpellFiles.write(sp.getServer(), name, packet.spell())) {
                sendFileAck(sp, FILE_ACTION_EXPORT, false, "screen.modern_magic.export_fail", "", List.of(),
                        new CompoundTag(), Map.of());
                return;
            }
            sendFileAck(sp, FILE_ACTION_EXPORT, true, "screen.modern_magic.export_ok", name + SpellFiles.EXT,
                    List.of(), new CompoundTag(), Map.of());
        });
    }

    public static void handleImport(ImportSpellPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp) || !(sp.containerMenu instanceof AltarMenu)) {
                return;
            }
            if (SpellRunner.findWand(sp).isEmpty()) {
                sendFileAck(sp, FILE_ACTION_IMPORT, false, "screen.modern_magic.need_wand", "", List.of(),
                        new CompoundTag(), Map.of());
                return;
            }
            String name = SpellFiles.sanitize(packet.name());
            if (name.isEmpty()) {
                sendFileAck(sp, FILE_ACTION_IMPORT, false, "screen.modern_magic.name_empty", "", List.of(),
                        new CompoundTag(), Map.of());
                return;
            }
            if (!SpellFiles.exists(sp.getServer(), name)) {
                sendFileAck(sp, FILE_ACTION_IMPORT, false, "screen.modern_magic.import_fail_notfound",
                        name + SpellFiles.EXT, List.of(), new CompoundTag(), Map.of());
                return;
            }
            CompoundTag tag = SpellFiles.parse(SpellFiles.file(sp.getServer(), name));
            if (tag == null) {
                sendFileAck(sp, FILE_ACTION_IMPORT, false, "screen.modern_magic.import_fail_invalid", "", List.of(),
                        new CompoundTag(), Map.of());
                return;
            }
            List<String> unknown = unknownTypes(tag);
            if (!unknown.isEmpty()) {
                sendFileAck(sp, FILE_ACTION_IMPORT, false, "screen.modern_magic.import_fail_unknown", "", List.of(),
                        new CompoundTag(), Map.of());
                return;
            }
            SpellGraph newGraph = SpellGraph.fromTag(tag);
            SpellGraph current = SpellGraph.fromTag(packet.current());
            boolean creative = sp.getAbilities().instabuild;
            Map<ResourceLocation, Integer> haveTypes = nodeTypeCounts(current);
            Map<ResourceLocation, Integer> neededTypes = nodeTypeCounts(newGraph);
            Map<ResourceLocation, Integer> deltas = new HashMap<>();
            if (!creative) {
                List<ResourceLocation> missing = new ArrayList<>();
                for (Map.Entry<ResourceLocation, Integer> e : neededTypes.entrySet()) {
                    SpellNodeType type = SpellRegistry.get(e.getKey());
                    if (type == null) {
                        continue;
                    }
                    int available = Crystals.countOf(sp, type.icon().getItem())
                            + haveTypes.getOrDefault(e.getKey(), 0);
                    if (available < e.getValue()) {
                        missing.add(e.getKey());
                    }
                }
                if (!missing.isEmpty()) {
                    sendFileAck(sp, FILE_ACTION_IMPORT, false, "screen.modern_magic.import_fail_missing", "", missing,
                            new CompoundTag(), Map.of());
                    return;
                }
                for (Map.Entry<ResourceLocation, Integer> e : haveTypes.entrySet()) {
                    SpellNodeType type = SpellRegistry.get(e.getKey());
                    if (type != null) {
                        Item item = type.icon().getItem();
                        giveItems(sp, item, e.getValue());
                        deltas.merge(BuiltInRegistries.ITEM.getKey(item), e.getValue(), Integer::sum);
                    }
                }
                for (Map.Entry<ResourceLocation, Integer> e : neededTypes.entrySet()) {
                    SpellNodeType type = SpellRegistry.get(e.getKey());
                    if (type != null) {
                        Item item = type.icon().getItem();
                        Crystals.consumeItem(sp, item, e.getValue());
                        deltas.merge(BuiltInRegistries.ITEM.getKey(item), -e.getValue(), Integer::sum);
                    }
                }
            }
            if (!neededTypes.isEmpty()) {
                Quests.onSpellEdited(sp);
            }
            sendFileAck(sp, FILE_ACTION_IMPORT, true, "screen.modern_magic.import_ok", name + SpellFiles.EXT,
                    List.of(), newGraph.toTag(), deltas);
        });
    }

    public static void handleClear(ClearSpellPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp) || !(sp.containerMenu instanceof AltarMenu)) {
                return;
            }
            if (SpellRunner.findWand(sp).isEmpty()) {
                sendFileAck(sp, FILE_ACTION_CLEAR, false, "screen.modern_magic.need_wand", "", List.of(),
                        new CompoundTag(), Map.of());
                return;
            }
            Map<ResourceLocation, Integer> deltas = new HashMap<>();
            if (!sp.getAbilities().instabuild) {
                for (Map.Entry<ResourceLocation, Integer> e : nodeTypeCounts(SpellGraph.fromTag(packet.current())).entrySet()) {
                    SpellNodeType type = SpellRegistry.get(e.getKey());
                    if (type != null) {
                        Item item = type.icon().getItem();
                        giveItems(sp, item, e.getValue());
                        deltas.merge(BuiltInRegistries.ITEM.getKey(item), e.getValue(), Integer::sum);
                    }
                }
            }
            sendFileAck(sp, FILE_ACTION_CLEAR, true, "screen.modern_magic.cleared", "", List.of(),
                    new CompoundTag(), deltas);
        });
    }

    public static void handleFileAck(SpellFileAckPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof AltarScreen screen) {
                screen.receiveFileAck(packet);
            }
        });
    }

    private static void sendFileAck(ServerPlayer sp, int action, boolean ok, String key, String arg,
                                    List<ResourceLocation> nodes, CompoundTag spell, Map<ResourceLocation, Integer> deltas) {
        PacketDistributor.sendToPlayer(sp, new SpellFileAckPacket(action, ok, new FileMsg(key, arg, nodes), spell, deltas));
    }

    private static Map<ResourceLocation, Integer> nodeTypeCounts(SpellGraph graph) {
        Map<ResourceLocation, Integer> counts = new HashMap<>();
        for (SpellNode node : graph.nodes()) {
            if (graph.isStart(node.id())) {
                continue;
            }
            counts.merge(node.type().id(), 1, Integer::sum);
        }
        return counts;
    }

    private static void giveItems(ServerPlayer sp, Item item, int amount) {
        ItemStack stack = new ItemStack(item, amount);
        if (!sp.getInventory().add(stack)) {
            sp.drop(stack, false);
        }
    }

    private static List<String> unknownTypes(CompoundTag tag) {
        List<String> names = new ArrayList<>();
        ListTag list = tag.getList("nodes", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            String typeStr = list.getCompound(i).getString("type");
            ResourceLocation id = ResourceLocation.tryParse(typeStr);
            if (id == null || SpellRegistry.get(id) == null) {
                String display = id == null ? typeStr : id.getPath();
                if (!names.contains(display)) {
                    names.add(display);
                }
            }
        }
        return names;
    }

    public static void handleManaSync(ManaSyncPacket packet, IPayloadContext ctx) {

        ctx.enqueueWork(() -> WandHud.receiveManaSync(packet));
    }

    public static void sendManaSync(ServerPlayer sp, ItemStack wand) {
        if (sp.connection == null) {
            return;
        }
        sendWandSlotSync(sp, wand);
        PacketDistributor.sendToPlayer(sp, new ManaSyncPacket(
                WandData.getManaDisplay(wand, sp.level()),
                WandData.getMaxMana(wand),
                (int) Math.round(WandData.getRegen(wand) * 100.0),
                WandData.getCooldown(wand),
                WandData.tag(wand).getLong(WandData.KEY_LAST_CAST),
                sp.level().getGameTime()));
    }

    private static final java.util.Map<UUID, LastSync> LAST_WAND_SYNC = new java.util.HashMap<>();

    private record LastSync(long tick, UUID wandId) {
    }

    public static void sendWandSlotSync(ServerPlayer sp, ItemStack wand) {
        if (sp.connection == null) {
            return;
        }
        long now = sp.level().getGameTime();
        UUID wandId = WandData.getWandId(wand);
        LastSync last = LAST_WAND_SYNC.get(sp.getUUID());
        if (last != null && last.tick() == now && last.wandId().equals(wandId)) {
            return;
        }
        LAST_WAND_SYNC.put(sp.getUUID(), new LastSync(now, wandId));
        if (sp.getMainHandItem().getItem() instanceof top.ydog01.mmagic.item.WandItem
                && WandData.getWandId(sp.getMainHandItem()).equals(wandId)) {
            sendHeldItemSync(sp);
            return;
        }
        if (sp.getOffhandItem().getItem() instanceof top.ydog01.mmagic.item.WandItem
                && WandData.getWandId(sp.getOffhandItem()).equals(wandId)) {
            sp.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, 45, sp.getOffhandItem()));
            return;
        }
        java.util.List<ItemStack> items = sp.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            ItemStack s = items.get(i);
            if (s.getItem() instanceof top.ydog01.mmagic.item.WandItem && WandData.getWandId(s).equals(wandId)) {
                sp.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, i, s));
                return;
            }
        }
    }

    public static void sendHeldItemSync(ServerPlayer sp) {
        if (sp.connection == null) {
            return;
        }
        ItemStack held = sp.getMainHandItem();
        sp.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, sp.getInventory().selected, held));
    }

    public static void handleSync(SyncAltarPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof AltarScreen screen) {
                screen.receiveSync(packet);
            }
        });
    }

    public static void sendSync(ServerPlayer sp) {
        if (sp.connection == null) {
            return;
        }
        ItemStack wand = SpellRunner.findWand(sp);

        int crystals = sp.getAbilities().instabuild ? -1 : Crystals.count(sp);
        if (wand.isEmpty()) {

            PacketDistributor.sendToPlayer(sp, new SyncAltarPacket(
                    new WandAttrs(WandData.DEFAULT_MAX_MANA,
                            (int) Math.round(WandData.DEFAULT_REGEN * 100.0),
                            WandData.DEFAULT_COOLDOWN),
                    crystals,
                    false,
                    "",
                    new CompoundTag()
            ));
            return;
        }
        CompoundTag spell = WandData.getGraph(wand).toTag();

        sendWandSlotSync(sp, wand);
        PacketDistributor.sendToPlayer(sp, new SyncAltarPacket(
                new WandAttrs(WandData.getMaxMana(wand),
                        (int) Math.round(WandData.getRegen(wand) * 100.0),
                        WandData.getCooldown(wand)),
                crystals,
                true,
                WandData.getName(wand),
                spell
        ));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
