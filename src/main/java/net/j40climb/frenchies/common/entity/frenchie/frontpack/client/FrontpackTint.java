package net.j40climb.frenchies.common.entity.frenchie.frontpack.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

enum FrontpackTint implements ItemTintSource {
    INSTANCE;

    static final MapCodec<FrontpackTint> CODEC = MapCodec.unit(INSTANCE);

    static @Nullable Integer dyedFabric(ItemStack stack) {
        var dye = stack.get(DataComponents.DYED_COLOR);
        if (dye == null) return null;
        int color = ARGB.opaque(dye.rgb());
        return ARGB.srgbLerp(0.25F, color, ARGB.greyscale(color));
    }

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        Integer tint = dyedFabric(stack);
        return tint == null ? 0xFF393939 : tint;
    }

    @Override
    public MapCodec<FrontpackTint> type() {
        return CODEC;
    }
}
