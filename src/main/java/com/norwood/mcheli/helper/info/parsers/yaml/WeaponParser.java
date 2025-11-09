package com.norwood.mcheli.helper.info.parsers.yaml;

import static com.norwood.mcheli.helper.info.parsers.yaml.YamlParser.*;

import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import com.norwood.mcheli.MCH_Color;
import com.norwood.mcheli.MCH_DamageFactor;
import com.norwood.mcheli.compat.hbm.MistContainer;
import com.norwood.mcheli.compat.hbm.MukeContainer;
import com.norwood.mcheli.compat.hbm.NTSettingContainer;
import com.norwood.mcheli.compat.hbm.VNTSettingContainer;
import com.norwood.mcheli.helicopter.MCH_EntityHeli;
import com.norwood.mcheli.helper.MCH_Utils;
import com.norwood.mcheli.plane.MCH_EntityPlane;
import com.norwood.mcheli.ship.MCH_EntityShip;
import com.norwood.mcheli.tank.MCH_EntityTank;
import com.norwood.mcheli.vehicle.MCH_EntityVehicle;
import com.norwood.mcheli.weapon.MCH_Cartridge;
import com.norwood.mcheli.weapon.MCH_SightType;
import com.norwood.mcheli.weapon.MCH_WeaponInfo;

@SuppressWarnings("unchecked")
public class WeaponParser {

    private WeaponParser() {}

    private static MCH_WeaponInfo.Payload parsePayload(String s) {
        if (s == null || s.isEmpty())
            return MCH_WeaponInfo.Payload.NONE;

        return switch (s.trim().toUpperCase(Locale.ROOT)) {
            case "NTM_VNT", "VNT" -> MCH_WeaponInfo.Payload.NTM_VNT;
            case "NTM_NT", "NT" -> MCH_WeaponInfo.Payload.NTM_NT;
            case "NTM_NUKE", "NUKE" -> MCH_WeaponInfo.Payload.NTM_NUKE;
            case "NTM_MININUKE", "MININUKE" -> MCH_WeaponInfo.Payload.NTM_MINI_NUKE;
            case "CHEMICAL", "NTM_CHEMICAL" -> MCH_WeaponInfo.Payload.NTM_CHEMICAL;
            case "NTM_MIST", "MIST" -> MCH_WeaponInfo.Payload.NTM_MIST;
            default -> MCH_WeaponInfo.Payload.NONE;
        };
    }

    private static @Nullable Class<? extends Entity> tryLoadClass(String fullClassName) {
        try {
            Class<?> clazz = Class.forName(fullClassName);
            if (Entity.class.isAssignableFrom(clazz)) {
                return clazz.asSubclass(Entity.class);
            }
            return null;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static MCH_WeaponInfo parse(MCH_WeaponInfo info, Map<String, Object> root) {
        for (Map.Entry<String, Object> entry : root.entrySet()) {
            switch (entry.getKey()) {
                case "DisplayName" -> info.displayName = ((String) entry.getValue()).trim();
                case "BaseDamage" -> info.power = ((Number) entry.getValue()).intValue();
                case "Ballistics" -> parseBallisitcs(info, (Map<String, Object>) entry.getValue());
                case "Camera" -> parseCam(info, (Map<String, Object>) entry.getValue());
                case "Missile" -> parseMissile(info, (Map<String, Object>) entry.getValue());
                case "Sound" -> parseSound(info, (Map<String, Object>) entry.getValue());
                case "Type" -> info.type = ((String) entry.getValue()).trim().toLowerCase(Locale.ROOT);
                case "Recoil" -> parseRecoil(info, (Map<String, Object>) entry.getValue());
                case "Submunition", "Bomblet" -> parseBomblet(info, (Map<String, Object>) entry.getValue());
                case "Damage" -> parseDamage(info, (Map<String, Object>) entry.getValue());
                case "Ammo" -> parseAmmo(info, (Map<String, Object>) entry.getValue());
                case "Render" -> parseRender(info, (Map<String, Object>) entry.getValue());
                case "Explosion" -> parseExplosion(info, (Map<String, Object>) entry.getValue());
                case "Mode" -> info.modeNum = getClamped(1000, entry.getValue());
                case "FixMode" -> info.fixMode = getClamped(10, entry.getValue());
                case "Group" -> info.group = ((String) entry.getValue()).toLowerCase(Locale.ROOT).trim();
                case "FireCooldown" -> info.delay = ((Number) entry.getValue()).intValue();
                case "CanLoadChunks" -> info.enableChunkLoader = (Boolean) entry.getValue();
                case "SwitchCooldown" -> info.weaponSwitchCount = ((Number) entry.getValue()).intValue();
                case "CrossType" -> info.crossType = ((Number) entry.getValue()).intValue();
                case "RidableOnly" -> info.ridableOnly = (Boolean) entry.getValue();
                case "Accuracy" -> info.accuracy = getClamped(0.0F, 1000.0F, entry.getValue());
                case "SelfDestruct" -> info.destruct = (Boolean) entry.getValue();
                case "DispenseItem" -> parseDispenseItem(info, (Map<String, Object>) entry.getValue());
                case "Length" -> info.length = getClamped(1, 300, entry.getValue());
                case "Radius" -> info.radius = getClamped(1, 1000, entry.getValue());
                case "Target" -> info.target = parseTarget((String) entry.getValue());
                case "MarkTime" -> info.markTime = getClamped(1, 30000, entry.getValue()) + 1;
                case "DamageFactor" -> parseDamageFactor(info, (Map<String, Number>) entry.getValue());
                case "NTM" -> parseHBM(info, (Map<String, Object>) entry.getValue());

                default -> logUnkownEntry(entry, "Weapon");
            }
        }
        return info;
    }

    private static void parseAmmo(MCH_WeaponInfo info, Map<String, Object> map) {
        for (var entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "ReloadTime" -> info.reloadTime = getClamped(3, 1000, entry.getValue());
                case "MagSize" -> info.round = getClamped(1, 30000, entry.getValue());
                case "MaxAmmo" -> info.maxAmmo = getClamped(30000, entry.getValue());
                case "ResupplyCount" -> info.suppliedNum = getClamped(1, 30000, entry.getValue());
                case "AmmoItems" -> info.roundItems = ((List<Map<String, Object>>) entry.getValue()).stream()
                        .map(WeaponParser::parseRoundMap).collect(Collectors.toList());
                default -> logUnkownEntry(entry, "Ammo");
            }
        }
    }

    private static void parseRadar(MCH_WeaponInfo info, Map<String, Object> map) {
        for (var entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "IsRadarMissile" -> info.isRadarMissile = (Boolean) entry.getValue();
                case "ActiveRadar" -> info.activeRadar = (Boolean) entry.getValue();
                case "PassiveRadar" -> info.passiveRadar = (Boolean) entry.getValue();
                default -> logUnkownEntry(entry, "Radar");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void parseMissile(MCH_WeaponInfo info, Map<String, Object> map) {
        for (var entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "MarkerRocket" -> parseMarkerRocket(info, (Map<String, Object>) entry.getValue());
                case "LaserGuidance" -> info.laserGuidance = (Boolean) entry.getValue();
                case "HasLaserGuidancePod" -> info.hasLaserGuidancePod = (Boolean) entry.getValue();
                case "IsOffAxis" -> info.enableOffAxis = (Boolean) entry.getValue();
                case "GuidedTorpedo" -> info.isGuidedTorpedo = (Boolean) entry.getValue();
                case "PredictTargetPos" -> info.predictTargetPos = (Boolean) entry.getValue();
                case "EnableBVR" -> info.enableBVR = (Boolean) entry.getValue();
                case "MinRangeBVR" -> info.minRangeBVR = ((Number) entry.getValue()).intValue();
                case "ScanInterval" -> info.scanInterval = ((Number) entry.getValue()).intValue();

                case "TickEndHoming" -> info.tickEndHoming = getClamped(-1, 100000, entry.getValue());
                case "PDHDNMaxDegree" -> info.pdHDNMaxDegree = getClamped(-1.0F, 90.0F, entry.getValue());
                case "PDHDNMaxDegreeLockOutCount" -> info.pdHDNMaxDegreeLockOutCount = getClamped(200,
                        entry.getValue());
                case "TurningFactor" -> info.turningFactor = ((Number) entry.getValue()).doubleValue();
                case "MaxDegreeOfMissile" -> info.maxDegreeOfMissile = getClamped(100000, entry.getValue());
                case "Heat" -> parseHeat(info, (Map<String, Object>) entry.getValue());

                case "CanBeIntercepted" -> info.canBeIntercepted = (Boolean) entry.getValue();
                case "LockOn" -> parseLockOn(info, (Map<String, Object>) entry.getValue());
                case "Radar" -> parseRadar(info, (Map<String, Object>) entry.getValue());

                default -> logUnkownEntry(entry, "Missile");
            }
        }
    }

    private static void parseLockOn(MCH_WeaponInfo info, Map<String, Object> map) {
        for (var entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "CanLockMissile" -> info.canLockMissile = (Boolean) entry.getValue();
                case "Time" -> info.lockTime = getClamped(0, 1000, entry.getValue());
                case "Delay" -> info.rigidityTime = getClamped(0, 1_000_000, entry.getValue());
                case "MaxRange" -> info.maxLockOnRange = getClamped(2000, entry.getValue());
                case "MaxAngle" -> info.maxLockOnAngle = getClamped(200, entry.getValue());
                case "MinHeight" -> info.lockMinHeight = getClamped(-1, 100, entry.getValue());
                case "PassiveRadarLockOutCount" -> info.passiveRadarLockOutCount = getClamped(200, entry.getValue());
                case "LockedChaffMax" -> info.numLockedChaffMax = ((Number) entry.getValue()).intValue();
                default -> logUnkownEntry(entry, "LockOn");
            }
        }
    }

    private static void parseHeat(MCH_WeaponInfo info, Map<String, Object> map) {
        for (var entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "isHeatMissile" -> info.isHeatSeekerMissile = (Boolean) entry.getValue();
                case "HeatCount" -> info.heatCount = getClamped(100000, entry.getValue());
                case "MaxHeatCount" -> info.maxHeatCount = getClamped(100000, entry.getValue());
                case "AntiFlareCount" -> info.antiFlareCount = getClamped(-1, 200, entry.getValue());
                default -> logUnkownEntry(entry, "Heat");
            }
        }
    }

    private static void parseExplosion(MCH_WeaponInfo info, Map<String, Object> value) {
        for (Map.Entry<String, Object> entry : value.entrySet()) {
            switch (entry.getKey()) {
                case "Power" -> info.explosion = ((Number) entry.getValue()).intValue();
                case "Type" -> info.explosionType = (String) entry.getValue();
                case "ProximityFuseDist" -> info.proximityFuseDist = getClamped(0.0F, 2000.0F, entry.getValue());
                case "DestructionPower" -> info.explosionBlock = ((Number) entry.getValue()).intValue();
                case "PowerUnderwater" -> info.explosionInWater = ((Number) entry.getValue()).intValue();
                case "ExplosionAltitude" -> info.explosionAltitude = ((Number) entry.getValue()).intValue();
                case "FuseRebound" -> info.bound = getClamped(0.0F, 100000.0F, entry.getValue());
                case "FuseTime" -> info.timeFuse = ((Number) entry.getValue()).intValue();
                case "FuseDelay" -> info.delayFuse = ((Number) entry.getValue()).intValue();
                case "CanAirburst" -> info.canAirburst = (Boolean) entry.getValue();
                case "ExplosionAirburst" -> info.explosionAirburst = getClamped(50, entry.getValue());
                case "Flaming" -> info.flaming = ((Boolean) entry.getValue());
                case "FAE", "FuelAir" -> info.isFAE = (Boolean) entry.getValue();
                case "CanDestroyBlocks" -> info.disableDestroyBlock = !(Boolean) entry.getValue();
            }
        }
    }

    private static void parseDispenseItem(MCH_WeaponInfo info, Map<String, Object> value) {
        String loc = ((String) MCH_Utils.getAny(value, Arrays.asList("Location", "Loc", "Name"), null)).toLowerCase()
                .trim();
        if (loc != null && !loc.isEmpty());
        info.dispenseItemLoc = loc;
        info.dispenseDamege = ((Number) MCH_Utils.getAny(value, Arrays.asList("Meta", "Damage"), 0)).intValue();
        if (value.containsKey("DispenseRange"))
            info.dispenseRange = getClamped(1, 100, value.get("DispenseRange"));
    }

    private static void parseBomblet(MCH_WeaponInfo info, Map<String, Object> value) {
        for (Map.Entry<String, Object> entry : value.entrySet()) {
            switch (entry.getKey()) {
                case "Count" -> info.bomblet = getClamped(0, 1000, entry.getValue());
                case "DeployTime" -> info.bombletSTime = getClamped(0, 1000, entry.getValue());
                case "Spread" -> info.bombletDiff = getClamped(0.0F, 1000.0F, entry.getValue());
            }
        }
    }

    private static void parseCam(MCH_WeaponInfo info, Map<String, Object> value) {
        for (Map.Entry<String, Object> entry : value.entrySet()) {
            switch (entry.getKey()) {
                case "EnableMortarRadar" -> info.hasMortarRadar = (Boolean) entry.getValue();
                case "MortarRadarMaxDist" -> info.mortarRadarMaxDist = ((Number) entry.getValue()).doubleValue();
                case "DisplayMortarDistance" -> info.displayMortarDistance = ((Boolean) entry.getValue());
                case "FixPitch" -> info.fixCameraPitch = ((Boolean) entry.getValue());
                case "RotationSpeedPitch" -> info.cameraRotationSpeedPitch = getClamped(0.0F, 100.0F, entry.getValue());
                case "Sight" -> {
                    String sight = ((String) entry.getValue()).toLowerCase(Locale.ROOT);
                    if (sight.equals("movesight")) info.sight = MCH_SightType.ROCKET;
                    else if (sight.equals("missilesight")) info.sight = MCH_SightType.LOCK;
                }
                case "Zoom" -> {
                    Object val = entry.getValue();
                    if (val instanceof List<?>list && !list.isEmpty()) {
                        float[] zoomArray = new float[list.size()];
                        for (int i = 0; i < list.size(); i++) {
                            zoomArray[i] = getClamped(10F, list.get(i));
                        }
                        info.zoom = zoomArray;
                    } else if (val instanceof Number num) {
                        info.zoom = new float[] { num.floatValue() };
                    } else throw new IllegalArgumentException("Zoom must be either a number or a list of numbers!");

                }
            }
        }
    }

    // Okay..?
    private static int parseTarget(String str) {
        str = str.toLowerCase(Locale.ROOT);
        int flags = 0;
        if (str.contains("block")) return 64;
        if (str.contains("planes")) flags |= 32;
        if (str.contains("helicopters")) flags |= 16;
        if (str.contains("vehicles") || str.contains("tanks")) flags |= 8;
        if (str.contains("players")) flags |= 4;
        if (str.contains("monsters")) flags |= 2;
        if (str.contains("others")) flags |= 1;
        return flags;
    }

    private static void parseDamageFactor(MCH_WeaponInfo info, Map<String, Number> map) {
        Map<Class<? extends Entity>, Integer> damageFactorMap = new HashMap<>();
        for (Map.Entry<String, Number> entry : map.entrySet()) {
            var clazz = switch (entry.getKey()) {
                case "Player" -> EntityPlayer.class;
                case "Heli", "Helicopter" -> MCH_EntityHeli.class;
                case "Plane" -> MCH_EntityPlane.class;
                case "Ship" -> MCH_EntityShip.class;
                case "Tank" -> MCH_EntityTank.class;
                case "Vehicle" -> MCH_EntityVehicle.class;
                default -> tryLoadClass(entry.getKey());
            };
            if (clazz == null) continue;
            damageFactorMap.put(clazz, entry.getValue().intValue());
        }
        if (!damageFactorMap.isEmpty()) {
            info.damageFactor = new MCH_DamageFactor();
            damageFactorMap.forEach(info.damageFactor::add);
        }
    }

    private static MCH_Cartridge parseCardridge(Map<String, Object> map) {
        String name = null;
        float accel = 0F;
        float yaw = 0F;
        float pitch = 0.0F;
        float scale = 1F;
        float gravity = -0.04F;
        float bound = 0.5F;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "Name" -> name = ((String) entry.getValue()).toLowerCase();
                case "Accel", "Acceleration" -> accel = ((Number) entry.getValue()).floatValue();
                case "Yaw" -> yaw = ((Number) entry.getValue()).floatValue();
                case "Pitch" -> pitch = ((Number) entry.getValue()).floatValue();
                case "Scale" -> scale = ((Number) entry.getValue()).floatValue();
                case "Gravity" -> gravity = ((Number) entry.getValue()).floatValue();
                case "Bound" -> bound = ((Number) entry.getValue()).floatValue();
            }
        }
        return new MCH_Cartridge(name, accel, yaw, pitch, bound, gravity, scale);
    }

    private static void parseRender(MCH_WeaponInfo info, Map<String, Object> value) {
        for (Map.Entry<String, Object> entry : value.entrySet()) {
            switch (entry.getKey()) {
                case "MuzzleFlashes" -> info.listMuzzleFlash = parseMuzzleFlashes(
                        (List<Map<String, Object>>) entry.getValue());
                case "MuzzleSmoke" -> info.listMuzzleFlashSmoke = parseMuzzleFlashes(
                        (List<Map<String, Object>>) entry.getValue());
                case "BulletModel" -> info.bulletModelName = ((String) entry.getValue()).trim().toLowerCase();
                case "BombletModel" -> info.bombletModelName = ((String) entry.getValue()).trim().toLowerCase();
                case "TrajectoryParticle" -> {
                    var rawString = ((String) entry.getValue()).trim().toLowerCase();
                    info.trajectoryParticleName = "none".equals(rawString) ? "" : rawString;
                }
                case "Smoke" -> parseSmoke(info, (Map<String, Object>) entry.getValue());
                case "TrajectoryParticleStartTick" -> info.trajectoryParticleStartTick = getClamped(10_000,
                        entry.getValue());

                case "FlakParticlesCrack" -> info.flakParticlesCrack = getClamped(300, entry.getValue());
                case "ParticlesFlak" -> info.numParticlesFlak = getClamped(100, entry.getValue());
                case "SpentCasings" -> info.cartridge = parseCardridge((Map<String, Object>) entry.getValue());
            }
        }
    }

    private static void parseSmoke(MCH_WeaponInfo info, Map<String, Object> value) {
        for (Map.Entry<String, Object> entry : value.entrySet()) {
            switch (entry.getKey()) {
                case "DisableSmoke" -> info.disableSmoke = (Boolean) entry.getValue();
                case "SmokeSize" -> info.smokeSize = getClamped(0F, 100F, entry.getValue());
                case "SmokeNum" -> info.smokeNum = getClamped(1, 100, entry.getValue());
                case "SmokeMaxAge" -> info.smokeMaxAge = getClamped(2, 1000, entry.getValue());
                case "BulletColorInWater" -> info.colorInWater = new MCH_Color(
                        parseHexColor((String) entry.getValue()));
                case "BulletColor" -> info.color = new MCH_Color(parseHexColor((String) entry.getValue()));
                // Apparently Smoke color and bullet color set the same variable
            }
        }
    }

    private static List<MCH_WeaponInfo.MuzzleFlash> parseMuzzleFlashes(List<Map<String, Object>> rawList) {
        if (rawList == null) return Collections.emptyList();

        return rawList.stream()
                .map(map -> {
                    MCH_WeaponInfo.MuzzleFlashRaw rawFlash = new MCH_WeaponInfo.MuzzleFlashRaw();
                    rawFlash.setDistance(((Number) map.get("Distance")).floatValue());
                    rawFlash.setSize(((Number) map.get("Size")).floatValue());
                    rawFlash.setRange(((Number) map.get("Range")).floatValue());
                    rawFlash.setAge(((Number) map.get("Age")).intValue());
                    rawFlash.setCount(((Number) map.get("Count")).intValue());
                    rawFlash.setColor((String) map.get("Color"));
                    return rawFlash;
                })
                .map(MCH_WeaponInfo.MuzzleFlash::new)
                .collect(Collectors.toList());
    }

    /**
     *
     * Payload type - Decides the type of the explosion and its effect;
     * EffectOnly - Whenever to only use the particle effect, but default to the explosion sfx selected by PayloadType
     * Fluid - Fluid to disperse on impact, works like the canisters
     */
    private static void parseHBM(MCH_WeaponInfo info, Map<String, Object> map) {
        info.useHBM = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "PayloadType", "Type" -> info.payloadNTM = parsePayload((String) entry.getValue());
                case "EffectOnly" -> info.effectOnly = (Boolean) entry.getValue();
                case "Mist" -> parseMist(info, (Map<String, Object>) entry.getValue());
                case "MiniNuke", "Muke" -> {
                    Object muke = entry.getValue();
                    if (muke instanceof String mukeString) {
                        switch (mukeString.toUpperCase().trim()) {
                            case "SAFE" -> info.mukeContainer = MukeContainer.PARAMS_SAFE;
                            case "TOTS" -> info.mukeContainer = MukeContainer.PARAMS_TOTS;
                            case "LOW" -> info.mukeContainer = MukeContainer.PARAMS_LOW;
                            case "MEDIUM" -> info.mukeContainer = MukeContainer.PARAMS_MEDIUM;
                            case "HIGH" -> info.mukeContainer = MukeContainer.PARAMS_HIGH;
                            default -> throw new IllegalArgumentException(
                                    "Unknown Muke type string: '" + mukeString +
                                            "'. Expected one of: SAFE, TOTS, LOW, MEDIUM, HIGH.");
                        }
                    } else if (muke instanceof Map<?, ?>mukeMap) {
                        parseMukeContainer(info, (Map<String, Object>) mukeMap);
                    } else {
                        throw new IllegalArgumentException(
                                "Invalid Muke type: expected a String or Map, but got: " + muke.getClass().getName());
                    }
                }

                case "VNT" -> parseVNT(info, (Map<String, Object>) entry.getValue());

                case "NT" -> parseNTExplosion(info, (Map<String, Object>) entry.getValue());
                default -> logUnkownEntry(entry, "NTM");
            }
        }
    }

    private static void parseVNT(MCH_WeaponInfo info, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "Attributes" -> info.vntSettingContainer = new VNTSettingContainer(
                        (Map<String, Object>) entry.getValue());
                case "Effect" -> {
                    if (entry.getValue() instanceof String str) {
                        info.vntSettingContainer.explosionEffect = switch (str.toUpperCase(Locale.ROOT)) {
                            case "LARGE" -> VNTSettingContainer.ExplosionEffect.large();
                            case "STANDARD" -> VNTSettingContainer.ExplosionEffect.standard();
                            case "MEDIUM", "LOW" -> VNTSettingContainer.ExplosionEffect.medium();
                            default -> throw new IllegalArgumentException(
                                    "Unknown Muke type string: '" + str +
                                            "'. Expected one of: LARGE, STANDARD, MEDIUM, LOW.");
                        };
                    } else if (entry.getValue() instanceof Map<?, ?>vntMapRaw) {
                        var vntMap = (Map<String, Object>) vntMapRaw;
                        var effect = VNTSettingContainer.ExplosionEffect.standard();
                        for (Map.Entry<String, Object> vntEntry : vntMap.entrySet()) {
                            var value = vntEntry.getValue();
                            switch (vntEntry.getKey()) {
                                case "IsSmall" -> effect.isSmall = (Boolean) value;
                                case "CloudCount" -> effect.cloudCount = ((Number) value).intValue();
                                case "CloudScale" -> effect.cloudScale = ((Number) value).floatValue();
                                case "CloudSpeedMult" -> effect.cloudSpeedMult = ((Number) value).floatValue();
                                case "WaveScale" -> effect.waveScale = ((Number) value).floatValue();
                                case "DebrisCount" -> effect.debrisCount = ((Number) value).intValue();
                                case "DebrisSize" -> effect.debrisSize = ((Number) value).intValue();
                                case "DebrisRetry" -> effect.debrisRetry = ((Number) value).intValue();
                                case "DebrisVelocity" -> effect.debrisVelocity = ((Number) value).floatValue();
                                case "DebrisHorizontalDeviation" -> effect.debrisHorizontalDeviation = ((Number) value)
                                        .floatValue();
                                case "DebrisVerticalOffset" -> effect.debrisVerticalOffset = ((Number) value)
                                        .floatValue();
                                case "SoundRange" -> effect.soundRange = ((Number) value).floatValue();
                                default -> logUnkownEntry(entry, "VNTEffect");
                            }
                        }
                        info.vntSettingContainer.explosionEffect = effect;
                    }
                }
            }

        }
    }

    private static void parseMist(MCH_WeaponInfo info, Map<String, Object> map) {
        MistContainer container = new MistContainer();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "FluidType", "Fluid" -> container.fluidType = ((String) entry.getValue()).toUpperCase(Locale.ROOT)
                        .trim();
                case "CloudCount", "Count" -> container.cloudCount = ((Number) entry.getValue()).intValue();
                case "Width" -> container.width = ((Number) entry.getValue()).floatValue();
                case "Height" -> container.height = ((Number) entry.getValue()).floatValue();
                case "AreaSpread", "Spread" -> container.areaSpread = ((Number) entry.getValue()).intValue();
                case "Lifetime" -> container.lifetime = ((Number) entry.getValue()).intValue();
                case "LifetimeVariance" -> container.lifetimeVariance = ((Number) entry.getValue()).intValue();
                default -> logUnkownEntry(entry, "Mist");
            }
        }
        info.mistContainer = container;
    }

    @SuppressWarnings("unchecked")
    private static void parseMukeContainer(MCH_WeaponInfo info, Map<String, Object> map) {
        MukeContainer container = new MukeContainer();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "UseTorex" -> container.miniNuke = !((Boolean) entry.getValue());
                case "Safe" -> container.safe = (Boolean) entry.getValue();
                case "BlastRadius" -> container.blastRadius = ((Number) entry.getValue()).floatValue();
                case "KillRadius" -> container.killRadius = ((Number) entry.getValue()).floatValue();
                case "RadiationLevel" -> container.radiationLevel = ((Number) entry.getValue()).floatValue();
                case "Particle" -> container.particle = (String) entry.getValue();
                case "ShrapnelCount" -> container.shrapnelCount = ((Number) entry.getValue()).intValue();
                case "Resolution", "Res" -> container.resolution = ((Number) entry.getValue()).intValue();
                case "Attributes", "Attrib" -> {
                    Object val = entry.getValue();
                    if (val instanceof List<?>list) {
                        container.attributes = list.stream()
                                .map(Object::toString)
                                .collect(Collectors.toList());
                    }
                }

                default -> logUnkownEntry(entry, "MiniNuke");
            }
        }
        info.mukeContainer = container;
    }

    private static void parseNTExplosion(MCH_WeaponInfo info, Map<String, Object> map) {
        int res = 16;
        List<String> attribs = null;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "Resolution", "Res" -> res = ((Number) entry.getValue()).intValue();
                case "Attributes", "Attrib" -> attribs = (List<String>) entry.getValue();
            }
        }
        info.ntSettingContainer = new NTSettingContainer(attribs, res);
    }

    private static void parseMarkerRocket(MCH_WeaponInfo info, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {

                case "Count" -> info.markerRocketSpawnNum = ((Number) entry.getValue()).intValue();
                case "Spread" -> info.markerRocketSpawnDiff = ((Number) entry.getValue()).intValue();
                case "SpawnHeight" -> info.markerRocketSpawnHeight = ((Number) entry.getValue()).intValue();
                case "Acceleration" -> info.markerRocketSpawnSpeed = ((Number) entry.getValue()).intValue();

            }
        }
    }

    // TODO: Oredict
    static MCH_WeaponInfo.RoundItem parseRoundMap(Map<String, Object> roundMap) {
        int count = getClamped(1, 64, MCH_Utils.getAny(roundMap, Arrays.asList("Count", "Num"), 1));
        String loc = ((String) MCH_Utils.getAny(roundMap, Arrays.asList("Loc", "Name"), null)).toLowerCase(Locale.ROOT)
                .trim();
        int meta = getClamped(Short.MAX_VALUE, MCH_Utils.getAny(roundMap, Arrays.asList("Meta", "Damage"), 1));
        if (loc == null) throw new IllegalArgumentException("Ammo item must have a resource path!");
        return new MCH_WeaponInfo.RoundItem(count, loc, meta);
    }

    private static void parseBallisitcs(MCH_WeaponInfo info, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "Acceleration", "Accel" -> info.acceleration = getClamped(0.0F, 100.0F, entry.getValue());
                case "AccelerationInWater" -> info.accelerationInWater = getClamped(0.0F, 100.0F, entry.getValue());
                case "Gravity" -> info.gravity = getClamped(-50.0F, 50.0F, entry.getValue());
                case "GravityInWater" -> info.gravityInWater = getClamped(-50.0F, 50.0F, entry.getValue());
                case "VelocityInWater" -> info.velocityInWater = ((Number) entry.getValue()).floatValue();
                case "SpeedFactor" -> info.speedFactor = ((Number) entry.getValue()).floatValue();
                case "SpeedFactorStartTick" -> info.speedFactorStartTick = ((Number) entry.getValue()).intValue();
                case "SpeedFactorEndTick" -> info.speedFactorEndTick = ((Number) entry.getValue()).intValue();
                case "SpeedDependsAircraft" -> info.speedDependsAircraft = (Boolean) entry.getValue();
                case "Piercing" -> info.piercing = getClamped(1_000, entry.getValue());
                case "FlakSpread" -> info.flakParticlesDiff = ((Number) entry.getValue()).floatValue();
            }
        }
    }

    private static void parseRecoil(MCH_WeaponInfo info, Map<String, Object> map) {
        info.recoilBufCount = 1;
        info.recoilBufCountSpeed = 1;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            switch (key) {
                case "Power" -> info.recoil = getClamped(100F, entry.getValue());
                case "Pitch" -> info.recoilPitch = ((Number) value).floatValue();
                case "Yaw" -> info.recoilYaw = ((Number) value).floatValue();
                case "PitchRange" -> info.recoilPitchRange = ((Number) value).floatValue();
                case "YawRange" -> info.recoilYawRange = ((Number) value).floatValue();
                case "RecoverFactor" -> info.recoilRecoverFactor = ((Number) value).floatValue();

                case "BufferCount" -> info.recoilBufCount = ((Number) value).intValue();
                case "BufferSpeed" -> info.recoilBufCountSpeed = ((Number) value).intValue();
            }
        }

        if (info.recoilBufCountSpeed > info.recoilBufCount / 2) {
            info.recoilBufCountSpeed = info.recoilBufCount / 2;
        }
    }

    private static void parseSound(MCH_WeaponInfo info, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "Name" -> info.soundFileName = ((String) entry.getValue()).toLowerCase(Locale.ROOT).trim();// for
                                                                                                                // txt
                                                                                                                // parser
                                                                                                                // this
                                                                                                                // field
                                                                                                                // is
                                                                                                                // "Sound"
                case "Locations" -> parseSoundLoc(info, (Map<String, Object>) entry.getValue());
                case "Delay" -> info.soundDelay = getClamped(0, 1000, entry.getValue());
                case "Volume" -> info.soundVolume = getClamped(0.0F, 1000.0F, entry.getValue());
                case "Pitch" -> info.soundPitch = getClamped(0.0F, 1.0F, entry.getValue());
                case "PitchRandom" -> info.soundPitchRandom = getClamped(0.0F, 1.0F, entry.getValue());
                case "HitSoundRange" -> info.hitSoundRange = ((Number) entry.getValue()).intValue();

            }
        }
    }

    private static void parseSoundLoc(MCH_WeaponInfo info, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "Hit" -> info.hitSound = ((String) entry.getValue()).toLowerCase(Locale.ROOT).trim();
                case "HitMetal" -> info.hitSoundIron = ((String) entry.getValue()).toLowerCase(Locale.ROOT).trim();
                case "Railgun" -> info.railgunSound = ((String) entry.getValue()).toLowerCase(Locale.ROOT).trim();
                case "WeaponSwitch" -> info.weaponSwitchSound = ((String) entry.getValue()).toLowerCase(Locale.ROOT)
                        .trim();

            }
        }
    }

    private static void parseDamage(MCH_WeaponInfo info, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "ExplosionDamageVsLiving" -> info.explosionDamageVsLiving = ((Number) entry.getValue())
                        .floatValue();
                case "ExplosionDamageVsPlayer" -> info.explosionDamageVsPlayer = ((Number) entry.getValue())
                        .floatValue();
                case "ExplosionDamageVsPlane" -> info.explosionDamageVsPlane = ((Number) entry.getValue()).floatValue();
                case "ExplosionDamageVsVehicle" -> info.explosionDamageVsVehicle = ((Number) entry.getValue())
                        .floatValue();
                case "ExplosionDamageVsTank" -> info.explosionDamageVsTank = ((Number) entry.getValue()).floatValue();
                case "ExplosionDamageVsHeli" -> info.explosionDamageVsHeli = ((Number) entry.getValue()).floatValue();
            }
        }
    }
}
