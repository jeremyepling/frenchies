package net.j40climb.frenchies.test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

final class KennelReload {
    private record Endpoint(ServerLevel level, ChunkPos chunk, UUID dog) {}
    private static final Map<Endpoint, Runnable> listeners = new HashMap<>();

    static {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            var listener = listeners.get(new Endpoint(level, entity.chunkPosition(), entity.getUUID()));
            if (listener != null) listener.run();
        });
    }

    static Runnable watch(ServerLevel level, ChunkPos kennel, UUID dog, Runnable onLoad) {
        // Installed after the kennel has unloaded. A recreated dog at its owner cannot match this endpoint.
        var endpoint = new Endpoint(level, kennel, dog);
        listeners.put(endpoint, onLoad);
        return () -> listeners.remove(endpoint);
    }
}
