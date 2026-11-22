package net.j40climb.frenchies.test;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;

public final class FabricTestRegistration implements ModInitializer {
    @Override
    public void onInitialize() {
        DynamicRegistrySetupCallback.EVENT.register(view -> view.getOptional(Registries.TEST_INSTANCE).ifPresent(tests -> {
            var environments = view.getOptional(Registries.TEST_ENVIRONMENT).orElseThrow();
            new CompanionTests((id, environment) -> Registry.registerForHolder(environments, id, environment),
                    (id, test) -> Registry.register(tests, id, test)).register();
        }));
    }
}
