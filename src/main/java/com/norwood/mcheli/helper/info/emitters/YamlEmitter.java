package com.norwood.mcheli.helper.info.emitters;

import com.google.common.primitives.Floats;
import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_BoundingBox;
import com.norwood.mcheli.aircraft.MCH_SeatInfo;
import com.norwood.mcheli.aircraft.MCH_SeatRackInfo;
import com.norwood.mcheli.helicopter.MCH_HeliInfo;
import com.norwood.mcheli.helper.info.parsers.yaml.ComponentParser;
import com.norwood.mcheli.hud.MCH_Hud;
import com.norwood.mcheli.item.MCH_ItemInfo;
import com.norwood.mcheli.plane.MCH_PlaneInfo;
import com.norwood.mcheli.ship.MCH_ShipInfo;
import com.norwood.mcheli.tank.MCH_TankInfo;
import com.norwood.mcheli.throwable.MCH_ThrowableInfo;
import com.norwood.mcheli.vehicle.MCH_VehicleInfo;
import com.norwood.mcheli.weapon.MCH_WeaponInfo;
import com.norwood.mcheli.wrapper.W_Entity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@SuppressWarnings("AutoBoxing")
public class YamlEmitter implements IEmitter {

    private static final Yaml YAML;

    static {
        DumperOptions opts = new DumperOptions();
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        opts.setPrettyFlow(true);
        InlineAwareRepresenter rep = new InlineAwareRepresenter(opts);
        rep.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        YAML = new Yaml(rep, opts);
    }

    public static void writeTo(Path out, CharSequence content) throws IOException {
        if (out.getParent() != null) Files.createDirectories(out.getParent());
        Files.write(out, content.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

//    @SafeVarargs
//    private static <E> InlineSeq<E> inline(E... values) {
//        InlineSeq<E> seq = new InlineSeq<>(values.length);
//        Collections.addAll(seq, values);
//        return seq;
//    }

    private static InlineSeq<Double> inline(double... values) {
        InlineSeq<Double> seq = new InlineSeq<>(values.length);
        for (double v : values) seq.add(v);
        return seq;
    }

    private static InlineSeq<Double> vec(Vec3d v) {
        return inline(v.x, v.y, v.z);
    }

    private static InlineSeq<Double> vecMinusYOffset(Vec3d v) {
        return inline(v.x, v.y - W_Entity.GLOBAL_Y_OFFSET, v.z);
    }

    private static String tankWeight(int ordinal) {
        return ordinal == 1 ? "TANK" : ordinal == 2 ? "CAR" : "UKNOWN";
    }

    private static @Nullable String sightToString(com.norwood.mcheli.weapon.MCH_SightType sight) {
        if (sight == null) return null;
        return switch (sight) {
            case LOCK -> "missilesight";
            case ROCKET -> "movesight";
            default -> null;
        };
    }

    private static String flareTypeFromInt(int v) {
        return switch (v) {
            case 0 -> "NONE";
            case 1 -> "NORMAL";
            case 2 -> "LARGE_AIRCRAFT";
            case 3 -> "SIDE";
            case 4 -> "FRONT";
            case 5 -> "DOWN";
            case 10 -> "SMOKE_LAUNCHER";
            default -> null;
        };
    }

    private static String toHexRGB(int color) {
        int rgb = color & 0xFFFFFF;
        return String.format("#%06X", rgb);
    }

    @Override
    public String emitHelicopter(MCH_HeliInfo info) {
        Map<String, Object> root = baseAircraft(info);
        Map<String, Object> heli = new LinkedHashMap<>();
        heli.put("IsFoldableBlade", info.isEnableFoldBlade);
        root.put("HeliFeatures", heli);
        Map<String, List<Map<String, Object>>> components = new LinkedHashMap<>();
        addCommonComponents(components, info);
        if (!components.isEmpty()) root.put("Components", components);
        return YAML.dump(root);
    }

    @Override
    public String emitPlane(MCH_PlaneInfo info) {
        Map<String, Object> root = baseAircraft(info);
        Map<String, Object> plane = new LinkedHashMap<>();
        plane.put("VariableSweepWing", info.isVariableSweepWing);
        plane.put("SweepWingSpeed", info.sweepWingSpeed);
        if (info.isEnableVtol) {
            Map<String, Object> vtol = new LinkedHashMap<>();
            vtol.put("IsDefault", info.isDefaultVtol);
            if (info.vtolYaw != 0.3F) vtol.put("Yaw", info.vtolYaw);
            if (info.vtolPitch != 0.2F) vtol.put("Pitch", info.vtolPitch);
            plane.put("EnableVtol", vtol);
        } else {
            plane.put("EnableVtol", false);
        }
        plane.put("EnableAutoPilot", info.isEnableAutoPilot);
        root.put("PlaneFeatures", plane);
        Map<String, List<Map<String, Object>>> components = new LinkedHashMap<>();
        addCommonComponents(components, info);
        // Plane-specific
        if (info.rotorList != null && !info.rotorList.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (MCH_PlaneInfo.Rotor r : info.rotorList) {
                Map<String, Object> m = drawnPart(r);
                if (r.maxRotFactor != 0) m.put("RotFactor", r.maxRotFactor);
                list.add(m);
            }
            components.put("PlaneRotor", list);
        }
        if (info.wingList != null && !info.wingList.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (MCH_PlaneInfo.Wing w : info.wingList) {
                Map<String, Object> m = drawnPart(w);
                m.put("MaxRotation", w.maxRot);
                if (w.pylonList != null && !w.pylonList.isEmpty()) {
                    List<Map<String, Object>> pylons = new ArrayList<>();
                    for (MCH_PlaneInfo.Pylon p : w.pylonList) {
                        Map<String, Object> pm = drawnPart(p);
                        pm.put("MaxRotation", p.maxRot);
                        pylons.add(pm);
                    }
                    m.put("Pylons", pylons);
                }
                list.add(m);
            }
            components.put("Wing", list);
        }
        if (info.nozzles != null && !info.nozzles.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (MCH_AircraftInfo.DrawnPart np : info.nozzles) list.add(drawnPart(np));
            components.put("Nozzle", list);
        }
        if (!components.isEmpty()) root.put("Components", components);
        return YAML.dump(root);
    }

    @Override
    public String emitShip(MCH_ShipInfo info) {
        Map<String, Object> root = baseAircraft(info);
        Map<String, List<Map<String, Object>>> components = new LinkedHashMap<>();
        addCommonComponents(components, info);
        if (!components.isEmpty()) root.put("Components", components);
        return YAML.dump(root);
    }

    @Override
    public String emitTank(MCH_TankInfo info) {
        Map<String, Object> root = baseAircraft(info);
        Map<String, Object> tank = new LinkedHashMap<>();
        tank.put("WeightType", tankWeight(info.weightType));
        tank.put("WeightedCenterZ", info.weightedCenterZ);
        root.put("TankFeatures", tank);
        Map<String, List<Map<String, Object>>> components = new LinkedHashMap<>();
        addCommonComponents(components, info);
        if (!components.isEmpty()) root.put("Components", components);
        return YAML.dump(root);
    }

    @Override
    public String emitVehicle(MCH_VehicleInfo info) {
        Map<String, Object> root = baseAircraft(info);
        Map<String, Object> veh = new LinkedHashMap<>();
        veh.put("CanMove", info.isEnableMove);
        veh.put("CanRotate", info.isEnableRot);
        root.put("VehicleFeatures", veh);
        Map<String, List<Map<String, Object>>> components = new LinkedHashMap<>();
        addCommonComponents(components, info);
        if (!info.partList.isEmpty()) {
            List<Map<String, Object>> vparts = new ArrayList<>();
            for (MCH_VehicleInfo.VPart vp : info.partList) {
                Map<String, Object> m = drawnPart(vp);
                m.put("DrawFP", vp.drawFP);
                m.put("CanYaw", vp.rotYaw);
                m.put("CanPitch", vp.rotPitch);
                m.put("Type", ComponentParser.VpartType.values()[vp.type].name());
                if (vp.recoilBuf != 0) m.put("RecoilBuff", vp.recoilBuf);
                if (vp.child != null && !vp.child.isEmpty()) {
                    List<Map<String, Object>> children = new ArrayList<>();
                    for (MCH_VehicleInfo.VPart cp : vp.child) {
                        Map<String, Object> cm = drawnPart(cp);
                        cm.put("DrawFP", cp.drawFP);
                        cm.put("CanYaw", cp.rotYaw);
                        cm.put("CanPitch", cp.rotPitch);
                        cm.put("Type", ComponentParser.VpartType.values()[cp.type].name());
                        if (cp.recoilBuf != 0) cm.put("RecoilBuff", cp.recoilBuf);
                        children.add(cm);
                    }
                    m.put("Children", children);
                }
                vparts.add(m);
            }
            components.put("Vpart", vparts);
        }
        if (!components.isEmpty()) root.put("Components", components);
        return YAML.dump(root);
    }

    @Override
    public String emitWeapon(MCH_WeaponInfo info) {
        Map<String, Object> root = new LinkedHashMap<>();
        if (notBlank(info.displayName)) root.put("DisplayName", info.displayName);
        if (notBlank(info.type)) root.put("Type", info.type);
        if (notBlank(info.group)) root.put("Group", info.group);

        Map<String, Object> ball = new LinkedHashMap<>();
        ball.put("Acceleration", info.acceleration);
        ball.put("AccelerationInWater", info.accelerationInWater);
        ball.put("Gravity", info.gravity);
        ball.put("GravityInWater", info.gravityInWater);
        ball.put("VelocityInWater", info.velocityInWater);
        ball.put("Power", info.power);
        if (info.speedFactor != 0.0f) ball.put("SpeedFactor", info.speedFactor);
        if (info.speedFactorStartTick != 0) ball.put("SpeedFactorStartTick", info.speedFactorStartTick);
        if (info.speedFactorEndTick != 0) ball.put("SpeedFactorEndTick", info.speedFactorEndTick);
        if (info.speedDependsAircraft) ball.put("SpeedDependsAircraft", true);
        if (info.piercing != 0) ball.put("Piercing", info.piercing);
        root.put("Ballistics", ball);

        Map<String, Object> snd = new LinkedHashMap<>();
        if (notBlank(info.soundFileName)) snd.put("Name", info.soundFileName.toLowerCase(Locale.ROOT));
        snd.put("Delay", info.soundDelay);
        snd.put("Volume", info.soundVolume);
        snd.put("Pitch", info.soundPitch);
        if (info.soundPitchRandom != 0.0f) snd.put("PitchRandom", info.soundPitchRandom);
        if (notBlank(info.hitSound)) snd.put("HitSound", info.hitSound.toLowerCase(Locale.ROOT));
        if (notBlank(info.hitSoundIron)) snd.put("HitSoundIron", info.hitSoundIron.toLowerCase(Locale.ROOT));
        if (!snd.isEmpty()) root.put("Sound", snd);

        if (notBlank(info.explosionType)) root.put("ExplosionType", info.explosionType);
        if (info.explosion != 0) root.put("Explosion", info.explosion);
        if (info.explosionBlock >= 0) root.put("ExplosionBlock", info.explosionBlock);
        if (info.explosionInWater != 0) root.put("ExplosionInWater", info.explosionInWater);
        if (info.explosionAltitude != 0) root.put("ExplosionAltitude", info.explosionAltitude);
        if (info.timeFuse != 0) root.put("TimeFuse", info.timeFuse);
        if (info.delayFuse != 0) root.put("DelayFuse", info.delayFuse);
        if (info.bound != 0.0f) root.put("Bound", info.bound);
        if (info.flaming) root.put("Flaming", true);
        if (info.displayMortarDistance) root.put("DisplayMortarDistance", true);
        if (info.fixCameraPitch) root.put("FixCameraPitch", true);
        if (info.cameraRotationSpeedPitch != 1.0F) root.put("CameraRotationSpeedPitch", info.cameraRotationSpeedPitch);
        String sight = sightToString(info.sight);
        if (sight != null) root.put("Sight", sight);

        if (info.maxDegreeOfMissile != 60) root.put("MaxDegreeOfMissile", info.maxDegreeOfMissile);
        if (info.tickEndHoming != -1) root.put("TickEndHoming", info.tickEndHoming);
        if (info.flakParticlesCrack != 10) root.put("FlakParticlesCrack", info.flakParticlesCrack);
        if (info.numParticlesFlak != 3) root.put("ParticlesFlak", info.numParticlesFlak);
        if (info.flakParticlesDiff != 0.3F) root.put("FlakParticlesDiff", info.flakParticlesDiff);
        if (info.isRadarMissile) root.put("IsRadarMissile", true);
        if (!info.isHeatSeekerMissile) root.put("IsHeatSeekerMissile", false);
        if (info.maxLockOnRange != 300) root.put("MaxLockOnRange", info.maxLockOnRange);
        if (info.maxLockOnAngle != 10) root.put("MaxLockOnAngle", info.maxLockOnAngle);
        if (info.pdHDNMaxDegree != 1000.0f) root.put("PDHDNMaxDegree", info.pdHDNMaxDegree);
        if (info.pdHDNMaxDegreeLockOutCount != 10)
            root.put("PDHDNMaxDegreeLockOutCount", info.pdHDNMaxDegreeLockOutCount);
        if (info.antiFlareCount != -1) root.put("AntiFlareCount", info.antiFlareCount);
        if (info.lockMinHeight != 25) root.put("LockMinHeight", info.lockMinHeight);
        if (info.passiveRadar) root.put("PassiveRadar", true);
        if (info.passiveRadarLockOutCount != 20) root.put("PassiveRadarLockOutCount", info.passiveRadarLockOutCount);
        if (info.laserGuidance) root.put("LaserGuidance", true);
        if (!info.hasLaserGuidancePod) root.put("HasLaserGuidancePod", false);
        if (info.activeRadar) root.put("ActiveRadar", true);
        if (!info.enableOffAxis) root.put("EnableOffAxis", false);
        if (info.turningFactor != 0.5) root.put("TurningFactor", info.turningFactor);
        if (info.enableChunkLoader) root.put("EnableChunkLoader", true);
        if (info.scanInterval != 20) root.put("ScanInterval", info.scanInterval);
        if (info.weaponSwitchCount != 0) root.put("WeaponSwitchCount", info.weaponSwitchCount);
        if (notBlank(info.weaponSwitchSound))
            root.put("WeaponSwitchSound", info.weaponSwitchSound.toLowerCase(Locale.ROOT));
        if (info.canLockMissile) root.put("CanLockMissile", true);
        if (info.enableBVR) root.put("EnableBVR", true);
        if (info.minRangeBVR != 300) root.put("MinRangeBVR", info.minRangeBVR);
        if (!info.predictTargetPos) root.put("PredictTargetPos", false);
        if (info.hitSoundRange != 100) root.put("HitSoundRange", info.hitSoundRange);
        if (info.numLockedChaffMax != 2) root.put("NumLockedChaffMax", info.numLockedChaffMax);
        if (!info.disableDestroyBlock) root.put("DisableDestroyBlock", false);
        if (notBlank(info.railgunSound)) root.put("RailgunSound", info.railgunSound.toLowerCase(Locale.ROOT));
        if (info.canBeIntercepted) root.put("CanBeIntercepted", true);
        if (info.canAirburst) root.put("CanAirburst", true);
        if (info.explosionAirburst > 0) root.put("ExplosionAirburst", info.explosionAirburst);
        if (info.crossType != 0) root.put("CrossType", info.crossType);
        if (info.hasMortarRadar) root.put("EnableMortarRadar", true);
        if (info.mortarRadarMaxDist != -1) root.put("MortarRadarMaxDist", info.mortarRadarMaxDist);
        if (info.round != 0) root.put("Round", info.round);
        if (info.suppliedNum != 1) root.put("SuppliedNum", info.suppliedNum);

        root.put("Delay", info.delay);
        root.put("ReloadTime", info.reloadTime);
        root.put("MaxAmmo", info.maxAmmo);

        if (info.roundItems != null && !info.roundItems.isEmpty()) {
            List<Map<String, Object>> rounds = new ArrayList<>();
            for (MCH_WeaponInfo.RoundItem ri : info.roundItems) {
                Map<String, Object> rm = new LinkedHashMap<>();
                rm.put("Count", ri.num);
                rm.put("Name", ri.itemName.toString().toLowerCase(Locale.ROOT));
                rm.put("Meta", ri.damage);
                rounds.add(rm);
            }
            root.put("AmmoItems", rounds);
        }
        if (info.lockTime != 30) root.put("LockTime", info.lockTime);
        if (info.ridableOnly) root.put("RidableOnly", true);
        if (info.proximityFuseDist != 0.0F) root.put("ProximityFuseDist", info.proximityFuseDist);
        if (info.rigidityTime != 7) root.put("RigidityTime", info.rigidityTime);
        if (info.accuracy != 0.0F) root.put("Accuracy", info.accuracy);
        if (info.bomblet != 0) root.put("Bomblet", info.bomblet);
        if (info.bombletSTime != 10) root.put("BombletSTime", info.bombletSTime);
        if (info.bombletDiff != 0.3F) root.put("BombletDiff", info.bombletDiff);
        if (info.modeNum != 0) root.put("Mode", info.modeNum);
        if (info.fixMode != 0) root.put("FixMode", info.fixMode);
        if (info.heatCount != 0) root.put("HeatCount", info.heatCount);
        if (info.maxHeatCount != 0) root.put("MaxHeatCount", info.maxHeatCount);

        // HBM compat
        Map<String, Object> ntm = new LinkedHashMap<>();
        if (info.payloadNTM != null && info.payloadNTM != MCH_WeaponInfo.Payload.NONE) {
            ntm.put("PayloadType", info.payloadNTM.name());
        }
        if (info.effectOnly) ntm.put("EffectOnly", true);
        if (info.fluidTypeNTM != null) ntm.put("FluidType", info.fluidTypeNTM);
        if (!ntm.isEmpty()) root.put("NTM", ntm);

        // Marker Rocket params
        Map<String, Object> marker = new LinkedHashMap<>();
        if (info.markerRocketSpawnNum != 5) marker.put("MarkerRocketSpawnNum", info.markerRocketSpawnNum);
        if (info.markerRocketSpawnDiff != 15) marker.put("MarkerRocketSpawnDiff", info.markerRocketSpawnDiff);
        if (info.markerRocketSpawnHeight != 200) marker.put("MarkerRocketSpawnHeight", info.markerRocketSpawnHeight);
        if (info.markerRocketSpawnSpeed != 5) marker.put("MarkerRocketSpawnSpeed", info.markerRocketSpawnSpeed);
        if (!marker.isEmpty()) root.put("MarkerRocket", marker);

        // Damage block
        Map<String, Object> dmg = new LinkedHashMap<>();
        if (info.explosionDamageVsLiving != 1.0f) dmg.put("ExplosionDamageVsLiving", info.explosionDamageVsLiving);
        if (info.explosionDamageVsPlayer != 1.0f) dmg.put("ExplosionDamageVsPlayer", info.explosionDamageVsPlayer);
        if (info.explosionDamageVsPlane != 1.0f) dmg.put("ExplosionDamageVsPlane", info.explosionDamageVsPlane);
        if (info.explosionDamageVsVehicle != 1.0f) dmg.put("ExplosionDamageVsVehicle", info.explosionDamageVsVehicle);
        if (info.explosionDamageVsTank != 1.0f) dmg.put("ExplosionDamageVsTank", info.explosionDamageVsTank);
        if (info.explosionDamageVsHeli != 1.0f) dmg.put("ExplosionDamageVsHeli", info.explosionDamageVsHeli);
        if (!dmg.isEmpty()) root.put("Damage", dmg);

        // Render settings
        Map<String, Object> render = new LinkedHashMap<>();
        if (info.listMuzzleFlash != null && !info.listMuzzleFlash.isEmpty()) {
            List<Map<String, Object>> flashes = new ArrayList<>();
            for (MCH_WeaponInfo.MuzzleFlash mf : info.listMuzzleFlash) {
                Map<String, Object> mm = new LinkedHashMap<>();
                mm.put("Distance", mf.dist);
                mm.put("Size", mf.size);
                mm.put("Range", mf.range);
                mm.put("Age", mf.age);
                mm.put("Count", mf.num);
                int r = (int) Math.round(Math.max(0, Math.min(1, mf.r)) * 255.0);
                int g = (int) Math.round(Math.max(0, Math.min(1, mf.g)) * 255.0);
                int b = (int) Math.round(Math.max(0, Math.min(1, mf.b)) * 255.0);
                int rgb = (r << 16) | (g << 8) | b;
                mm.put("Color", String.format("#%06X", rgb));
                flashes.add(mm);
            }
            render.put("MuzzleFlashes", flashes);
        }
        if (info.listMuzzleFlashSmoke != null && !info.listMuzzleFlashSmoke.isEmpty()) {
            List<Map<String, Object>> flashes = new ArrayList<>();
            for (MCH_WeaponInfo.MuzzleFlash mf : info.listMuzzleFlashSmoke) {
                Map<String, Object> mm = new LinkedHashMap<>();
                mm.put("Distance", mf.dist);
                mm.put("Size", mf.size);
                mm.put("Range", mf.range);
                mm.put("Age", mf.age);
                mm.put("Count", mf.num);
                int r = (int) Math.round(Math.max(0, Math.min(1, mf.r)) * 255.0);
                int g = (int) Math.round(Math.max(0, Math.min(1, mf.g)) * 255.0);
                int b = (int) Math.round(Math.max(0, Math.min(1, mf.b)) * 255.0);
                int rgb = (r << 16) | (g << 8) | b;
                mm.put("Color", String.format("#%06X", rgb));
                flashes.add(mm);
            }
            render.put("MuzzleSmoke", flashes);
        }
        if (notBlank(info.bulletModelName)) render.put("BulletModel", info.bulletModelName);
        if (notBlank(info.bombletModelName)) render.put("BombletModel", info.bombletModelName);
        if (!render.isEmpty()) root.put("Render", render);

        if (info.zoom != null && info.zoom.length > 0) root.put("Zoom", Floats.asList(info.zoom));

        return YAML.dump(root);
    }

    @Override
    public String emitThrowable(MCH_ThrowableInfo info) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("DisplayName", info.displayName);
        return YAML.dump(root);
    }

    @Override
    public String emitHud(MCH_Hud hud) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("Name", hud.name);
        root.put("FileName", hud.fileName);
        root.put("Items", hud.list == null ? 0 : hud.list.size());
        return YAML.dump(root);
    }

    @Override
    public String emitItem(MCH_ItemInfo info) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("DisplayName", info.displayName);
        return YAML.dump(root);
    }

    private Map<String, Object> baseAircraft(MCH_AircraftInfo info) {
        Map<String, Object> root = new LinkedHashMap<>();

        if (info.displayNameLang != null && !info.displayNameLang.isEmpty()) {
            Map<String, Object> dn = new LinkedHashMap<>();
            dn.put("DEFAULT", info.displayName);
            dn.putAll(info.displayNameLang);
            root.put("DisplayName", dn);
        } else {
            root.put("DisplayName", info.displayName);
        }

        if (info.itemID > 0) root.put("ItemID", info.itemID);
        if (notBlank(info.category)) root.put("Category", info.category.toUpperCase(Locale.ROOT));
        if (info.recipeString != null && !info.recipeString.isEmpty()) {
            Map<String, Object> rec = new LinkedHashMap<>();
            rec.put("isShaped", info.isShapedRecipe);
            rec.put("Pattern", info.recipeString);
            root.put("Recepie", rec);
        }

        root.put("CanRide", info.canRide);
        root.put("RotorSpeed", info.rotorSpeed);
        if (info.turretPosition != null) root.put("TurretPosition", vec(info.turretPosition));
        if (info.unmountPosition != null) root.put("GlobalUnmountPos", vec(info.unmountPosition));
        root.put("CreativeOnly", info.creativeOnly);
        root.put("Regeneration", info.regeneration);
        root.put("Invulnerable", info.invulnerable);
        root.put("MaxFuel", info.maxFuel);
        root.put("MaxHP", info.maxHp);
        root.put("Stealth", info.stealth);
        root.put("FuelConsumption", info.fuelConsumption);
        root.put("FuelSupplyRange", info.fuelSupplyRange);
        root.put("AmmoSupplyRange", info.ammoSupplyRange);
        if (info.repairOtherVehiclesRange != 0.0F || info.repairOtherVehiclesValue != 0) {
            Map<String, Object> repair = new LinkedHashMap<>();
            repair.put("range", info.repairOtherVehiclesRange);
            repair.put("value", info.repairOtherVehiclesValue);
            root.put("RepairOtherVehicles", repair);
        }
        if (notBlank(info.nameOnModernAARadar)) root.put("NameOnModernAARadar", info.nameOnModernAARadar);
        if (notBlank(info.nameOnEarlyAARadar)) root.put("NameOnEarlyAARadar", info.nameOnEarlyAARadar);
        if (notBlank(info.nameOnModernASRadar)) root.put("NameOnModernASRadar", info.nameOnModernASRadar);
        if (notBlank(info.nameOnEarlyASRadar)) root.put("NameOnEarlyASRadar", info.nameOnEarlyASRadar);
        if (info.explosionSizeByCrash != 5) root.put("ExplosionSizeByCrash", (int) info.explosionSizeByCrash);
        if (info.throttleDownFactor != 1) root.put("ThrottleDownFactor", info.throttleDownFactor);

        // Global camera section
        Map<String, Object> camera = new LinkedHashMap<>();
        camera.put("ThirdPersonDist", info.thirdPersonDist);
        camera.put("Zoom", info.cameraZoom);
        camera.put("DefaultFreeLook", info.defaultFreelook);
        camera.put("RotationSpeed", info.cameraRotationSpeed);
        if (info.cameraPosition != null && !info.cameraPosition.isEmpty()) {
            List<Map<String, Object>> camList = new ArrayList<>();
            for (MCH_AircraftInfo.CameraPosition cp : info.cameraPosition) {
                Map<String, Object> cm = new LinkedHashMap<>();
                cm.put("Pos", vecMinusYOffset(cp.pos));
                if (cp.fixRot) cm.put("FixedRot", true);
                if (cp.yaw != 0) cm.put("Yaw", cp.yaw);
                if (cp.pitch != 0) cm.put("Pitch", cp.pitch);
                camList.add(cm);
            }
            camera.put("Pos", camList);
        }
        root.put("Camera", camera);
        if (info.alwaysCameraView) root.put("AlwaysCameraView", true);

        // Sound
        Map<String, Object> sound = new LinkedHashMap<>();
        if (notBlank(info.soundMove)) sound.put("MoveSound", info.soundMove.toLowerCase(Locale.ROOT));
        sound.put("Vol", info.soundVolume);
        sound.put("Pitch", info.soundPitch);
        sound.put("Range", info.soundRange);
        root.put("Sound", sound);

        // Physical properties
        Map<String, Object> phys = new LinkedHashMap<>();
        phys.put("Speed", info.speed);
        phys.put("CanFloat", info.isFloat);
        phys.put("FloatOffset", -info.floatOffset);
        phys.put("MotionFactor", info.motionFactor);
        phys.put("Gravity", info.gravity);
        phys.put("RotationSnapValue", info.autoPilotRot);
        phys.put("GravityInWater", info.gravityInWater);
        phys.put("StepHeight", info.stepHeight);
        phys.put("CanRotOnGround", info.canRotOnGround);
        phys.put("CanMoveOnGround", info.canMoveOnGround);
        phys.put("OnGroundPitch", info.onGroundPitch);
        phys.put("PivotTurnThrottle", info.pivotTurnThrottle);
        Map<String, Object> mobility = new LinkedHashMap<>();
        mobility.put("Yaw", info.mobilityYaw);
        mobility.put("Pitch", info.mobilityPitch);
        mobility.put("Roll", info.mobilityRoll);
        mobility.put("YawOnGround", info.mobilityYawOnGround);
        phys.put("Mobility", mobility);
        Map<String, Object> gpf = new LinkedHashMap<>();
        gpf.put("Pitch", info.onGroundPitchFactor);
        gpf.put("Roll", info.onGroundRollFactor);
        phys.put("GroundPitchFactors", gpf);
        Map<String, Object> bodySize = new LinkedHashMap<>();
        bodySize.put("Height", info.bodyHeight);
        bodySize.put("Width", info.bodyWidth);
        phys.put("BodySize", bodySize);
        if (info.limitRotation) {
            Map<String, Object> rotLimits = new LinkedHashMap<>();
            Map<String, Object> pitch = new LinkedHashMap<>();
            pitch.put("Min", info.minRotationPitch);
            pitch.put("Max", info.maxRotationPitch);
            rotLimits.put("Pitch", pitch);
            Map<String, Object> roll = new LinkedHashMap<>();
            roll.put("Min", info.minRotationRoll);
            roll.put("Max", info.maxRotationRoll);
            rotLimits.put("Roll", roll);
            phys.put("RotationLimits", rotLimits);
        }
        root.put("PhysicalProperties", phys);

        // Render
        Map<String, Object> render = new LinkedHashMap<>();
        render.put("SmoothShading", info.smoothShading);
        render.put("HideRiders", info.hideEntity);
        render.put("ModelWidth", info.entityWidth);
        render.put("ModelHeight", info.entityHeight);
        render.put("ModelPitch", info.entityPitch);
        render.put("ModelRoll", info.entityRoll);
        render.put("ParticleScale", info.particlesScale);
        render.put("OneProbeScale", info.oneProbeScale);
        render.put("EnableSeaSurfaceParticle", info.enableSeaSurfaceParticle);
        if (info.particleSplashs != null && !info.particleSplashs.isEmpty()) {
            List<Map<String, Object>> splash = new ArrayList<>();
            for (MCH_AircraftInfo.ParticleSplash ps : info.particleSplashs) {
                Map<String, Object> pm = new LinkedHashMap<>();
                pm.put("Position", vec(ps.pos));
                pm.put("Count", ps.num);
                pm.put("Size", ps.size);
                pm.put("Acceleration", ps.acceleration);
                pm.put("Age", ps.age);
                pm.put("Motion", ps.motionY);
                pm.put("Gravity", ps.gravity);
                splash.add(pm);
            }
            render.put("SplashParticles", splash);
        }
        root.put("Render", render);

        // Armor
        Map<String, Object> armor = new LinkedHashMap<>();
        armor.put("ArmorDamageFactor", info.armorDamageFactor);
        armor.put("ArmorMinDamage", info.armorMinDamage);
        armor.put("ArmorMaxDamage", info.armorMaxDamage);
        armor.put("DamageFactor", info.damageFactor);
        armor.put("SubmergedDamageHeight", info.submergedDamageHeight);
        root.put("Armor", armor);

        // Weapons list
        List<Map<String, Object>> weapons = new ArrayList<>();
        if (info.weaponSetList != null) {
            for (MCH_AircraftInfo.WeaponSet set : info.weaponSetList) {
                if (set.weapons == null) continue;
                for (MCH_AircraftInfo.Weapon w : set.weapons) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("Type", set.type);
                    m.put("Position", vecMinusYOffset(w.pos));
                    if (w.yaw != 0) m.put("Yaw", w.yaw);
                    if (w.pitch != 0) m.put("Pitch", w.pitch);
                    if (w.seatID > 0) m.put("SeatID", w.seatID + 1);
                    if (!w.canUsePilot) m.put("CanUsePilot", false);
                    if (w.defaultYaw != 0) m.put("DefaultYaw", w.defaultYaw);
                    if (w.minYaw != 0) m.put("MinYaw", w.minYaw);
                    if (w.maxYaw != 0) m.put("MaxYaw", w.maxYaw);
                    if (w.minPitch != 0) m.put("MinPitch", w.minPitch);
                    if (w.maxPitch != 0) m.put("MaxPitch", w.maxPitch);
                    if (w.turret) m.put("Turret", true);
                    weapons.add(m);
                }
            }
        }
        if (!weapons.isEmpty()) root.put("Weapons", weapons);

        // Seats
        if (info.seatList != null && !info.seatList.isEmpty()) {
            List<Map<String, Object>> seats = new ArrayList<>();
            for (MCH_SeatInfo seatInfo : info.seatList) {
                if (seatInfo instanceof MCH_SeatRackInfo) continue;
                Map<String, Object> sm = new LinkedHashMap<>();
                sm.put("Position", vec(seatInfo.pos));
                if (seatInfo.gunner) sm.put("Gunner", true);
                if (seatInfo.switchgunner) sm.put("SwitchGunner", true);
                if (seatInfo.fixRot) sm.put("FixRot", true);
                if (seatInfo.fixYaw != 0.0f) sm.put("FixYaw", seatInfo.fixYaw);
                if (seatInfo.fixPitch != 0.0f) sm.put("FixPitch", seatInfo.fixPitch);
                if (seatInfo.minPitch != -30.0f) sm.put("MinPitch", seatInfo.minPitch);
                if (seatInfo.maxPitch != 70.0f) sm.put("MaxPitch", seatInfo.maxPitch);
                if (seatInfo.rotSeat) sm.put("RotSeat", true);
                if (seatInfo.invCamPos) sm.put("InvCamPos", true);
                if (seatInfo.getCamPos() != null) {
                    Map<String, Object> cm = new LinkedHashMap<>();
                    cm.put("Pos", vecMinusYOffset(seatInfo.getCamPos().pos));
                    if (seatInfo.getCamPos().fixRot) cm.put("FixedRot", true);
                    if (seatInfo.getCamPos().yaw != 0.0f) cm.put("Yaw", seatInfo.getCamPos().yaw);
                    if (seatInfo.getCamPos().pitch != 0.0f) cm.put("Pitch", seatInfo.getCamPos().pitch);
                    sm.put("Camera", cm);
                }
                seats.add(sm);
            }
            root.put("Seats", seats);
        }

        // Racks
        if (info.entityRackList != null && !info.entityRackList.isEmpty()) {
            List<Map<String, Object>> racks = new ArrayList<>();
            for (MCH_SeatRackInfo r : info.entityRackList) {
                Map<String, Object> rm = new LinkedHashMap<>();
                rm.put("Position", vec(r.pos));
                if (r.getCamPos() != null) {
                    Map<String, Object> cm = new LinkedHashMap<>();
                    cm.put("Pos", vecMinusYOffset(r.getCamPos().pos));
                    if (r.getCamPos().fixRot) cm.put("FixedRot", true);
                    racks.add(rm);
                    rm.put("Camera", cm);
                } else {
                    racks.add(rm);
                }
                if (r.names != null && r.names.length > 0) {
                    if (r.names.length == 1) rm.put("Name", r.names[0]);
                    else rm.put("Names", Arrays.asList(r.names));
                }
                if (r.range != 0.0f) rm.put("Range", r.range);
                if (r.openParaAlt != 0.0f) rm.put("OpenParaAlt", r.openParaAlt);
            }
            root.put("Racks", racks);
        }

        // RideRack mapping
        if (info.rideRacks != null && !info.rideRacks.isEmpty()) {
            Map<String, Object> ride = new LinkedHashMap<>();
            for (MCH_AircraftInfo.RideRack rr : info.rideRacks) {
                ride.put(rr.name, rr.rackID);
            }
            root.put("RideRack", ride);
        }

        // Bounding Boxes
        if (info.extraBoundingBox != null && !info.extraBoundingBox.isEmpty()) {
            List<Map<String, Object>> boxes = new ArrayList<>();
            for (MCH_BoundingBox bb : info.extraBoundingBox) {
                Map<String, Object> bm = new LinkedHashMap<>();
                bm.put("Position", inline(bb.offsetX, bb.offsetY, bb.offsetZ));
                bm.put("Size", inline(bb.width, bb.height, bb.widthZ));
                if (bb.getDamageFactor() != 1.0f) bm.put("DamageFactor", bb.getDamageFactor());
                if (bb.name != null && !bb.name.isEmpty()) bm.put("Name", bb.name);
                if (bb.boundingBoxType != null) bm.put("Type", bb.boundingBoxType.name());
                boxes.add(bm);
            }
            root.put("BoundingBoxes", boxes);
        }

        // Wheels
        if (info.wheels != null && !info.wheels.isEmpty()) {
            Map<String, Object> wheels = new LinkedHashMap<>();
            List<Map<String, Object>> hitboxes = new ArrayList<>();
            for (MCH_AircraftInfo.Wheel w : info.wheels) {
                Map<String, Object> wm = new LinkedHashMap<>();
                wm.put("Position", vec(w.pos));
                if (w.size != 1.0f) wm.put("Scale", w.size);
                hitboxes.add(wm);
            }
            wheels.put("Hitboxes", hitboxes);
            wheels.put("WheelRotation", info.partWheelRot);
            wheels.put("TrackRotation", info.trackRollerRot);
            root.put("Wheels", wheels);
        }

        // UAV
        Map<String, Object> uav = new LinkedHashMap<>();
        uav.put("IsUav", info.isUAV);
        uav.put("IsSmallUav", info.isSmallUAV);
        uav.put("IsNewUav", info.isNewUAV);
        uav.put("IsTargetDrone", info.isTargetDrone);
        root.put("Uav", uav);

        // Aircraft features
        Map<String, Object> feats = new LinkedHashMap<>();
        feats.put("GunnerMode", info.isEnableGunnerMode);
        feats.put("InventorySize", info.inventorySize);
        feats.put("NightVision", info.isEnableNightVision);
        feats.put("EntityRadar", info.isEnableEntityRadar);
        feats.put("CanReverse", info.enableBack);
        feats.put("CanRotateOnGround", info.canRotOnGround);
        feats.put("ConcurrentGunner", info.isEnableConcurrentGunnerMode);
        feats.put("EjectionSeat", info.isEnableEjectionSeat);
        feats.put("ThrottleUpDown", info.throttleUpDown);
        feats.put("ThrottleUpDownEntity", info.throttleUpDownOnEntity);
        if (info.isEnableParachuting) {
            Map<String, Object> parachute = new LinkedHashMap<>();
            if (info.mobDropOption != null) {
                if (info.mobDropOption.pos != null) parachute.put("Pos", vec(info.mobDropOption.pos));
                if (info.mobDropOption.interval != 0) parachute.put("Interval", info.mobDropOption.interval);
            }
            if (parachute.isEmpty()) feats.put("Parachuting", true);
            else feats.put("Parachuting", parachute);
        }
        if (info.flare != null) {
            Map<String, Object> flare = new LinkedHashMap<>();
            flare.put("Pos", vec(info.flare.pos));
            if (info.flare.types != null && info.flare.types.length > 0) {
                List<String> types = new ArrayList<>();
                for (int t : info.flare.types) {
                    String ft = flareTypeFromInt(t);
                    if (ft != null) types.add(ft);
                }
                if (!types.isEmpty()) flare.put("Types", types);
            }
            feats.put("Flare", flare);
        }
        root.put("AircraftFeatures", feats);

        return root;
    }

    private Map<String, Object> drawnPart(MCH_AircraftInfo.DrawnPart p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("Position", vec(p.pos));
        if (p.rot != Vec3d.ZERO) m.put("Rotation", vec(p.rot));
        if (notBlank(p.modelName)) m.put("PartName", p.modelName);
        return m;
    }

    private void addCommonComponents(Map<String, List<Map<String, Object>>> components, MCH_AircraftInfo info) {
        if (info.cameraList != null && !info.cameraList.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (MCH_AircraftInfo.Camera c : info.cameraList) {
                Map<String, Object> m = drawnPart(c);
                if (!c.yawSync) m.put("YawSync", false);
                if (c.pitchSync) m.put("PitchSync", true);
                list.add(m);
            }
            components.put("Camera", list);
        }
        if (info.canopyList != null && !info.canopyList.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (MCH_AircraftInfo.Canopy c : info.canopyList) {
                Map<String, Object> m = drawnPart(c);
                m.put("MaxRotation", c.maxRotFactor);
                if (c.isSlide) m.put("IsSliding", true);
                list.add(m);
            }
            components.put("Canopy", list);
        }
        if (info.hatchList != null && !info.hatchList.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (MCH_AircraftInfo.Hatch h : info.hatchList) {
                Map<String, Object> m = drawnPart(h);
                m.put("MaxRotation", h.maxRot);
                if (h.isSlide) m.put("IsSliding", true);
                list.add(m);
            }
            components.put("Hatch", list);
        }
        if (info.lightHatchList != null && !info.lightHatchList.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (MCH_AircraftInfo.Hatch h : info.lightHatchList) {
                Map<String, Object> m = drawnPart(h);
                m.put("MaxRotation", h.maxRot);
                if (h.isSlide) m.put("IsSliding", true);
                list.add(m);
            }
            components.put("LightHatch", list);
        }
        if (info.partWeaponBay != null && !info.partWeaponBay.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (MCH_AircraftInfo.WeaponBay wb : info.partWeaponBay) {
                String weaponNameStr = wb.weaponName;
                if (!notBlank(weaponNameStr)) {
                    if (wb.weaponIds != null && wb.weaponIds.length > 0) {
                        List<String> names = new ArrayList<>();
                        for (Integer id : wb.weaponIds) {
                            MCH_AircraftInfo.WeaponSet ws = info.getWeaponSetById(id);
                            if (ws != null && notBlank(ws.type)) names.add(ws.type);
                        }
                        if (!names.isEmpty()) weaponNameStr = String.join(" / ", names);
                    }
                }
                if (!notBlank(weaponNameStr)) continue;
                Map<String, Object> m = drawnPart(wb);
                m.put("WeaponName", weaponNameStr);
                m.put("MaxRotation", wb.maxRotFactor * 90.0F);
                if (wb.isSlide) m.put("IsSliding", true);
                list.add(m);
            }
            if (!list.isEmpty()) components.put("WeaponBay", list);
        }
        if (info.partRotPart != null && !info.partRotPart.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (MCH_AircraftInfo.RotPart r : info.partRotPart) {
                Map<String, Object> m = drawnPart(r);
                if (r.rotSpeed != 0) m.put("Speed", r.rotSpeed);
                if (r.rotAlways) m.put("AlwaysRotate", true);
                list.add(m);
            }
            components.put("Rotation", list);
        }
        if (info.partSteeringWheel != null && !info.partSteeringWheel.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (MCH_AircraftInfo.PartWheel w : info.partSteeringWheel) {
                Map<String, Object> m = drawnPart(w);
                m.put("Direction", w.rotDir);
                if (w.pos2 != null) m.put("Pivot", vec(w.pos2));
                list.add(m);
            }
            components.put("SteeringWheel", list);
        }
        if (info.partWheel != null && !info.partWheel.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (MCH_AircraftInfo.PartWheel w : info.partWheel) {
                Map<String, Object> m = drawnPart(w);
                m.put("Direction", w.rotDir);
                if (w.pos2 != null) m.put("Pivot", vec(w.pos2));
                list.add(m);
            }
            components.put("Wheel", list);
        }
        if (info.landingGear != null && !info.landingGear.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (MCH_AircraftInfo.LandingGear g : info.landingGear) {
                Map<String, Object> m = drawnPart(g);
                m.put("MaxRotation", g.maxRotFactor * 90.0F);
                if (g.reverse) m.put("IsReverse", true);
                if (g.hatch) m.put("IsHatch", true);
                if (g.enableRot2) {
                    m.put("ArticulatedRotation", vec(g.rot2));
                    m.put("MaxArticulatedRotation", g.maxRotFactor2 * 90.0F);
                }
                if (g.slide != null) m.put("SlideVec", vec(g.slide));
                list.add(m);
            }
            components.put("LandingGear", list);
        }
        if (info.partWeapon != null && !info.partWeapon.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (MCH_AircraftInfo.PartWeapon wp : info.partWeapon) {
                Map<String, Object> m = drawnPart(wp);
                if (wp.name != null && wp.name.length > 0) m.put("WeaponNames", String.join("/", wp.name));
                if (wp.rotBarrel) m.put("BarrelRot", true);
                if (wp.isMissile) m.put("IsMissile", true);
                if (wp.hideGM) m.put("HideGM", true);
                if (wp.yaw) m.put("Yaw", true);
                if (wp.pitch) m.put("Pitch", true);
                if (wp.recoilBuf != 0) m.put("RecoilBuf", wp.recoilBuf);
                if (wp.turret) m.put("Turret", true);
                if (wp.child != null && !wp.child.isEmpty()) {
                    List<Map<String, Object>> children = new ArrayList<>();
                    for (MCH_AircraftInfo.PartWeaponChild ch : wp.child) {
                        Map<String, Object> cm = new LinkedHashMap<>();
                        cm.put("Position", vec(ch.pos));
                        if (ch.rot != Vec3d.ZERO) cm.put("Rotation", vec(ch.rot));
                        if (ch.yaw) cm.put("Yaw", true);
                        if (ch.pitch) cm.put("Pitch", true);
                        if (ch.recoilBuf != 0) cm.put("RecoilBuf", ch.recoilBuf);
                        children.add(cm);
                    }
                    m.put("Children", children);
                }
                list.add(m);
            }
            components.put("Weapon", list);
        }
        if (info.searchLights != null && !info.searchLights.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (MCH_AircraftInfo.SearchLight sl : info.searchLights) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("Position", vec(sl.pos));
                m.put("ColorStart", toHexRGB(sl.colorStart));
                m.put("ColorEnd", toHexRGB(sl.colorEnd));
                m.put("Height", sl.height);
                m.put("Width", sl.width);
                if (sl.yaw != 0) m.put("Yaw", sl.yaw);
                if (sl.pitch != 0) m.put("Pitch", sl.pitch);
                if (sl.stRot != 0) m.put("StRot", sl.stRot);
                if (sl.fixDir) m.put("FixedDirection", true);
                if (sl.steering) m.put("Steering", true);
                list.add(m);
            }
            components.put("SearchLight", list);
        }
        if (info.partTrackRoller != null && !info.partTrackRoller.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (MCH_AircraftInfo.TrackRoller tr : info.partTrackRoller) list.add(drawnPart(tr));
            components.put("TrackRoller", list);
        }
        if (info.partThrottle != null && !info.partThrottle.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (MCH_AircraftInfo.Throttle th : info.partThrottle) {
                Map<String, Object> m = drawnPart(th);
                if (th.slide != null) m.put("SlidePos", vec(th.slide));
                if (th.rot2 != 0) m.put("MaxAngle", th.rot2);
                list.add(m);
            }
            components.put("Throttle", list);
        }
        if (info.partCrawlerTrack != null && !info.partCrawlerTrack.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (MCH_AircraftInfo.CrawlerTrack ct : info.partCrawlerTrack) {
                Map<String, Object> m = drawnPart(ct);
                if (ct.len != 0) m.put("SegmentLength", ct.len / 0.9F);
                if (ct.z != 0) m.put("ZOffset", ct.z);
                if (ct.cx != null && ct.cy != null) {
                    List<List<Double>> tl = new ArrayList<>();
                    for (int i = 0; i < Math.min(ct.cx.length, ct.cy.length); i++) {
                        tl.add(inline(ct.cx[i], ct.cy[i]));
                    }
                    if (!tl.isEmpty()) m.put("TrackList", tl);
                }
                list.add(m);
            }
            components.put("CrawlerTrack", list);
        }
        if (info instanceof MCH_HeliInfo hi) {
            if (hi.rotorList != null && !hi.rotorList.isEmpty()) {
                List<Map<String, Object>> list = new ArrayList<>();
                for (MCH_HeliInfo.Rotor r : hi.rotorList) {
                    Map<String, Object> m = drawnPart(r);
                    if (r.bladeNum != 0) m.put("BladeCount", r.bladeNum);
                    if (r.bladeRot != 0) m.put("BladeRot", r.bladeRot);
                    if (r.haveFoldFunc) m.put("CanFold", true);
                    if (r.oldRenderMethod) m.put("OldRenderer", true);
                    list.add(m);
                }
                components.put("HeliRotor", list);
            }
        }
    }

    private static final class InlineSeq<E> extends ArrayList<E> {
        InlineSeq(int cap) {
            super(cap);
        }
    }

    private static final class InlineAwareRepresenter extends Representer {
        public InlineAwareRepresenter(DumperOptions options) {
            super(options);
        }

        @Override
        protected Node representSequence(Tag tag, Iterable<?> sequence, DumperOptions.FlowStyle flowStyle) {
            if (sequence instanceof InlineSeq) {
                return super.representSequence(tag, sequence, DumperOptions.FlowStyle.FLOW);
            }
            return super.representSequence(tag, sequence, flowStyle);
        }
    }
}
