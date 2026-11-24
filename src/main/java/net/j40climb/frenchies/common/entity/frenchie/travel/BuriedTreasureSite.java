package net.j40climb.frenchies.common.entity.frenchie.travel;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.jetbrains.annotations.Nullable;

record BuriedTreasureSite(BlockPos chest, BlockPos cover) {
    static @Nullable BuriedTreasureSite find(ServerLevel level, BlockPos marker) {
        var pos = ChunkPos.containing(marker);
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x(), pos.z());
        if (chunk == null) return null;

        BlockPos chest = findChest(chunk, marker);
        if (chest == null) return null;
        int surface = Math.max(chest.getY(), chunk.getHeight(Heightmap.Types.OCEAN_FLOOR, chest.getX(), chest.getZ()));
        // Stop at the first open space above the contiguous cover. A detached roof or
        // canopy should not become the digging target for a chest beneath it.
        BlockPos cover = chest;
        int limit = Math.min(surface, chest.getY() + 48);
        for (int y = chest.getY() + 1; y <= limit; y++) {
            BlockPos above = chest.atY(y);
            if (chunk.getBlockState(above).getCollisionShape(level, above).isEmpty()) {
                return new BuriedTreasureSite(chest, cover);
            }
            cover = above;
        }
        return new BuriedTreasureSite(chest, chest.atY(surface));
    }

    private static @Nullable BlockPos findChest(LevelChunk chunk, BlockPos marker) {
        // The vanilla piece moves its one-block bounding box to the chest during generation.
        // That position stays available after a player's first open clears the loot table.
        for (var start : chunk.getAllStarts().values()) {
            if (!start.isValid()) continue;
            for (var piece : start.getPieces()) {
                if (piece.getType() != StructurePieceType.BURIED_TREASURE_PIECE) continue;
                var box = piece.getBoundingBox();
                var pos = new BlockPos(box.minX(), box.minY(), box.minZ());
                if (sameColumn(pos, marker) && chunk.getBlockEntity(pos) instanceof ChestBlockEntity) return pos;
            }
        }

        // Some worlds only retain the unopened loot marker. Consider block entities in the
        // marked column and reject ambiguity without touching any container inventory.
        BlockPos match = null;
        for (BlockPos pos : chunk.getBlockEntitiesPos()) {
            if (!sameColumn(pos, marker) || !(chunk.getBlockEntity(pos) instanceof ChestBlockEntity chest)
                    || !BuiltInLootTables.BURIED_TREASURE.equals(chest.getLootTable())) continue;
            if (match != null) return null;
            match = pos;
        }
        return match;
    }

    private static boolean sameColumn(BlockPos pos, BlockPos marker) {
        return pos.getX() == marker.getX() && pos.getZ() == marker.getZ();
    }
}
