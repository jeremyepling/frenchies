package net.j40climb.frenchies.common.entity.frenchie.reaction;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.server.level.ServerLevel;

/** The entity registers these goals alongside its existing safety, command and play priorities. */
public final class CompanionReactions {
    private final EscapeDangerGoal escape;
    private final LavaEdgeGoal lava;
    final OwnerConcernGoal concern;
    private final QuietCompanyGoal quiet;

    public CompanionReactions(FrenchieEntity dog) {
        escape = new EscapeDangerGoal(dog);
        lava = new LavaEdgeGoal(dog);
        concern = new OwnerConcernGoal(dog);
        quiet = new QuietCompanyGoal(dog);
    }

    public Goal escapeDanger() { return escape; }
    public boolean isEscapingDanger() { return escape.canUse(); }
    public void checkFooting(ServerLevel level) { escape.checkFooting(level); }

    public Goal lavaAvoidance() { return lava; }
    public Goal ownerConcern() { return concern; }
    public boolean isFrightened() { return concern.frightened(); }
    public Goal quietCompanionship() { return quiet; }

    public boolean interruptsPlay() { return lava.active() || concern.active(); }
    public boolean keepsAwake() { return lava.active() || concern.pending(); }
    public void observeOwner() { quiet.observeOwner(); }
}
