package net.j40climb.frenchies.common.entity.frenchie.reaction;

import net.j40climb.frenchies.common.entity.frenchie.CompanionSettings;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;

public final class CompanionEvents {
    private CompanionEvents() {}

    public static void thunder(LightningBolt bolt) {
        if (!CompanionSettings.weatherReactions() || !CompanionSettings.thunderFear()
                || bolt.isSilent() || !(bolt.level() instanceof ServerLevel level)) return;
        for (var dog : level.getEntitiesOfClass(FrenchieEntity.class, bolt.getBoundingBox().inflate(48),
                dog -> dog.isTame() && dog.distanceToSqr(bolt) <= 48 * 48)) dog.reactions().concern.hearThunder();
    }

    public static void afterDamage(LivingEntity victim, DamageSource source, float amount) {
        if (!(victim instanceof ServerPlayer owner) || !CompanionSettings.fallReactions()
                || amount < 4 || !source.is(DamageTypeTags.IS_FALL) || !owner.isAlive()) return;
        for (var dog : owner.level().getEntitiesOfClass(FrenchieEntity.class, owner.getBoundingBox().inflate(24),
                dog -> dog.isOwnedBy(owner) && dog.distanceToSqr(owner) <= 24 * 24)) dog.reactions().concern.ownerFell();
    }
}
