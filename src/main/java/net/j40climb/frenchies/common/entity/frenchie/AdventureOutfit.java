package net.j40climb.frenchies.common.entity.frenchie;

import net.minecraft.world.entity.animal.happyghast.HappyGhast;

/** One environmental outfit at a time. Winter accessories and explorer gear use separate anchors. */
public enum AdventureOutfit {
    NONE, CAPTAIN, SCUBA, MINING, WINTER, NETHER, ASTRONAUT, AVIATOR;

    public boolean hasHeadwear() {
        return this != NONE && this != WINTER;
    }

    public static boolean isGhastFlying(HappyGhast ghast) {
        // The mount can hover while parked beside a platform.
        return !ghast.onGround() && (ghast.getDeltaMovement().lengthSqr() > 0.012
                || ghast.level().noCollision(ghast, ghast.getBoundingBox().move(0, -1.25, 0)));
    }

    static AdventureOutfit byId(int id) {
        return switch (id) {
            case 1 -> CAPTAIN;
            case 2 -> SCUBA;
            case 3 -> MINING;
            case 4 -> WINTER;
            case 5 -> NETHER;
            case 6 -> ASTRONAUT;
            case 7 -> AVIATOR;
            default -> NONE;
        };
    }

    static AdventureOutfit select(boolean scuba, boolean flight, boolean boat, boolean nether,
                                  boolean end, boolean cave, boolean winter) {
        if (scuba) return SCUBA;
        if (flight) return AVIATOR;
        if (boat) return CAPTAIN;
        if (nether) return NETHER;
        if (end) return ASTRONAUT;
        if (cave) return MINING;
        return winter ? WINTER : NONE;
    }
}
