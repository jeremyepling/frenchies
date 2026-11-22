package net.j40climb.frenchies.common.entity.frenchie;

public enum DangerReaction {
    NONE(0), STARTLE(14), DUCK(24), TUMBLE(18);

    public final int duration;

    DangerReaction(int duration) {
        this.duration = duration;
    }

    static DangerReaction byId(byte id) {
        return switch (id) {
            case 1 -> STARTLE;
            case 2 -> DUCK;
            case 3 -> TUMBLE;
            default -> NONE;
        };
    }
}
