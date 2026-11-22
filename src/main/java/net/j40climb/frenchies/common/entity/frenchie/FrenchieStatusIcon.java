package net.j40climb.frenchies.common.entity.frenchie;

import net.j40climb.frenchies.Frenchies;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public enum FrenchieStatusIcon {
    COMPASS("compass"), PARCEL("parcel"), CREEPER("creeper"), SLEEP("sleep");

    private final AtlasSprite sprite;

    FrenchieStatusIcon(String path) {
        sprite = new AtlasSprite(AtlasIds.GUI, Identifier.fromNamespaceAndPath(Frenchies.LEGACY_NAMESPACE, "frenchie/" + path));
    }

    public static @Nullable FrenchieStatusIcon of(FrenchieEntity dog) {
        if (dog.isWarningFlash()) return CREEPER;
        if (dog.isDeathTracking()) return COMPASS;
        if (dog.isDelivering()) return PARCEL;
        if (dog.isBedtimeBuddy()) return SLEEP;
        return null;
    }

    public Component label(@Nullable Component name) {
        var label = Component.object(sprite);
        if (name != null) label.append(" ").append(name);
        return label;
    }
}
