package net.j40climb.frenchies.common.entity.frenchie;

/** Transient server-selected activity, shared with every tracking client. */
public enum PlayPose {
    NONE,
    BUBBLE_WATCH, BUBBLE_APPROACH, BUBBLE_BOW, BUBBLE_POUNCE, BUBBLE_NOSE,
    SLIME_NOTICE, SLIME_APPROACH, SLIME_BOW, SLIME_HOP, SLIME_CIRCLE,
    AMBIENT_SIT, AMBIENT_REST;

    private static final PlayPose[] POSES = values();

    static PlayPose byId(byte id) {
        return id >= 0 && id < POSES.length ? POSES[id] : NONE;
    }
}
