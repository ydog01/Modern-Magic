package top.ydog01.mmagic.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SpellFiles {
    public static final String EXT = ".spell";
    private static final String DIR_NAME = "modern_magic_spells";
    private static final int MAX_LEN = 40;

    private SpellFiles() {
    }

    public static Path dir(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(DIR_NAME);
    }

    public static Path file(MinecraftServer server, String name) {
        return dir(server).resolve(name + EXT);
    }

    public static boolean exists(MinecraftServer server, String name) {
        return Files.exists(file(server, name));
    }

    public static String sanitize(String raw) {
        String name = raw == null ? "" : raw.trim();
        StringBuilder sb = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        String s = sb.toString();
        while (s.startsWith(".")) {
            s = s.substring(1);
        }
        if (s.length() > MAX_LEN) {
            s = s.substring(0, MAX_LEN);
        }
        return s;
    }

    public static boolean write(MinecraftServer server, String name, CompoundTag tag) {
        try {
            Files.createDirectories(dir(server));
            Files.writeString(file(server, name), tag.toString(), StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static CompoundTag parse(Path path) {
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            if (TagParser.parseTag(text) instanceof CompoundTag tag) {
                return tag;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
