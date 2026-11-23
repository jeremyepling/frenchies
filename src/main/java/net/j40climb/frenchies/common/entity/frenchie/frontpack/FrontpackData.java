package net.j40climb.frenchies.common.entity.frenchie.frontpack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.j40climb.frenchies.common.entity.frenchie.CompanionSettings;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieVariant;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

/**
 * Data structure for storing a carried Frenchie on a player.
 * The player attachment persists the dog and synchronizes its appearance.
 *
 * When hasCarriedFrenchie=true, the frenchieNBT contains the full entity state
 * (variant, health, age, pose, etc.) and the Frenchie entity is despawned.
 */
public record FrontpackData(
    boolean hasCarriedFrenchie,
    CompoundTag frenchieNBT,
    long pickupTimestamp,
    boolean releasePending,
    long dozeAt
) {
    public FrontpackData(boolean hasCarriedFrenchie, CompoundTag frenchieNBT, long pickupTimestamp) {
        this(hasCarriedFrenchie, frenchieNBT, pickupTimestamp, false);
    }

    // A carried dog settles again after loading. Only tracking clients need the current nap time.
    public FrontpackData(boolean carried, CompoundTag dog, long pickup, boolean pending) {
        this(carried, dog, pickup, pending, -1L);
    }

    public FrontpackData withDozeAt(long time) {
        return new FrontpackData(hasCarriedFrenchie, frenchieNBT, pickupTimestamp, releasePending, time);
    }

    public FrontpackData withReleasePending(boolean pending) {
        return new FrontpackData(hasCarriedFrenchie, frenchieNBT, pickupTimestamp, pending, dozeAt);
    }

    /**
     * Codec for NBT persistence (disk save/load).
     */
    public static final Codec<FrontpackData> CODEC = RecordCodecBuilder.create(builder ->
        builder.group(
            Codec.BOOL.fieldOf("hasCarriedFrenchie").forGetter(FrontpackData::hasCarriedFrenchie),
            CompoundTag.CODEC.fieldOf("frenchieNBT").forGetter(FrontpackData::frenchieNBT),
            Codec.LONG.fieldOf("pickupTimestamp").forGetter(FrontpackData::pickupTimestamp),
            Codec.BOOL.optionalFieldOf("releasePending", false).forGetter(FrontpackData::releasePending)
        ).apply(builder, FrontpackData::new)
    );

    /**
     * StreamCodec for network synchronization (client-server sync).
     */
    public static final StreamCodec<ByteBuf, FrontpackData> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, FrontpackData::hasCarriedFrenchie,
        ByteBufCodecs.COMPOUND_TAG, FrontpackData::appearanceTag,
        ByteBufCodecs.VAR_LONG, FrontpackData::pickupTimestamp,
        ByteBufCodecs.VAR_LONG, FrontpackData::dozeAt,
        (carried, dog, pickup, dozeAt) -> new FrontpackData(carried, dog, pickup, false, dozeAt)
    );

    public @Nullable FrenchieVariant carriedCoat() {
        return hasCarriedFrenchie ? FrenchieVariant.byId(frenchieNBT.getIntOr("Variant", 0)) : null;
    }

    // Only send the job's presence. The parcel, its address and the death location stay on the server.
    private CompoundTag appearanceTag() {
        CompoundTag appearance = new CompoundTag();
        if (hasCarriedFrenchie) {
            appearance.putInt("Variant", frenchieNBT.getIntOr("Variant", 0));
            if (!CompanionSettings.automaticAdventureOutfits()) appearance.putBoolean("AutomaticAdventureOutfits", false);
            var name = frenchieNBT.get("CustomName");
            if (name != null) appearance.put("CustomName", name.copy());
            if (frenchieNBT.contains("FrenchieParcel")) appearance.putBoolean("FrenchieParcel", true);
            if (frenchieNBT.contains("FrenchieDeathTrail")) appearance.putBoolean("FrenchieDeathTrail", true);
        }
        return appearance;
    }

    /**
     * Default state: not carrying any Frenchie.
     */
    public static final FrontpackData DEFAULT = new FrontpackData(false, new CompoundTag(), 0L);
}
