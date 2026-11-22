package net.j40climb.frenchies.common.entity.frenchie.play;

/** Synchronized animation stage IDs shared by server goals and the client model. */
public final class PlayStages {
    private PlayStages() {}

    public static final class Trick {
        private Trick() {}
        public static final int NONE = 0, CROUCH = 1, SPRING = 2, FLOP = 3, WIGGLE = 4, ROLL_UP = 5, LAP = 6;
    }

    public static final class BellyRub {
        private BellyRub() {}
        public static final int NONE = 0, ROLL_OVER = 1, WAITING = 2, RUBBING = 3, ROLL_UP = 4;
    }

    public static final class Digging {
        private Digging() {}
        public static final int NONE = 0, APPROACH = 1, DIG = 2, REVEAL = 3;
    }

    public static final class Weather {
        private Weather() {}
        public static final int NONE = 0, SHAKE = 1, GRASS_APPROACH = 2, GRASS_SHOULDER = 3,
            GRASS_ROLL = 4, GRASS_DRAG = 5, SNOW_APPROACH = 6, SNOW_DIVE = 7,
            SNOW_PLOW = 8, SNOW_POP = 9;
    }

    public static final class Celebration {
        private Celebration() {}
        public static final int NONE = 0, BOW = 1, RUN = 2, HOP = 3, SPLOOT = 4;
    }
}
