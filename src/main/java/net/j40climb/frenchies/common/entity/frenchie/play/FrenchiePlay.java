package net.j40climb.frenchies.common.entity.frenchie.play;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.assistance.DiggingAssistantGoal;
import net.j40climb.frenchies.common.entity.frenchie.assistance.HerdLivestockGoal;
import net.j40climb.frenchies.common.entity.frenchie.reaction.DangerReaction;
import net.j40climb.frenchies.common.entity.frenchie.reaction.FrenchieAwareness;
import net.j40climb.frenchies.common.entity.frenchie.weather.WeatherPlayGoal;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** Coordinates play for one Frenchie. Each goal owns its activity and any pending invitation. */
public final class FrenchiePlay {
    private final FrenchieEntity dog;
    private final ToyPlayGoal toy;
    private final TrickGoal trick;
    private final BellyRubGoal bellyRub;
    private final DiggingAssistantGoal digging;
    private final WeatherPlayGoal weather;
    private final BubblePlayGoal bubbles;
    private final SlimeTeaseGoal slime;
    private long nextDangerCheck;
    private boolean dangerNearby;

    public FrenchiePlay(FrenchieEntity dog) {
        this.dog = dog;
        toy = new ToyPlayGoal(dog);
        trick = new TrickGoal(dog);
        bellyRub = new BellyRubGoal(dog);
        digging = new DiggingAssistantGoal(dog);
        weather = new WeatherPlayGoal(dog);
        bubbles = new BubblePlayGoal(dog);
        slime = new SlimeTeaseGoal(dog);
    }

    public void registerGoals(GoalSelector selector) {
        selector.addGoal(4, toy);
        selector.addGoal(4, trick);
        selector.addGoal(4, bellyRub);
        selector.addGoal(5, digging);
        selector.addGoal(5, new HerdLivestockGoal(dog));
        selector.addGoal(6, weather);
        selector.addGoal(6, bubbles);
        selector.addGoal(7, slime);
    }

    public ToyPlayGoal toy() { return toy; }
    public BubblePlayGoal bubbles() { return bubbles; }
    public boolean requestDigging(BlockPos broken) { return digging.request(broken); }
    public void inviteSnowball(Vec3 impact) { weather.inviteSnowball(impact); }

    public void requestTrick() { trick.request(); }
    public void rubBelly(ServerPlayer owner) { bellyRub.rub(owner); }
    public void cancelBellyRub() { bellyRub.command(); }
    public boolean bellyInvitationPending() { return bellyRub.invitationPending(); }

    public void tick() {
        weather.observeWeather();
        bellyRub.observeInvitation();
    }

    public void save(ValueOutput output) {
        output.putLong("NextSlimePlay", slime.cooldown());
        digging.save(output);
        weather.save(output);
        output.putLong("NextTrick", trick.cooldown());
        output.putLong("NextBellyInvitation", bellyRub.cooldown());
    }

    public void load(ValueInput input) {
        slime.loadCooldown(input.getLongOr("NextSlimePlay", 0));
        digging.load(input);
        weather.load(input);
        trick.loadCooldown(input.getLongOr("NextTrick", 0));
        bellyRub.loadCooldown(input.getLongOr("NextBellyInvitation", 0));
    }

    boolean interruptedByOtherActivity() {
        return dog.respondingToWhistle() || dog.dangerReaction() != DangerReaction.NONE
                || dog.isEscapingDanger() || dog.reactions().interruptsPlay() || dog.hasBedtimePlan()
                || dog.level().getGameTime() - dog.warningStart() < FrenchieAwareness.WARNING_TICKS;
    }

    public boolean canPlay() {
        return !weather.active() && !dog.isOrderedToSit() && !dog.isSitting() && !dog.isFrenchieSleeping() && canWatch(false);
    }

    public boolean canWatch(boolean allowWeatherPlay) {
        if (!dog.isAlive() || dog.isNoAi() || interruptedByOtherActivity() || dog.hasCompanionJob() || dog.hasToyJob()
                || !allowWeatherPlay && weather.active()
                || dog.trickStage() != PlayStages.Trick.NONE || dog.bellyStage() != PlayStages.BellyRub.NONE
                || dog.isPassenger() || dog.isLeashed() || dog.isInLove()
                || dog.isInWater() || dog.isInLava() || dog.isOnFire() || dog.isFullyFrozen() || dog.hurtTime > 0 || dog.getTarget() != null
                || dog.delivery().isDialogOpen()) return false;
        long now = dog.level().getGameTime();
        if (now >= nextDangerCheck) {
            dangerNearby = PlaySafety.dangerNearby(dog);
            nextDangerCheck = now + 4;
        }
        return !dangerNearby;
    }

    private boolean stopsOnSleep(Goal goal) {
        return goal == bubbles || goal == digging || goal == weather || goal == trick || goal == bellyRub
                || goal == slime || goal == dog.reactions().quietCompanionship() || goal instanceof HerdLivestockGoal;
    }

    public void fallAsleep(GoalSelector selector) {
        // Vanilla suspends the selector while asleep, so stop these running goals now.
        stopRunning(selector, this::stopsOnSleep);
    }

    public void interrupt(GoalSelector selector) {
        stopRunning(selector, goal -> goal == toy || goal == dog.reactions().ownerConcern() || stopsOnSleep(goal));

        // A thrown toy, invitation or sleeping spectator can be pending outside the selector.
        toy.stop();
        bubbles.stop();
        digging.stop();
        weather.interrupt();
        trick.stop();
        bellyRub.stop();
    }

    private static void stopRunning(GoalSelector selector, Predicate<Goal> shouldStop) {
        for (var goal : selector.getAvailableGoals()) {
            if (goal.isRunning() && shouldStop.test(goal.getGoal())) goal.stop();
        }
    }
}
