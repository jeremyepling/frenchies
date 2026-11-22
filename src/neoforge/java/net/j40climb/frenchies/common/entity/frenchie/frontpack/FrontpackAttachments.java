package net.j40climb.frenchies.common.entity.frenchie.frontpack;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import static net.j40climb.frenchies.setup.FrenchieRegistry.FRENCH_FRONTPACK_DATA;
import static net.j40climb.frenchies.setup.FrenchieRegistry.FRENCHIE_WARNING;

public final class FrontpackAttachments {
    public static FrontpackData pack(Entity entity) {
        return entity.getData(FRENCH_FRONTPACK_DATA);
    }

    public static @Nullable FrontpackData packOrNull(Entity entity) {
        return entity.getExistingDataOrNull(FRENCH_FRONTPACK_DATA);
    }

    public static void setPack(Entity entity, FrontpackData data) {
        entity.setData(FRENCH_FRONTPACK_DATA, data);
    }

    public static @Nullable FrontpackWarning warningOrNull(Entity entity) {
        return entity.getExistingDataOrNull(FRENCHIE_WARNING);
    }

    public static void setWarning(Entity entity, FrontpackWarning warning) {
        entity.setData(FRENCHIE_WARNING, warning);
    }

    public static void clearWarning(Entity entity) {
        entity.removeData(FRENCHIE_WARNING);
    }
}
