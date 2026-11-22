package net.j40climb.frenchies.common.entity.frenchie;

import net.j40climb.frenchies.Frenchies;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

@EventBusSubscriber(modid = Frenchies.MOD_ID)
public final class CompanionTargeting {
    private CompanionTargeting() {}

    @SubscribeEvent
    public static void changeTarget(LivingChangeTargetEvent event) {
        // Includes brain attack memories and direct setTarget calls that bypass target predicates.
        if (FrenchieEntity.isProtectedMobTarget(event.getNewAboutToBeSetTarget())) {
            event.setNewAboutToBeSetTarget(null);
        }
    }
}
