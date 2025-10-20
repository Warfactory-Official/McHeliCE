package com.norwood.mcheli.helper.info.parsers.yaml;

import com.norwood.mcheli.compat.hbm.VNTSettingContainer;
import com.norwood.mcheli.helper.MCH_Utils;
import com.norwood.mcheli.weapon.MCH_SightType;
import com.norwood.mcheli.weapon.MCH_WeaponInfo;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static com.norwood.mcheli.helper.info.parsers.yaml.YamlParser.getClamped;
import static com.norwood.mcheli.helper.info.parsers.yaml.YamlParser.logUnkownEntry;

@NoArgsConstructor
@SuppressWarnings("unchecked")
public class WeaponParser {

    public MCH_WeaponInfo parse(MCH_WeaponInfo info, Map<String, Object> root) {
        for (Map.Entry<String, Object> entry : root.entrySet()) {
            switch (entry.getKey()) {
                case "Ballistics" -> parseBallisitcs(info, (Map<String, Object>) entry.getValue());
                case "Sound" -> parseSound(info, (Map<String, Object>) entry.getValue());
                case "Type" -> info.type = ((String) entry.getValue()).trim().toLowerCase(Locale.ROOT);
                case "Recoil" -> parseRecoil(info, (Map<String, Object>) entry.getValue());
                case "Damage" -> parseDamage(info, (Map<String, Object>) entry.getValue());
                case "NTM" -> parseHBM(info, (Map<String,Object>)entry.getValue());
                case "MarkerRocket" -> parseMarkerRocket(info, (Map<String,Object>)entry.getValue());

                case "DisplayName" -> info.displayName = ((String) entry.getValue()).trim();
                case "Group" -> info.group = ((String) entry.getValue()).toLowerCase(Locale.ROOT).trim();
                case "Explosion" -> info.explosion = ((Number) entry.getValue()).intValue();
                case "ExplosionBlock" -> info.explosionBlock = ((Number) entry.getValue()).intValue();
                case "ExplosionInWater" -> info.explosionInWater = ((Number) entry.getValue()).intValue();
                case "ExplosionAltitude" -> info.explosionAltitude = ((Number) entry.getValue()).intValue();
                case "TimeFuse" -> info.timeFuse = ((Number) entry.getValue()).intValue();
                case "DelayFuse" -> info.delayFuse = ((Number) entry.getValue()).intValue();
                case "Bound" -> info.bound = getClamped(0.0F, 100000.0F, (Number) entry.getValue());
                case "Flaming" -> info.flaming = ((Boolean) entry.getValue());
                case "DisplayMortarDistance" -> info.displayMortarDistance = ((Boolean) entry.getValue());
                case "FixCameraPitch" -> info.fixCameraPitch = ((Boolean) entry.getValue());
                case "CameraRotationSpeedPitch" ->
                        info.cameraRotationSpeedPitch = getClamped(0.0F, 100.0F, (Number) entry.getValue());
                case "Sight" -> {
                    String value = ((String) entry.getValue()).toLowerCase(Locale.ROOT);
                    if (value.equals("movesight")) info.sight = MCH_SightType.ROCKET;
                    else if (value.equals("missilesight")) info.sight = MCH_SightType.LOCK;
                }
                case "Zoom" -> {
                    Object val = entry.getValue();
                    if (val instanceof List<?> list && !list.isEmpty()) {
                        info.zoom = new float[list.size()];
                        for (int i = 0; i < list.size(); i++) {
                            info.zoom[i] = getClamped(0.1F, 10.0F, (Number) list.get(i));
                        }
                    }
                }
                case "Delay" -> info.delay = ((Number) entry.getValue()).intValue();
                case "ExplosionType" -> info.explosionType = (String) entry.getValue();
//                case "NukeYield" -> info.nukeYield = getClamped(100000, (Number) entry.getValue());
//                case "ChemYield" -> info.chemYield = getClamped(100000, (Number) entry.getValue());
//                case "EffectYield" -> info.effectYield = getClamped(100000, (Number) entry.getValue());
//                case "NukeEffectOnly" -> info.nukeEffectOnly = ((Boolean) entry.getValue());
                case "MaxDegreeOfMissile" -> info.maxDegreeOfMissile = getClamped(100000, (Number) entry.getValue());
                case "TickEndHoming" -> info.tickEndHoming = getClamped(-1, 100000, (Number) entry.getValue());
                case "FlakParticlesCrack" -> info.flakParticlesCrack = getClamped(300, (Number) entry.getValue());
                case "ParticlesFlak" -> info.numParticlesFlak = getClamped(100, (Number) entry.getValue());
                case "FlakParticlesDiff" -> info.flakParticlesDiff = ((Number) entry.getValue()).floatValue();
                case "IsRadarMissile" -> info.isRadarMissile = ((Boolean) entry.getValue());
                case "IsHeatSeekerMissile" -> info.isHeatSeekerMissile = ((Boolean) entry.getValue());
                case "MaxLockOnRange" -> info.maxLockOnRange = getClamped(2000, (Number) entry.getValue());
                case "MaxLockOnAngle" -> info.maxLockOnAngle = getClamped(200, (Number) entry.getValue());
                case "PDHDNMaxDegree" -> info.pdHDNMaxDegree = getClamped(-1.0F, 90.0F, (Number) entry.getValue());
                case "PDHDNMaxDegreeLockOutCount" ->
                        info.pdHDNMaxDegreeLockOutCount = getClamped(200, (Number) entry.getValue());
                case "AntiFlareCount" -> info.antiFlareCount = getClamped(-1, 200, (Number) entry.getValue());
                case "LockMinHeight" -> info.lockMinHeight = getClamped(-1, 100, (Number) entry.getValue());
                case "PassiveRadar" -> info.passiveRadar = ((Boolean) entry.getValue());
                case "PassiveRadarLockOutCount" ->
                        info.passiveRadarLockOutCount = getClamped(200, (Number) entry.getValue());
                case "LaserGuidance" -> info.laserGuidance = ((Boolean) entry.getValue());
                case "HasLaserGuidancePod" -> info.hasLaserGuidancePod = ((Boolean) entry.getValue());
                case "ActiveRadar" -> info.activeRadar = ((Boolean) entry.getValue());
                case "EnableOffAxis" -> info.enableOffAxis = ((Boolean) entry.getValue());
                case "TurningFactor" -> info.turningFactor = ((Number) entry.getValue()).doubleValue();
                case "EnableChunkLoader" -> info.enableChunkLoader = (Boolean) entry.getValue();
                case "ScanInterval" -> info.scanInterval = ((Number) entry.getValue()).intValue();
                case "WeaponSwitchCount" -> info.weaponSwitchCount = ((Number) entry.getValue()).intValue();
                case "WeaponSwitchSound" ->
                        info.weaponSwitchSound = ((String) entry.getValue()).toLowerCase(Locale.ROOT).trim();
                case "CanLockMissile" -> info.canLockMissile = (Boolean) entry.getValue();
                case "EnableBVR" -> info.enableBVR = (Boolean) entry.getValue();
                case "MinRangeBVR" -> info.minRangeBVR = ((Number) entry.getValue()).intValue();
                case "PredictTargetPos" -> info.predictTargetPos = (Boolean) entry.getValue();
                case "HitSoundRange" -> info.hitSoundRange = ((Number) entry.getValue()).intValue();
                case "NumLockedChaffMax" -> info.numLockedChaffMax = ((Number) entry.getValue()).intValue();
                case "ExplosionDamageVsLiving" ->
                        info.explosionDamageVsLiving = ((Number) entry.getValue()).floatValue();
                case "ExplosionDamageVsPlayer" ->
                        info.explosionDamageVsPlayer = ((Number) entry.getValue()).floatValue();
                case "ExplosionDamageVsPlane" -> info.explosionDamageVsPlane = ((Number) entry.getValue()).floatValue();
                case "ExplosionDamageVsVehicle" ->
                        info.explosionDamageVsVehicle = ((Number) entry.getValue()).floatValue();
                case "ExplosionDamageVsTank" -> info.explosionDamageVsTank = ((Number) entry.getValue()).floatValue();
                case "ExplosionDamageVsHeli" -> info.explosionDamageVsHeli = ((Number) entry.getValue()).floatValue();
                case "DisableDestroyBlock" -> info.disableDestroyBlock = (Boolean) entry.getValue();
                case "RailgunSound" -> info.railgunSound = ((String) entry.getValue()).toLowerCase(Locale.ROOT).trim();
                case "CanBeIntercepted" -> info.canBeIntercepted = (Boolean) entry.getValue();
                case "CanAirburst" -> info.canAirburst = (Boolean) entry.getValue();
                case "ExplosionAirburst" -> info.explosionAirburst = getClamped(50, (Number) entry.getValue());
                case "CrossType" -> info.crossType = ((Number) entry.getValue()).intValue();
                case "EnableMortarRadar" -> info.hasMortarRadar = (Boolean) entry.getValue();
                case "MortarRadarMaxDist" -> info.mortarRadarMaxDist = ((Number) entry.getValue()).doubleValue();
                case "ReloadTime" -> info.reloadTime = getClamped(3, 1000, (Number) entry.getValue());
                case "Round" -> info.round = getClamped(1, 30000, (Number) entry.getValue());
                case "MaxAmmo" -> info.maxAmmo = getClamped(30000, (Number) entry.getValue());
                case "SuppliedNum" -> info.suppliedNum = getClamped(1, 30000, (Number) entry.getValue());
                case "AmmoItems" ->
                        info.roundItems = ((List<Map<String, Object>>) entry.getValue()).stream().map(this::parseRoundMap).collect(Collectors.toList());
                case "LockTime" -> info.lockTime = getClamped(0, 1000, (Number) entry.getValue());
                case "RidableOnly" -> info.ridableOnly = (Boolean) entry.getValue();
                case "ProximityFuseDist" ->
                        info.proximityFuseDist = getClamped(0.0F, 2000.0F, (Number) entry.getValue());
                case "RigidityTime" -> info.rigidityTime = getClamped(0, 1_000_000, (Number) entry.getValue());
                case "Accuracy" -> info.accuracy = getClamped(0.0F, 1000.0F, (Number) entry.getValue());
                case "Bomblet" -> info.bomblet = getClamped(0, 1000, (Number) entry.getValue());
                case "BombletSTime" -> info.bombletSTime = getClamped(0, 1000, (Number) entry.getValue());
                case "BombletDiff" -> info.bombletDiff = getClamped(0.0F, 1000.0F, (Number) entry.getValue());



                default -> logUnkownEntry(entry, "Weapon");
            }
        }
        return info;
    }

    private void parseHBM(MCH_WeaponInfo info, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "PayloadType" -> info.payloadNTM =  parsePayload((String) entry.getValue());
                case "EffectOnly" -> info.effectOnly =  (Boolean) entry.getValue();
                case "VNT" -> info.vntSettingContainer = new VNTSettingContainer((Map<String,Object>) entry.getValue());
                case "FluidType" -> info.fluidTypeNTM = (String)entry.getValue();
            }}
    }

    private void parseMarkerRocket(MCH_WeaponInfo info, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {

                case "MarkerRocketSpawnNum" -> info.markerRocketSpawnNum = ((Number) entry.getValue()).intValue();
                case "MarkerRocketSpawnDiff" -> info.markerRocketSpawnDiff = ((Number) entry.getValue()).intValue();
                case "MarkerRocketSpawnHeight" -> info.markerRocketSpawnHeight = ((Number) entry.getValue()).intValue();
                case "MarkerRocketSpawnSpeed" -> info.markerRocketSpawnSpeed = ((Number) entry.getValue()).intValue();

            }}
    }

    public static MCH_WeaponInfo.Payload parsePayload(String s) {
        if (s == null || s.isEmpty())
            return MCH_WeaponInfo.Payload.NONE;

        return switch (s.trim().toUpperCase(Locale.ROOT)) {
            case "NTM_EXP_SMALL", "SMALL" -> MCH_WeaponInfo.Payload.NTM_EXP_SMALL;
            case "NTM_EXP_LARGE", "LARGE" -> MCH_WeaponInfo.Payload.NTM_EXP_LARGE;
            case "NTM_NUKE", "NUKE" -> MCH_WeaponInfo.Payload.NTM_NUKE;
            default -> MCH_WeaponInfo.Payload.NONE;
        };
    }

    //TODO: Oredict
    MCH_WeaponInfo.RoundItem parseRoundMap(Map<String, Object> roundMap) {
        int count = getClamped(1, 64, (Number) MCH_Utils.getAny(roundMap, Arrays.asList("Count", "Num"), 1));
        String loc = ((String) MCH_Utils.getAny(roundMap, Arrays.asList("Loc", "Name"), null)).toLowerCase(Locale.ROOT).trim();
        int meta = getClamped(Short.MAX_VALUE, (Number) MCH_Utils.getAny(roundMap, Arrays.asList("Meta", "Damage"), 1));
        if (loc == null) throw new IllegalArgumentException("Ammo item must have a resource path!");
        return new MCH_WeaponInfo.RoundItem(count, loc, meta);
    }


   private void parseBallisitcs(MCH_WeaponInfo info, Map<String,Object> map){
       for (Map.Entry<String, Object> entry : map.entrySet()) {
           switch (entry.getKey()) {
               case "Acceleration", "Accel" -> info.acceleration = getClamped(0.0F, 100.0F, (Number) entry.getValue());
               case "AccelerationInWater" ->
                       info.accelerationInWater = getClamped(0.0F, 100.0F, (Number) entry.getValue());
               case "Gravity" -> info.gravity = getClamped(-50.0F, 50.0F, (Number) entry.getValue());
               case "GravityInWater" -> info.gravityInWater = getClamped(-50.0F, 50.0F, (Number) entry.getValue());
               case "VelocityInWater" -> info.velocityInWater = ((Number) entry.getValue()).floatValue();
               case "Power" -> info.power = ((Number) entry.getValue()).intValue();
               case "SpeedFactor" -> info.speedFactor = ((Number) entry.getValue()).floatValue();
               case "SpeedFactorStartTick" -> info.speedFactorStartTick = ((Number) entry.getValue()).intValue();
               case "SpeedFactorEndTick" -> info.speedFactorEndTick = ((Number) entry.getValue()).intValue();
               case "SpeedDependsAircraft" -> info.speedDependsAircraft = (Boolean) entry.getValue();
           }}
   }

    private void parseRecoil(MCH_WeaponInfo info, Map<String,Object> map){
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "Pitch" -> info.recoilPitch = ((Number) entry.getValue()).floatValue();
                case "Yaw" -> info.recoilYaw = ((Number) entry.getValue()).floatValue();
                case "PitchRange" -> info.recoilPitchRange = ((Number) entry.getValue()).floatValue();
                case "YawRange" -> info.recoilYawRange = ((Number) entry.getValue()).floatValue();
                case "RecoverFactor" -> info.recoilRecoverFactor = ((Number) entry.getValue()).floatValue();
            }}
    }

    private void parseSound(MCH_WeaponInfo info, Map<String,Object> map){
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "Name" -> info.soundFileName = ((String) entry.getValue()).toLowerCase(Locale.ROOT).trim();
                case "Delay" -> info.soundDelay = getClamped(0, 1000, (Number) entry.getValue());
                case "Volume" -> info.soundVolume = getClamped(0.0F, 1000.0F, (Number) entry.getValue());
                case "Pitch" -> info.soundPitch = getClamped(0.0F, 1.0F, (Number) entry.getValue());
                case "PitchRandom" -> info.soundPitchRandom = getClamped(0.0F, 1.0F, (Number) entry.getValue());
                case "HitSound" -> info.hitSound = ((String) entry.getValue()).toLowerCase(Locale.ROOT).trim();
                case "HitSoundIron" -> info.hitSoundIron = ((String) entry.getValue()).toLowerCase(Locale.ROOT).trim();
            }}
    }
    private void parseDamage(MCH_WeaponInfo info, Map<String,Object> map){
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {


            }}
    }

}
