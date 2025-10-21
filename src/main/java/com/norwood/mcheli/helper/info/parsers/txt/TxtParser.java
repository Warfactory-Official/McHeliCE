package com.norwood.mcheli.helper.info.parsers.txt;

import com.norwood.mcheli.MCH_BaseInfo;
import com.norwood.mcheli.MCH_Color;
import com.norwood.mcheli.MCH_DamageFactor;
import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_AircraftInfo.*;
import com.norwood.mcheli.aircraft.MCH_BoundingBox;
import com.norwood.mcheli.aircraft.MCH_SeatInfo;
import com.norwood.mcheli.aircraft.MCH_SeatRackInfo;
import com.norwood.mcheli.helicopter.MCH_EntityHeli;
import com.norwood.mcheli.helicopter.MCH_HeliInfo;
import com.norwood.mcheli.helper.addon.AddonResourceLocation;
import com.norwood.mcheli.helper.info.ContentParseException;
import com.norwood.mcheli.helper.info.ContentParsers;
import com.norwood.mcheli.helper.info.parsers.IParser;
import com.norwood.mcheli.hud.*;
import com.norwood.mcheli.item.MCH_ItemInfo;
import com.norwood.mcheli.plane.MCP_EntityPlane;
import com.norwood.mcheli.plane.MCH_PlaneInfo;
import com.norwood.mcheli.ship.MCH_EntityShip;
import com.norwood.mcheli.ship.MCH_ShipInfo;
import com.norwood.mcheli.tank.MCH_EntityTank;
import com.norwood.mcheli.tank.MCH_TankInfo;
import com.norwood.mcheli.throwable.MCH_ThrowableInfo;
import com.norwood.mcheli.vehicle.MCH_EntityVehicle;
import com.norwood.mcheli.vehicle.MCH_VehicleInfo;
import com.norwood.mcheli.vehicle.MCH_VehicleInfo.VPart;
import com.norwood.mcheli.weapon.MCH_Cartridge;
import com.norwood.mcheli.weapon.MCH_SightType;
import com.norwood.mcheli.weapon.MCH_WeaponInfo;
import com.norwood.mcheli.weapon.MCH_WeaponInfo.MuzzleFlash;
import com.norwood.mcheli.weapon.MCH_WeaponInfo.RoundItem;
import com.norwood.mcheli.weapon.MCH_WeaponInfoManager;
import com.norwood.mcheli.wrapper.W_Item;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

// TODO: refactor the if-else chain with a massive switch or something
public class TxtParser implements IParser {
    public static final TxtParser INSTANCE = new TxtParser();

    private TxtParser() {
    }

    public static void register() {
        ContentParsers.register("txt", INSTANCE);
    }

    @Override
    @Nullable
    public MCH_HeliInfo parseHelicopter(AddonResourceLocation location, String filepath, List<String> lines, boolean reload) throws Exception {
        return parse(location, filepath, lines, reload, () -> new MCH_HeliInfo(location, filepath), this::applyAircraftLine,
                this::applyHelicopterLine);
    }

    @Override
    @Nullable
    public MCH_PlaneInfo parsePlane(AddonResourceLocation location, String filepath, List<String> lines, boolean reload) throws Exception {
        return parse(location, filepath, lines, reload, () -> new MCH_PlaneInfo(location, filepath), this::applyAircraftLine, this::applyPlaneLine);
    }

    @Override
    @Nullable
    public MCH_ShipInfo parseShip(AddonResourceLocation location, String filepath, List<String> lines, boolean reload) throws Exception {
        return parse(location, filepath, lines, reload, () -> new MCH_ShipInfo(location, filepath), this::applyAircraftLine, this::applyShipLine);
    }

    @Override
    @Nullable
    public MCH_TankInfo parseTank(AddonResourceLocation location, String filepath, List<String> lines, boolean reload) throws Exception {
        return parse(location, filepath, lines, reload, () -> new MCH_TankInfo(location, filepath), this::applyAircraftLine, this::applyTankLine);
    }

    @Override
    @Nullable
    public MCH_VehicleInfo parseVehicle(AddonResourceLocation location, String filepath, List<String> lines, boolean reload) throws Exception {
        return parse(location, filepath, lines, reload, () -> new MCH_VehicleInfo(location, filepath), this::applyAircraftLine,
                this::applyVehicleLine);
    }

    @Override
    @Nullable
    public MCH_WeaponInfo parseWeapon(AddonResourceLocation location, String filepath, List<String> lines, boolean reload) throws Exception {
        return parse(location, filepath, lines, reload, () -> new MCH_WeaponInfo(location, filepath), this::applyWeaponLine);
    }

    @Override
    @Nullable
    public MCH_ThrowableInfo parseThrowable(AddonResourceLocation location, String filepath, List<String> lines, boolean reload) throws Exception {
        return parse(location, filepath, lines, reload, () -> new MCH_ThrowableInfo(location, filepath), this::applyThrowableLine);
    }

    @Override
    @Nullable
    public MCH_Hud parseHud(AddonResourceLocation location, String filepath, List<String> lines, boolean reload) throws Exception {
        MCH_Hud info = new MCH_Hud(location, filepath);
        int lineNumber = 0;
        try {
            for (String raw : lines) {
                lineNumber++;
                String str = raw.trim();
                if (str.equalsIgnoreCase("endif")) {
                    str = "endif=0";
                } else if (str.equalsIgnoreCase("exit")) {
                    str = "exit=0";
                }

                int eqIdx = str.indexOf('=');
                if (eqIdx >= 0 && str.length() > eqIdx + 1) {
                    String item = str.substring(0, eqIdx).trim().toLowerCase();
                    String data = str.substring(eqIdx + 1).trim();
                    if (!reload || info.canReloadItem(item)) {
                        applyHudLine(info, lineNumber, item, data);
                    }
                }
            }
            return info;
        } catch (Exception ex) {
            throw new ContentParseException(ex, lineNumber);
        }
    }


    @Override
    @Nullable
    public MCH_ItemInfo parseItem(AddonResourceLocation location, String filepath, List<String> lines, boolean reload) throws Exception {
        String name = location.getPath();
        return parse(location, filepath, lines, reload, () -> new MCH_ItemInfo(location, filepath, name), this::applyItemLine);
    }

    @SafeVarargs
    private final <T extends MCH_BaseInfo> T parse(AddonResourceLocation location, String filepath, List<String> lines, boolean reload,
                                                   Supplier<T> factory, LineHandler<? super T>... handlers) throws Exception {
        T info = factory.get();
        parseLines(lines, (lineNumber, item, data) -> {
            if (reload && !info.canReloadItem(item)) {
                return;
            }
            for (LineHandler<? super T> handler : handlers) {
                handler.accept(info, lineNumber, item, data);
            }
        });
        return info;
    }

    private void parseLines(List<String> lines, LineProcessor processor) throws ContentParseException {
        int lineIdx = 0;
        try {
            for (String raw : lines) {
                lineIdx++;
                String str = raw.trim();
                int eqIdx = str.indexOf('=');
                if (eqIdx >= 0 && str.length() > eqIdx + 1) {
                    String item = str.substring(0, eqIdx).trim().toLowerCase();
                    String data = str.substring(eqIdx + 1).trim();
                    processor.accept(lineIdx, item, data);
                }
            }
        } catch (Exception ex) {
            throw new ContentParseException(ex, lineIdx);
        }
    }

    private void applyAircraftLine(MCH_AircraftInfo info, int lineNumber, String item, String data) {
        if (item.compareTo("displayname") == 0) {
            info.displayName = data.trim();
        } else if (item.compareTo("adddisplayname") == 0) {
            String[] s = data.split("\\s*,\\s*");
            if (s.length == 2) {
                info.displayNameLang.put(s[0].toLowerCase().trim(), s[1].trim());
            }
        } else if (item.equalsIgnoreCase("Category")) {
            info.category = data.toUpperCase().replaceAll("[,;:]", ".").replaceAll("[ \t]", "");
        } else if (item.equalsIgnoreCase("CanRide")) {
            info.canRide = info.toBool(data, true);
        } else if (item.equalsIgnoreCase("CreativeOnly")) {
            info.creativeOnly = info.toBool(data, false);
        } else if (item.equalsIgnoreCase("Invulnerable")) {
            info.invulnerable = info.toBool(data, false);
        } else if (item.equalsIgnoreCase("MaxFuel")) {
            info.maxFuel = info.toInt(data, 0, 100000000);
        } else if (item.equalsIgnoreCase("FuelConsumption")) {
            info.fuelConsumption = info.toFloat(data, 0.0F, 10000.0F);
        } else if (item.equalsIgnoreCase("FuelSupplyRange")) {
            info.fuelSupplyRange = info.toFloat(data, 0.0F, 1000.0F);
        } else if (item.equalsIgnoreCase("AmmoSupplyRange")) {
            info.ammoSupplyRange = info.toFloat(data, 0.0F, 1000.0F);
        } else if (item.equalsIgnoreCase("RepairOtherVehicles")) {
            String[] s = info.splitParam(data);
            if (s.length >= 1) {
                info.repairOtherVehiclesRange = info.toFloat(s[0], 0.0F, 1000.0F);
                if (s.length >= 2) {
                    info.repairOtherVehiclesValue = info.toInt(s[1], 0, 10000000);
                }
            }

            //else if(item.equalsIgnoreCase("RadarType")) {
            //    try {
            //        info.radarType = EnumRadarType.valueOf(data);
            //    } catch (Exception e) {
            //        info.radarType = EnumRadarType.MODERN_AA;
            //    }
            //}
            //else if(item.equalsIgnoreCase("RWRType")) {
            //    try {
            //        info.rwrType = EnumRWRType.valueOf(data);
            //    } catch (Exception e) {
            //        info.rwrType = EnumRWRType.DIGITAL;
            //    }
            //}
            else if (item.equalsIgnoreCase("NameOnModernAARadar")) {
                info.nameOnModernAARadar = data;
            } else if (item.equalsIgnoreCase("NameOnEarlyAARadar")) {
                info.nameOnEarlyAARadar = data;
            } else if (item.equalsIgnoreCase("NameOnModernASRadar")) {
                info.nameOnModernASRadar = data;
            } else if (item.equalsIgnoreCase("NameOnEarlyASRadar")) {
                info.nameOnEarlyASRadar = data;
            } else if (item.equalsIgnoreCase("ExplosionSizeByCrash")) {
                info.explosionSizeByCrash = info.toInt(data, 0, 100);
            } else if (item.equalsIgnoreCase("ThrottleDownFactor")) {
                info.throttleDownFactor = info.toFloat(data, 0, 10);
            }

        } else if (item.compareTo("itemid") == 0) {
            info.itemID = info.toInt(data, 0, 65535);
        } else if (item.compareTo("addtexture") == 0) {
            info.addTextureName(data.toLowerCase());
        } else if (item.compareTo("particlesscale") == 0) {
            info.particlesScale = info.toFloat(data, 0.0F, 50.0F);
        } else if (item.equalsIgnoreCase("EnableSeaSurfaceParticle")) {
            info.enableSeaSurfaceParticle = info.toBool(data);
        } else if (item.equalsIgnoreCase("AddParticleSplash")) {
            String[] s = info.splitParam(data);
            if (s.length >= 3) {
                Vec3d v = info.toVec3(s[0], s[1], s[2]);
                int num = s.length >= 4 ? info.toInt(s[3], 1, 100) : 2;
                float size = s.length >= 5 ? info.toFloat(s[4]) : 2.0F;
                float acc = s.length >= 6 ? info.toFloat(s[5]) : 1.0F;
                int age = s.length >= 7 ? info.toInt(s[6], 1, 100000) : 80;
                float motionY = s.length >= 8 ? info.toFloat(s[7]) : 0.01F;
                float gravity = s.length >= 9 ? info.toFloat(s[8]) : 0.0F;
                info.particleSplashs.add(new ParticleSplash(info, v, num, size, acc, age, motionY, gravity));
            }
        } else if (item.equalsIgnoreCase("AddSearchLight") || item.equalsIgnoreCase("AddFixedSearchLight") ||
                   item.equalsIgnoreCase("AddSteeringSearchLight")) {
            String[] s = info.splitParam(data);
            if (s.length >= 7) {
                Vec3d v = info.toVec3(s[0], s[1], s[2]);
                int cs = info.hex2dec(s[3]);
                int ce = info.hex2dec(s[4]);
                float h = info.toFloat(s[5]);
                float w = info.toFloat(s[6]);
                float yaw = s.length >= 8 ? info.toFloat(s[7]) : 0.0F;
                float pitch = s.length >= 9 ? info.toFloat(s[8]) : 0.0F;
                float stRot = s.length >= 10 ? info.toFloat(s[9]) : 0.0F;
                boolean fixDir = !item.equalsIgnoreCase("AddSearchLight");
                boolean steering = item.equalsIgnoreCase("AddSteeringSearchLight");
                info.searchLights.add(new SearchLight(info, v, cs, ce, h, w, fixDir, yaw, pitch, steering, stRot));
            }
        } else if (item.equalsIgnoreCase("AddPartLightHatch")) {
            String[] s = info.splitParam(data);
            if (s.length >= 6) {
                float mx = s.length >= 7 ? info.toFloat(s[6], -1800.0F, 1800.0F) : 90.0F;
                info.lightHatchList.add(
                        new Hatch(info, info.toFloat(s[0]), info.toFloat(s[1]), info.toFloat(s[2]), info.toFloat(s[3]), info.toFloat(s[4]),
                                info.toFloat(s[5]), mx, "light_hatch" + info.lightHatchList.size(), false));
            }
        } else if (item.equalsIgnoreCase("AddRepellingHook")) {
            String[] s = info.splitParam(data);
            if (s != null && s.length >= 3) {
                int inv = s.length >= 4 ? info.toInt(s[3], 1, 100000) : 10;
                info.repellingHooks.add(new RepellingHook(info, info.toVec3(s[0], s[1], s[2]), inv));
            }
        } else if (item.equalsIgnoreCase("AddRack")) {
            String[] s = data.toLowerCase().split("\\s*,\\s*");
            if (s.length >= 7) {
                String[] names = s[0].split("\\s*/\\s*");
                float range = s.length >= 8 ? info.toFloat(s[7]) : 6.0F;
                float para = s.length >= 9 ? info.toFloat(s[8], 0.0F, 1000000.0F) : 20.0F;
                float yaw = s.length >= 10 ? info.toFloat(s[9]) : 0.0F;
                float pitch = s.length >= 11 ? info.toFloat(s[10]) : 0.0F;
                boolean rs = s.length >= 12 && info.toBool(s[11]);
                info.entityRackList.add(new MCH_SeatRackInfo(names, info.toDouble(s[1]), info.toDouble(s[2]), info.toDouble(s[3]),
                        new CameraPosition(info, info.toVec3(s[4], s[5], s[6]).add(0.0, 1.5, 0.0)), range, para, yaw, pitch, rs));
            }
        } else if (item.equalsIgnoreCase("RideRack")) {
            String[] s = info.splitParam(data);
            if (s.length >= 2) {
                MCH_AircraftInfo.RideRack r = new RideRack(info, s[0].trim().toLowerCase(), info.toInt(s[1], 1, 10000));
                info.rideRacks.add(r);
            }
        } else if (item.equalsIgnoreCase("AddSeat") || item.equalsIgnoreCase("AddGunnerSeat") || item.equalsIgnoreCase("AddFixRotSeat")) {
            if (info.seatList.size() >= info.getInfo_MaxSeatNum()) {
                return;
            }

            String[] s = info.splitParam(data);
            if (s.length < 3) {
                return;
            }

            Vec3d p = info.toVec3(s[0], s[1], s[2]);
            if (item.equalsIgnoreCase("AddSeat")) {
                boolean rs = s.length >= 4 && info.toBool(s[3]);
                MCH_SeatInfo seat = new MCH_SeatInfo(p, rs);
                info.seatList.add(seat);
            } else {
                MCH_SeatInfo seat;
                if (s.length >= 6) {
                    MCH_AircraftInfo.CameraPosition c = new CameraPosition(info, info.toVec3(s[3], s[4], s[5]));
                    boolean sg = s.length >= 7 && info.toBool(s[6]);
                    if (item.equalsIgnoreCase("AddGunnerSeat")) {
                        if (s.length >= 9) {
                            float minPitch = info.toFloat(s[7], -90.0F, 90.0F);
                            float maxPitch = info.toFloat(s[8], -90.0F, 90.0F);
                            if (minPitch > maxPitch) {
                                float t = minPitch;
                                minPitch = maxPitch;
                                maxPitch = t;
                            }

                            boolean rs = s.length >= 10 && info.toBool(s[9]);
                            seat = new MCH_SeatInfo(p, true, c, true, sg, false, 0.0F, 0.0F, minPitch, maxPitch, rs);
                        } else {
                            seat = new MCH_SeatInfo(p, true, c, true, sg, false, 0.0F, 0.0F, false);
                        }
                    } else {
                        boolean fixRot = s.length >= 9;
                        float fixYaw = fixRot ? info.toFloat(s[7]) : 0.0F;
                        float fixPitch = fixRot ? info.toFloat(s[8]) : 0.0F;
                        boolean rs = s.length >= 10 && info.toBool(s[9]);
                        seat = new MCH_SeatInfo(p, true, c, true, sg, fixRot, fixYaw, fixPitch, rs);
                    }
                } else {
                    seat = new MCH_SeatInfo(p, true, new CameraPosition(info), false, false, false, 0.0F, 0.0F, false);
                }

                info.seatList.add(seat);
            }
        } else if (item.equalsIgnoreCase("SetWheelPos")) {
            String[] sx = info.splitParam(data);
            if (sx.length >= 4) {
                float x = Math.abs(info.toFloat(sx[0]));
                float y = info.toFloat(sx[1]);
                info.wheels.clear();

                for (int i = 2; i < sx.length; i++) {
                    info.wheels.add(new Wheel(info, new Vec3d(x, y, info.toFloat(sx[i]))));
                }

                info.wheels.sort((arg0, arg1) -> arg0.pos.z > arg1.pos.z ? -1 : 1);
            }
        } else if (item.equalsIgnoreCase("ExclusionSeat")) {
            String[] sx = info.splitParam(data);
            if (sx.length >= 2) {
                Integer[] a = new Integer[sx.length];

                for (int i = 0; i < a.length; i++) {
                    a[i] = info.toInt(sx[i], 1, 10000) - 1;
                }

                info.exclusionSeatList.add(a);
            }
        } else if (MCH_MOD.proxy.isRemote() && item.equalsIgnoreCase("HUD")) {
            info.hudList.clear();
            String[] ss = data.split("\\s*,\\s*");

            for (String sx : ss) {
                MCH_Hud hud = MCH_HudManager.get(sx);
                if (hud == null) {
                    hud = MCH_Hud.NoDisp;
                }

                info.hudList.add(hud);
            }
        } else if (item.compareTo("enablenightvision") == 0) {
            info.isEnableNightVision = info.toBool(data);
        } else if (item.compareTo("enableentityradar") == 0) {
            info.isEnableEntityRadar = info.toBool(data);
        } else if (item.equalsIgnoreCase("EnableEjectionSeat")) {
            info.isEnableEjectionSeat = info.toBool(data);
        } else if (item.equalsIgnoreCase("EnableParachuting")) {
            info.isEnableParachuting = info.toBool(data);
        } else if (item.equalsIgnoreCase("MobDropOption")) {
            String[] sx = info.splitParam(data);
            if (sx.length >= 3) {
                info.mobDropOption.pos = info.toVec3(sx[0], sx[1], sx[2]);
                info.mobDropOption.interval = sx.length >= 4 ? info.toInt(sx[3]) : 12;
            }
        } else if (item.equalsIgnoreCase("Width")) {
            info.bodyWidth = info.toFloat(data, 0.1F, 1000.0F);
        } else if (item.equalsIgnoreCase("Height")) {
            info.bodyHeight = info.toFloat(data, 0.1F, 1000.0F);
        } else if (item.compareTo("float") == 0) {
            info.isFloat = info.toBool(data);
        } else if (item.compareTo("floatoffset") == 0) {
            info.floatOffset = -info.toFloat(data);
        } else if (item.compareTo("gravity") == 0) {
            info.gravity = info.toFloat(data, -50.0F, 50.0F);
        } else if (item.compareTo("gravityinwater") == 0) {
            info.gravityInWater = info.toFloat(data, -50.0F, 50.0F);
        } else if (item.compareTo("cameraposition") == 0) {
            String[] sx = data.split("\\s*,\\s*");
            if (sx.length >= 3) {
                info.alwaysCameraView = sx.length >= 4 && info.toBool(sx[3]);
                boolean fixRot = sx.length >= 5;
                float yaw = sx.length >= 5 ? info.toFloat(sx[4]) : 0.0F;
                float pitch = sx.length >= 6 ? info.toFloat(sx[5]) : 0.0F;
                info.cameraPosition.add(new CameraPosition(info, info.toVec3(sx[0], sx[1], sx[2]), fixRot, yaw, pitch));
            }
        } else if (item.equalsIgnoreCase("UnmountPosition")) {
            String[] sx = data.split("\\s*,\\s*");
            if (sx.length >= 3) {
                info.unmountPosition = info.toVec3(sx[0], sx[1], sx[2]);
            }
        } else if (item.equalsIgnoreCase("ThirdPersonDist")) {
            info.thirdPersonDist = info.toFloat(data, 4.0F, 100.0F);
        } else if (item.equalsIgnoreCase("TurretPosition")) {
            String[] sx = data.split("\\s*,\\s*");
            if (sx.length >= 3) {
                info.turretPosition = info.toVec3(sx[0], sx[1], sx[2]);
            }
        } else if (item.equalsIgnoreCase("CameraRotationSpeed")) {
            info.cameraRotationSpeed = info.toFloat(data, 0.0F, 10000.0F);
        } else if (item.compareTo("regeneration") == 0) {
            info.regeneration = info.toBool(data);
        } else if (item.compareTo("speed") == 0) {
            info.speed = info.toFloat(data, 0.0F, info.getMaxSpeed());
        } else if (item.equalsIgnoreCase("EnableBack")) {
            info.enableBack = info.toBool(data);
        } else if (item.equalsIgnoreCase("MotionFactor")) {
            info.motionFactor = info.toFloat(data, 0.0F, 1.0F);
        } else if (item.equalsIgnoreCase("MobilityYawOnGround")) {
            info.mobilityYawOnGround = info.toFloat(data, 0.0F, 100.0F);
        } else if (item.equalsIgnoreCase("MobilityYaw")) {
            info.mobilityYaw = info.toFloat(data, 0.0F, 100.0F);
        } else if (item.equalsIgnoreCase("MobilityPitch")) {
            info.mobilityPitch = info.toFloat(data, 0.0F, 100.0F);
        } else if (item.equalsIgnoreCase("MobilityRoll")) {
            info.mobilityRoll = info.toFloat(data, 0.0F, 100.0F);
        } else if (item.equalsIgnoreCase("MinRotationPitch")) {
            info.limitRotation = true;
            info.minRotationPitch = info.toFloat(data, info.getMinRotationPitch(), 0.0F);
        } else if (item.equalsIgnoreCase("MaxRotationPitch")) {
            info.limitRotation = true;
            info.maxRotationPitch = info.toFloat(data, 0.0F, info.getMaxRotationPitch());
        } else if (item.equalsIgnoreCase("MinRotationRoll")) {
            info.limitRotation = true;
            info.minRotationRoll = info.toFloat(data, info.getMinRotationRoll(), 0.0F);
        } else if (item.equalsIgnoreCase("MaxRotationRoll")) {
            info.limitRotation = true;
            info.maxRotationRoll = info.toFloat(data, 0.0F, info.getMaxRotationRoll());
        } else if (item.compareTo("throttleupdown") == 0) {
            info.throttleUpDown = info.toFloat(data, 0.0F, 3.0F);
        } else if (item.equalsIgnoreCase("ThrottleUpDownOnEntity")) {
            info.throttleUpDownOnEntity = info.toFloat(data, 0.0F, 100_000.0F);
        } else if (item.equalsIgnoreCase("Stealth")) {
            info.stealth = info.toFloat(data, 0.0F, 1.0F);
        } else if (item.equalsIgnoreCase("EntityWidth")) {
            info.entityWidth = info.toFloat(data, -100.0F, 100.0F);
        } else if (item.equalsIgnoreCase("EntityHeight")) {
            info.entityHeight = info.toFloat(data, -100.0F, 100.0F);
        } else if (item.equalsIgnoreCase("EntityPitch")) {
            info.entityPitch = info.toFloat(data, -360.0F, 360.0F);
        } else if (item.equalsIgnoreCase("EntityRoll")) {
            info.entityRoll = info.toFloat(data, -360.0F, 360.0F);
        } else if (item.equalsIgnoreCase("StepHeight")) {
            info.stepHeight = info.toFloat(data, 0.0F, 1000.0F);
        } else if (item.equalsIgnoreCase("CanMoveOnGround")) {
            info.canMoveOnGround = info.toBool(data);
        } else if (item.equalsIgnoreCase("CanRotOnGround")) {
            info.canRotOnGround = info.toBool(data);
        } else if (item.equalsIgnoreCase("AddWeapon") || item.equalsIgnoreCase("AddTurretWeapon")) {
            String[] sx = data.split("\\s*,\\s*");
            String type = sx[0].toLowerCase();
            if (sx.length >= 4 && MCH_WeaponInfoManager.contains(type)) {
                float y = sx.length >= 5 ? info.toFloat(sx[4]) : 0.0F;
                float p = sx.length >= 6 ? info.toFloat(sx[5]) : 0.0F;
                boolean canUsePilot = sx.length < 7 || info.toBool(sx[6]);
                int seatID = sx.length >= 8 ? info.toInt(sx[7], 1, info.getInfo_MaxSeatNum()) - 1 : 0;
                if (seatID <= 0) {
                    canUsePilot = true;
                }

                float dfy = sx.length >= 9 ? info.toFloat(sx[8]) : 0.0F;
                dfy = MathHelper.wrapDegrees(dfy);
                float mny = sx.length >= 10 ? info.toFloat(sx[9]) : 0.0F;
                float mxy = sx.length >= 11 ? info.toFloat(sx[10]) : 0.0F;
                float mnp = sx.length >= 12 ? info.toFloat(sx[11]) : 0.0F;
                float mxp = sx.length >= 13 ? info.toFloat(sx[12]) : 0.0F;
                MCH_AircraftInfo.Weapon e =
                        new Weapon(info, info.toFloat(sx[1]), info.toFloat(sx[2]), info.toFloat(sx[3]), y, p, canUsePilot, seatID, dfy, mny, mxy, mnp,
                                mxp, item.equalsIgnoreCase("AddTurretWeapon"));
                WeaponSet set = info.getOrCreateWeaponSet(type);
                set.weapons.add(e);
            }
        } else if (item.equalsIgnoreCase("AddPartWeapon") || item.equalsIgnoreCase("AddPartRotWeapon") ||
                   item.equalsIgnoreCase("AddPartTurretWeapon") || item.equalsIgnoreCase("AddPartTurretRotWeapon") ||
                   item.equalsIgnoreCase("AddPartWeaponMissile")) {
            String[] sx = data.split("\\s*,\\s*");
            if (sx.length >= 7) {
                float rx = 0.0F;
                float ry = 0.0F;
                float rz = 0.0F;
                float rb = 0.0F;
                boolean isRot = item.equalsIgnoreCase("AddPartRotWeapon") || item.equalsIgnoreCase("AddPartTurretRotWeapon");
                boolean isMissile = item.equalsIgnoreCase("AddPartWeaponMissile");
                boolean turret = item.equalsIgnoreCase("AddPartTurretWeapon") || item.equalsIgnoreCase("AddPartTurretRotWeapon");
                if (isRot) {
                    rx = sx.length >= 10 ? info.toFloat(sx[7]) : 0.0F;
                    ry = sx.length >= 10 ? info.toFloat(sx[8]) : 0.0F;
                    rz = sx.length >= 10 ? info.toFloat(sx[9]) : -1.0F;
                } else {
                    rb = sx.length >= 8 ? info.toFloat(sx[7]) : 0.0F;
                }

                MCH_AircraftInfo.PartWeapon w =
                        new PartWeapon(info, info.splitParamSlash(sx[0].toLowerCase().trim()), isRot, isMissile, info.toBool(sx[1]),
                                info.toBool(sx[2]), info.toBool(sx[3]), info.toFloat(sx[4]), info.toFloat(sx[5]), info.toFloat(sx[6]),
                                "weapon" + info.partWeapon.size(), rx, ry, rz, rb, turret);
                info.setLastWeaponPart(w);
                info.partWeapon.add(w);
            }
        } else if (item.equalsIgnoreCase("AddPartWeaponChild")) {
            String[] sx = data.split("\\s*,\\s*");
            if (sx.length >= 5 && info.getLastWeaponPart() != null) {
                float rb = sx.length >= 6 ? info.toFloat(sx[5]) : 0.0F;
                MCH_AircraftInfo.PartWeaponChild w =
                        new PartWeaponChild(info, info.getLastWeaponPart().name, info.toBool(sx[0]), info.toBool(sx[1]), info.toFloat(sx[2]),
                                info.toFloat(sx[3]), info.toFloat(sx[4]),
                                info.getLastWeaponPart().modelName + "_" + info.getLastWeaponPart().child.size(), 0.0F, 0.0F, 0.0F, rb);
                info.getLastWeaponPart().child.add(w);
            }
        } else if (item.compareTo("addrecipe") == 0 || item.compareTo("addshapelessrecipe") == 0) {
            info.isShapedRecipe = item.compareTo("addrecipe") == 0;
            info.recipeString.add(data.toUpperCase());
        } else if (item.compareTo("maxhp") == 0) {
            info.maxHp = info.toInt(data, 1, 1000000000);
        } else if (item.compareTo("inventorysize") == 0) {
            info.inventorySize = info.toInt(data, 0, 54);
        } else if (item.compareTo("damagefactor") == 0) {
            info.damageFactor = info.toFloat(data, 0.0F, 1.0F);
        } else if (item.equalsIgnoreCase("SubmergedDamageHeight")) {
            info.submergedDamageHeight = info.toFloat(data, -1000.0F, 1000.0F);
        } else if (item.equalsIgnoreCase("ArmorDamageFactor")) {
            info.armorDamageFactor = info.toFloat(data, 0.0F, 10000.0F);
        } else if (item.equalsIgnoreCase("ArmorMinDamage")) {
            info.armorMinDamage = info.toFloat(data, 0.0F, 1000000.0F);
        } else if (item.equalsIgnoreCase("ArmorMaxDamage")) {
            info.armorMaxDamage = info.toFloat(data, 0.0F, 1000000.0F);
        } else if (item.equalsIgnoreCase("FlareType")) {
            String[] sx = data.split("\\s*,\\s*");
            info.flare.types = new int[sx.length];

            for (int i = 0; i < sx.length; i++) {
                info.flare.types[i] = info.toInt(sx[i], 1, 10);
            }
        } else if (item.equalsIgnoreCase("FlareOption")) {
            String[] sx = info.splitParam(data);
            if (sx.length >= 3) {
                info.flare.pos = info.toVec3(sx[0], sx[1], sx[2]);
            }
        } else if (item.equalsIgnoreCase("Sound")) {
            info.soundMove = data.toLowerCase();
        } else if (item.equalsIgnoreCase("SoundRange")) {
            info.soundRange = info.toFloat(data, 1.0F, 1000.0F);
        } else if (item.equalsIgnoreCase("SoundVolume")) {
            info.soundVolume = info.toFloat(data, 0.0F, 10.0F);
        } else if (item.equalsIgnoreCase("SoundPitch")) {
            info.soundPitch = info.toFloat(data, 0.0F, 10.0F);
        } else if (item.equalsIgnoreCase("UAV")) {
            info.isUAV = info.toBool(data);
            info.isSmallUAV = false;
        } else if (item.equalsIgnoreCase("SmallUAV")) {
            info.isUAV = info.toBool(data);
            info.isSmallUAV = true;
        } else if (item.equalsIgnoreCase("TargetDrone")) {
            info.isTargetDrone = info.toBool(data);
        } else if (item.compareTo("autopilotrot") == 0) {
            info.autoPilotRot = info.toFloat(data, -5.0F, 5.0F);
        } else if (item.compareTo("ongroundpitch") == 0) {
            info.onGroundPitch = -info.toFloat(data, -90.0F, 90.0F);
        } else if (item.compareTo("enablegunnermode") == 0) {
            info.isEnableGunnerMode = info.toBool(data);
        } else if (item.compareTo("hideentity") == 0) {
            info.hideEntity = info.toBool(data);
        } else if (item.equalsIgnoreCase("SmoothShading")) {
            info.smoothShading = info.toBool(data);
        } else if (item.compareTo("concurrentgunnermode") == 0) {
            info.isEnableConcurrentGunnerMode = info.toBool(data);
        } else if (item.equalsIgnoreCase("AddPartWeaponBay") || item.equalsIgnoreCase("AddPartSlideWeaponBay")) {
            boolean slide = item.equalsIgnoreCase("AddPartSlideWeaponBay");
            String[] sx = data.split("\\s*,\\s*");
            MCH_AircraftInfo.WeaponBay n;
            if (slide) {
                if (sx.length >= 4) {
                    n = new WeaponBay(info, sx[0].trim().toLowerCase(), info.toFloat(sx[1]), info.toFloat(sx[2]), info.toFloat(sx[3]), 0.0F, 0.0F,
                            0.0F, 90.0F, "wb" + info.partWeaponBay.size(), true);
                    info.partWeaponBay.add(n);
                }
            } else if (sx.length >= 7) {
                float mx = sx.length >= 8 ? info.toFloat(sx[7], -180.0F, 180.0F) : 90.0F;
                n = new WeaponBay(info, sx[0].trim().toLowerCase(), info.toFloat(sx[1]), info.toFloat(sx[2]), info.toFloat(sx[3]),
                        info.toFloat(sx[4]), info.toFloat(sx[5]), info.toFloat(sx[6]), mx / 90.0F, "wb" + info.partWeaponBay.size(), false);
                info.partWeaponBay.add(n);
            }
        } else if (item.compareTo("addparthatch") == 0 || item.compareTo("addpartslidehatch") == 0) {
            boolean slide = item.compareTo("addpartslidehatch") == 0;
            String[] sx = data.split("\\s*,\\s*");
            MCH_AircraftInfo.Hatch n;
            if (slide) {
                if (sx.length >= 3) {
                    n = new Hatch(info, info.toFloat(sx[0]), info.toFloat(sx[1]), info.toFloat(sx[2]), 0.0F, 0.0F, 0.0F, 90.0F,
                            "hatch" + info.hatchList.size(), true);
                    info.hatchList.add(n);
                }
            } else if (sx.length >= 6) {
                float mx = sx.length >= 7 ? info.toFloat(sx[6], -180.0F, 180.0F) : 90.0F;
                n = new Hatch(info, info.toFloat(sx[0]), info.toFloat(sx[1]), info.toFloat(sx[2]), info.toFloat(sx[3]), info.toFloat(sx[4]),
                        info.toFloat(sx[5]), mx, "hatch" + info.hatchList.size(), false);
                info.hatchList.add(n);
            }
        } else if (item.compareTo("addpartcanopy") == 0 || item.compareTo("addpartslidecanopy") == 0) {
            String[] sx = data.split("\\s*,\\s*");
            boolean slide = item.compareTo("addpartslidecanopy") == 0;
            int canopyNum = info.canopyList.size();
            if (canopyNum > 0) {
                canopyNum--;
            }

            if (slide) {
                if (sx.length >= 3) {
                    MCH_AircraftInfo.Canopy c =
                            new Canopy(info, info.toFloat(sx[0]), info.toFloat(sx[1]), info.toFloat(sx[2]), 0.0F, 0.0F, 0.0F, 90.0F,
                                    "canopy" + canopyNum, true);
                    info.canopyList.add(c);
                    if (canopyNum == 0) {
                        c = new Canopy(info, info.toFloat(sx[0]), info.toFloat(sx[1]), info.toFloat(sx[2]), 0.0F, 0.0F, 0.0F, 90.0F, "canopy", true);
                        info.canopyList.add(c);
                    }
                }
            } else if (sx.length >= 6) {
                float mx = sx.length >= 7 ? info.toFloat(sx[6], -180.0F, 180.0F) : 90.0F;
                mx /= 90.0F;
                MCH_AircraftInfo.Canopy c =
                        new Canopy(info, info.toFloat(sx[0]), info.toFloat(sx[1]), info.toFloat(sx[2]), info.toFloat(sx[3]), info.toFloat(sx[4]),
                                info.toFloat(sx[5]), mx, "canopy" + canopyNum, false);
                info.canopyList.add(c);
                if (canopyNum == 0) {
                    c = new Canopy(info, info.toFloat(sx[0]), info.toFloat(sx[1]), info.toFloat(sx[2]), info.toFloat(sx[3]), info.toFloat(sx[4]),
                            info.toFloat(sx[5]), mx, "canopy", false);
                    info.canopyList.add(c);
                }
            }
        } else if (item.equalsIgnoreCase("AddPartLG") || item.equalsIgnoreCase("AddPartSlideRotLG") || item.equalsIgnoreCase("AddPartLGRev") ||
                   item.equalsIgnoreCase("AddPartLGHatch")) {
            String[] sxx = data.split("\\s*,\\s*");
            if (!item.equalsIgnoreCase("AddPartSlideRotLG") && sxx.length >= 6) {
                float maxRot = sxx.length >= 7 ? info.toFloat(sxx[6], -180.0F, 180.0F) : 90.0F;
                maxRot /= 90.0F;
                MCH_AircraftInfo.LandingGear n =
                        new LandingGear(info, info.toFloat(sxx[0]), info.toFloat(sxx[1]), info.toFloat(sxx[2]), info.toFloat(sxx[3]),
                                info.toFloat(sxx[4]), info.toFloat(sxx[5]), "lg" + info.landingGear.size(), maxRot,
                                item.equalsIgnoreCase("AddPartLgRev"), item.equalsIgnoreCase("AddPartLGHatch"));
                if (sxx.length >= 8) {
                    n.enableRot2 = true;
                    n.maxRotFactor2 = sxx.length >= 11 ? info.toFloat(sxx[10], -180.0F, 180.0F) : 90.0F;
                    n.maxRotFactor2 /= 90.0F;
                    n.rot2 = new Vec3d(info.toFloat(sxx[7]), info.toFloat(sxx[8]), info.toFloat(sxx[9]));
                }

                info.landingGear.add(n);
            }

            if (item.equalsIgnoreCase("AddPartSlideRotLG") && sxx.length >= 9) {
                float maxRot = sxx.length >= 10 ? info.toFloat(sxx[9], -180.0F, 180.0F) : 90.0F;
                maxRot /= 90.0F;
                MCH_AircraftInfo.LandingGear n =
                        new LandingGear(info, info.toFloat(sxx[3]), info.toFloat(sxx[4]), info.toFloat(sxx[5]), info.toFloat(sxx[6]),
                                info.toFloat(sxx[7]), info.toFloat(sxx[8]), "lg" + info.landingGear.size(), maxRot, false, false);
                n.slide = new Vec3d(info.toFloat(sxx[0]), info.toFloat(sxx[1]), info.toFloat(sxx[2]));
                info.landingGear.add(n);
            }
        } else if (item.equalsIgnoreCase("AddPartThrottle")) {
            String[] sxxx = data.split("\\s*,\\s*");
            if (sxxx.length >= 7) {
                float x = sxxx.length >= 8 ? info.toFloat(sxxx[7]) : 0.0F;
                float yx = sxxx.length >= 9 ? info.toFloat(sxxx[8]) : 0.0F;
                float z = sxxx.length >= 10 ? info.toFloat(sxxx[9]) : 0.0F;
                MCH_AircraftInfo.Throttle c =
                        new Throttle(info, info.toFloat(sxxx[0]), info.toFloat(sxxx[1]), info.toFloat(sxxx[2]), info.toFloat(sxxx[3]),
                                info.toFloat(sxxx[4]), info.toFloat(sxxx[5]), info.toFloat(sxxx[6]), "throttle" + info.partThrottle.size(), x, yx, z);
                info.partThrottle.add(c);
            }
        } else if (item.equalsIgnoreCase("AddPartRotation")) {
            String[] sxxx = data.split("\\s*,\\s*");
            if (sxxx.length >= 7) {
                boolean always = sxxx.length < 8 || info.toBool(sxxx[7]);
                MCH_AircraftInfo.RotPart c =
                        new RotPart(info, info.toFloat(sxxx[0]), info.toFloat(sxxx[1]), info.toFloat(sxxx[2]), info.toFloat(sxxx[3]),
                                info.toFloat(sxxx[4]), info.toFloat(sxxx[5]), info.toFloat(sxxx[6]), always, "rotpart" + info.partThrottle.size());
                info.partRotPart.add(c);
            }
        } else if (item.compareTo("addpartcamera") == 0) {
            String[] sxxx = data.split("\\s*,\\s*");
            if (sxxx.length >= 3) {
                boolean ys = sxxx.length < 4 || info.toBool(sxxx[3]);
                boolean ps = sxxx.length >= 5 && info.toBool(sxxx[4]);
                MCH_AircraftInfo.Camera c = new Camera(info, info.toFloat(sxxx[0]), info.toFloat(sxxx[1]), info.toFloat(sxxx[2]), 0.0F, -1.0F, 0.0F,
                        "camera" + info.cameraList.size(), ys, ps);
                info.cameraList.add(c);
            }
        } else if (item.equalsIgnoreCase("AddPartWheel")) {
            String[] sxxx = info.splitParam(data);
            if (sxxx.length >= 3) {
                float rd = sxxx.length >= 4 ? info.toFloat(sxxx[3], -1800.0F, 1800.0F) : 0.0F;
                float rx = sxxx.length >= 7 ? info.toFloat(sxxx[4]) : 0.0F;
                float ry = sxxx.length >= 7 ? info.toFloat(sxxx[5]) : 1.0F;
                float rz = sxxx.length >= 7 ? info.toFloat(sxxx[6]) : 0.0F;
                float px = sxxx.length >= 10 ? info.toFloat(sxxx[7]) : info.toFloat(sxxx[0]);
                float py = sxxx.length >= 10 ? info.toFloat(sxxx[8]) : info.toFloat(sxxx[1]);
                float pz = sxxx.length >= 10 ? info.toFloat(sxxx[9]) : info.toFloat(sxxx[2]);
                info.partWheel.add(
                        new PartWheel(info, info.toFloat(sxxx[0]), info.toFloat(sxxx[1]), info.toFloat(sxxx[2]), rx, ry, rz, rd, px, py, pz,
                                "wheel" + info.partWheel.size()));
            }
        } else if (item.equalsIgnoreCase("AddPartSteeringWheel")) {
            String[] sxxx = info.splitParam(data);
            if (sxxx.length >= 7) {
                info.partSteeringWheel.add(new PartWheel(info, info.toFloat(sxxx[0]), info.toFloat(sxxx[1]), info.toFloat(sxxx[2]), info.toFloat(
                        sxxx[3]), info.toFloat(sxxx[4]), info.toFloat(sxxx[5]), info.toFloat(sxxx[6]), "steering_wheel" +
                                                                                                       info.partSteeringWheel.size()));
            }
        } else if (item.equalsIgnoreCase("AddTrackRoller")) {
            String[] sxxx = info.splitParam(data);
            if (sxxx.length >= 3) {
                info.partTrackRoller.add(new TrackRoller(info, info.toFloat(sxxx[0]), info.toFloat(sxxx[1]), info.toFloat(sxxx[2]),
                        "track_roller" + info.partTrackRoller.size()));
            }
        } else if (item.equalsIgnoreCase("AddCrawlerTrack")) {
            info.partCrawlerTrack.add(info.createCrawlerTrack(data, "crawler_track" + info.partCrawlerTrack.size()));
        } else if (item.equalsIgnoreCase("PivotTurnThrottle")) {
            info.pivotTurnThrottle = info.toFloat(data, 0.0F, 1.0F);
        } else if (item.equalsIgnoreCase("TrackRollerRot")) {
            info.trackRollerRot = info.toFloat(data, -10000.0F, 10000.0F);
        } else if (item.equalsIgnoreCase("PartWheelRot")) {
            info.partWheelRot = info.toFloat(data, -10000.0F, 10000.0F);
        } else if (item.compareTo("camerazoom") == 0) {
            info.cameraZoom = info.toInt(data, 1, 10);
        } else if (item.equalsIgnoreCase("DefaultFreelook")) {
            info.defaultFreelook = info.toBool(data);
        } else if (item.equalsIgnoreCase("BoundingBox")) {
            String[] sxxx = data.split("\\s*,\\s*");
            if (sxxx.length >= 5) {
                float df = sxxx.length >= 6 ? info.toFloat(sxxx[5]) : 1.0F;
                MCH_BoundingBox c = new MCH_BoundingBox(info.toFloat(sxxx[0]), info.toFloat(sxxx[1]), info.toFloat(sxxx[2]), info.toFloat(sxxx[3]),
                        info.toFloat(sxxx[4]), df);
                info.extraBoundingBox.add(c);
                if (c.getBoundingBox().maxY > info.markerHeight) {
                    info.markerHeight = (float) c.getBoundingBox().maxY;
                }

                info.markerWidth = (float) Math.max(info.markerWidth, Math.abs(c.getBoundingBox().maxX) / 2.0);
                info.markerWidth = (float) Math.max(info.markerWidth, Math.abs(c.getBoundingBox().minX) / 2.0);
                info.markerWidth = (float) Math.max(info.markerWidth, Math.abs(c.getBoundingBox().maxZ) / 2.0);
                info.markerWidth = (float) Math.max(info.markerWidth, Math.abs(c.getBoundingBox().minZ) / 2.0);
                info.bbZmin = (float) Math.min(info.bbZmin, c.getBoundingBox().minZ);
                info.bbZmax = (float) Math.min(info.bbZmax, c.getBoundingBox().maxZ);
            }
        } else if (item.equalsIgnoreCase("RotorSpeed")) {
            info.rotorSpeed = info.toFloat(data, -10000.0F, 10000.0F);
            if (info.rotorSpeed > 0.01) {
                info.rotorSpeed = (float) (info.rotorSpeed - 0.01);
            }

            if (info.rotorSpeed < -0.01) {
                info.rotorSpeed = (float) (info.rotorSpeed + 0.01);
            }
        } else if (item.equalsIgnoreCase("OnGroundPitchFactor")) {
            info.onGroundPitchFactor = info.toFloat(data, 0.0F, 180.0F);
        } else if (item.equalsIgnoreCase("OnGroundRollFactor")) {
            info.onGroundRollFactor = info.toFloat(data, 0.0F, 180.0F);
        }
    }

    private void applyHelicopterLine(MCH_HeliInfo info, int lineNumber, String item, String data) {
        if (item.compareTo("enablefoldblade") == 0) {
            info.isEnableFoldBlade = info.toBool(data);
        } else if (item.compareTo("addrotor") == 0 || item.compareTo("addrotorold") == 0) {
            String[] s = data.split("\\s*,\\s*");
            if (s.length == 8 || s.length == 9) {
                boolean cfb = s.length == 9 && info.toBool(s[8]);
                MCH_HeliInfo.Rotor e =
                        new MCH_HeliInfo.Rotor(info, info.toInt(s[0]), info.toInt(s[1]), info.toFloat(s[2]), info.toFloat(s[3]), info.toFloat(s[4]),
                                info.toFloat(s[5]), info.toFloat(s[6]), info.toFloat(s[7]), "blade" + info.rotorList.size(), cfb,
                                item.compareTo("addrotorold") == 0);
                info.rotorList.add(e);
            }
        }
    }

    private void applyPlaneLine(MCH_PlaneInfo info, int lineNumber, String item, String data) {
        if (item.compareTo("addpartrotor") == 0) {
            String[] s = data.split("\\s*,\\s*");
            if (s.length >= 6) {
                float m = s.length >= 7 ? info.toFloat(s[6], -180.0F, 180.0F) / 90.0F : 1.0F;
                MCH_PlaneInfo.Rotor e = new MCH_PlaneInfo.Rotor(info, info.toFloat(s[0]), info.toFloat(s[1]), info.toFloat(s[2]), info.toFloat(s[3]),
                        info.toFloat(s[4]), info.toFloat(s[5]), m, "rotor" + info.rotorList.size());
                info.rotorList.add(e);
            }
        } else if (item.compareTo("addblade") == 0) {
            int idx = info.rotorList.size() - 1;
            MCH_PlaneInfo.Rotor r = !info.rotorList.isEmpty() ? info.rotorList.get(idx) : null;
            if (r != null) {
                String[] s = data.split("\\s*,\\s*");
                if (s.length == 8) {
                    MCH_PlaneInfo.Blade b = new MCH_PlaneInfo.Blade(info, info.toInt(s[0]), info.toInt(s[1]), info.toFloat(s[2]), info.toFloat(s[3]),
                            info.toFloat(s[4]), info.toFloat(s[5]), info.toFloat(s[6]), info.toFloat(s[7]), "blade" + idx);
                    r.blades.add(b);
                }
            }
        } else if (item.compareTo("addpartwing") == 0) {
            String[] s = data.split("\\s*,\\s*");
            if (s.length == 7) {
                MCH_PlaneInfo.Wing n = new MCH_PlaneInfo.Wing(info, info.toFloat(s[0]), info.toFloat(s[1]), info.toFloat(s[2]), info.toFloat(s[3]),
                        info.toFloat(s[4]), info.toFloat(s[5]), info.toFloat(s[6]), "wing" + info.wingList.size());
                info.wingList.add(n);
            }
        } else if (item.equalsIgnoreCase("AddPartPylon")) {
            String[] s = data.split("\\s*,\\s*");
            if (s.length >= 7 && !info.wingList.isEmpty()) {
                MCH_PlaneInfo.Wing w = info.wingList.get(info.wingList.size() - 1);
                if (w.pylonList == null) {
                    w.pylonList = new ArrayList<>();
                }

                MCH_PlaneInfo.Pylon n = new MCH_PlaneInfo.Pylon(info, info.toFloat(s[0]), info.toFloat(s[1]), info.toFloat(s[2]), info.toFloat(s[3]),
                        info.toFloat(s[4]), info.toFloat(s[5]), info.toFloat(s[6]), w.modelName + "_pylon" + w.pylonList.size());
                w.pylonList.add(n);
            }
        } else if (item.compareTo("addpartnozzle") == 0) {
            String[] s = data.split("\\s*,\\s*");
            if (s.length == 6) {
                MCH_AircraftInfo.DrawnPart n =
                        new DrawnPart(info, info.toFloat(s[0]), info.toFloat(s[1]), info.toFloat(s[2]), info.toFloat(s[3]), info.toFloat(s[4]),
                                info.toFloat(s[5]), "nozzle" + info.nozzles.size());
                info.nozzles.add(n);
            }
        } else if (item.compareTo("variablesweepwing") == 0) {
            info.isVariableSweepWing = info.toBool(data);
        } else if (item.compareTo("sweepwingspeed") == 0) {
            info.sweepWingSpeed = info.toFloat(data, 0.0F, 5.0F);
        } else if (item.compareTo("enablevtol") == 0) {
            info.isEnableVtol = info.toBool(data);
        } else if (item.compareTo("defaultvtol") == 0) {
            info.isDefaultVtol = info.toBool(data);
        } else if (item.compareTo("vtolyaw") == 0) {
            info.vtolYaw = info.toFloat(data, 0.0F, 1.0F);
        } else if (item.compareTo("vtolpitch") == 0) {
            info.vtolPitch = info.toFloat(data, 0.01F, 1.0F);
        } else if (item.compareTo("enableautopilot") == 0) {
            info.isEnableAutoPilot = info.toBool(data);
        }
    }

    private void applyShipLine(MCH_ShipInfo info, int lineNumber, String item, String data) {
        if (item.compareTo("addpartrotor") == 0) {
            String[] s = data.split("\\s*,\\s*");
            if (s.length >= 6) {
                float m = s.length >= 7 ? info.toFloat(s[6], -180.0F, 180.0F) / 90.0F : 1.0F;
                MCH_ShipInfo.Rotor e = new MCH_ShipInfo.Rotor(info, info.toFloat(s[0]), info.toFloat(s[1]), info.toFloat(s[2]), info.toFloat(s[3]),
                        info.toFloat(s[4]), info.toFloat(s[5]), m, "rotor" + info.rotorList.size());
                info.rotorList.add(e);
            }
        } else if (item.compareTo("addblade") == 0) {
            int idx = info.rotorList.size() - 1;
            MCH_ShipInfo.Rotor r = !info.rotorList.isEmpty() ? info.rotorList.get(idx) : null;
            if (r != null) {
                String[] s = data.split("\\s*,\\s*");
                if (s.length == 8) {
                    MCH_ShipInfo.Blade b = new MCH_ShipInfo.Blade(info, info.toInt(s[0]), info.toInt(s[1]), info.toFloat(s[2]), info.toFloat(s[3]),
                            info.toFloat(s[4]), info.toFloat(s[5]), info.toFloat(s[6]), info.toFloat(s[7]), "blade" + idx);
                    r.blades.add(b);
                }
            }
        } else if (item.compareTo("addpartwing") == 0) {
            String[] s = data.split("\\s*,\\s*");
            if (s.length == 7) {
                MCH_ShipInfo.Wing n = new MCH_ShipInfo.Wing(info, info.toFloat(s[0]), info.toFloat(s[1]), info.toFloat(s[2]), info.toFloat(s[3]),
                        info.toFloat(s[4]), info.toFloat(s[5]), info.toFloat(s[6]), "wing" + info.wingList.size());
                info.wingList.add(n);
            }
        } else if (item.equalsIgnoreCase("AddPartPylon")) {
            String[] s = data.split("\\s*,\\s*");
            if (s.length >= 7 && !info.wingList.isEmpty()) {
                MCH_ShipInfo.Wing w = info.wingList.get(info.wingList.size() - 1);
                if (w.pylonList == null) {
                    w.pylonList = new ArrayList<>();
                }

                MCH_ShipInfo.Pylon n = new MCH_ShipInfo.Pylon(info, info.toFloat(s[0]), info.toFloat(s[1]), info.toFloat(s[2]), info.toFloat(s[3]),
                        info.toFloat(s[4]), info.toFloat(s[5]), info.toFloat(s[6]), w.modelName + "_pylon" + w.pylonList.size());
                w.pylonList.add(n);
            }
        } else if (item.compareTo("addpartnozzle") == 0) {
            String[] s = data.split("\\s*,\\s*");
            if (s.length == 6) {
                MCH_AircraftInfo.DrawnPart n =
                        new DrawnPart(info, info.toFloat(s[0]), info.toFloat(s[1]), info.toFloat(s[2]), info.toFloat(s[3]), info.toFloat(s[4]),
                                info.toFloat(s[5]), "nozzle" + info.nozzles.size());
                info.nozzles.add(n);
            }
        } else if (item.compareTo("variablesweepwing") == 0) {
            info.isVariableSweepWing = info.toBool(data);
        } else if (item.compareTo("sweepwingspeed") == 0) {
            info.sweepWingSpeed = info.toFloat(data, 0.0F, 5.0F);
        } else if (item.compareTo("enablevtol") == 0) {
            info.isEnableVtol = info.toBool(data);
        } else if (item.compareTo("defaultvtol") == 0) {
            info.isDefaultVtol = info.toBool(data);
        } else if (item.compareTo("vtolyaw") == 0) {
            info.vtolYaw = info.toFloat(data, 0.0F, 1.0F);
        } else if (item.compareTo("vtolpitch") == 0) {
            info.vtolPitch = info.toFloat(data, 0.01F, 1.0F);
        } else if (item.compareTo("enableautopilot") == 0) {
            info.isEnableAutoPilot = info.toBool(data);
        }
    }

    private void applyTankLine(MCH_TankInfo info, int lineNumber, String item, String data) {
        if (item.equalsIgnoreCase("WeightType")) {
            data = data.toLowerCase();
            info.weightType = data.equals("car") ? 1 : (data.equals("tank") ? 2 : 0);
        } else if (item.equalsIgnoreCase("WeightedCenterZ")) {
            info.weightedCenterZ = info.toFloat(data, -1000.0F, 1000.0F);
        }
    }

    private void applyVehicleLine(MCH_VehicleInfo info, int lineNumber, String item, String data) {
        if (item.compareTo("canmove") == 0) {
            info.isEnableMove = info.toBool(data);
        } else if (item.compareTo("canrotation") == 0) {
            info.isEnableRot = info.toBool(data);
        } else if (item.compareTo("rotationpitchmin") == 0) {
            applyAircraftLine(info, lineNumber, "minrotationpitch", data);
        } else if (item.compareTo("rotationpitchmax") == 0) {
            applyAircraftLine(info, lineNumber, "maxrotationpitch", data);
        } else if (item.compareTo("addpart") == 0) {
            String[] s = data.split("\\s*,\\s*");
            if (s.length >= 7) {
                float rb = s.length >= 8 ? info.toFloat(s[7]) : 0.0F;
                MCH_VehicleInfo.VPart n =
                        new VPart(info, info.toFloat(s[4]), info.toFloat(s[5]), info.toFloat(s[6]), "part" + info.partList.size(), info.toBool(s[0]),
                                info.toBool(s[1]), info.toBool(s[2]), info.toInt(s[3]), rb);
                info.partList.add(n);
            }
        } else if (item.compareTo("addchildpart") == 0 && !info.partList.isEmpty()) {
            String[] s = data.split("\\s*,\\s*");
            if (s.length >= 7) {
                float rb = s.length >= 8 ? info.toFloat(s[7]) : 0.0F;
                MCH_VehicleInfo.VPart p = info.partList.get(info.partList.size() - 1);
                if (p.child == null) {
                    p.child = new ArrayList<>();
                }

                MCH_VehicleInfo.VPart n =
                        new VPart(info, info.toFloat(s[4]), info.toFloat(s[5]), info.toFloat(s[6]), p.modelName + "_" + p.child.size(),
                                info.toBool(s[0]), info.toBool(s[1]), info.toBool(s[2]), info.toInt(s[3]), rb);
                p.child.add(n);
            }
        }
    }

    private void applyWeaponLine(MCH_WeaponInfo info, int lineNumber, String item, String data) {
        if (item.compareTo("displayname") == 0) {
            info.displayName = data;
        } else if (item.compareTo("type") == 0) {
            info.type = data.toLowerCase();
            if (info.type.equalsIgnoreCase("bomb") || info.type.equalsIgnoreCase("dispenser")) {
                info.gravity = -0.03F;
                info.gravityInWater = -0.03F;
            }
        } else if (item.compareTo("group") == 0) {
            info.group = data.toLowerCase().trim();
        } else if (item.compareTo("power") == 0) {
            info.power = info.toInt(data);
        } else if (item.equalsIgnoreCase("sound")) {
            info.soundFileName = data.toLowerCase().trim();
        } else if (item.compareTo("acceleration") == 0) {
            info.acceleration = info.toFloat(data, 0.0F, 100.0F);
        } else if (item.compareTo("accelerationinwater") == 0) {
            info.accelerationInWater = info.toFloat(data, 0.0F, 100.0F);
        } else if (item.compareTo("gravity") == 0) {
            info.gravity = info.toFloat(data, -50.0F, 50.0F);
        } else if (item.compareTo("gravityinwater") == 0) {
            info.gravityInWater = info.toFloat(data, -50.0F, 50.0F);
        } else if (item.equalsIgnoreCase("VelocityInWater")) {
            info.velocityInWater = info.toFloat(data);
        } else if (item.compareTo("explosion") == 0) {
            info.explosion = info.toInt(data, 0, 50);
        } else if (item.equalsIgnoreCase("explosionBlock")) {
            info.explosionBlock = info.toInt(data, 0, 50);
        } else if (item.compareTo("explosioninwater") == 0) {
            info.explosionInWater = info.toInt(data, 0, 50);
        } else if (item.equalsIgnoreCase("ExplosionAltitude")) {
            info.explosionAltitude = info.toInt(data, 0, 100);
        } else if (item.equalsIgnoreCase("TimeFuse")) {
            info.timeFuse = info.toInt(data, 0, 100000);
        } else if (item.equalsIgnoreCase("DelayFuse")) {
            info.delayFuse = info.toInt(data, 0, 100000);
        } else if (item.equalsIgnoreCase("Bound")) {
            info.bound = info.toFloat(data, 0.0F, 100000.0F);
        } else if (item.compareTo("flaming") == 0) {
            info.flaming = info.toBool(data);
        } else if (item.equalsIgnoreCase("DisplayMortarDistance")) {
            info.displayMortarDistance = info.toBool(data);
        } else if (item.equalsIgnoreCase("FixCameraPitch")) {
            info.fixCameraPitch = info.toBool(data);
        } else if (item.equalsIgnoreCase("CameraRotationSpeedPitch")) {
            info.cameraRotationSpeedPitch = info.toFloat(data, 0.0F, 100.0F);
        } else if (item.compareTo("sight") == 0) {
            data = data.toLowerCase();
            if (data.compareTo("movesight") == 0) {
                info.sight = MCH_SightType.ROCKET;
            }

            if (data.compareTo("missilesight") == 0) {
                info.sight = MCH_SightType.LOCK;
            }
        } else if (item.equalsIgnoreCase("Zoom")) {
            String[] s = info.splitParam(data);
            if (s.length > 0) {
                info.zoom = new float[s.length];

                for (int i = 0; i < s.length; i++) {
                    info.zoom[i] = info.toFloat(s[i], 0.1F, 10.0F);
                }
            }
        } else if (item.compareTo("delay") == 0) {
            info.delay = info.toInt(data, 0, 100000);
        } else if (item.compareTo("reloadtime") == 0) {
            info.reloadTime = info.toInt(data, 3, 1000);
        } else if (item.compareTo("round") == 0) {
            info.round = info.toInt(data, 1, 30000);
        } else if (item.equalsIgnoreCase("MaxAmmo")) {
            info.maxAmmo = info.toInt(data, 0, 30000);
        } else if (item.equalsIgnoreCase("SuppliedNum")) {
            info.suppliedNum = info.toInt(data, 1, 30000);
        } else if (item.equalsIgnoreCase("Item")) {
            String[] s = data.split("\\s*,\\s*");
            if (s.length >= 2 && !s[1].isEmpty() && info.roundItems.size() < 3) {
                int n = info.toInt(s[0], 1, 64);
                if (n > 0) {
                    int damage = s.length >= 3 ? info.toInt(s[2], 0, 100000000) : 0;
                    info.roundItems.add(new RoundItem(n, s[1].toLowerCase().trim(), damage));
                }
            }
        } else if (item.compareTo("sounddelay") == 0) {
            info.soundDelay = info.toInt(data, 0, 1000);
        } else if (item.compareTo("soundpattern") != 0) {
            if (item.compareTo("soundvolume") == 0) {
                info.soundVolume = info.toFloat(data, 0.0F, 1000.0F);
            } else if (item.compareTo("soundpitch") == 0) {
                info.soundPitch = info.toFloat(data, 0.0F, 1.0F);
            } else if (item.equalsIgnoreCase("SoundPitchRandom")) {
                info.soundPitchRandom = info.toFloat(data, 0.0F, 1.0F);
            } else if (item.compareTo("locktime") == 0) {
                info.lockTime = info.toInt(data, 2, 1000);
            } else if (item.equalsIgnoreCase("RidableOnly")) {
                info.ridableOnly = info.toBool(data);
            } else if (item.compareTo("proximityfusedist") == 0) {
                info.proximityFuseDist = info.toFloat(data, 0.0F, 2000.0F);
            } else if (item.equalsIgnoreCase("RigidityTime")) {
                info.rigidityTime = info.toInt(data, 0, 1000000);
            } else if (item.compareTo("accuracy") == 0) {
                info.accuracy = info.toFloat(data, 0.0F, 1000.0F);
            } else if (item.compareTo("bomblet") == 0) {
                info.bomblet = info.toInt(data, 0, 1000);
            } else if (item.compareTo("bombletstime") == 0) {
                info.bombletSTime = info.toInt(data, 0, 1000);
            } else if (item.equalsIgnoreCase("BombletDiff")) {
                info.bombletDiff = info.toFloat(data, 0.0F, 1000.0F);
            } else if (item.equalsIgnoreCase("RecoilBufCount")) {
                String[] s = info.splitParam(data);
                if (s.length >= 1) {
                    info.recoilBufCount = info.toInt(s[0], 1, 10000);
                }

                if (s.length >= 2 && info.recoilBufCount > 2) {
                    info.recoilBufCountSpeed = info.toInt(s[1], 1, 10000) - 1;
                    if (info.recoilBufCountSpeed > info.recoilBufCount / 2) {
                        info.recoilBufCountSpeed = info.recoilBufCount / 2;
                    }
                }
            } else if (item.compareTo("modenum") == 0) {
                info.modeNum = info.toInt(data, 0, 1000);
            } else if (item.equalsIgnoreCase("FixMode")) {
                info.fixMode = info.toInt(data, 0, 10);
            } else if (item.compareTo("piercing") == 0) {
                info.piercing = info.toInt(data, 0, 100000);
            } else if (item.compareTo("heatcount") == 0) {
                info.heatCount = info.toInt(data, 0, 100000);
            } else if (item.compareTo("maxheatcount") == 0) {
                info.maxHeatCount = info.toInt(data, 0, 100000);
            } else if (item.compareTo("modelbullet") == 0) {
                info.bulletModelName = data.toLowerCase().trim();
            } else if (item.equalsIgnoreCase("ModelBomblet")) {
                info.bombletModelName = data.toLowerCase().trim();
            } else if (item.compareTo("fae") == 0) {
                info.isFAE = info.toBool(data);
            } else if (item.compareTo("guidedtorpedo") == 0) {
                info.isGuidedTorpedo = info.toBool(data);
            } else if (item.compareTo("destruct") == 0) {
                info.destruct = info.toBool(data);
            } else if (item.equalsIgnoreCase("AddMuzzleFlash")) {
                String[] sx = info.splitParam(data);
                if (sx.length >= 7) {
                    if (info.listMuzzleFlash == null) {
                        info.listMuzzleFlash = new ArrayList<>();
                    }

                    info.listMuzzleFlash.add(
                            new MuzzleFlash(info.toFloat(sx[0]), info.toFloat(sx[1]), 0.0F, info.toInt(sx[2]), info.toFloat(sx[3]) / 255.0F,
                                    info.toFloat(sx[4]) / 255.0F, info.toFloat(sx[5]) / 255.0F, info.toFloat(sx[6]) / 255.0F, 1));
                }
            } else if (item.equalsIgnoreCase("AddMuzzleFlashSmoke")) {
                String[] sx = info.splitParam(data);
                if (sx.length >= 9) {
                    if (info.listMuzzleFlashSmoke == null) {
                        info.listMuzzleFlashSmoke = new ArrayList<>();
                    }

                    info.listMuzzleFlashSmoke.add(new MuzzleFlash(info.toFloat(sx[0]), info.toFloat(sx[2]), info.toFloat(sx[3]), info.toInt(sx[4]),
                            info.toFloat(sx[5]) / 255.0F, info.toFloat(sx[6]) / 255.0F, info.toFloat(sx[7]) / 255.0F, info.toFloat(sx[8]) / 255.0F,
                            info.toInt(sx[1], 1, 1000)));
                }
            } else if (item.equalsIgnoreCase("TrajectoryParticle")) {
                info.trajectoryParticleName = data.toLowerCase().trim();
                if (info.trajectoryParticleName.equalsIgnoreCase("none")) {
                    info.trajectoryParticleName = "";
                }
            } else if (item.equalsIgnoreCase("TrajectoryParticleStartTick")) {
                info.trajectoryParticleStartTick = info.toInt(data, 0, 10000);
            } else if (item.equalsIgnoreCase("DisableSmoke")) {
                info.disableSmoke = info.toBool(data);
            } else if (item.equalsIgnoreCase("SetCartridge")) {
                String[] sx = data.split("\\s*,\\s*");
                if (sx.length > 0 && !sx[0].isEmpty()) {
                    float ac = sx.length >= 2 ? info.toFloat(sx[1]) : 0.0F;
                    float yw = sx.length >= 3 ? info.toFloat(sx[2]) : 0.0F;
                    float pt = sx.length >= 4 ? info.toFloat(sx[3]) : 0.0F;
                    float sc = sx.length >= 5 ? info.toFloat(sx[4]) : 1.0F;
                    float gr = sx.length >= 6 ? info.toFloat(sx[5]) : -0.04F;
                    float bo = sx.length >= 7 ? info.toFloat(sx[6]) : 0.5F;
                    info.cartridge = new MCH_Cartridge(sx[0].toLowerCase(), ac, yw, pt, bo, gr, sc);
                }
            } else if (item.equalsIgnoreCase("BulletColorInWater") || item.equalsIgnoreCase("BulletColor") || item.equalsIgnoreCase("SmokeColor")) {
                String[] sx = data.split("\\s*,\\s*");
                if (sx.length >= 4) {
                    MCH_Color c = new MCH_Color(0.003921569F * info.toInt(sx[0], 0, 255), 0.003921569F * info.toInt(sx[1], 0, 255),
                            0.003921569F * info.toInt(sx[2], 0, 255), 0.003921569F * info.toInt(sx[3], 0, 255));
                    if (item.equalsIgnoreCase("BulletColorInWater")) {
                        info.colorInWater = c;
                    } else {
                        info.color = c;
                    }
                }
            } else if (item.equalsIgnoreCase("nukeYield")) {
//                info.nukeYield = info.toInt(data, 0, 100000);
            } else if (item.equalsIgnoreCase("chemYield")) {
//                info.chemYield = info.toInt(data, 0, 100000);
            } else if (item.equalsIgnoreCase("chemSpeed")) {
//                info.chemSpeed = info.toDouble(data);
            } else if (item.equalsIgnoreCase("chemType")) {
//                info.chemType = info.toInt(data, 0, 3);
            } else if (item.equalsIgnoreCase("NukeEffectOnly")) {
//                info.nukeEffectOnly = info.toBool(data);
            } else if (item.equalsIgnoreCase("MaxDegreeOfMissile")) {
                info.maxDegreeOfMissile = info.toInt(data, 0, 100000);
            } else if (item.equalsIgnoreCase("TickEndHoming")) {
                info.tickEndHoming = info.toInt(data, -1, 100000);
            } else if (item.equalsIgnoreCase("FlakParticlesCrack")) {
                info.flakParticlesCrack = info.toInt(data, 0, 300);
            } else if (item.equalsIgnoreCase("ParticlesFlak")) {
                info.numParticlesFlak = info.toInt(data, 0, 100);
            } else if (item.equalsIgnoreCase("FlakParticlesDiff")) {
                info.flakParticlesDiff = info.toFloat(data);
            } else if (item.equalsIgnoreCase("IsRadarMissile")) {
                info.isRadarMissile = info.toBool(data);
            } else if (item.equalsIgnoreCase("IsHeatSeekerMissile")) {
                info.isHeatSeekerMissile = info.toBool(data);
            } else if (item.equalsIgnoreCase("MaxLockOnRange")) {
                info.maxLockOnRange = info.toInt(data, 0, 2000);
            } else if (item.equalsIgnoreCase("MaxLockOnAngle")) {
                info.maxLockOnAngle = info.toInt(data, 0, 200);
            } else if (item.equalsIgnoreCase("PDHDNMaxDegree")) {
                info.pdHDNMaxDegree = info.toFloat(data, -1, 90);
            } else if (item.equalsIgnoreCase("PDHDNMaxDegreeLockOutCount")) {
                info.pdHDNMaxDegreeLockOutCount = info.toInt(data, 0, 200);
            } else if (item.equalsIgnoreCase("AntiFlareCount")) {
                info.antiFlareCount = info.toInt(data, -1, 200);
            } else if (item.equalsIgnoreCase("LockMinHeight")) {
                info.lockMinHeight = info.toInt(data, -1, 100);
            } else if (item.equalsIgnoreCase("PassiveRadar")) {
                info.passiveRadar = info.toBool(data);
            } else if (item.equalsIgnoreCase("PassiveRadarLockOutCount")) {
                info.passiveRadarLockOutCount = info.toInt(data, 0, 200);
            } else if (item.equalsIgnoreCase("LaserGuidance")) {
                info.laserGuidance = info.toBool(data);
            } else if (item.equalsIgnoreCase("HasLaserGuidancePod")) {
                info.hasLaserGuidancePod = info.toBool(data);
            } else if (item.equalsIgnoreCase("ActiveRadar")) {
                info.activeRadar = info.toBool(data);
            } else if (item.equalsIgnoreCase("EnableOffAxis")) {
                info.enableOffAxis = info.toBool(data);
            } else if (item.equalsIgnoreCase("TurningFactor")) {
                info.turningFactor = info.toDouble(data);
            } else if (item.equalsIgnoreCase("EnableChunkLoader")) {
                info.enableChunkLoader = info.toBool(data);
            } else if (item.equalsIgnoreCase("ScanInterval")) {
                info.scanInterval = info.toInt(data);
            } else if (item.equalsIgnoreCase("WeaponSwitchCount")) {
                info.weaponSwitchCount = info.toInt(data);
            } else if (item.equalsIgnoreCase("WeaponSwitchSound")) {
                info.weaponSwitchSound = data.toLowerCase().trim();
            } else if (item.equalsIgnoreCase("RecoilPitch")) {
                info.recoilPitch = info.toFloat(data);
            } else if (item.equalsIgnoreCase("RecoilYaw")) {
                info.recoilYaw = info.toFloat(data);
            } else if (item.equalsIgnoreCase("RecoilPitchRange")) {
                info.recoilPitchRange = info.toFloat(data);
            } else if (item.equalsIgnoreCase("RecoilYawRange")) {
                info.recoilYawRange = info.toFloat(data);
            } else if (item.equalsIgnoreCase("RecoilRecoverFactor")) {
                info.recoilRecoverFactor = info.toFloat(data);
            } else if (item.equalsIgnoreCase("SpeedFactor")) {
                info.speedFactor = info.toFloat(data);
            } else if (item.equalsIgnoreCase("SpeedFactorStartTick")) {
                info.speedFactorStartTick = info.toInt(data);
            } else if (item.equalsIgnoreCase("SpeedFactorEndTick")) {
                info.speedFactorEndTick = info.toInt(data);
            } else if (item.equalsIgnoreCase("SpeedDependsAircraft")) {
                info.speedDependsAircraft = info.toBool(data);
            } else if (item.equalsIgnoreCase("CanLockMissile")) {
                info.canLockMissile = info.toBool(data);
            } else if (item.equalsIgnoreCase("EnableBVR")) {
                info.enableBVR = info.toBool(data);
            } else if (item.equalsIgnoreCase("MinRangeBVR")) {
                info.minRangeBVR = info.toInt(data);
            } else if (item.equalsIgnoreCase("SmokeSize")) {
                info.smokeSize = info.toFloat(data, 0.0F, 100.0F);
            } else if (item.equalsIgnoreCase("SmokeNum")) {
                info.smokeNum = info.toInt(data, 1, 100);
            } else if (item.equalsIgnoreCase("SmokeMaxAge")) {
                info.smokeMaxAge = info.toInt(data, 2, 1000);
            } else if (item.equalsIgnoreCase("DispenseItem")) {
                String[] sx = data.split("\\s*,\\s*");
                if (sx.length >= 2) {
                    info.dispenseDamege = info.toInt(sx[1], 0, 100000000);
                }

                info.dispenseItemLoc = sx[0].toLowerCase().trim();
            } else if (item.equalsIgnoreCase("DispenseRange")) {
                info.dispenseRange = info.toInt(data, 1, 100);
            } else if (item.equalsIgnoreCase("Length")) {
                info.length = info.toInt(data, 1, 300);
            } else if (item.equalsIgnoreCase("Radius")) {
                info.radius = info.toInt(data, 1, 1000);
            } else if (item.equalsIgnoreCase("Target")) {
                if (data.contains("block")) {
                    info.target = 64;
                } else {
                    info.target = 0;
                    info.target = info.target | (data.contains("planes") ? 32 : 0);
                    info.target = info.target | (data.contains("helicopters") ? 16 : 0);
                    info.target = info.target | (data.contains("vehicles") ? 8 : 0);
                    info.target = info.target | (data.contains("tanks") ? 8 : 0);
                    info.target = info.target | (data.contains("players") ? 4 : 0);
                    info.target = info.target | (data.contains("monsters") ? 2 : 0);
                    info.target = info.target | (data.contains("others") ? 1 : 0);
                }
            } else if (item.equalsIgnoreCase("MarkTime")) {
                info.markTime = info.toInt(data, 1, 30000) + 1;
            } else if (item.equalsIgnoreCase("Recoil")) {
                info.recoil = info.toFloat(data, 0.0F, 100.0F);
            } else if (item.equalsIgnoreCase("DamageFactor")) {
                String[] sx = info.splitParam(data);
                if (sx.length >= 2) {
                    Class<? extends Entity> c = null;
                    String className = sx[0].toLowerCase();
                    c = switch (className) {
                        case "player" -> EntityPlayer.class;
                        case "heli", "helicopter" -> MCH_EntityHeli.class;
                        case "plane" -> MCP_EntityPlane.class;
                        case "ship" -> MCH_EntityShip.class;
                        case "tank" -> MCH_EntityTank.class;
                        case "vehicle" -> MCH_EntityVehicle.class;
                        default -> c;
                    };

                    if (c != null) {
                        if (info.damageFactor == null) {
                            info.damageFactor = new MCH_DamageFactor();
                        }

                        info.damageFactor.add(c, info.toFloat(sx[1], 0.0F, 1000000.0F));
                    }
                }
            }
        }
    }

    private void applyThrowableLine(MCH_ThrowableInfo info, int lineNumber, String item, String data) {
        if (item.compareTo("displayname") == 0) {
            info.displayName = data;
        } else if (item.compareTo("adddisplayname") == 0) {
            String[] s = data.split("\\s*,\\s*");
            if (s.length == 2) {
                info.displayNameLang.put(s[0].trim(), s[1].trim());
            }
        } else if (item.compareTo("itemid") == 0) {
            info.itemID = info.toInt(data, 0, 65535);
        } else if (item.compareTo("addrecipe") == 0 || item.compareTo("addshapelessrecipe") == 0) {
            info.isShapedRecipe = item.compareTo("addrecipe") == 0;
            info.recipeString.add(data.toUpperCase());
        } else if (item.compareTo("power") == 0) {
            info.power = info.toInt(data);
        } else if (item.compareTo("acceleration") == 0) {
            info.acceleration = info.toFloat(data, 0.0F, 100.0F);
        } else if (item.compareTo("accelerationinwater") == 0) {
            info.accelerationInWater = info.toFloat(data, 0.0F, 100.0F);
        } else if (item.equalsIgnoreCase("DispenseAcceleration")) {
            info.dispenseAcceleration = info.toFloat(data, 0.0F, 1000.0F);
        } else if (item.compareTo("explosion") == 0) {
            info.explosion = info.toInt(data, 0, 50);
        } else if (item.equalsIgnoreCase("DelayFuse")) {
            info.delayFuse = info.toInt(data, 0, 100000);
        } else if (item.equalsIgnoreCase("Bound")) {
            info.bound = info.toFloat(data, 0.0F, 100000.0F);
        } else if (item.equalsIgnoreCase("TimeFuse")) {
            info.timeFuse = info.toInt(data, 0, 100000);
        } else if (item.compareTo("flaming") == 0) {
            info.flaming = info.toBool(data);
        } else if (item.equalsIgnoreCase("StackSize")) {
            info.stackSize = info.toInt(data, 1, 64);
        } else if (item.compareTo("soundvolume") == 0) {
            info.soundVolume = info.toFloat(data, 0.0F, 1000.0F);
        } else if (item.compareTo("soundpitch") == 0) {
            info.soundPitch = info.toFloat(data, 0.0F, 1.0F);
        } else if (item.compareTo("proximityfusedist") == 0) {
            info.proximityFuseDist = info.toFloat(data, 0.0F, 20.0F);
        } else if (item.compareTo("accuracy") == 0) {
            info.accuracy = info.toFloat(data, 0.0F, 1000.0F);
        } else if (item.equalsIgnoreCase("aliveTime")) {
            info.aliveTime = info.toInt(data, 0, 1000000);
        } else if (item.compareTo("bomblet") == 0) {
            info.bomblet = info.toInt(data, 0, 1000);
        } else if (item.equalsIgnoreCase("BombletDiff")) {
            info.bombletDiff = info.toFloat(data, 0.0F, 1000.0F);
        } else if (item.equalsIgnoreCase("SmokeSize")) {
            info.smokeSize = info.toFloat(data, 0.0F, 1000.0F);
        } else if (item.equalsIgnoreCase("SmokeNum")) {
            info.smokeNum = info.toInt(data, 0, 1000);
        } else if (item.equalsIgnoreCase("SmokeVelocityVertical")) {
            info.smokeVelocityVertical = info.toFloat(data, -100.0F, 100.0F);
        } else if (item.equalsIgnoreCase("SmokeVelocityHorizontal")) {
            info.smokeVelocityHorizontal = info.toFloat(data, 0.0F, 1000.0F);
        } else if (item.compareTo("gravity") == 0) {
            info.gravity = info.toFloat(data, -50.0F, 50.0F);
        } else if (item.equalsIgnoreCase("gravityInWater")) {
            info.gravityInWater = info.toFloat(data, -50.0F, 50.0F);
        } else if (item.compareTo("particle") == 0) {
            info.particleName = data.toLowerCase().trim();
            if (info.particleName.equalsIgnoreCase("none")) {
                info.particleName = "";
            }
        } else if (item.equalsIgnoreCase("DisableSmoke")) {
            info.disableSmoke = info.toBool(data);
        } else if (item.equalsIgnoreCase("SmokeColor")) {
            String[] s = data.split("\\s*,\\s*");
            if (s.length >= 3) {
                info.smokeColor = new MCH_Color(1.0F, 0.003921569F * info.toInt(s[0], 0, 255), 0.003921569F * info.toInt(s[1], 0, 255),
                        0.003921569F * info.toInt(s[2], 0, 255));
            }
        }
    }

    private void applyHudLine(MCH_Hud info, int lineNumber, String item, String data) {
        String[] prm = data.split("\\s*,\\s*");
        if (prm.length != 0) {
            if (item.equalsIgnoreCase("If")) {
                if (info.isWaitEndif) {
                    throw new RuntimeException("Endif not found!");
                }

                info.list.add(new MCH_HudItemConditional(lineNumber, false, prm[0]));
                info.isWaitEndif = true;
            } else if (item.equalsIgnoreCase("Endif")) {
                if (!info.isWaitEndif) {
                    throw new RuntimeException("IF in a pair can not be found!");
                }

                info.list.add(new MCH_HudItemConditional(lineNumber, true, ""));
                info.isWaitEndif = false;
            } else if (item.equalsIgnoreCase("DrawString") || item.equalsIgnoreCase("DrawCenteredString")) {
                if (prm.length >= 3) {
                    String s = prm[2];
                    if (s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
                        s = s.substring(1, s.length() - 1);
                        info.list.add(new MCH_HudItemString(lineNumber, prm[0], prm[1], s, prm, item.equalsIgnoreCase("DrawCenteredString")));
                    }
                }
            } else if (item.equalsIgnoreCase("Exit")) {
                info.list.add(new MCH_HudItemExit(lineNumber));
            } else if (item.equalsIgnoreCase("Color")) {
                if (prm.length == 1) {
                    MCH_HudItemColor c = MCH_HudItemColor.createByParams(lineNumber, new String[]{prm[0]});
                    if (c != null) {
                        info.list.add(c);
                    }
                } else if (prm.length == 4) {
                    String[] s = new String[]{prm[0], prm[1], prm[2], prm[3]};
                    MCH_HudItemColor c = MCH_HudItemColor.createByParams(lineNumber, s);
                    if (c != null) {
                        info.list.add(c);
                    }
                }
            } else if (item.equalsIgnoreCase("DrawTexture")) {
                if (prm.length >= 9 && prm.length <= 10) {
                    String rot = prm.length == 10 ? prm[9] : "0";
                    info.list.add(new MCH_HudItemTexture(lineNumber, prm[0], prm[1], prm[2], prm[3], prm[4], prm[5], prm[6], prm[7], prm[8], rot));
                }
            } else if (item.equalsIgnoreCase("DrawRect")) {
                if (prm.length == 4) {
                    info.list.add(new MCH_HudItemRect(lineNumber, prm[0], prm[1], prm[2], prm[3]));
                }
            } else if (item.equalsIgnoreCase("DrawLine")) {
                int len = prm.length;
                if (len >= 4 && len % 2 == 0) {
                    info.list.add(new MCH_HudItemLine(lineNumber, prm));
                }
            } else if (item.equalsIgnoreCase("DrawLineStipple")) {
                int len = prm.length;
                if (len >= 6 && len % 2 == 0) {
                    info.list.add(new MCH_HudItemLineStipple(lineNumber, prm));
                }
            } else if (item.equalsIgnoreCase("Call")) {
                int len = prm.length;
                if (len == 1) {
                    info.list.add(new MCH_HudItemCall(lineNumber, prm[0]));
                }
            } else if (item.equalsIgnoreCase("DrawEntityRadar") || item.equalsIgnoreCase("DrawEnemyRadar")) {
                if (prm.length == 5) {
                    info.list.add(new MCH_HudItemRadar(lineNumber, item.equalsIgnoreCase("DrawEntityRadar"), prm[0], prm[1], prm[2], prm[3], prm[4]));
                }
            } else if (item.equalsIgnoreCase("DrawGraduationYaw") || item.equalsIgnoreCase("DrawGraduationPitch1") ||
                       item.equalsIgnoreCase("DrawGraduationPitch2") || item.equalsIgnoreCase("DrawGraduationPitch3")) {
                if (prm.length == 4) {
                    int type = -1;
                    if (item.equalsIgnoreCase("DrawGraduationYaw")) {
                        type = 0;
                    }

                    if (item.equalsIgnoreCase("DrawGraduationPitch1")) {
                        type = 1;
                    }

                    if (item.equalsIgnoreCase("DrawGraduationPitch2")) {
                        type = 2;
                    }

                    if (item.equalsIgnoreCase("DrawGraduationPitch3")) {
                        type = 3;
                    }

                    info.list.add(new MCH_HudItemGraduation(lineNumber, type, prm[0], prm[1], prm[2], prm[3]));
                }
            } else if (item.equalsIgnoreCase("DrawCameraRot") && prm.length == 2) {
                info.list.add(new MCH_HudItemCameraRot(lineNumber, prm[0], prm[1]));
            }
        }
    }

    private void applyItemLine(MCH_ItemInfo info, int lineNumber, String item, String data) {
        if (item.compareTo("displayname") == 0) {
            info.displayName = data;
        } else {
            String[] s;
            if (item.compareTo("adddisplayname") == 0) {
                s = data.split("\\s*,\\s*");
                if (s != null && s.length == 2) {
                    info.displayNameLang.put(s[0].trim(), s[1].trim());
                }
            } else if (item.compareTo("itemid") == 0) {
                info.itemID = info.toInt(data, 0, '\uffff');
            } else if (item.compareTo("addrecipe") != 0 && item.compareTo("addshapelessrecipe") != 0) {
                if (item.equalsIgnoreCase("StackSize")) {
                    info.stackSize = info.toInt(data, 1, 64);
                }
            } else {
                info.isShapedRecipe = item.compareTo("addrecipe") == 0;
                info.recipeString.add(data.toUpperCase());
            }
        }

    }

    @FunctionalInterface
    private interface LineProcessor {
        void accept(int lineNumber, String item, String data) throws Exception;
    }

    @FunctionalInterface
    private interface LineHandler<T extends MCH_BaseInfo> {
        void accept(T info, int lineNumber, String item, String data) throws Exception;
    }
}
