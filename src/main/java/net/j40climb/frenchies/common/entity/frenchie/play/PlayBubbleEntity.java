package net.j40climb.frenchies.common.entity.frenchie.play;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
//? if >=26.3 {
/*import net.minecraft.world.entity.LinearInterpolationHandler;
*///?}
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** A short-lived, server-owned toy bubble. It has no hit, push or persistence behavior. */
public final class PlayBubbleEntity extends Entity {
    private static final EntityDataAccessor<Integer> NOSE_DOG =
            SynchedEntityData.defineId(PlayBubbleEntity.class, EntityDataSerializers.INT);
    //? if <26.3 {
    private final InterpolationHandler interpolation = new InterpolationHandler(this, 2);
    //?}
    private final long expiresAt;
    private int claimant = -1;
    private long landedAt;

    public PlayBubbleEntity(EntityType<? extends PlayBubbleEntity> type, Level level) {
        super(type, level);
        expiresAt = level.getGameTime() + 78 + getRandom().nextInt(23);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(NOSE_DOG, -1);
    }

    public int noseDogId() {
        return entityData.get(NOSE_DOG);
    }

    boolean claim(FrenchieEntity dog) {
        if (claimant != -1 || !isAlive()) return false;
        claimant = dog.getId();
        return true;
    }

    void release(FrenchieEntity dog) {
        if (claimant != dog.getId()) return;
        claimant = -1;
        if (noseDogId() != -1) pop(false);
    }

    void settleOn(FrenchieEntity dog) {
        landedAt = level().getGameTime();
        entityData.set(NOSE_DOG, dog.getId());
        setDeltaMovement(Vec3.ZERO);
        setPos(nosePosition(dog));
    }

    // Match the scale and limited head yaw of the rendered adult and puppy muzzles.
    static Vec3 nosePosition(FrenchieEntity dog) {
        double scale = dog.isBaby() ? 0.5 : 1.0;
        float yaw = dog.yBodyRot + Math.clamp(Mth.wrapDegrees(dog.getYHeadRot() - dog.yBodyRot), -30F, 30F);
        Vec3 forward = Vec3.directionFromRotation(0, yaw);
        return dog.position().add(forward.scale(0.47 * scale)).add(0, 0.48 * scale, 0);
    }

    @Override
    public void tick() {
        super.tick();
        //? if <26.3 {
        if (level().isClientSide()) {
            interpolation.interpolate();
            return;
        }
        //?} else {
        /*if (level().isClientSide()) return;
        *///?}
        if (noseDogId() != -1) {
            if (level().getGameTime() - landedAt > 18 || !(level().getEntity(noseDogId()) instanceof FrenchieEntity dog) || !dog.isAlive()) {
                pop(false);
            } else {
                setPos(nosePosition(dog));
            }
        } else if (level().getGameTime() >= expiresAt) {
            pop(false);
        } else {
            Vec3 velocity = getDeltaMovement();
            // The initial puff spreads into a fan, slows, then settles towards nose height.
            double flutter = Math.sin((tickCount + getId() * 3) * 0.14) * 0.005;
            Vec3 drift = velocity.add(0, flutter, 0);
            if (!level().noBlockCollision(this, getBoundingBox().move(drift)) || isInWater()) {
                pop(false);
            } else {
                setPos(position().add(drift));
                setDeltaMovement(velocity.x * 0.974, Math.max(-0.011, velocity.y - 0.00065), velocity.z * 0.974);
            }
        }
    }

    void pop(boolean touched) {
        if (!(level() instanceof ServerLevel level) || isRemoved()) return;
        level.sendParticles(ParticleTypes.BUBBLE_POP, getX(), getY() + 0.07, getZ(), 3, 0.07, 0.06, 0.07, 0);
        if (touched) level.playSound(null, blockPosition(), SoundEvents.BUBBLE_COLUMN_BUBBLE_POP,
                SoundSource.NEUTRAL, 0.26F, 1.3F);
        discard();
    }

    //? if <26.3 {
    @Override public InterpolationHandler getInterpolation() { return interpolation; }
    //?} else {
    /*@Override
    protected InterpolationHandler createInterpolationHandler() {
        return LinearInterpolationHandler.create(this, 2);
    }
    *///?}
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float damage) { return false; }
    @Override protected void readAdditionalSaveData(ValueInput input) {}
    @Override protected void addAdditionalSaveData(ValueOutput output) {}
}
