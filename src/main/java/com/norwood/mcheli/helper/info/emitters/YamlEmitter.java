package com.norwood.mcheli.helper.info.emitters;

import com.google.common.primitives.Floats;
import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_BoundingBox;
import com.norwood.mcheli.aircraft.MCH_SeatInfo;
import com.norwood.mcheli.aircraft.MCH_SeatRackInfo;
import com.norwood.mcheli.helicopter.MCH_HeliInfo;
import com.norwood.mcheli.helper.info.parsers.yaml.ComponentParser;
import com.norwood.mcheli.helper.info.parsers.yaml.HUDParser;
import com.norwood.mcheli.hud.*;
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
import java.util.stream.Collectors;

import static com.google.common.base.Predicates.instanceOf;

@SuppressWarnings("AutoBoxing")
public class YamlEmitter implements IEmitter {

    private static final Yaml YAML;

    static {
        DumperOptions opts = new DumperOptions();
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        opts.setPrettyFlow(true);
        InlineAwareRepresenter rep = new InlineAwareRepresenter(opts);
        rep.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setWidth(Integer.MAX_VALUE);
        options.setSplitLines(false);
        YAML = new Yaml(new InlineAwareRepresenter(options), options);

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

    private static <T> InlineSeq<T> inlineSeq(Collection<T> vals) {
        InlineSeq<T> seq = new InlineSeq<>(vals.size());
        seq.addAll(vals);
        return seq;
    }

    private static double round3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private static float round3(float value) {
        return Math.round(value * 1000.0f) / 1000.0f;
    }

    private static InlineSeq<Double> inline(double... values) {
        InlineSeq<Double> seq = new InlineSeq<>(values.length);
        for (double v : values) seq.add(round3(v));
        return seq;
    }

    private static InlineSeq<String> inline(String... values) {
        InlineSeq<String> seq = new InlineSeq<>(values.length);
        for (String v : values) {
            seq.add(v);
        }
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

    private static String targetToString(int flags) {
        if ((flags & 64) != 0) return "Block";
        List<String> parts = new ArrayList<>();
        if ((flags & 32) != 0) parts.add("Planes");
        if ((flags & 16) != 0) parts.add("Helicopters");
        if ((flags & 8) != 0) parts.add("Vehicles");
        if ((flags & 4) != 0) parts.add("Players");
        if ((flags & 2) != 0) parts.add("Monsters");
        if ((flags & 1) != 0) parts.add("Others");
        return String.join(",", parts);
    }

    @Override
    public String emitHelicopter(MCH_HeliInfo info) {
        MCH_HeliInfo dummyInfo = new MCH_HeliInfo(info.getLocation(), info.getContentPath());
        Map<String, Object> root = baseAircraft(info, dummyInfo);
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
        var dummyInfo = new MCH_PlaneInfo(info.getLocation(), info.getContentPath());
        Map<String, Object> root = baseAircraft(info, dummyInfo);
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
                if (r.maxRotFactor != 0) m.put("RotFactor", r.maxRotFactor * 90.0F);
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
        var dummyInfo = new MCH_ShipInfo(info.getLocation(), info.getContentPath());
        Map<String, Object> root = baseAircraft(info, dummyInfo);
        Map<String, List<Map<String, Object>>> components = new LinkedHashMap<>();
        addCommonComponents(components, info);
        if (!components.isEmpty()) root.put("Components", components);
        return YAML.dump(root);
    }

    @Override
    public String emitTank(MCH_TankInfo info) {
        var dummyInfo = new MCH_TankInfo(info.getLocation(), info.getContentPath());
        Map<String, Object> root = baseAircraft(info, dummyInfo);
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
        var dummyInfo = new MCH_VehicleInfo(info.getLocation(), info.getContentPath());
        Map<String, Object> root = baseAircraft(info, dummyInfo);
        Map<String, Object> veh = new LinkedHashMap<>();
        if (info.isEnableMove != dummyInfo.isEnableMove)
            veh.put("CanMove", info.isEnableMove);
        if (info.isEnableRot != dummyInfo.isEnableRot)
            veh.put("CanRotate", info.isEnableRot);
        if (!veh.isEmpty())
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
        if (info.power != 0) root.put("BaseDamage", info.power);

        // Ballistics
        Map<String, Object> ball = new LinkedHashMap<>();
        ball.put("Acceleration", info.acceleration);
        ball.put("AccelerationInWater", info.accelerationInWater);
        ball.put("Gravity", info.gravity);
        ball.put("GravityInWater", info.gravityInWater);
        ball.put("VelocityInWater", info.velocityInWater);
        if (info.speedFactor != 0.0f) ball.put("SpeedFactor", info.speedFactor);
        if (info.speedFactorStartTick != 0) ball.put("SpeedFactorStartTick", info.speedFactorStartTick);
        if (info.speedFactorEndTick != 0) ball.put("SpeedFactorEndTick", info.speedFactorEndTick);
        if (info.speedDependsAircraft) ball.put("SpeedDependsAircraft", true);
        if (info.piercing != 0) ball.put("Piercing", info.piercing);
        if (info.flakParticlesDiff != 0.3F) ball.put("FlakSpread", info.flakParticlesDiff);
        root.put("Ballistics", ball);

        // Sound
        Map<String, Object> snd = new LinkedHashMap<>();
        if (info.soundDelay != 0) snd.put("Delay", info.soundDelay);
        snd.put("Volume", info.soundVolume);
        snd.put("Pitch", info.soundPitch);
        if (info.soundPitchRandom != 0.0f) snd.put("PitchRandom", info.soundPitchRandom);
        if (info.hitSoundRange != 100) snd.put("HitSoundRange", info.hitSoundRange);
        if (notBlank(info.soundFileName)) snd.put("Name", info.soundFileName.toLowerCase(Locale.ROOT));
        Map<String, Object> sndLoc = new LinkedHashMap<>();
        if (notBlank(info.hitSound)) sndLoc.put("Hit", info.hitSound.toLowerCase(Locale.ROOT));
        if (notBlank(info.hitSoundIron)) sndLoc.put("HitMetal", info.hitSoundIron.toLowerCase(Locale.ROOT));
        if (notBlank(info.railgunSound)) sndLoc.put("Railgun", info.railgunSound.toLowerCase(Locale.ROOT));
        if (notBlank(info.weaponSwitchSound))
            sndLoc.put("WeaponSwitch", info.weaponSwitchSound.toLowerCase(Locale.ROOT));
        if (!sndLoc.isEmpty()) snd.put("Locations", sndLoc);
        if (!snd.isEmpty()) root.put("Sound", snd);

        // Explosion
        Map<String, Object> expl = new LinkedHashMap<>();
        if (info.explosion != 0) expl.put("Power", info.explosion);
        if (notBlank(info.explosionType)) expl.put("Type", info.explosionType);
        if (info.explosionBlock >= 0) expl.put("DestructionPower", info.explosionBlock);
        if (info.explosionInWater != 0) expl.put("PowerUnderwater", info.explosionInWater);
        if (info.explosionAltitude != 0) expl.put("ExplosionAltitude", info.explosionAltitude);
        if (info.timeFuse != 0) expl.put("FuseTime", info.timeFuse);
        if (info.delayFuse != 0) expl.put("FuseDelay", info.delayFuse);
        if (info.bound != 0.0f) expl.put("FuseRebound", info.bound);
        if (info.flaming) expl.put("Flaming", true);
        if (info.canAirburst) expl.put("CanAirburst", true);
        if (info.explosionAirburst > 0) expl.put("ExplosionAirburst", info.explosionAirburst);
        if (info.proximityFuseDist != 0.0F) expl.put("ProximityFuseDist", info.proximityFuseDist);
        expl.put("CanDestroyBlocks", !info.disableDestroyBlock);
        if (!expl.isEmpty()) root.put("Explosion", expl);

        // Camera
        Map<String, Object> cam = new LinkedHashMap<>();
        if (info.hasMortarRadar) cam.put("EnableMortarRadar", true);
        if (info.mortarRadarMaxDist != -1) cam.put("MortarRadarMaxDist", info.mortarRadarMaxDist);
        if (info.displayMortarDistance) cam.put("DisplayMortarDistance", true);
        if (info.fixCameraPitch) cam.put("FixPitch", true);
        if (info.cameraRotationSpeedPitch != 1.0F) cam.put("RotationSpeedPitch", info.cameraRotationSpeedPitch);
        String sight = sightToString(info.sight);
        if (sight != null) cam.put("Sight", sight);
        if (info.zoom != null && info.zoom.length > 0) cam.put("Zoom", Floats.asList(info.zoom));
        if (!cam.isEmpty()) root.put("Camera", cam);

        // Missile
        Map<String, Object> missile = new LinkedHashMap<>();
        if (info.laserGuidance) missile.put("LaserGuidance", true);
        if (!info.hasLaserGuidancePod) missile.put("HasLaserGuidancePod", false);
        if (!info.enableOffAxis) missile.put("IsOffAxis", info.enableOffAxis);
        if (info.isGuidedTorpedo) missile.put("GuidedTorpedo", true);
        if (!info.predictTargetPos) missile.put("PredictTargetPos", false);
        if (info.enableBVR) missile.put("EnableBVR", true);
        if (info.minRangeBVR != 300) missile.put("MinRangeBVR", info.minRangeBVR);
        if (info.scanInterval != 20) missile.put("ScanInterval", info.scanInterval);
        if (info.tickEndHoming != -1) missile.put("TickEndHoming", info.tickEndHoming);
        if (info.pdHDNMaxDegree != 1000.0f) missile.put("PDHDNMaxDegree", info.pdHDNMaxDegree);
        if (info.pdHDNMaxDegreeLockOutCount != 10)
            missile.put("PDHDNMaxDegreeLockOutCount", info.pdHDNMaxDegreeLockOutCount);
        if (info.turningFactor != 0.5) missile.put("TurningFactor", info.turningFactor);
        if (info.maxDegreeOfMissile != 60) missile.put("MaxDegreeOfMissile", info.maxDegreeOfMissile);
        if (info.canBeIntercepted) missile.put("CanBeIntercepted", true);

        // LockOn
        Map<String, Object> lockOn = new LinkedHashMap<>();
        if (info.canLockMissile) lockOn.put("CanLockMissile", true);
        if (info.lockTime != 30) lockOn.put("Time", info.lockTime);
        if (info.rigidityTime != 7) lockOn.put("Delay", info.rigidityTime);
        if (info.maxLockOnRange != 300) lockOn.put("MaxRange", info.maxLockOnRange);
        if (info.maxLockOnAngle != 10) lockOn.put("MaxAngle", info.maxLockOnAngle);
        if (info.lockMinHeight != 25) lockOn.put("MinHeight", info.lockMinHeight);
        if (info.passiveRadarLockOutCount != 20) lockOn.put("PassiveRadarLockOutCount", info.passiveRadarLockOutCount);
        if (info.numLockedChaffMax != 2) lockOn.put("LockedChaffMax", info.numLockedChaffMax);
        if (!lockOn.isEmpty()) missile.put("LockOn", lockOn);

        // Radar
        Map<String, Object> radar = new LinkedHashMap<>();
        if (info.isRadarMissile) radar.put("IsRadarMissile", true);
        if (info.activeRadar) radar.put("ActiveRadar", true);
        if (info.passiveRadar) radar.put("PassiveRadar", true);
        if (!radar.isEmpty()) missile.put("Radar", radar);

        // Heat
        Map<String, Object> heat = new LinkedHashMap<>();
        if (!info.isHeatSeekerMissile) heat.put("isHeatMissile", false);
        if (info.heatCount != 0) heat.put("HeatCount", info.heatCount);
        if (info.maxHeatCount != 0) heat.put("MaxHeatCount", info.maxHeatCount);
        if (info.antiFlareCount != -1) heat.put("AntiFlareCount", info.antiFlareCount);
        if (!heat.isEmpty()) missile.put("Heat", heat);

        // Marker Rocket params under missile
        Map<String, Object> marker = new LinkedHashMap<>();
        if (info.markerRocketSpawnNum != 5) marker.put("Count", info.markerRocketSpawnNum);
        if (info.markerRocketSpawnDiff != 15) marker.put("Spread", info.markerRocketSpawnDiff);
        if (info.markerRocketSpawnHeight != 200) marker.put("SpawnHeight", info.markerRocketSpawnHeight);
        if (info.markerRocketSpawnSpeed != 5) marker.put("Acceleration", info.markerRocketSpawnSpeed);
        if (!marker.isEmpty()) missile.put("MarkerRocket", marker);

        if (!missile.isEmpty()) root.put("Missile", missile);

        // Ammo
        Map<String, Object> ammo = new LinkedHashMap<>();
        if (info.reloadTime != 30) ammo.put("ReloadTime", info.reloadTime);
        if (info.round != 0) ammo.put("MagSize", info.round);
        if (info.maxAmmo != 0) ammo.put("MaxAmmo", info.maxAmmo);
        if (info.suppliedNum != 1) ammo.put("ResupplyCount", info.suppliedNum);
        if (info.roundItems != null && !info.roundItems.isEmpty()) {
            List<Map<String, Object>> rounds = new ArrayList<>();
            for (MCH_WeaponInfo.RoundItem ri : info.roundItems) {
                Map<String, Object> rm = new LinkedHashMap<>();
                rm.put("Count", ri.num);
                rm.put("Name", ri.itemName.toString().toLowerCase(Locale.ROOT));
                rm.put("Meta", ri.damage);
                rounds.add(rm);
            }
            ammo.put("AmmoItems", rounds);
        }
        if (!ammo.isEmpty()) root.put("Ammo", ammo);
        if (info.delay != 0) root.put("FireCooldown", info.delay);

        // Top-level toggles and misc
        if (info.target != 0) root.put("Target", targetToString(info.target));
        if (info.modeNum != 0) root.put("Mode", info.modeNum);
        if (info.fixMode != 0) root.put("FixMode", info.fixMode);
        if (info.ridableOnly) root.put("RidableOnly", true);
        if (info.accuracy != 0.0F) root.put("Accuracy", info.accuracy);
        if (info.destruct) root.put("SelfDestruct", true);
        if (info.enableChunkLoader) root.put("CanLoadChunks", true);
        if (info.weaponSwitchCount != 0) root.put("SwitchCooldown", info.weaponSwitchCount);
        if (info.crossType != 0) root.put("CrossType", info.crossType);

        // HBM compat
        Map<String, Object> ntm = new LinkedHashMap<>();
        if (info.payloadNTM != null && info.payloadNTM != MCH_WeaponInfo.Payload.NONE) {
            ntm.put("PayloadType", info.payloadNTM.name());
        }
        if (info.effectOnly) ntm.put("EffectOnly", true);
        if (info.fluidTypeNTM != null) ntm.put("FluidType", info.fluidTypeNTM);
        if (!ntm.isEmpty()) root.put("NTM", ntm);

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
        if (notBlank(info.trajectoryParticleName)) render.put("TrajectoryParticle", info.trajectoryParticleName);
        if (info.trajectoryParticleStartTick != 0)
            render.put("TrajectoryParticleStartTick", info.trajectoryParticleStartTick);
        if (info.flakParticlesCrack != 10) render.put("FlakParticlesCrack", info.flakParticlesCrack);
        if (info.numParticlesFlak != 3) render.put("ParticlesFlak", info.numParticlesFlak);
        // Smoke sub-block
        Map<String, Object> smoke = new LinkedHashMap<>();
        if (info.disableSmoke) smoke.put("DisableSmoke", true);
        if (info.smokeSize != 2.0F) smoke.put("SmokeSize", info.smokeSize);
        if (info.smokeNum != 1) smoke.put("SmokeNum", info.smokeNum);
        if (info.smokeMaxAge != 100) smoke.put("SmokeMaxAge", info.smokeMaxAge);
        if (info.colorInWater != null) {
            int r = (int) Math.round(Math.max(0, Math.min(1, info.colorInWater.r)) * 255.0);
            int g = (int) Math.round(Math.max(0, Math.min(1, info.colorInWater.g)) * 255.0);
            int b = (int) Math.round(Math.max(0, Math.min(1, info.colorInWater.b)) * 255.0);
            int rgb = (r << 16) | (g << 8) | b;
            smoke.put("BulletColorInWater", String.format("#%06X", rgb));
        }
        if (info.color != null) {
            int r2 = (int) Math.round(Math.max(0, Math.min(1, info.color.r)) * 255.0);
            int g2 = (int) Math.round(Math.max(0, Math.min(1, info.color.g)) * 255.0);
            int b2 = (int) Math.round(Math.max(0, Math.min(1, info.color.b)) * 255.0);
            int rgb2 = (r2 << 16) | (g2 << 8) | b2;
            smoke.put("BulletColor", String.format("#%06X", rgb2));
        }
        if (!smoke.isEmpty()) render.put("Smoke", smoke);
        if (!render.isEmpty()) root.put("Render", render);

        return YAML.dump(root);
    }

    @Override
    public String emitThrowable(MCH_ThrowableInfo info) {
        Map<String, Object> root = new LinkedHashMap<>();
        var dummy = new MCH_ThrowableInfo(info.location,info.filePath); //Contains default values

        // DisplayName
        if (!Objects.equals(info.displayName, dummy.displayName) || !info.displayNameLang.isEmpty()) {
            if (info.displayNameLang.isEmpty()) {
                root.put("DisplayName", info.displayName);
            } else {
                Map<String, String> nameMap = new LinkedHashMap<>(info.displayNameLang);
                nameMap.put("DEFAULT", info.displayName);
                root.put("DisplayName", nameMap);
            }
        }

        // ItemID
        if (info.itemID != dummy.itemID) root.put("ItemID", info.itemID);

        // Sound
        Map<String, Object> soundMap = new LinkedHashMap<>();
        if (info.soundVolume != dummy.soundVolume) soundMap.put("Volume", info.soundVolume);
        if (info.soundPitch != dummy.soundPitch) soundMap.put("Pitch", info.soundPitch);
        if (!soundMap.isEmpty()) root.put("Sound", soundMap);

        // Recepie
        if (info.isShapedRecipe != dummy.isShapedRecipe || info.recipeString != null && !info.recipeString.isEmpty()) {
            Map<String, Object> recipeMap = new LinkedHashMap<>();
            recipeMap.put("isShaped", info.isShapedRecipe);
            if (info.recipeString != null && !info.recipeString.isEmpty()) {
                List<String> pattern = new ArrayList<>();
                for (String s : info.recipeString) {
                    pattern.add(s.trim().toUpperCase());
                }
                recipeMap.put("Pattern", pattern);
            }
            root.put("Recepie", recipeMap);
        }

        // Numeric & boolean fields
        if (info.power != dummy.power) root.put("Power", info.power);
        if (info.acceleration != dummy.acceleration) root.put("Acceleration", info.acceleration);
        if (info.accelerationInWater != dummy.accelerationInWater) root.put("AccelerationInWater", info.accelerationInWater);
        if (info.dispenseAcceleration != dummy.dispenseAcceleration) root.put("DispenseAcceleration", info.dispenseAcceleration);
        if (info.explosion != dummy.explosion) root.put("Explosion", info.explosion);
        if (info.delayFuse != dummy.delayFuse) root.put("DelayFuse", info.delayFuse);
        if (info.bound != dummy.bound) root.put("Bound", info.bound);
        if (info.timeFuse != dummy.timeFuse) root.put("TimeFuse", info.timeFuse);
        if (info.flaming != dummy.flaming) root.put("Flaming", info.flaming);
        if (info.stackSize != dummy.stackSize) root.put("StackSize", info.stackSize);
        if (info.proximityFuseDist != dummy.proximityFuseDist) root.put("ProximityFuseDist", info.proximityFuseDist);
        if (info.accuracy != dummy.accuracy) root.put("Accuracy", info.accuracy);
        if (info.aliveTime != dummy.aliveTime) root.put("AliveTime", info.aliveTime);
        if (info.bomblet != dummy.bomblet) root.put("Bomblet", info.bomblet);
        if (info.bombletDiff != dummy.bombletDiff) root.put("BombletSpread", info.bombletDiff);
        if (info.gravity != dummy.gravity) root.put("Gravity", info.gravity);
        if (info.gravityInWater != dummy.gravityInWater) root.put("GravityInWater", info.gravityInWater);

        // Particle
        if (!Objects.equals(info.particleName, dummy.particleName)) root.put("Particle", info.particleName);

        // Smoke
        if (info.disableSmoke != dummy.disableSmoke || info.smokeSize != dummy.smokeSize
                || info.smokeNum != dummy.smokeNum || info.smokeColor != null
                || info.smokeVelocityHorizontal != dummy.smokeVelocityHorizontal
                || info.smokeVelocityVertical != dummy.smokeVelocityVertical) {
            Map<String, Object> smokeMap = new LinkedHashMap<>();
            if (info.disableSmoke != dummy.disableSmoke) smokeMap.put("DisableSmoke", info.disableSmoke);
            if (info.smokeSize != dummy.smokeSize) smokeMap.put("Size", info.smokeSize);
            if (info.smokeNum != dummy.smokeNum) smokeMap.put("Count", info.smokeNum);
            if (info.smokeColor != null) smokeMap.put("Color", info.smokeColor.toHexString());

            Map<String, Object> velMap = new LinkedHashMap<>();
            if (info.smokeVelocityVertical != dummy.smokeVelocityVertical) velMap.put("Vertical", info.smokeVelocityVertical);
            if (info.smokeVelocityHorizontal != dummy.smokeVelocityHorizontal) velMap.put("Horizontal", info.smokeVelocityHorizontal);
            if (!velMap.isEmpty()) smokeMap.put("Velocity", velMap);

            root.put("Smoke", smokeMap);
        }

        return YAML.dump(root);
    }

    @Override
    public String emitHud(MCH_Hud hud) {
        List<Object> rootList = new ArrayList<>();
        ListIterator<MCH_HudItem> iterator = hud.list.listIterator();

        while (iterator.hasNext()) {
            MCH_HudItem item = iterator.next();

            if (item instanceof MCH_HudItemConditional conditional && !conditional.isEndif()) {
                Map<String, Object> conditionalMap = new LinkedHashMap<>();
                conditionalMap.put("If", conditional.getConditional());
                conditionalMap.put("Do", collectConditionalList(iterator));
                rootList.add(conditionalMap);
            } else if (!(item instanceof MCH_HudItemConditional conditional && conditional.isEndif())) {
                Map.Entry<String, Object> entry = getHudEntry(item);
                if (entry != null) {
                    Map<String, Object> itemMap = new LinkedHashMap<>();
                    itemMap.put(entry.getKey(), entry.getValue());
                    rootList.add(itemMap);
                }
            }
        }

        return YAML.dump(rootList);
    }

    private List<Object> collectConditionalList(Iterator<MCH_HudItem> iterator) {
        List<Object> list = new ArrayList<>();

        while (iterator.hasNext()) {
            MCH_HudItem item = iterator.next();

            if (item instanceof MCH_HudItemConditional conditional && conditional.isEndif()) {
                break;
            }

            // Handle nested conditionals
            if (item instanceof MCH_HudItemConditional nested && !nested.isEndif()) {
                Map<String, Object> nestedMap = new LinkedHashMap<>();
                nestedMap.put("If", nested.getConditional());
                nestedMap.put("Do", collectConditionalList(iterator));
                list.add(nestedMap);
            } else {
                Map.Entry<String, Object> entry = getHudEntry(item);
                if (entry != null) {
                    Map<String, Object> itemMap = new LinkedHashMap<>();
                    itemMap.put(entry.getKey(), entry.getValue());
                    list.add(itemMap);
                }
            }
        }

        return list;
    }


    public Map.Entry<String, Object> getHudEntry(MCH_HudItem hudItem) {
        if (hudItem instanceof MCH_HudItemColor) {
            MCH_HudItemColor color = (MCH_HudItemColor) hudItem;
            return new AbstractMap.SimpleEntry<>("Color", color.getUpdateColor());
        }

        if (hudItem instanceof MCH_HudItemTexture) {
            MCH_HudItemTexture texture = (MCH_HudItemTexture) hudItem;
            Map<String, Object> textureSettings = new LinkedHashMap<>();
            textureSettings.put("Name", texture.getName());
            textureSettings.put("Position", inline(texture.getLeft(), texture.getTop()));
            textureSettings.put("Size", inline(texture.getWidth(), texture.getHeight()));
            textureSettings.put("UVPos", inline(texture.getULeft(), texture.getVTop()));
            textureSettings.put("UVSize", inline(texture.getUWidth(), texture.getVHeight()));
            textureSettings.put("Rotation", texture.getRot());
            return new AbstractMap.SimpleEntry<>("DrawTexture", textureSettings);
        }

        if (hudItem instanceof MCH_HudItemString) {
            MCH_HudItemString string = (MCH_HudItemString) hudItem;
            Map<String, Object> stringSettings = new LinkedHashMap<>();
            stringSettings.put("Format", string.getFormat());
            stringSettings.put("Position", inline(string.getPosX(), string.getPosY()));
            stringSettings.put("Arguments", inline(
                    Arrays.stream(string.getArgs())
                            .map(x -> x.name().toLowerCase())
                            .toArray(String[]::new)
            ));
            stringSettings.put("Center", string.isCenteredString());
            return new AbstractMap.SimpleEntry<>("DrawString", stringSettings);
        }

        if (hudItem instanceof MCH_HudItemCameraRot) {
            MCH_HudItemCameraRot cameraRot = (MCH_HudItemCameraRot) hudItem;
            Map<String, Object> rotSettings = new LinkedHashMap<>();
            rotSettings.put("Position", inline(cameraRot.getDrawPosX(), cameraRot.getDrawPosY()));
            return new AbstractMap.SimpleEntry<>("CameraRotation", rotSettings);
        }

        if (hudItem instanceof MCH_HudItemRect) {
            MCH_HudItemRect rect = (MCH_HudItemRect) hudItem;
            Map<String, Object> rectSettings = new LinkedHashMap<>();
            rectSettings.put("Position", inline(rect.getLeft(), rect.getTop()));
            rectSettings.put("Size", inline(rect.getWidth(), rect.getHeight()));
            return new AbstractMap.SimpleEntry<>("DrawRectangle", rectSettings);
        }

        if (hudItem instanceof MCH_HudItemLine) {
            MCH_HudItemLine line = (MCH_HudItemLine) hudItem;
            Map<String, Object> lineSettings = new LinkedHashMap<>();
            String[] posArray = line.getPos();

            List<List<String>> positions = new ArrayList<>();
            for (int i = 0; i < posArray.length; i += 2) {
                positions.add(
                        inline(posArray[i], posArray[i + 1])
                );
            }

            lineSettings.put("Position", positions);
            lineSettings.put("Striped", false);
            return new AbstractMap.SimpleEntry<>("DrawLine", lineSettings);
        }

        if (hudItem instanceof MCH_HudItemLineStipple) {
            MCH_HudItemLineStipple line = (MCH_HudItemLineStipple) hudItem;
            Map<String, Object> lineSettings = new LinkedHashMap<>();
            String[] posArray = line.getPos();

            List<List<String>> positions = new ArrayList<>();
            for (int i = 0; i < posArray.length; i += 2) {
                positions.add(
                        inline(posArray[i], posArray[i + 1])
                );
            }

            lineSettings.put("Position", positions);
            lineSettings.put("Striped", true);
            return new AbstractMap.SimpleEntry<>("DrawLine", lineSettings);
        }

        if (hudItem instanceof MCH_HudItemCall) {
            MCH_HudItemCall call = (MCH_HudItemCall) hudItem;
            return new AbstractMap.SimpleEntry<>("Call", call.getHudName());
        }

        if (hudItem instanceof MCH_HudItemRadar) {
            MCH_HudItemRadar radar = (MCH_HudItemRadar) hudItem;
            Map<String, Object> radarSettings = new LinkedHashMap<>();
            radarSettings.put("Position", inline(radar.getLeft(), radar.getTop()));
            radarSettings.put("Size", inline(radar.getWidth(), radar.getHeight()));
            radarSettings.put("Rotation", radar.getRot());
            radarSettings.put("EntityRadar", radar.isEntityRadar());
            return new AbstractMap.SimpleEntry<>("DrawRadar", radarSettings);
        }

        if (hudItem instanceof MCH_HudItemGraduation) {
            MCH_HudItemGraduation grad = (MCH_HudItemGraduation) hudItem;
            Map<String, Object> gradSettings = new LinkedHashMap<>();

            HUDParser.GraduationType typeEnum = grad.getType() >= 0
                    ? HUDParser.GraduationType.values()[grad.getType()]
                    : null;
            if (typeEnum != null) {
                gradSettings.put("Type", typeEnum.name());
            }

            gradSettings.put("Position", inline(grad.getDrawPosX(), grad.getDrawPosY()));
            gradSettings.put("Rotation", grad.getDrawRot());
            gradSettings.put("Roll", grad.getDrawRoll());

            return new AbstractMap.SimpleEntry<>("DrawGraduation", gradSettings);
        }

        return null;
    }


    @Override
    public String emitItem(MCH_ItemInfo info) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("DisplayName", info.displayName);
        return YAML.dump(root);
    }

    private float convertRotorSpeed(float speed) {
        float rounded = round3(speed);
        if (rounded > 0.01F) rounded -= 0.01F;
        if (rounded < -0.01F) rounded += 0.01F;
        return rounded;
    }

    private Map<String, Object> baseAircraft(MCH_AircraftInfo info, MCH_AircraftInfo dummyInfo) {

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

        if (info.canRide != dummyInfo.canRide) root.put("CanRide", info.canRide);
        if (info.rotorSpeed != dummyInfo.rotorSpeed) root.put("RotorSpeed", convertRotorSpeed(info.rotorSpeed));
        if (!info.turretPosition.equals(Vec3d.ZERO)) root.put("TurretPosition", vec(info.turretPosition));
        if (info.unmountPosition != null) root.put("GlobalUnmountPos", vec(info.unmountPosition));
        if (info.creativeOnly != dummyInfo.creativeOnly) root.put("CreativeOnly", info.creativeOnly);
        if (info.regeneration != dummyInfo.regeneration) root.put("Regeneration", info.regeneration);
        if (info.invulnerable != dummyInfo.invulnerable) root.put("Invulnerable", info.invulnerable);
        if (info.maxFuel != dummyInfo.maxFuel) root.put("MaxFuel", info.maxFuel);
        if (info.maxHp != dummyInfo.maxHp) root.put("MaxHP", info.maxHp);
        if (info.stealth != dummyInfo.stealth) root.put("Stealth", info.stealth);
        if (info.fuelConsumption != dummyInfo.fuelConsumption) root.put("FuelConsumption", info.fuelConsumption);
        if (info.fuelSupplyRange != dummyInfo.fuelSupplyRange) root.put("FuelSupplyRange", info.fuelSupplyRange);
        if (info.ammoSupplyRange != dummyInfo.ammoSupplyRange) root.put("AmmoSupplyRange", info.ammoSupplyRange);

        if (info.repairOtherVehiclesRange != 0.0F || info.repairOtherVehiclesValue != 0) {
            Map<String, Object> repair = new LinkedHashMap<>();
            repair.put("range", info.repairOtherVehiclesRange);
            repair.put("value", info.repairOtherVehiclesValue);
            root.put("RepairOtherVehicles", repair);
        }
        if (notBlank(info.nameOnModernAARadar) && !info.nameOnModernAARadar.equals(dummyInfo.nameOnModernAARadar))
            root.put("NameOnModernAARadar", info.nameOnModernAARadar);

        if (notBlank(info.nameOnEarlyAARadar) && !info.nameOnEarlyAARadar.equals(dummyInfo.nameOnEarlyAARadar))
            root.put("NameOnEarlyAARadar", info.nameOnEarlyAARadar);

        if (notBlank(info.nameOnModernASRadar) && !info.nameOnModernASRadar.equals(dummyInfo.nameOnModernASRadar))
            root.put("NameOnModernASRadar", info.nameOnModernASRadar);

        if (notBlank(info.nameOnEarlyASRadar) && !info.nameOnEarlyASRadar.equals(dummyInfo.nameOnEarlyASRadar))
            root.put("NameOnEarlyASRadar", info.nameOnEarlyASRadar);

        if (info.explosionSizeByCrash != dummyInfo.explosionSizeByCrash)
            root.put("ExplosionSizeByCrash", (int) info.explosionSizeByCrash);
        if (info.throttleDownFactor != dummyInfo.explosionSizeByCrash)
            root.put("ThrottleDownFactor", info.throttleDownFactor);

        // Global camera section
        Map<String, Object> camera = new LinkedHashMap<>();
        if (info.thirdPersonDist != dummyInfo.thirdPersonDist) camera.put("ThirdPersonDist", info.thirdPersonDist);
        if (info.cameraZoom != dummyInfo.cameraZoom) camera.put("Zoom", info.cameraZoom);
        if (info.defaultFreelook != dummyInfo.defaultFreelook) camera.put("DefaultFreeLook", info.defaultFreelook);
        if (info.cameraRotationSpeed != dummyInfo.cameraRotationSpeed)
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
        if (!camera.isEmpty()) root.put("Camera", camera);
        if (info.alwaysCameraView) root.put("AlwaysCameraView", true);

        // Sound
        Map<String, Object> sound = new LinkedHashMap<>();

        if (notBlank(info.soundMove) && !info.soundMove.equalsIgnoreCase(dummyInfo.soundMove))
            sound.put("MoveSound", info.soundMove.toLowerCase(Locale.ROOT));

        if (info.soundVolume != dummyInfo.soundVolume) sound.put("Vol", info.soundVolume);

        if (info.soundPitch != dummyInfo.soundPitch) sound.put("Pitch", info.soundPitch);

        if (info.soundRange != dummyInfo.soundRange) sound.put("Range", info.soundRange);

        if (!sound.isEmpty()) root.put("Sound", sound);

        // Physical properties
        Map<String, Object> phys = new LinkedHashMap<>();

        if (info.speed != dummyInfo.speed) phys.put("Speed", info.speed);

        if (info.isFloat != dummyInfo.isFloat) phys.put("CanFloat", info.isFloat);

        if (info.floatOffset != dummyInfo.floatOffset) phys.put("FloatOffset", -info.floatOffset);

        if (info.motionFactor != dummyInfo.motionFactor) phys.put("MotionFactor", info.motionFactor);

        if (info.gravity != dummyInfo.gravity) phys.put("Gravity", info.gravity);

        if (info.autoPilotRot != dummyInfo.autoPilotRot) phys.put("RotationSnapValue", info.autoPilotRot);

        if (info.gravityInWater != dummyInfo.gravityInWater) phys.put("GravityInWater", info.gravityInWater);

        if (info.stepHeight != dummyInfo.stepHeight) phys.put("StepHeight", info.stepHeight);

        if (info.canRotOnGround != dummyInfo.canRotOnGround) phys.put("CanRotOnGround", info.canRotOnGround);

        if (info.canMoveOnGround != dummyInfo.canMoveOnGround) phys.put("CanMoveOnGround", info.canMoveOnGround);

        if (info.onGroundPitch != dummyInfo.onGroundPitch) phys.put("OnGroundPitch", info.onGroundPitch);

        if (info.pivotTurnThrottle != dummyInfo.pivotTurnThrottle)
            phys.put("PivotTurnThrottle", info.pivotTurnThrottle);

// Mobility
        Map<String, Object> mobility = new LinkedHashMap<>();
        if (info.mobilityYaw != dummyInfo.mobilityYaw) mobility.put("Yaw", info.mobilityYaw);
        if (info.mobilityPitch != dummyInfo.mobilityPitch) mobility.put("Pitch", info.mobilityPitch);
        if (info.mobilityRoll != dummyInfo.mobilityRoll) mobility.put("Roll", info.mobilityRoll);
        if (info.mobilityYawOnGround != dummyInfo.mobilityYawOnGround)
            mobility.put("YawOnGround", info.mobilityYawOnGround);
        if (!mobility.isEmpty()) phys.put("Mobility", mobility);

        // Ground pitch factors
        Map<String, Object> gpf = new LinkedHashMap<>();
        if (info.onGroundPitchFactor != dummyInfo.onGroundPitchFactor) gpf.put("Pitch", info.onGroundPitchFactor);
        if (info.onGroundRollFactor != dummyInfo.onGroundRollFactor) gpf.put("Roll", info.onGroundRollFactor);
        if (!gpf.isEmpty()) phys.put("GroundPitchFactors", gpf);

        // Body size
        Map<String, Object> bodySize = new LinkedHashMap<>();
        if (info.bodyHeight != dummyInfo.bodyHeight) bodySize.put("Height", info.bodyHeight);
        if (info.bodyWidth != dummyInfo.bodyWidth) bodySize.put("Width", info.bodyWidth);
        if (!bodySize.isEmpty()) phys.put("BodySize", bodySize);

        // Rotation limits (only if enabled and non-default)
        if (info.limitRotation) {
            Map<String, Object> rotLimits = new LinkedHashMap<>();
            Map<String, Object> pitch = new LinkedHashMap<>();
            if (info.minRotationPitch != dummyInfo.minRotationPitch) pitch.put("Min", info.minRotationPitch);
            if (info.maxRotationPitch != dummyInfo.maxRotationPitch) pitch.put("Max", info.maxRotationPitch);
            if (!pitch.isEmpty()) rotLimits.put("Pitch", pitch);

            Map<String, Object> roll = new LinkedHashMap<>();
            if (info.minRotationRoll != dummyInfo.minRotationRoll) roll.put("Min", info.minRotationRoll);
            if (info.maxRotationRoll != dummyInfo.maxRotationRoll) roll.put("Max", info.maxRotationRoll);
            if (!roll.isEmpty()) rotLimits.put("Roll", roll);

            if (!rotLimits.isEmpty()) phys.put("RotationLimits", rotLimits);
        }

        if (!phys.isEmpty()) root.put("PhysicalProperties", phys);


        // Render
        Map<String, Object> render = new LinkedHashMap<>();

        if (info.smoothShading != dummyInfo.smoothShading) render.put("SmoothShading", info.smoothShading);

        if (info.hideEntity != dummyInfo.hideEntity) render.put("HideRiders", info.hideEntity);

        if (info.entityWidth != dummyInfo.entityWidth) render.put("ModelWidth", info.entityWidth);

        if (info.entityHeight != dummyInfo.entityHeight) render.put("ModelHeight", info.entityHeight);

        if (info.entityPitch != dummyInfo.entityPitch) render.put("ModelPitch", info.entityPitch);

        if (info.entityRoll != dummyInfo.entityRoll) render.put("ModelRoll", info.entityRoll);

        if (info.particlesScale != dummyInfo.particlesScale) render.put("ParticleScale", info.particlesScale);

        if (info.oneProbeScale != dummyInfo.oneProbeScale) render.put("OneProbeScale", info.oneProbeScale);

        if (info.enableSeaSurfaceParticle != dummyInfo.enableSeaSurfaceParticle)
            render.put("EnableSeaSurfaceParticle", info.enableSeaSurfaceParticle);

        // Splash particles
        if (info.particleSplashs != null && !info.particleSplashs.isEmpty()) {
            List<Map<String, Object>> splash = new ArrayList<>();

            for (MCH_AircraftInfo.ParticleSplash ps : info.particleSplashs) {
                Map<String, Object> pm = new LinkedHashMap<>();

                MCH_AircraftInfo.ParticleSplash dummyPs = dummyInfo.particleSplashs != null && dummyInfo.particleSplashs.size() > splash.size() ? dummyInfo.particleSplashs.get(splash.size()) : null;

                if (dummyPs == null || !ps.pos.equals(dummyPs.pos)) pm.put("Position", vec(ps.pos));
                if (dummyPs == null || ps.num != dummyPs.num) pm.put("Count", ps.num);
                if (dummyPs == null || ps.size != dummyPs.size) pm.put("Size", ps.size);
                if (dummyPs == null || ps.acceleration != dummyPs.acceleration) pm.put("Acceleration", ps.acceleration);
                if (dummyPs == null || ps.age != dummyPs.age) pm.put("Age", ps.age);
                if (dummyPs == null || ps.motionY != dummyPs.motionY) pm.put("Motion", ps.motionY);
                if (dummyPs == null || ps.gravity != dummyPs.gravity) pm.put("Gravity", ps.gravity);

                if (!pm.isEmpty()) splash.add(pm);
            }

            if (!splash.isEmpty()) render.put("SplashParticles", splash);
        }

        if (!render.isEmpty()) root.put("Render", render);

        // Armor
        Map<String, Object> armor = new LinkedHashMap<>();

        if (info.armorDamageFactor != dummyInfo.armorDamageFactor)
            armor.put("ArmorDamageFactor", info.armorDamageFactor);

        if (info.armorMinDamage != dummyInfo.armorMinDamage) armor.put("ArmorMinDamage", info.armorMinDamage);

        if (info.armorMaxDamage != dummyInfo.armorMaxDamage) armor.put("ArmorMaxDamage", info.armorMaxDamage);

        if (info.damageFactor != dummyInfo.damageFactor) armor.put("DamageFactor", info.damageFactor);

        if (info.submergedDamageHeight != dummyInfo.submergedDamageHeight)
            armor.put("SubmergedDamageHeight", info.submergedDamageHeight);

        if (!armor.isEmpty()) root.put("Armor", armor);

// Weapons list
        List<Map<String, Object>> weapons = new ArrayList<>();

        if (info.weaponSetList != null) {
            for (MCH_AircraftInfo.WeaponSet set : info.weaponSetList) {
                if (set.weapons == null) continue;

                for (MCH_AircraftInfo.Weapon w : set.weapons) {
                    Map<String, Object> m = new LinkedHashMap<>();

                    m.put("Type", set.type);
                    m.put("Position", vecMinusYOffset(w.pos));

                    if (w.yaw != 0 || (dummyInfo.weaponSetList != null && dummyInfo.weaponSetList.contains(set) && w.yaw != dummyInfo.weaponSetList.get(dummyInfo.weaponSetList.indexOf(set)).weapons.get(set.weapons.indexOf(w)).yaw))
                        m.put("Yaw", w.yaw);

                    if (w.pitch != 0 || (dummyInfo.weaponSetList != null && dummyInfo.weaponSetList.contains(set) && w.pitch != dummyInfo.weaponSetList.get(dummyInfo.weaponSetList.indexOf(set)).weapons.get(set.weapons.indexOf(w)).pitch))
                        m.put("Pitch", w.pitch);

                    if (w.seatID > 0) m.put("SeatID", w.seatID + 1);
                    if (!w.canUsePilot) m.put("CanUsePilot", false);
                    if (w.defaultYaw != 0) m.put("DefaultYaw", w.defaultYaw);
                    if (w.minYaw != 0) m.put("MinYaw", w.minYaw);
                    if (w.maxYaw != 0) m.put("MaxYaw", w.maxYaw);
                    if (w.minPitch != 0) m.put("MinPitch", w.minPitch);
                    if (w.maxPitch != 0) m.put("MaxPitch", w.maxPitch);
                    if (w.turret) m.put("Turret", true);

                    if (!m.isEmpty()) weapons.add(m);
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
            if (info.hudList != null && !info.hudList.isEmpty()) {
                int smallerListSize = 0;
                int hudSize = info.hudList.size();
                int seatSize = info.getNumSeat();
                //Content creators cannot adhere to their own fucking rules sometimes
                if (hudSize > seatSize || hudSize == seatSize) {
                    smallerListSize = seatSize;
                } else smallerListSize = hudSize;

                for (int index = 0; index < smallerListSize; index++) {
                    var hud = info.hudList.get(index);
                    if (hud == null) continue;
                    String name = hud.name;
                    if ("none".equals(name)) continue;
                    seats.get(index).put("Hud", name);
                }
            }
            if (info.exclusionSeatList != null && !info.exclusionSeatList.isEmpty())
                appendExlusionEntries(seats, info, false);


            root.put("Seats", seats);
        }

        // Racks
        if (info.getNumSeat() < info.getNumSeatAndRack()) {
            List<Map<String, Object>> racks = new ArrayList<>();
            List<MCH_SeatRackInfo> rackInfos = info.seatList.stream().filter(instanceOf(MCH_SeatRackInfo.class)).map(MCH_SeatRackInfo.class::cast).collect(Collectors.toList());
            for (MCH_SeatRackInfo r : rackInfos) {
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
            if (info.exclusionSeatList != null && !info.exclusionSeatList.isEmpty())
                appendExlusionEntries(racks, info, true);

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
                if (bb.boundingBoxType != null) bm.put("Type", bb.boundingBoxType.name());
                bm.put("Position", inline(bb.offsetX, bb.offsetY, bb.offsetZ));
                bm.put("Size", inline(bb.width, bb.height, bb.widthZ));
                if (bb.getDamageFactor() != 1.0f) bm.put("DamageFactor", bb.getDamageFactor());
                if (bb.name != null && !bb.name.isEmpty()) bm.put("Name", bb.name);
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
        if (info.isUAV != dummyInfo.isUAV) uav.put("IsUav", info.isUAV);
        if (info.isSmallUAV != dummyInfo.isSmallUAV) uav.put("IsSmallUav", info.isSmallUAV);
        if (info.isNewUAV != dummyInfo.isNewUAV) uav.put("IsNewUav", info.isNewUAV);
        if (info.isTargetDrone != dummyInfo.isTargetDrone) uav.put("IsTargetDrone", info.isTargetDrone);
        if (!uav.isEmpty()) root.put("Uav", uav);

        // Aircraft features
        Map<String, Object> feats = new LinkedHashMap<>();
        if (info.isEnableGunnerMode != dummyInfo.isEnableGunnerMode) feats.put("GunnerMode", info.isEnableGunnerMode);
        if (info.inventorySize != dummyInfo.inventorySize) feats.put("InventorySize", info.inventorySize);
        if (info.isEnableNightVision != dummyInfo.isEnableNightVision)
            feats.put("NightVision", info.isEnableNightVision);
        if (info.isEnableEntityRadar != dummyInfo.isEnableEntityRadar)
            feats.put("EntityRadar", info.isEnableEntityRadar);
        if (info.enableBack != dummyInfo.enableBack) feats.put("CanReverse", info.enableBack);
        if (info.canRotOnGround != dummyInfo.canRotOnGround) feats.put("CanRotateOnGround", info.canRotOnGround);
        if (info.isEnableConcurrentGunnerMode != dummyInfo.isEnableConcurrentGunnerMode)
            feats.put("ConcurrentGunner", info.isEnableConcurrentGunnerMode);
        if (info.isEnableEjectionSeat != dummyInfo.isEnableEjectionSeat)
            feats.put("EjectionSeat", info.isEnableEjectionSeat);
        if (info.throttleUpDown != dummyInfo.throttleUpDown) feats.put("ThrottleUpDown", info.throttleUpDown);
        if (info.throttleUpDownOnEntity != dummyInfo.throttleUpDownOnEntity)
            feats.put("ThrottleUpDownEntity", info.throttleUpDownOnEntity);

        // Parachuting
        if (info.isEnableParachuting) {
            Map<String, Object> parachute = new LinkedHashMap<>();
            if (info.mobDropOption != null) {
                if (info.mobDropOption.pos != null) parachute.put("Pos", vec(info.mobDropOption.pos));
                if (info.mobDropOption.interval != 0) parachute.put("Interval", info.mobDropOption.interval);
            }
            feats.put("Parachuting", parachute.isEmpty() ? true : parachute);
        }

        // Flare
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

        if (!feats.isEmpty()) root.put("AircraftFeatures", feats);


        return root;
    }

    private Map<String, Object> drawnPart(MCH_AircraftInfo.DrawnPart p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("Position", vec(p.pos));
        if (p.rot != Vec3d.ZERO) m.put("Rotation", vec(p.rot));
        if (notBlank(p.modelName)) m.put("PartName", p.modelName);
        return m;
    }

    private void appendExlusionEntries(List<Map<String, Object>> targetList, MCH_AircraftInfo info, boolean isRackList) {
        if (info.exclusionSeatList == null || info.exclusionSeatList.isEmpty()) return;

        final int seatCount = info.getNumSeat();
        final int rackCount = info.getNumRack();
        final int total = seatCount + rackCount;
        final Map<Integer, ExAgg> agg = new LinkedHashMap<>();

        for (Integer[] raw : info.exclusionSeatList) {
            if (raw == null || raw.length < 2) continue;

            Integer ownerBoxed = raw[0];
            if (ownerBoxed == null) continue;
            int ownerZ = ownerBoxed;
            if (ownerZ < 0 || ownerZ >= total) continue;

            var ex = agg.computeIfAbsent(ownerZ, k -> new ExAgg());

            for (int i = 1; i < raw.length; i++) {
                Integer boxed = raw[i];
                if (boxed == null) continue;
                int tZ = boxed;
                if (tZ < 0 || tZ >= total || tZ == ownerZ) continue;

                if (tZ < seatCount) ex.seats.add(tZ);
                else ex.racks.add(tZ - seatCount);
            }
        }

        for (var e : agg.entrySet()) {
            int ownerZ = e.getKey();
            boolean ownerIsRack = ownerZ >= seatCount;
            if (ownerIsRack != isRackList) continue;

            int ownerIndexInTarget = ownerIsRack ? ownerZ - seatCount : ownerZ;
            if (ownerIndexInTarget < 0 || ownerIndexInTarget >= targetList.size()) continue;

            List<Integer> seats1 = new ArrayList<>();
            for (int s : e.getValue().seats) seats1.add(s + 1);

            List<Integer> racks1 = new ArrayList<>();
            for (int r : e.getValue().racks) racks1.add(r + 1);

            Collections.sort(seats1);
            Collections.sort(racks1);

            if (seats1.isEmpty() && racks1.isEmpty()) continue;

            Map<String, Object> exMap = new LinkedHashMap<>();
            if (!seats1.isEmpty()) exMap.put("Seats", inlineSeq(seats1));
            if (!racks1.isEmpty()) exMap.put("Racks", inlineSeq(racks1));

            targetList.get(ownerIndexInTarget).put("ExcludeWith", exMap);
        }
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
                m.put("MaxRotation", wb.maxRotFactor);
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
        // Repelling hooks
        if (info.repellingHooks != null && !info.repellingHooks.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (MCH_AircraftInfo.RepellingHook hook : info.repellingHooks) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("Position", vec(hook.pos));
                if (hook.interval != 0) m.put("Interval", hook.interval);
                list.add(m);
            }
            if (!list.isEmpty()) components.put("RepelHook", list);
        }
    }

    private static final class ExAgg {
        final Set<Integer> seats = new LinkedHashSet<>(); // 0-based
        final Set<Integer> racks = new LinkedHashSet<>(); // 0-based
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
