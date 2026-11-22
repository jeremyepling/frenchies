package net.j40climb.frenchies.common.entity.frenchie.frontpack.client;

import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.jspecify.annotations.Nullable;

final class FrontpackRenderData {
    private static final RenderStateDataKey<FrontpackAppearance> KEY = RenderStateDataKey.create();

    static @Nullable FrontpackAppearance get(AvatarRenderState state) {
        return state.getData(KEY);
    }

    static void set(AvatarRenderState state, @Nullable FrontpackAppearance appearance) {
        state.setData(KEY, appearance);
    }
}
