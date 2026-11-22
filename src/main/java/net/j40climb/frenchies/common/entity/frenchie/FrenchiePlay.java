package net.j40climb.frenchies.common.entity.frenchie;

import java.util.function.Predicate;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;

/** Coordinates play for one Frenchie. Each goal owns its activity and any pending invitation. */
final class FrenchiePlay {
    private final FrenchieEntity dog;
    final ToyPlayGoal toy;
    final TrickGoal trick;
    final BellyRubGoal bellyRub;
    final CelebrationGoal celebration;
    final DiggingAssistantGoal digging;
    final WeatherPlayGoal weather;
    final BubblePlayGoal bubbles;
    private long nextDangerCheck;
    private boolean dangerNearby;

    FrenchiePlay(FrenchieEntity dog) {
        this.dog = dog;
        toy = new ToyPlayGoal(dog);
        trick = new TrickGoal(dog);
        bellyRub = new BellyRubGoal(dog);
        celebration = new CelebrationGoal(dog);
        digging = new DiggingAssistantGoal(dog);
        weather = new WeatherPlayGoal(dog);
        bubbles = new BubblePlayGoal(dog);
    }

    boolean interruptedByOtherActivity() {
        return dog.respondingToWhistle() || dog.dangerReaction() != DangerReaction.NONE
                || dog.isEscapingDanger() || dog.hasBedtimePlan()
                || dog.level().getGameTime() - dog.warningStart() < FrenchieAwareness.WARNING_TICKS;
    }

    boolean canPlay() {
        return !weather.active() && !dog.isOrderedToSit() && !dog.isSitting() && !dog.isFrenchieSleeping() && canWatch(false);
    }

    boolean canWatch(boolean allowWeatherPlay) {
        if (!dog.isAlive() || dog.isNoAi() || interruptedByOtherActivity() || dog.hasCompanionJob() || dog.hasToyJob()
                || !allowWeatherPlay && weather.active() || celebration.requestedOrRunning()
                || dog.trickStage() != TrickGoal.NONE || dog.bellyStage() != BellyRubGoal.NONE
                || dog.isPassenger() || dog.isLeashed() || dog.isInLove()
                || dog.isInWater() || dog.isInLava() || dog.isOnFire() || dog.isFullyFrozen() || dog.hurtTime > 0 || dog.getTarget() != null
                || dog.deliveryDialog != null && dog.deliveryDialog.isOpen()) return false;
        long now = dog.level().getGameTime();
        if (now >= nextDangerCheck) {
            dangerNearby = PlaySafety.dangerNearby(dog);
            nextDangerCheck = now + 4;
        }
        return !dangerNearby;
    }

    private boolean stopsOnSleep(Goal goal) {
        return goal == bubbles || goal == digging || goal == weather || goal == trick || goal == bellyRub
                || goal instanceof SlimeTeaseGoal;
    }

    void fallAsleep(GoalSelector selector) {
        // Vanilla suspends the selector while asleep, so stop these running goals now.
        stopRunning(selector, this::stopsOnSleep);
    }

    void interrupt(GoalSelector selector) {
        stopRunning(selector, goal -> goal == toy || goal == celebration || stopsOnSleep(goal));

        // A thrown toy, invitation or sleeping spectator can be pending outside the selector.
        toy.stop();
        bubbles.stop();
        celebration.stop();
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
