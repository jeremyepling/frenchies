package net.j40climb.frenchies.common.entity.frenchie.travel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.jetbrains.annotations.Nullable;

record ExplorerMap(MapId id, GlobalPos marker, boolean treasure) {
    static final String SAVE_KEY = "FrenchieExplorerMap";
    static final Codec<ExplorerMap> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MapId.CODEC.fieldOf("id").forGetter(ExplorerMap::id),
            GlobalPos.CODEC.fieldOf("marker").forGetter(ExplorerMap::marker),
            Codec.BOOL.fieldOf("treasure").forGetter(ExplorerMap::treasure)
    ).apply(instance, ExplorerMap::new));

    // Registry IDs are shared across the targets, including the extra explorer trades in 26.3.
    private static final Set<String> STRUCTURES = Set.of("mansion", "monument", "village_desert", "village_plains",
            "village_savanna", "village_snowy", "village_taiga", "jungle_temple", "swamp_hut", "trial_chambers",
            "abandoned_camp", "ancient_city", "desert_pyramid", "mineshaft", "ocean_ruin_warm");

    static @Nullable ExplorerMap read(ServerLevel level, ItemStack stack) {
        MapId id = stack.get(DataComponents.MAP_ID);
        if (id == null) return null;
        var data = MapItem.getSavedData(id, level);
        if (data == null) return null;

        var decorations = stack.getOrDefault(DataComponents.MAP_DECORATIONS, MapDecorations.EMPTY).decorations();
        // Vanilla loot and trades write the target under "+". Its coordinates are full precision,
        // unlike the scaled, transient decorations in the saved map's client display.
        var target = decorations.get("+");
        if (!isStructure(target)) {
            target = null;
            for (var decoration : decorations.values()) {
                if (!isStructure(decoration)) continue;
                if (target != null && !target.equals(decoration)) return null;
                target = decoration;
            }
        }
        if (target == null || !Double.isFinite(target.x()) || !Double.isFinite(target.z())) return null;
        int radius = 64 << data.scale;
        if (Math.abs(target.x() - data.centerX) > radius || Math.abs(target.z() - data.centerZ) > radius) return null;
        BlockPos marker = BlockPos.containing(target.x(), 0, target.z());
        return new ExplorerMap(id, GlobalPos.of(data.dimension, marker), target.type().equals(MapDecorationTypes.RED_X));
    }

    private static boolean isStructure(@Nullable MapDecorations.Entry entry) {
        if (entry == null) return false;
        if (entry.type().equals(MapDecorationTypes.RED_X)) return true;
        return entry.type().unwrapKey().map(key -> {
            Identifier id = key.identifier();
            return id.getNamespace().equals("minecraft") && STRUCTURES.contains(id.getPath());
        }).orElse(false);
    }
}
