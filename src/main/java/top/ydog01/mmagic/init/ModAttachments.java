package top.ydog01.mmagic.init;

import com.mojang.serialization.Codec;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import top.ydog01.mmagic.ModernMagic;
import top.ydog01.mmagic.quest.QuestProgress;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ModernMagic.MODID);

    public static final Supplier<AttachmentType<QuestProgress>> QUEST_PROGRESS = ATTACHMENT_TYPES.register(
            "quest_progress",
            () -> AttachmentType.builder(() -> new QuestProgress(new java.util.HashMap<>()))
                    .serialize(QuestProgress.CODEC).copyOnDeath().build());

    public static final Supplier<AttachmentType<Boolean>> STARTER_BOOK = ATTACHMENT_TYPES.register(
            "starter_book",
            () -> AttachmentType.builder(() -> Boolean.FALSE).serialize(Codec.BOOL).copyOnDeath().build());

    private ModAttachments() {
    }
}
