package net.j40climb.frenchies.common.entity.frenchie.recall;

import com.mojang.logging.LogUtils;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

/** Discover old pets once per dimension. Reads entity saves without spawning entities or loading terrain. */
public final class LegacyCompanionIndex {
    private static final Map<ServerLevel, SimpleRegionStorage> STORAGE = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Pattern REGION = Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca");

    private LegacyCompanionIndex() {}

    public static void register(ServerLevel level, SimpleRegionStorage storage) {
        STORAGE.put(level, storage);
    }

    public static void start(MinecraftServer server) {
        var roster = CompanionRoster.get(server.overworld());
        for (var level : server.getAllLevels()) {
            var storage = STORAGE.remove(level);
            if (storage == null || roster.indexed(level)) continue;
            var folder = DimensionType.getStorageFolder(level.dimension(), server.getWorldPath(LevelResource.ROOT)).resolve("entities");
            roster.scansInProgress++;
            Thread.ofVirtual().name("Frenchie companion index").start(() -> {
                boolean finished = false;
                try {
                    if (Files.isDirectory(folder)) scan(level, folder, storage);
                    finished = server.isRunning();
                } catch (Exception ex) {
                    LogUtils.getLogger().warn("Could not finish indexing Frenchies in {}", folder, ex);
                } finally {
                    boolean complete = finished;
                    if (server.isRunning()) server.execute(() -> roster.finishedIndex(level, complete));
                }
            });
        }
    }

    private static void scan(ServerLevel level, Path folder, SimpleRegionStorage storage) throws IOException {
        var server = level.getServer();
        try (var files = Files.newDirectoryStream(folder, "r.*.*.mca")) {
            for (var file : files) {
                var match = REGION.matcher(file.getFileName().toString());
                if (!match.matches()) continue;
                int startX = Integer.parseInt(match.group(1)) * 32;
                int startZ = Integer.parseInt(match.group(2)) * 32;
                // The Anvil location table has one big-endian offset per chunk in this 32×32 region.
                byte[] table;
                try (var in = new DataInputStream(Files.newInputStream(file))) {
                    table = in.readNBytes(4096);
                }
                var locations = ByteBuffer.wrap(table).asIntBuffer();
                for (int slot = 0; slot < locations.limit(); slot++) {
                    if (!server.isRunning()) return;
                    if (locations.get(slot) == 0) continue;
                    var chunk = new ChunkPos(startX + slot % 32, startZ + slot / 32);
                    var data = storage.read(chunk).join();
                    if (data.isEmpty()) continue;
                    var pos = GlobalPos.of(level.dimension(), chunk.getMiddleBlockPosition(0));
                    data.get().getListOrEmpty("Entities").compoundStream().forEach(entity -> visit(level, pos, entity));
                }
            }
        }
    }

    private static void visit(ServerLevel level, GlobalPos chunk, CompoundTag tag) {
        if ("florafauna:frenchie".equals(tag.getStringOr("id", ""))) {
            var owner = tag.read("Owner", UUIDUtil.CODEC);
            var dog = tag.read("UUID", UUIDUtil.CODEC);
            if (owner.isPresent() && dog.isPresent()) {
                // Only owner and location are imported. The original dog keeps every possession until recalled.
                var name = tag.read("CustomName", ComponentSerialization.CODEC).map(c -> c.getString()).orElse("Frenchie");
                boolean follow = !tag.getBooleanOr("Sitting", false) && !tag.contains("leash")
                        && !tag.contains("FrenchieParcel") && !tag.contains("FrenchieDeathTrail")
                        && !tag.getBooleanOr("FrenchieGoingHome", false) && !tag.contains("FrenchieHandoffReaction");
                level.getServer().execute(() -> CompanionRoster.get(level).indexLegacy(dog.get(), owner.get(), name, chunk, follow));
            }
        }
        tag.getListOrEmpty("Passengers").compoundStream().forEach(passenger -> visit(level, chunk, passenger));
    }
}
