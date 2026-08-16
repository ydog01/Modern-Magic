package top.ydog01.mmagic.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record QuestProgress(Map<ResourceLocation, Entry> entries) {
    public record Entry(int progress, boolean claimed) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("progress").forGetter(Entry::progress),
                Codec.BOOL.fieldOf("claimed").forGetter(Entry::claimed)
        ).apply(i, Entry::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Entry::progress,
                ByteBufCodecs.BOOL, Entry::claimed,
                Entry::new
        );
    }

    public static final Codec<QuestProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(ResourceLocation.CODEC, Entry.CODEC)
                    .fieldOf("entries").forGetter(QuestProgress::entries)
    ).apply(i, QuestProgress::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuestProgress> STREAM_CODEC =
            ByteBufCodecs.map(QuestProgress::newMap, ResourceLocation.STREAM_CODEC, Entry.STREAM_CODEC)
                    .map(QuestProgress::new, QuestProgress::entries);

    private static Map<ResourceLocation, Entry> newMap(int size) {
        return new HashMap<>(size);
    }
}
