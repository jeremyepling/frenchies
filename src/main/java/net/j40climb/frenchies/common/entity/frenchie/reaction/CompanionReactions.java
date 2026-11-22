package net.j40climb.frenchies.common.entity.frenchie.reaction;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/** The entity registers these goals alongside its existing safety, command and play priorities. */
public final class CompanionReactions {
    private final LavaEdgeGoal lava;
    final OwnerConcernGoal concern;
    private final QuietCompanyGoal quiet;

    public CompanionReactions(FrenchieEntity dog) {
        lava = new LavaEdgeGoal(dog);
        concern = new OwnerConcernGoal(dog);
        quiet = new QuietCompanyGoal(dog);
    }

    public Goal lavaAvoidance() { return lava; }
    public Goal ownerConcern() { return concern; }
    public Goal quietCompanionship() { return quiet; }

    public boolean interruptsPlay() { return lava.active() || concern.active(); }
    public boolean keepsAwake() { return lava.active() || concern.pending(); }
    public void observeOwner() { quiet.observeOwner(); }
}
