package net.j40climb.frenchies.test;

import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

final class KennelReload {
    static Runnable watch(ServerLevel level, ChunkPos kennel, UUID dog, Runnable onLoad) {
        Consumer<EntityJoinLevelEvent> listener = event -> {
            var entity = event.getEntity();
            if (event.getLevel() == level && entity.getUUID().equals(dog) && entity.chunkPosition().equals(kennel)
                    && event.loadedFromDisk()) onLoad.run();
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, EntityJoinLevelEvent.class, listener);
        return () -> NeoForge.EVENT_BUS.unregister(listener);
    }
}
