package net.j40climb.frenchies.common.entity.frenchie.reaction;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariants;
import net.minecraft.world.entity.monster.Creeper;
//? if <26.3 {
import net.minecraft.world.entity.monster.EnderMan;
//?} else {
/*import net.minecraft.world.entity.monster.Enderman;
*///?}
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

public final class FrenchieAwareness {
    public static final int SECOND_BARK_TICK = 7;
    public static final int WARNING_TICKS = SECOND_BARK_TICK + 5;
    public static final int COOLDOWN_TICKS = 160;
    private static final int WARNING_RADIUS = 12;
    private final FrenchieEntity dog;
    private @Nullable Creeper threat;
    private long lookDownUntil;

    public FrenchieAwareness(FrenchieEntity dog) {
        this.dog = dog;
    }

    public void tick() {
        if (!(dog.level() instanceof ServerLevel level)) return;
        updateEndermanLook(level);
        if (!dog.isTame() || dog.isFrenchieSleeping()) return;
        long age = level.getGameTime() - dog.warningStart();
        if (age == SECOND_BARK_TICK) bark(dog, 1.18F);
        if (age < WARNING_TICKS && threat != null && threat.isAlive()) {
            faceThreat(threat);
            return;
        }
        threat = null;
        if (age < COOLDOWN_TICKS || dog.tickCount % 8 != 0) return;
        threat = findCreeper(level, dog);
        if (threat != null) {
            dog.startWarning(level.getGameTime());
            faceThreat(threat);
            bark(dog, 1.4F);
        }
    }

    private void updateEndermanLook(ServerLevel level) {
        long now = level.getGameTime();
        if (dog.isFrenchieSleeping() || dog.hurtTime > 0 || dog.getTarget() != null
                || dog.isEscapingDanger() || now - dog.warningStart() < WARNING_TICKS) {
            lookDownUntil = now;
        } else if (dog.tickCount % 10 == 0 && !level.getEntitiesOfClass(
                //? if <26.3 {
                EnderMan.class,
                //?} else {
                /*Enderman.class,
                *///?}
                dog.getBoundingBox().inflate(9), mob -> mob.isAlive() && dog.distanceToSqr(mob) <= 81
                        && dog.hasLineOfSight(mob)).isEmpty()) {
            lookDownUntil = now + 26;
        }
        dog.setAvoidingEnderman(now < lookDownUntil);
    }

    private void faceThreat(Creeper creeper) {
        dog.lookAt(creeper, 45, 45);
        dog.setYHeadRot(dog.getYRot());
        dog.setYBodyRot(dog.getYRot());
    }

    public static boolean flashing(long age) {
        return age >= 0 && (age < 5 || (age >= SECOND_BARK_TICK && age < WARNING_TICKS));
    }

    public static @Nullable Creeper findCreeper(ServerLevel level, LivingEntity observer) {
        return level.getEntitiesOfClass(Creeper.class, observer.getBoundingBox().inflate(WARNING_RADIUS),
                creeper -> creeper.isAlive() && observer.distanceToSqr(creeper) <= WARNING_RADIUS * WARNING_RADIUS
                        && observer.hasLineOfSight(creeper)).stream()
                .min(Comparator.comparingDouble(observer::distanceToSqr)).orElse(null);
    }

    public static void bark(LivingEntity observer, float pitch) {
        var bark = observer.registryAccess().lookupOrThrow(Registries.WOLF_SOUND_VARIANT)
                .getOrThrow(WolfSoundVariants.PUGLIN).value().adultSounds().ambientSound().value();
        observer.level().playSound(null, observer.getX(), observer.getY(), observer.getZ(), bark, SoundSource.NEUTRAL, 0.85F, pitch);
    }
}
