package net.j40climb.frenchies.common.entity.frenchie.frontpack;

import net.minecraft.util.context.ContextKey;
import net.j40climb.frenchies.Frenchies;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.jspecify.annotations.Nullable;

final class FrontpackRenderData {
    private static final ContextKey<FrontpackAppearance> KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(Frenchies.LEGACY_NAMESPACE, "frontpack_appearance"));

    static @Nullable FrontpackAppearance get(AvatarRenderState state) {
        return state.getRenderData(KEY);
    }

    static void set(AvatarRenderState state, @Nullable FrontpackAppearance appearance) {
        state.setRenderData(KEY, appearance);
    }
}
