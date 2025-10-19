package com.norwood.mcheli.helper.info;

import com.google.common.collect.Maps;
import com.norwood.mcheli.helper.MCH_Utils;
import com.norwood.mcheli.helper.addon.AddonResourceLocation;
import com.norwood.mcheli.helicopter.MCH_HeliInfo;
import com.norwood.mcheli.hud.MCH_Hud;
import com.norwood.mcheli.plane.MCP_PlaneInfo;
import com.norwood.mcheli.ship.MCH_ShipInfo;
import com.norwood.mcheli.tank.MCH_TankInfo;
import com.norwood.mcheli.ship.MCH_ShipInfo;
import com.norwood.mcheli.throwable.MCH_ThrowableInfo;
import com.norwood.mcheli.vehicle.MCH_VehicleInfo;
import com.norwood.mcheli.weapon.MCH_WeaponInfo;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.BiFunction;

public class ContentFactories {
    private static final Map<String, IContentFactory> TABLE = Maps.newHashMap();

    static {
        TABLE.put("helicopters", createFactory(ContentType.HELICOPTER, MCH_HeliInfo::new));
        TABLE.put("planes", createFactory(ContentType.PLANE, MCP_PlaneInfo::new));
        TABLE.put("ships", createFactory(ContentType.SHIP, MCH_ShipInfo::new));
        TABLE.put("tanks", createFactory(ContentType.TANK, MCH_TankInfo::new));
        TABLE.put("vehicles", createFactory(ContentType.VEHICLE, MCH_VehicleInfo::new));
        TABLE.put("throwable", createFactory(ContentType.THROWABLE, MCH_ThrowableInfo::new));
        TABLE.put("weapons", createFactory(ContentType.WEAPON, MCH_WeaponInfo::new));
        if (MCH_Utils.isClient()) {
            TABLE.put("hud", createFactory(ContentType.HUD, MCH_Hud::new));
        }
    }

    @Nullable
    public static IContentFactory getFactory(@Nullable String dirName) {
        return dirName == null ? null : TABLE.get(dirName);
    }

    private static IContentFactory createFactory(final ContentType type, final BiFunction<AddonResourceLocation, String, IContentData> function) {
        return new IContentFactory() {
            @Override
            public IContentData create(AddonResourceLocation location, String filepath) {
                return function.apply(location, filepath);
            }

            @Override
            public ContentType getType() {
                return type;
            }
        };
    }
}
