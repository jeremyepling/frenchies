package net.j40climb.frenchies.common.entity.frenchie;

import net.minecraft.world.phys.HitResult;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.server.level.ServerLevel;
//? if <26.3 {
import net.minecraft.world.entity.EntityType;
//?} else {
/*import net.minecraft.world.entity.EntityTypes;
*///?}
import net.minecraft.world.phys.AABB;

/** A landing snowball is only an invitation to investigate nearby safe snow. */
public final class SnowballPlayEvents {
    private SnowballPlayEvents() {}

    public static void onImpact(Projectile projectile, HitResult hit) {
        if (projectile.getType() != EntityType.SNOWBALL
                || !(projectile.level() instanceof ServerLevel level)) return;
        var impact = hit.getLocation();
        for (FrenchieEntity dog : level.getEntitiesOfClass(FrenchieEntity.class,
                new AABB(impact, impact).inflate(6), FrenchieEntity::isTame)) {
            dog.inviteSnowball(impact);
        }
    }
}
