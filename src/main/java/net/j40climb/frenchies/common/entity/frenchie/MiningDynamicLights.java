package net.j40climb.frenchies.common.entity.frenchie;

import dev.lambdaurora.lambdynlights.api.DynamicLightsContext;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import dev.lambdaurora.lambdynlights.api.entity.luminance.EntityLuminance;
import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager;
import net.j40climb.frenchies.Frenchies;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

/** Loaded only by LambDynamicLights on clients that have the optional provider. */
public final class MiningDynamicLights implements DynamicLightsInitializer {
    @Override
    public void onInitializeDynamicLights(DynamicLightsContext context) {
        context.entityLightSourceManager().onRegisterEvent().register(registration ->
                registration.register(FrenchieRegistry.FRENCHIE.get(), MiningLamp.INSTANCE));
    }

    private enum MiningLamp implements EntityLuminance {
        INSTANCE;

        private static final Type TYPE = Type.registerSimple(
                Identifier.fromNamespaceAndPath(Frenchies.MOD_ID, "mining_lamp"), INSTANCE);

        @Override public Type type() { return TYPE; }

        @Override
        public int getLuminance(ItemLightSourceManager items, Entity entity) {
            return entity instanceof FrenchieEntity dog && !dog.isInvisible() ? dog.miningLightLevel() : 0;
        }
    }
}
