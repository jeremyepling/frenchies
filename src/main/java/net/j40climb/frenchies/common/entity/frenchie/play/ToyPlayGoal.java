package net.j40climb.frenchies.common.entity.frenchie.play;

import net.j40climb.frenchies.common.entity.frenchie.CompanionSettings;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;

import java.util.EnumSet;
import java.util.UUID;
import net.j40climb.frenchies.common.item.SqueakyToyItem;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** A loose toy stays a normal pickup until a dog actually reaches it. Only the equipped stack is durable. */
public class ToyPlayGoal extends Goal {
    private final FrenchieEntity dog;
    private final TreeStickSearch treeSticks;
    private ItemEntity thrown;
    private boolean fetchingStick;
    private long returnStickUntil;
    private long offerStickAgain;
    private long fetchUntil;
    private Entity soundSource;
    private long watchUntil;
    private long reactAgainAt;
    private long lastPlayerSqueak;
    private UUID lastPlayer;
    private boolean audience;
    private boolean squeaked;
    private int pathTicks;

    public ToyPlayGoal(FrenchieEntity dog) {
        this.dog = dog;
        treeSticks = new TreeStickSearch(dog);
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    boolean carrying() {
        return dog.getOffhandItem().is(FrenchieRegistry.SQUEAKY_TOY.get());
    }

    public boolean hasJob() {
        // Significant reactions and commands may preempt a spontaneous stick fetch.
        return carrying() || (!fetchingStick && liveThrow());
    }

    private boolean liveThrow() {
        return thrown != null && thrown.isAlive()
                && thrown.getItem().is(fetchingStick ? Items.STICK : FrenchieRegistry.SQUEAKY_TOY.get())
                && dog.level().getGameTime() < fetchUntil;
    }

    private boolean available() {
        return !dog.play().interruptedByOtherActivity() && dog.isAlive() && !dog.hasCompanionJob() && !dog.isOrderedToSit() && !dog.isPassenger()
                && !dog.isFrenchieSleeping() && !dog.isInLove() && !dog.isOnFire() && dog.hurtTime == 0
                && dog.getTarget() == null && !dog.isInWater();
    }

    public boolean fetch(ItemEntity toy) {
        if (!available() || (liveThrow() && !fetchingStick) || !dog.getOffhandItem().isEmpty()) return false;
        treeSticks.clear();
        beginFetch(toy, false);
        return true;
    }

    boolean noticeChoppedLog(BlockPos log) {
        return treeSticks.notice(log);
    }

    private void beginFetch(ItemEntity toy, boolean stick) {
        fetchingStick = stick;
        thrown = toy;
        fetchUntil = dog.level().getGameTime() + 240;
        clearAudience();
        pathTicks = 0;
        squeaked = false;
    }

    public boolean watching(Entity source) {
        return audience && soundSource == source && dog.level().getGameTime() < watchUntil;
    }

    /** Returns true only when filling a new audience place. Dog squeaks can turn heads, never recruit. */
    public boolean hear(Entity source, boolean space) {
        long now = dog.level().getGameTime();
        if (!available() || carrying() || liveThrow() || now < reactAgainAt) return false;
        boolean alreadyWatching = watching(source);
        if (audience && now < watchUntil && !alreadyWatching) return false;
        boolean recruit = source instanceof Player && space && source.getUUID().equals(lastPlayer)
                && now - lastPlayerSqueak <= 60 && (!dog.isTame() || dog.isOwnedBy((Player) source));
        if (source instanceof Player) {
            lastPlayer = source.getUUID();
            lastPlayerSqueak = now;
        }
        reactAgainAt = now + 16;
        soundSource = source;
        audience = alreadyWatching || recruit;
        watchUntil = now + (audience ? 70 : 13);
        dog.perkToyEars();
        return audience && !alreadyWatching;
    }

    private void clearAudience() {
        soundSource = null;
        audience = false;
    }

    @Override
    public boolean canUse() {
        if (!available()) {
            thrown = null;
            clearAudience();
            return false;
        }
        if (!liveThrow()) thrown = null;
        if (thrown == null && !carrying()) {
            ItemEntity stick = treeSticks.find();
            if (stick != null) beginFetch(stick, true);
        }
        return thrown != null || ((carrying() || returningStick()) && dog.getOwner() instanceof Player p && p.isAlive() && !p.isSpectator()
                && dog.distanceToSqr(p) < 24 * 24)
                || (soundSource != null && soundSource.isAlive() && !soundSource.isRemoved()
                && dog.distanceToSqr(soundSource) <= 12 * 12 && dog.level().getGameTime() < watchUntil);
    }

    private boolean returningStick() {
        if (!dog.getOffhandItem().is(Items.STICK) || !CompanionSettings.stickRetrieval() || !dog.canPlay()) return false;
        long now = dog.level().getGameTime();
        if (now < offerStickAgain) return false;
        if (returnStickUntil == 0) returnStickUntil = now + 240;
        if (now < returnStickUntil) return true;
        restFromStickReturn(now);
        return false;
    }

    private void restFromStickReturn(long now) {
        returnStickUntil = 0;
        offerStickAgain = now + 600;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (thrown != null) {
            if (fetchingStick && !treeSticks.availableStick(thrown)) {
                stop();
                return;
            }
            if (dog.distanceToSqr(thrown) < 2.25 && thrown.onGround() && dog.hasLineOfSight(thrown)
                    && dog.getOffhandItem().isEmpty()) {
                ItemStack stack = thrown.getItem();
                dog.setItemSlot(EquipmentSlot.OFFHAND, stack.split(1));
                dog.setGuaranteedDrop(EquipmentSlot.OFFHAND);
                if (stack.isEmpty()) thrown.discard();
                else thrown.setItem(stack.copy());
                if (fetchingStick) {
                    treeSticks.clear();
                    returnStickUntil = dog.level().getGameTime() + 240;
                    offerStickAgain = 0;
                }
                thrown = null;
                pathTicks = 0;
            } else {
                approach(thrown);
            }
        } else if ((carrying() || returningStick()) && dog.getOwner() instanceof Player owner) {
            if (dog.distanceToSqr(owner) > 3.2) approach(owner);
            else dog.getNavigation().stop();
            dog.getLookControl().setLookAt(owner, 20, 20);
            if (dog.getOffhandItem().is(Items.STICK)) {
                if (dog.distanceToSqr(owner) <= 3.2 && dog.hasLineOfSight(owner)) offerStick(owner);
            } else if (!squeaked && dog.getRandom().nextInt(120) == 0) {
                squeaked = true;
                SqueakyToyItem.squeak((ServerLevel) dog.level(), dog);
            }
        } else if (soundSource != null) {
            dog.getLookControl().setLookAt(soundSource, 25, 22);
            if (audience && dog.distanceToSqr(soundSource) > 14 && pathTicks-- <= 0) {
                // Stop outside the player's space and spread dogs around the source.
                double angle = dog.getUUID().hashCode() * 2.399963;
                Vec3 spot = soundSource.position().add(Math.cos(angle) * 2.8, 0, Math.sin(angle) * 2.8);
                dog.getNavigation().moveTo(spot.x, spot.y, spot.z, 1.0);
                pathTicks = 12;
            } else if (dog.distanceToSqr(soundSource) <= 14) {
                dog.getNavigation().stop();
            }
        }
    }

    private void offerStick(Player owner) {
        var inventory = owner.getInventory();
        ItemStack held = dog.getOffhandItem();
        ItemStack offered = held.copyWithCount(1);
        // Creative Inventory.add may consume overflow. Require a real destination on both game modes.
        if ((inventory.getSlotWithRemainingSpace(offered) >= 0 || inventory.getFreeSlot() >= 0)
                && inventory.add(offered) && offered.isEmpty()) {
            dog.setItemSlot(EquipmentSlot.OFFHAND, held.copyWithCount(held.getCount() - 1));
        }
        restFromStickReturn(dog.level().getGameTime());
    }

    private void approach(Entity target) {
        dog.getLookControl().setLookAt(target, 30, 25);
        if (pathTicks-- <= 0) {
            dog.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), 1.15);
            pathTicks = 7;
        }
    }

    @Override
    public void stop() {
        dog.getNavigation().stop();
        clearAudience();
        treeSticks.clear();
        thrown = null;
        fetchingStick = false;
        pathTicks = 0;
    }
}
