package net.j40climb.frenchies.common.entity.frenchie.rest;

import net.j40climb.frenchies.mixin.MiningState;
import net.minecraft.server.level.ServerPlayer;

public final class CompanionRest {
    private CompanionRest() {}

    public static boolean ownerIsBusy(ServerPlayer owner) {
        //? if <26.3 {
        boolean swinging = owner.swinging;
        //?} else {
        /*boolean swinging = owner.isSwinging();
        *///?}
        return !owner.isAlive() || owner.isSpectator() || owner.isSleeping()
                || owner.isUsingItem() || swinging || owner.isSprinting() || owner.isInWater()
                || ((MiningState) owner.gameMode).frenchies$isMining()
                || owner.getLastHurtMob() != null && owner.tickCount - owner.getLastHurtMobTimestamp() < 100
                || owner.getLastHurtByMob() != null && owner.tickCount - owner.getLastHurtByMobTimestamp() < 100;
    }
}
