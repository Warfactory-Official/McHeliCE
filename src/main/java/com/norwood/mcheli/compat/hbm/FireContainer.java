package com.norwood.mcheli.compat.hbm;

import lombok.NoArgsConstructor;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import java.util.Locale;

@NoArgsConstructor
public class FireContainer {

    private static final String C_FIRE = "com.hbm.entity.effect.EntityFireLingering";
    private static final String C_FLAME = "com.hbm.particle.helper.FlameCreator";

    /** DIESEL | FIRE | BALEFIRE | PHOSPHORUS | OXY | BLACK */
    public String fireType = "DIESEL";
    public int count = 1;
    public float width = 5.0F;
    public float height = 3.0F;
    public int areaSpread = 0;
    public int lifetime = 150;
    public int lifetimeVariance = 0;
    public boolean particles = true;

    private int typeId() {
        return switch (fireType.toUpperCase(Locale.ROOT).trim()) {
            case "BALEFIRE", "BALE" -> 1;
            case "PHOSPHORUS", "PHOSPHOR", "WP" -> 2;
            case "OXY", "OXYGEN" -> 3;
            case "BLACK" -> 4;
            default -> 0; // DIESEL / regular fire
        };
    }

    /**
     * @param effectOnly when true, only the particle burst is shown (no damaging lingering-fire
     *                   entity), mirroring the other NTM payloads.
     */
    public void execute(World world, double x, double y, double z, boolean effectOnly) {
        if (!HBMReflect.available()) return;
        int type = typeId();

        if (!effectOnly) {
            int total = Math.max(1, count);
            for (int i = 0; i < total; i++) {
                double sx = areaSpread == 0 ? x : x + world.rand.nextInt(areaSpread * 2) - areaSpread;
                double sz = areaSpread == 0 ? z : z + world.rand.nextInt(areaSpread * 2) - areaSpread;
                int life = lifetimeVariance == 0 ? lifetime : lifetime + world.rand.nextInt(lifetimeVariance);

                Object fireObj = HBMReflect.construct(C_FIRE, world);
                if (!(fireObj instanceof Entity fire)) return; // HBM missing/changed -> stop

                HBMReflect.call(fire, "setType", type);
                HBMReflect.call(fire, "setArea", width, height);
                HBMReflect.call(fire, "setDuration", life);
                fire.setPosition(sx, y, sz);
                world.spawnEntity(fire);
            }
        }

        if (particles) {
            // FlameCreator meta: balefire keeps its distinct visual; everything else uses fire.
            int meta = type == 1 ? 1 : 0;
            HBMReflect.callStatic(C_FLAME, "composeEffect", world, x, y, z, meta);
        }
    }
}
