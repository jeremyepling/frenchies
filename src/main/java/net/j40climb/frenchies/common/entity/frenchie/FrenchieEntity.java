package net.j40climb.frenchies.common.entity.frenchie;

import net.j40climb.frenchies.common.entity.frenchie.delivery.DeliveryGoal;
import net.j40climb.frenchies.common.entity.frenchie.play.BubblePlayGoal;
import net.j40climb.frenchies.common.entity.frenchie.play.FrenchiePlay;
import net.j40climb.frenchies.common.entity.frenchie.play.PlayPose;
import net.j40climb.frenchies.common.entity.frenchie.play.PlayStages;
import net.j40climb.frenchies.common.entity.frenchie.play.ToyPlayGoal;
import net.j40climb.frenchies.common.entity.frenchie.reaction.CompanionReactions;
import net.j40climb.frenchies.common.entity.frenchie.reaction.DangerReaction;
import net.j40climb.frenchies.common.entity.frenchie.reaction.FrenchieAwareness;
import net.j40climb.frenchies.common.entity.frenchie.recall.CompanionRoster;
import net.j40climb.frenchies.common.entity.frenchie.recall.WhistleApproachGoal;
import net.j40climb.frenchies.common.entity.frenchie.rest.BedtimeBuddyGoal;
import net.j40climb.frenchies.common.entity.frenchie.travel.CaptainControls;
import net.j40climb.frenchies.common.entity.frenchie.travel.DeathTrackingGoal;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.GlobalPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Util;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
//? if neoforge {
import net.neoforged.neoforge.event.EventHooks;
//?}
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class FrenchieEntity extends TamableAnimal {

    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState swimAnimationState = new AnimationState();
    public final AnimationState sleepAnimationState = new AnimationState();
    public final AnimationState standToSitAnimationState = new AnimationState();
    public final AnimationState sitToStandAnimationState = new AnimationState();
    public final AnimationState sitPoseAnimationState = new AnimationState();
    public final AnimationState standToSleepAnimationState = new AnimationState();
    public final AnimationState sleepToStandAnimationState = new AnimationState();
    private static final EntityDataAccessor<Integer> CELEBRATION_STAGE =
            SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DANGER_REACTION =
            SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Long> DANGER_START =
            SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Byte> WEATHER_STAGE =
            SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Long> WEATHER_START =
            SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Byte> SNOW_LAYER =
            SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> TRICK_STAGE =
            SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> TRICK_STAGE_START =
            SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> BELLY_STAGE =
            SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> BELLY_STAGE_START =
            SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.LONG);
    private CompanionReactions reactions;
    private FrenchiePlay play;
    private long nextDangerReaction;
    private long respondingUntil;
    private long ownerAwaySince = -1;

    public void tryCelebrate() {
        if (!level().isClientSide()) play.requestCelebration(false);
    }

    public FrenchiePlay play() {
        return play;
    }

    public int celebrationStage() {
        return entityData.get(CELEBRATION_STAGE);
    }

    public int trickStage() { return entityData.get(TRICK_STAGE); }
    public long trickStageStart() { return entityData.get(TRICK_STAGE_START); }
    public void setTrickStage(int stage) {
        entityData.set(TRICK_STAGE, stage);
        entityData.set(TRICK_STAGE_START, level().getGameTime());
    }
    public int bellyStage() { return entityData.get(BELLY_STAGE); }
    public long bellyStageStart() { return entityData.get(BELLY_STAGE_START); }
    public void setBellyStage(int stage) {
        entityData.set(BELLY_STAGE, stage);
        entityData.set(BELLY_STAGE_START, level().getGameTime());
    }
    public boolean isBellyRubPose() { return bellyStage() != PlayStages.BellyRub.NONE; }

    public void setCelebrationStage(int stage) {
        entityData.set(CELEBRATION_STAGE, stage);
    }

    private void checkReunion() {
        if (!isTame() || tickCount % 20 != 0) return;
        LivingEntity owner = getOwner();
        long now = level().getGameTime();
        if (owner == null || distanceToSqr(owner) >= 32 * 32) {
            if (ownerAwaySince < 0) ownerAwaySince = now;
        } else if (distanceToSqr(owner) <= 7 * 7) {
            if (ownerAwaySince >= 0 && now - ownerAwaySince >= 2400) tryCelebrate();
            ownerAwaySince = -1;
        }
    }

    private float previousSideSleepAmount;
    private float sideSleepAmount;
    private final FrenchieAwareness awareness = new FrenchieAwareness(this);
    private static final EntityDataAccessor<Long> WARNING_START =
            SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.LONG);
    // 0 = ordinary pose, 1 = approaching the bed, 2 = settled beside the owner
    private static final EntityDataAccessor<Byte> BEDTIME =
            SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.BYTE);

    public static final EntityDataAccessor<Long> LAST_POSE_CHANGE_TICK =
            SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.LONG);
    public static final EntityDataAccessor<Long> LAST_SLEEP_CHANGE_TICK =
            SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> VARIANT =
            SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_SLEEPING =
            SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SIDE_SLEEPING =
            SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> JOB =
            SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.BYTE);
    private static final byte TRACKING = 1;
    private static final byte DELIVERING = 2;
    private static final byte GOING_HOME = 3;
    private static final EntityDataAccessor<Optional<EntityReference<LivingEntity>>> WAITING_RECIPIENT =
            SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE);
    private static final EntityDataAccessor<ItemStack> PARCEL = SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> HANDOFF = SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> TOY_EARS_UNTIL = SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Byte> PLAY_POSE = SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DIG_STAGE = SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DIG_STAGE_START = SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> HAS_TOY_STICK = SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.BOOLEAN);

    public PlayPose playPose() {
        return PlayPose.byId(entityData.get(PLAY_POSE));
    }

    public void setPlayPose(PlayPose pose) {
        entityData.set(PLAY_POSE, (byte) pose.ordinal());
    }

    public BubblePlayGoal bubblePlay() {
        return play == null ? null : play.bubbles();
    }

    public boolean canPlay() {
        return play.canPlay();
    }

    public int weatherStage() { return entityData.get(WEATHER_STAGE); }

    public int snowLayer() { return entityData.get(SNOW_LAYER); }

    public void setSnowLayer(int layer) { entityData.set(SNOW_LAYER, (byte) layer); }

    public void setWeatherStage(int stage) {
        entityData.set(WEATHER_STAGE, (byte) stage);
        entityData.set(WEATHER_START, level().getGameTime());
    }

    public float weatherAge(float partialTick) {
        return level().getGameTime() - entityData.get(WEATHER_START) + partialTick;
    }

    public void inviteSnowball(Vec3 at) {
        if (play != null) play.inviteSnowball(at);
    }

    public boolean canParticipateInWeatherPlay() {
        return play.canWatch(true);
    }

    public int handoffPhase() { return entityData.get(HANDOFF); }
    public ItemStack getParcel() { return entityData.get(PARCEL); }
    public void syncParcel(ItemStack parcel) { entityData.set(PARCEL, parcel.copy()); }
    public ItemStack getMouthItem() {
        if (!getParcel().isEmpty()) return getParcel();
        if (!getOffhandItem().isEmpty()) return getOffhandItem();
        return isShowingToyStick() ? new ItemStack(Items.STICK) : ItemStack.EMPTY;
    }

    public boolean isShowingToyStick() {
        return entityData.get(HAS_TOY_STICK) && getParcel().isEmpty() && getOffhandItem().isEmpty()
                && digStage() != PlayStages.Digging.APPROACH && digStage() != PlayStages.Digging.DIG;
    }

    public int digStage() { return entityData.get(DIG_STAGE); }

    public float digStageAge(float partialTick) {
        return level().getGameTime() - entityData.get(DIG_STAGE_START) + partialTick;
    }

    public void setDigStage(int stage) {
        entityData.set(DIG_STAGE, stage);
        entityData.set(DIG_STAGE_START, level().getGameTime());
    }

    public void setHasToyStick(boolean hasStick) { entityData.set(HAS_TOY_STICK, hasStick); }

    public boolean requestDigging(BlockPos broken) {
        return play != null && play.requestDigging(broken);
    }

    public CompanionReactions reactions() {
        return reactions;
    }

    public boolean canRespondToOwner() {
        return isAlive() && !isNoAi() && !isOrderedToSit() && !isPassenger()
                && !isLeashed() && !hasCompanionJob() && !hasToyJob()
                && !respondingToWhistle() && !isEscapingDanger() && !hasBedtimePlan()
                && !isInWater() && !isInLava()
                && dangerReaction() == DangerReaction.NONE && getTarget() == null
                && !isInLove() && !delivery.isDialogOpen();
    }

    public boolean isEscapingDanger() {
        return reactions != null && reactions.isEscapingDanger();
    }

    public boolean hasBedtimePlan() {
        return entityData.get(BEDTIME) != 0;
    }

    private final DeathTrackingGoal trail = new DeathTrackingGoal(this);
    private final DeliveryGoal delivery = new DeliveryGoal(this);

    public FrenchieEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
        if (level instanceof ServerLevel) {
            goalSelector.addGoal(1, trail);
            goalSelector.addGoal(1, delivery);
        }
    }

    @Override
    protected void registerGoals() {
        reactions = new CompanionReactions(this);
        this.goalSelector.addGoal(0, reactions.escapeDanger());
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(0, reactions.lavaAvoidance());
        this.goalSelector.addGoal(1, new WhistleApproachGoal(this));
        this.goalSelector.addGoal(1, new BedtimeBuddyGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(2, reactions.ownerConcern());
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.25, Ingredient.of(Items.GLOW_BERRIES), false) {
            @Override
            public boolean canUse() {
                // A treat still in the thrower's other hand should not strand the toy on the ground.
                return !hasToyJob() && !play.celebrationPending() && super.canUse();
            }
        });
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.25d, 18f, 7f));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.25));
        this.play = new FrenchiePlay(this);
        play.registerGoals(goalSelector);
        this.goalSelector.addGoal(7, reactions.quietCompanionship());
        this.goalSelector.addGoal(8, new RandomStrollGoal(this, 1.0) {
            @Override public boolean canUse() {
                return !play.bellyInvitationPending() && super.canUse();
            }
            @Override public boolean canContinueToUse() {
                return !play.bellyInvitationPending() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 10D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 24D)
                .add(Attributes.TEMPT_RANGE, 10D)
                .add(Attributes.STEP_HEIGHT, 1.0D)
                .add(Attributes.WATER_MOVEMENT_EFFICIENCY, 1.0D);
    }

    @Override
    public boolean isSwimming() {
        return this.isInWater(); // return true if entity is in water
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true; // Frenchie can breathe underwater to prevent drowning
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.GLOW_BERRIES.asItem());
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData spawnData) {
        FrenchieVariant variant = Util.getRandom(FrenchieVariant.values(), this.random);
        this.setVariant(variant);
        return super.finalizeSpawn(level, difficulty, spawnReason, spawnData);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(@NotNull ServerLevel level, AgeableMob otherParent) {
        FrenchieVariant variant = Util.getRandom(FrenchieVariant.values(), this.random);
        FrenchieEntity baby = FrenchieRegistry.FRENCHIE.get().create(level, EntitySpawnReason.BREEDING);
        assert baby != null;
        baby.setVariant(variant);
        return baby;
    }

    /* VARIANT */
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
        builder.define(LAST_POSE_CHANGE_TICK, 0L);
        builder.define(LAST_SLEEP_CHANGE_TICK, 0L);
        builder.define(IS_SLEEPING, false);
        builder.define(SIDE_SLEEPING, false);
        builder.define(WARNING_START, (long)-FrenchieAwareness.COOLDOWN_TICKS);
        builder.define(BEDTIME, (byte)0);
        builder.define(HANDOFF, 0);
        builder.define(PARCEL, ItemStack.EMPTY);
        builder.define(TOY_EARS_UNTIL, 0L);
        builder.define(CELEBRATION_STAGE, PlayStages.Celebration.NONE);
        builder.define(DANGER_REACTION, (byte) 0);
        builder.define(DANGER_START, 0L);
        builder.define(WEATHER_STAGE, (byte) 0);
        builder.define(WEATHER_START, 0L);
        builder.define(SNOW_LAYER, (byte) 0);
        builder.define(PLAY_POSE, (byte) PlayPose.NONE.ordinal());
        builder.define(DIG_STAGE, PlayStages.Digging.NONE);
        builder.define(DIG_STAGE_START, 0L);
        builder.define(HAS_TOY_STICK, false);
        builder.define(TRICK_STAGE, PlayStages.Trick.NONE);
        builder.define(TRICK_STAGE_START, 0L);
        builder.define(BELLY_STAGE, PlayStages.BellyRub.NONE);
        builder.define(BELLY_STAGE_START, 0L);
        builder.define(JOB, (byte) 0);
        builder.define(WAITING_RECIPIENT, Optional.empty());
    }

    public boolean isDelivering() {
        return level().isClientSide() ? entityData.get(JOB) == DELIVERING : delivery.canUse() && !delivery.goingHome();
    }

    public boolean isGoingHome() {
        return level().isClientSide() ? entityData.get(JOB) == GOING_HOME : delivery.goingHome();
    }

    public boolean sendHome(ServerPlayer owner, GlobalPos bed) {
        return delivery.startHome(owner, bed);
    }

    public boolean recallDelivery(ServerPlayer owner) {
        return delivery.recall(owner);
    }

    public boolean isDeathTracking() {
        return level().isClientSide() ? entityData.get(JOB) == TRACKING : trail.active();
    }

    public boolean hasCompanionJob() {
        return isDelivering() || isGoingHome() || isDeathTracking();
    }

    public DeliveryGoal delivery() {
        return delivery;
    }

    public void syncCompanionJob() {
        entityData.set(JOB, isDelivering() ? DELIVERING : isGoingHome() ? GOING_HOME : isDeathTracking() ? TRACKING : (byte) 0);
        entityData.set(HANDOFF, delivery.handoffPhase());
        entityData.set(WAITING_RECIPIENT, Optional.ofNullable(delivery.waitingRecipient()).map(EntityReference::of));
    }

    public boolean isWarningFlash() {
        return !isFrenchieSleeping() && FrenchieAwareness.flashing(level().getGameTime() - entityData.get(WARNING_START));
    }

    public long warningStart() {
        return entityData.get(WARNING_START);
    }

    public void startWarning(long now) {
        entityData.set(WARNING_START, now);
    }

    public boolean isBedtimeBuddy() {
        return entityData.get(BEDTIME) == 2;
    }

    public boolean isFrenchieSleeping() {
        return this.entityData.get(IS_SLEEPING);
    }

    @Override
    public boolean shouldShowName() {
        return super.shouldShowName() || FrenchieStatusIcon.of(this) != null;
    }

    public void setFrenchieSleeping(boolean sleeping) {
        if (sleeping && !isFrenchieSleeping() && !level().isClientSide()) {
            entityData.set(SIDE_SLEEPING, CompanionSettings.sideSleeping() && random.nextBoolean());
        }
        applySleepState(sleeping);
    }

    public boolean usesSideSleepPose() {
        return entityData.get(SIDE_SLEEPING);
    }

    public float sideSleepAmount(float partialTick) {
        return previousSideSleepAmount + partialTick * (sideSleepAmount - previousSideSleepAmount);
    }

    private void applySleepState(boolean sleeping) {
        if (this.entityData.get(IS_SLEEPING) != sleeping) {
            if (!level().isClientSide()) {
                if (sleeping) {
                    play.fallAsleep(goalSelector);
                } else {
                    play.bubbles().wakeUp();
                }
            }
            this.entityData.set(IS_SLEEPING, sleeping);
            this.resetLastSleepChangeTick(this.level().getGameTime());
        }
    }

    private int getTypeVariant() {
        return this.entityData.get(VARIANT);
    }

    public FrenchieVariant getVariant() {
        return FrenchieVariant.byId(this.getTypeVariant());
    }

    private void setVariant(FrenchieVariant variant) {
        this.entityData.set(VARIANT, variant.getId());
    }

    public ToyPlayGoal toyPlay() {
        return play == null ? null : play.toy();
    }

    public boolean hasToyJob() {
        return getOffhandItem().is(FrenchieRegistry.SQUEAKY_TOY.get()) || (play != null && play.toy().hasJob());
    }

    public void perkToyEars() {
        entityData.set(TOY_EARS_UNTIL, level().getGameTime() + 14);
    }

    public float toyEarLift(float partialTick) {
        return Math.clamp((entityData.get(TOY_EARS_UNTIL) - level().getGameTime() - partialTick) / 5f, 0f, 1f);
    }

    private final CaptainControls captain = new CaptainControls();

    @Override
    protected boolean canRide(Entity vehicle) {
        if (isTame() && vehicle instanceof AbstractBoat boat && !CaptainControls.canJoin(this, boat)) return false;
        return super.canRide(vehicle);
    }

    public boolean hasFlotationVest() {
        return getVehicle() instanceof AbstractBoat boat && boat.isInWater();
    }

    public DangerReaction dangerReaction() {
        DangerReaction reaction = DangerReaction.byId(entityData.get(DANGER_REACTION));
        return dangerReactionAge(0) < reaction.duration ? reaction : DangerReaction.NONE;
    }

    public float dangerReactionAge(float partialTick) {
        return level().getGameTime() - entityData.get(DANGER_START) + partialTick;
    }

    public void reactToDanger(DangerReaction reaction) {
        long now = level().getGameTime();
        if (now < nextDangerReaction) return;
        nextDangerReaction = now + 60;
        entityData.set(DANGER_START, now);
        entityData.set(DANGER_REACTION, (byte) reaction.ordinal());
        interruptPlay();
        setFrenchieSleeping(false);
    }

    public void interruptPlay() {
        play.interrupt(goalSelector);
    }

    @Override
    public void setTame(boolean tamed, boolean applySideEffects) {
        super.setTame(tamed, applySideEffects);
        if (tamed) {
            setHealth(getMaxHealth());
            clearFire();
            setTicksFrozen(0);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (!isTame() || source.is(DamageTypes.GENERIC_KILL)) return super.hurtServer(level, source, damage);
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) CompanionRoster.emergency(this);
        else if (source.is(DamageTypeTags.IS_EXPLOSION)) reactToDanger(DangerReaction.DUCK);
        else if (source.is(DamageTypeTags.IS_FALL)) reactToDanger(DangerReaction.TUMBLE);
        else reactToDanger(DangerReaction.STARTLE);
        return false;
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        if (!isTame()) return super.causeFallDamage(distance, multiplier, source);
        if (level() instanceof ServerLevel && calculateFallDamage(distance, multiplier) > 0) {
            reactToDanger(DangerReaction.TUMBLE);
        }
        return false;
    }

    @Override
    public boolean ignoreExplosion(Explosion explosion) {
        if (!isTame()) return super.ignoreExplosion(explosion);
        if (level() instanceof ServerLevel && distanceToSqr(explosion.center()) <= Math.pow(2 * explosion.radius(), 2)) {
            reactToDanger(DangerReaction.DUCK);
        }
        return true;
    }

    @Override
    public void setRemainingFireTicks(int ticks) {
        super.setRemainingFireTicks(isTame() ? Math.min(ticks, 0) : ticks);
    }

    @Override
    public boolean canFreeze() {
        return !isTame() && super.canFreeze();
    }

    @Override
    public boolean showVehicleHealth() {
        return !isTame() && super.showVehicleHealth();
    }

    @Override
    public boolean isInvulnerable() {
        // Vanilla combat predicates and the Warden's anger sensor both consult this flag.
        return isTame() || super.isInvulnerable();
    }

    @Override
    public boolean canBeHitByProjectile() {
        return !isTame() && super.canBeHitByProjectile();
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        return !isTame() && super.doHurtTarget(level, target);
    }

    public static boolean isProtectedMobTarget(@Nullable Object target) {
        return target instanceof FrenchieEntity dog && dog.isTame();
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !isTame() && super.canAttack(target);
    }

    @Override
    //? if <26.3 {
    public void knockback(double power, double x, double z) {
        if (!isTame()) super.knockback(power, x, z);
    //?} else {
    /*public void knockback(double power, double x, double z, DamageSource source, float damage, boolean comesFromEffect) {
        if (!isTame()) super.knockback(power, x, z, source, damage, comesFromEffect);
    *///?}
    }

    @Override
    public boolean isPushable() {
        return !isTame() && super.isPushable();
    }

    @Override
    protected void pushEntities() {
        if (!isTame()) super.pushEntities();
    }

    /* Interact */

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (hand == InteractionHand.MAIN_HAND && player.getMainHandItem().isEmpty()
                && player.getOffhandItem().is(FrenchieRegistry.FRENCHIE_WHISTLE.get())) return InteractionResult.PASS;
        if (player.getItemInHand(hand).is(FrenchieRegistry.FRENCHIE_WHISTLE.get())) {
            return FrenchieRegistry.FRENCHIE_WHISTLE.get().use(level(), player, hand);
        }
        // Accept before vanilla mob interactions can consume a held name tag, lead or other item.
        if (isAlive() && entityData.get(WAITING_RECIPIENT).filter(ref -> ref.matches(player)).isPresent()) {
            if (player instanceof ServerPlayer recipient) delivery.accept(recipient);
            return InteractionResult.SUCCESS;
        }
        if (handoffPhase() == 2) {
            // The signed bundle may now occupy the main hand. Check for a tip before it handles use.
            InteractionHand treatHand = isFood(player.getItemInHand(hand)) ? hand : InteractionHand.OFF_HAND;
            if (isFood(player.getItemInHand(treatHand))
                    && (level().isClientSide() || player instanceof ServerPlayer recipient && delivery.tip(recipient, treatHand))) {
                return InteractionResult.SUCCESS;
            }
        }
        return super.interact(player, hand, location);
    }

    @Override
    public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        if (isTame() && isOwnedBy(pPlayer) && pHand == InteractionHand.MAIN_HAND
                && pPlayer.isShiftKeyDown() && itemstack.is(Items.BONE)) {
            if (!level().isClientSide()) play.requestTrick();
            return InteractionResult.SUCCESS;
        }
        if (isTame() && isOwnedBy(pPlayer) && pHand == InteractionHand.MAIN_HAND
                && isBellyRubPose() && itemstack.isEmpty() && !pPlayer.isShiftKeyDown()) {
            if (!level().isClientSide() && pPlayer instanceof ServerPlayer owner) play.rubBelly(owner);
            return InteractionResult.SUCCESS;
        }
        if (isBellyRubPose() && isOwnedBy(pPlayer) && (pPlayer.isShiftKeyDown() || !itemstack.isEmpty())) {
            if (!level().isClientSide()) play.cancelBellyRub();
        }
        if (!pPlayer.isShiftKeyDown() && itemstack.isEmpty()
                && isOwnedBy(pPlayer) && (getOffhandItem().is(FrenchieRegistry.SQUEAKY_TOY.get()) || getOffhandItem().is(Items.STICK))) {
            if (!level().isClientSide()) {
                ItemStack fetched = getOffhandItem();
                setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                pPlayer.setItemInHand(pHand, fetched);
                if (fetched.is(FrenchieRegistry.SQUEAKY_TOY.get())) tryCelebrate();
            }
            return InteractionResult.SUCCESS;
        }
        if (itemstack.is(FrenchieRegistry.SQUEAKY_TOY.get()) || itemstack.is(FrenchieRegistry.BUBBLE_TOY.get())) {
            return itemstack.getItem().use(level(), pPlayer, pHand);
        }
        if (isPassenger()) return InteractionResult.SUCCESS;
        Item item = itemstack.getItem();
        Item itemForTaming = Items.BONE;
        if(item == itemForTaming && !isTame()) {
            if(this.level().isClientSide()) {
                return InteractionResult.CONSUME;
            } else {
                if (!pPlayer.getAbilities().instabuild) {
                    itemstack.shrink(1);
                }
                //? if neoforge {
                if (EventHooks.onAnimalTame(this, pPlayer)) return InteractionResult.SUCCESS;
                //?}
                super.tame(pPlayer);
                this.navigation.recomputePath();
                this.setTarget(null);
                this.level().broadcastEntityEvent(this, (byte)7);
                toggleSitting();
                if (pPlayer instanceof ServerPlayer owner) CompanionRoster.tamed(owner, this);
                return InteractionResult.SUCCESS;
            }
        }
        if (isTame() && isOwnedBy(pPlayer) && !isFood(itemstack)) {
            boolean compass = itemstack.is(Items.COMPASS) && !itemstack.has(DataComponents.LODESTONE_TRACKER);
            boolean bundle = item instanceof BundleItem;
            boolean empty = itemstack.isEmpty();
            if (pHand == InteractionHand.MAIN_HAND && empty && !hasCompanionJob() && !isBellyRubPose()) {
                ItemStack offhand = pPlayer.getOffhandItem();
                if (offhand.getItem() instanceof BundleItem || offhand.is(Items.COMPASS) || offhand.is(FrenchieRegistry.SQUEAKY_TOY.get())
                        || offhand.is(FrenchieRegistry.BUBBLE_TOY.get()) || offhand.is(FrenchieRegistry.FRENCHIE_WHISTLE.get()) || isFood(offhand)) {
                    return InteractionResult.PASS;
                }
            }
            if (pHand != InteractionHand.MAIN_HAND && !compass && !bundle && !(empty && hasCompanionJob())) {
                return super.mobInteract(pPlayer, pHand);
            }
            if (level().isClientSide()) return InteractionResult.SUCCESS;
            delivery.closeDialog();
            if (delivery.hasParcel() && !delivery.canUse() && pPlayer instanceof ServerPlayer owner) {
                if (empty) delivery.collect(owner, pHand);
                else owner.sendOverlayMessage(Component.translatable("frenchie.florafauna.whistle.collect", getName()));
            } else if (hasCompanionJob()) {
                if (empty && pPlayer instanceof ServerPlayer owner) {
                    if (delivery.canUse()) delivery.collect(owner, pHand);
                    else {
                        trail.cancel();
                        sitDown();
                        orderMessage(owner, "cancel");
                    }
                    syncCompanionJob();
                } else {
                    orderMessage(pPlayer, "busy");
                }
            } else if (compass && pPlayer instanceof ServerPlayer owner) {
                trail.startTracking(owner);
                syncCompanionJob();
            } else if (bundle && hasToyJob()) {
                orderMessage(pPlayer, "busy");
            } else if (bundle && pPlayer instanceof ServerPlayer owner) {
                delivery.openDialog(owner, pHand);
            } else {
                toggleSitting();
                orderMessage(pPlayer, isOrderedToSit() ? "sit" : "follow");
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(pPlayer, pHand);
    }

    public void cancelForWhistle() {
        delivery.closeDialog();
        delivery.cancelForCompanion();
        trail.cancel();
        interruptPlay();
        respondingUntil = level().getGameTime() + 60;
        standUp();
        setFrenchieSleeping(false);
        syncCompanionJob();
    }

    public boolean respondingToWhistle() {
        return level().getGameTime() < respondingUntil;
    }

    private void orderMessage(Player owner, String key) {
        owner.sendOverlayMessage(Component.translatable("frenchie.florafauna.order." + key, getName()));
    }
    /* SITTING */
    public boolean isSitting() {
        return this.getPose() == Pose.SITTING;
    }

    public void toggleSitting() {
        if (this.isSitting()) {
            standUp();
        } else {
            sitDown();
        }
    }
    public void sitDown() {
        if (!this.isSitting()) {
            //this.makeSound(SoundEvents.CAMEL_SIT);
            this.setPose(Pose.SITTING);
            this.gameEvent(GameEvent.ENTITY_ACTION);
            this.resetLastPoseChangeTick(this.level().getGameTime());
        }
        setOrderedToSit(true);
        setInSittingPose(true);
    }
    public void standUp() {
        if (this.isSitting()) {
            //this.makeSound(SoundEvents.CAMEL_STAND);
            this.setPose(Pose.STANDING);
            this.gameEvent(GameEvent.ENTITY_ACTION);
            this.resetLastPoseChangeTick(this.level().getGameTime());
        }
        setOrderedToSit(false);
        setInSittingPose(false);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("HasToyStick", entityData.get(HAS_TOY_STICK));
        play.save(output);
        if (nextDangerReaction > level().getGameTime()) output.putLong("NextDangerReaction", nextDangerReaction);
        output.putLong("OwnerAwaySince", ownerAwaySince);
        output.putInt("Variant", this.getTypeVariant());
        output.putString("Pose", this.getPose().name());
        output.putBoolean("IsSleeping", this.isFrenchieSleeping());
        if (isFrenchieSleeping() && usesSideSleepPose()) output.putBoolean("SideSleeping", true);
        trail.save(output);
        delivery.save(output);
        output.putLong("LastPoseChangeTick", this.entityData.get(LAST_POSE_CHANGE_TICK));
        output.putLong("LastSleepChangeTick", this.entityData.get(LAST_SLEEP_CHANGE_TICK));
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(HAS_TOY_STICK, input.getBooleanOr("HasToyStick", false));
        setDigStage(PlayStages.Digging.NONE);
        play.load(input);
        nextDangerReaction = input.getLongOr("NextDangerReaction", 0L);
        ownerAwaySince = input.getLongOr("OwnerAwaySince", -1L);
        trail.load(input);
        delivery.load(input);
        if (delivery.canUse()) trail.cancel();
        syncCompanionJob();
        this.setVariant(FrenchieVariant.byId(input.getIntOr("Variant", 0)));
        // Existing worlds store the uppercase enum name. Preserve all recognized poses.
        Pose savedPose = this.isOrderedToSit() ? Pose.SITTING : Pose.STANDING;
        String poseName = input.getStringOr("Pose", savedPose.name());
        for (Pose pose : Pose.values()) {
            if (pose.name().equalsIgnoreCase(poseName)) {
                savedPose = pose;
                break;
            }
        }
        this.setPose(savedPose);
        boolean sleeping = input.getBooleanOr("IsSleeping", false);
        entityData.set(SIDE_SLEEPING, sleeping && CompanionSettings.sideSleeping() && input.getBooleanOr("SideSleeping", false));
        applySleepState(sleeping);
        long lastPoseChangeTick = input.getLongOr("LastPoseChangeTick", 0L);
        this.entityData.set(LAST_POSE_CHANGE_TICK, lastPoseChangeTick);
        long lastSleepChangeTick = input.getLongOr("LastSleepChangeTick", 0L);
        this.entityData.set(LAST_SLEEP_CHANGE_TICK, lastSleepChangeTick);
    }

    /* Animation */

    @Override
    public void tick() {
        if (isTame() && level() instanceof ServerLevel level) {
            if (CompanionRoster.finishPendingDismissal(this, level)) return;
            reactions.checkFooting(level);
            if (isRemoved()) return;
        }
        super.tick();
        if (isRemoved()) return;

        if(this.level().isClientSide()) {
            idleAnimationState.animateWhen(onGround() && getPose() == Pose.STANDING && !isInWater()
                    && !isPassenger() && !isFrenchieSleeping() && getSleepTime() >= 20 && getPoseTime() >= 10
                    && walkAnimation.speed() < 0.05F && playPose() == PlayPose.NONE, tickCount);

            // Swim animation (simple boolean state)
            this.swimAnimationState.animateWhen(this.isInWater(), this.tickCount);

            // Sitting transitions and pose
            handleSittingAnimations();

            // Sleeping transitions and pose
            handleSleepingAnimations();
        }
        if (!this.level().isClientSide()) {
            play.tick();
            reactions.observeOwner();
            captain.tick(this);
            if (isTame() && !isRemoved()) CompanionRoster.track(this);
            checkReunion();
            updateSleepState();
            syncCompanionJob();
            awareness.tick();
            play.bubbles().tickSleepySpectator();
        }
    }

    private void handleSittingAnimations() {
        if (hasBedtimePlan()) {
            standToSitAnimationState.stop();
            sitPoseAnimationState.stop();
            sitToStandAnimationState.stop();
            return;
        }
        boolean sitting = this.isSitting();
        long poseTime = this.getPoseTime();

        if (sitting) {
            // Transitioning from standing to sitting
            if (poseTime < 10L) { // 0.5s = 10 ticks
                this.standToSitAnimationState.startIfStopped(this.tickCount);
                this.sitToStandAnimationState.stop();
                this.sitPoseAnimationState.stop();
            } else {
                // Sitting pose (looping)
                this.standToSitAnimationState.stop();
                this.sitToStandAnimationState.stop();
                this.sitPoseAnimationState.startIfStopped(this.tickCount);
            }
        } else {
            // Transitioning from sitting to standing
            if (poseTime < 10L) { // 0.5s = 10 ticks
                this.sitToStandAnimationState.startIfStopped(this.tickCount);
                this.standToSitAnimationState.stop();
                this.sitPoseAnimationState.stop();
            } else {
                // Standing - all sitting animations off
                this.standToSitAnimationState.stop();
                this.sitToStandAnimationState.stop();
                this.sitPoseAnimationState.stop();
            }
        }
    }

    private void handleSleepingAnimations() {
        boolean sleeping = this.isFrenchieSleeping();
        long sleepTime = this.getSleepTime();
        boolean onSide = sleeping && usesSideSleepPose();
        if (tickCount == 1 && onSide && sleepTime >= 20) sideSleepAmount = 1;
        previousSideSleepAmount = sideSleepAmount;
        sideSleepAmount = onSide ? Math.min(1, sideSleepAmount + 0.05F) : Math.max(0, sideSleepAmount - 0.05F);

        if (sleeping) {
            // Transitioning from standing to sleeping
            if (sleepTime < 20L) { // 1.0s = 20 ticks
                this.standToSleepAnimationState.startIfStopped(this.tickCount);
                this.sleepToStandAnimationState.stop();
                this.sleepAnimationState.stop();
            } else {
                // Sleeping pose (looping)
                this.standToSleepAnimationState.stop();
                this.sleepToStandAnimationState.stop();
                this.sleepAnimationState.startIfStopped(this.tickCount);
            }
        } else {
            // Transitioning from sleeping to standing
            if (sleepTime < 20L) { // 1.0s = 20 ticks
                this.sleepToStandAnimationState.startIfStopped(this.tickCount);
                this.standToSleepAnimationState.stop();
                this.sleepAnimationState.stop();
            } else {
                // Awake - all sleeping animations off
                this.standToSleepAnimationState.stop();
                this.sleepToStandAnimationState.stop();
                this.sleepAnimationState.stop();
            }
        }
    }

    public void resetLastPoseChangeTick(long pLastPoseChangeTick) {
        this.entityData.set(LAST_POSE_CHANGE_TICK, pLastPoseChangeTick);
    }

    public long getPoseTime() {
        return this.level().getGameTime() - this.entityData.get(LAST_POSE_CHANGE_TICK);
    }

    public void resetLastSleepChangeTick(long pLastSleepChangeTick) {
        this.entityData.set(LAST_SLEEP_CHANGE_TICK, pLastSleepChangeTick);
    }

    public long getSleepTime() {
        return this.level().getGameTime() - this.entityData.get(LAST_SLEEP_CHANGE_TICK);
    }


    /* Sleeping */

    private void updateSleepState() {
        if (respondingToWhistle() || isEscapingDanger() || reactions.keepsAwake()
                || dangerReaction() != DangerReaction.NONE) {
            entityData.set(BEDTIME, (byte) 0);
            setFrenchieSleeping(false);
            return;
        }
        var bed = BedtimeBuddyGoal.ownerBed(this);
        boolean settled = bed != null && BedtimeBuddyGoal.besideBed(this, bed);
        if (settled && !isBedtimeBuddy() && getOwner() instanceof ServerPlayer owner) {
            owner.sendOverlayMessage(Component.translatable("frenchie.florafauna.bedtime.settled", getName()));
        }
        entityData.set(BEDTIME, (byte)(settled ? 2 : bed == null ? 0 : 1));
        if (settled) getNavigation().stop();
        setFrenchieSleeping(settled || (bed == null && !isPassenger() && !hasToyJob() && !play.celebrationPending()
                && trickStage() == PlayStages.Trick.NONE && bellyStage() == PlayStages.BellyRub.NONE
                && !hasCompanionJob() && !isOrderedToSit() && !isInWater() && isPlayerSleepTime()));
    }

    private boolean isPlayerSleepTime() {
        return this.level().environmentAttributes()
                .getValue(EnvironmentAttributes.BED_RULE, this.blockPosition())
                .canSleep(this.level());
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || this.isFrenchieSleeping();
    }

    @Override
    protected void dropEquipment(ServerLevel level) {
        super.dropEquipment(level);
        delivery.dropOnDeath(level);
    }

    /*
    Sounds
     */

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.PANDA_AMBIENT;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.FROG_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return SoundEvents.BAT_DEATH;
    }
}
