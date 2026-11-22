package net.j40climb.frenchies.common.entity.frenchie.frontpack;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import static net.j40climb.frenchies.setup.FrenchieRegistry.FRENCH_FRONTPACK_DATA;
import static net.j40climb.frenchies.setup.FrenchieRegistry.FRENCHIE_WARNING;

public final class FrontpackAttachments {
    public static FrontpackData pack(Entity entity) {
        return entity.getAttachedOrCreate(FRENCH_FRONTPACK_DATA.get());
    }

    public static @Nullable FrontpackData packOrNull(Entity entity) {
        return entity.getAttached(FRENCH_FRONTPACK_DATA.get());
    }

    public static void setPack(Entity entity, FrontpackData data) {
        entity.setAttached(FRENCH_FRONTPACK_DATA.get(), data);
    }

    public static @Nullable FrontpackWarning warningOrNull(Entity entity) {
        return entity.getAttached(FRENCHIE_WARNING.get());
    }

    public static void setWarning(Entity entity, FrontpackWarning warning) {
        entity.setAttached(FRENCHIE_WARNING.get(), warning);
    }

    public static void clearWarning(Entity entity) {
        entity.removeAttached(FRENCHIE_WARNING.get());
    }
}
