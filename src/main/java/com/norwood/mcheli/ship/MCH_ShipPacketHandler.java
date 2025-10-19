package com.norwood.mcheli.ship;

import com.google.common.io.ByteArrayDataInput;
import com.norwood.mcheli.helper.network.HandleSide;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import com.norwood.mcheli.uav.MCH_EntityUavStation;
import com.norwood.mcheli.weapon.MCH_WeaponParam;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IThreadListener;
import net.minecraftforge.fml.relauncher.Side;

public class MCH_ShipPacketHandler {

    @HandleSide({Side.SERVER})
    public static void onPacket_PlayerControl(EntityPlayer player, ByteArrayDataInput data, IThreadListener scheduler) {
        if (!player.world.isRemote) {
            MCH_ShipPacketPlayerControl pc = new MCH_ShipPacketPlayerControl();
            pc.readData(data);
            scheduler.addScheduledTask(() -> {
                MCH_EntityShip plane = null;
                if (player.getRidingEntity() instanceof MCH_EntityShip) {
                    plane = (MCH_EntityShip) player.getRidingEntity();
                } else if (player.getRidingEntity() instanceof MCH_EntitySeat) {
                    if (((MCH_EntitySeat) player.getRidingEntity()).getParent() instanceof MCH_EntityShip) {
                        plane = (MCH_EntityShip) ((MCH_EntitySeat) player.getRidingEntity()).getParent();
                    }
                } else if (player.getRidingEntity() instanceof MCH_EntityUavStation uavStation) {
                    if (uavStation.getControlAircract() instanceof MCH_EntityShip) {
                        plane = (MCH_EntityShip) uavStation.getControlAircract();
                    }
                }

                if (plane != null) {
                    if (pc.isUnmount == 1) {
                        plane.unmountEntity();
                    } else if (pc.isUnmount == 2) {
                        plane.unmountCrew();
                    } else if (pc.ejectSeat) {
                        plane.ejectSeat(player);
                    } else {
                        if (pc.switchVtol == 0) {
                            plane.swithVtolMode(false);
                        }

                        if (pc.switchVtol == 1) {
                            plane.swithVtolMode(true);
                        }

                        if (pc.switchMode == 0) {
                            plane.switchGunnerMode(false);
                        }

                        if (pc.switchMode == 1) {
                            plane.switchGunnerMode(true);
                        }

                        if (pc.switchMode == 2) {
                            plane.switchHoveringMode(false);
                        }

                        if (pc.switchMode == 3) {
                            plane.switchHoveringMode(true);
                        }

                        if (pc.switchSearchLight) {
                            plane.setSearchLight(!plane.isSearchLightON());
                        }

                        if (pc.switchCameraMode > 0) {
                            plane.switchCameraMode(player, pc.switchCameraMode - 1);
                        }

                        if (pc.switchWeapon >= 0) {
                            plane.switchWeapon(player, pc.switchWeapon);
                        }

                        if (pc.useWeapon) {
                            MCH_WeaponParam prm = new MCH_WeaponParam();
                            prm.entity = plane;
                            prm.user = player;
                            prm.setPosAndRot(pc.useWeaponPosX, pc.useWeaponPosY, pc.useWeaponPosZ, 0.0F, 0.0F);
                            prm.option1 = pc.useWeaponOption1;
                            prm.option2 = pc.useWeaponOption2;
                            plane.useCurrentWeapon(prm);
                        }

                        if (plane.isPilot(player)) {
                            plane.throttleUp = pc.throttleUp;
                            plane.throttleDown = pc.throttleDown;
                            plane.moveLeft = pc.moveLeft;
                            plane.moveRight = pc.moveRight;
                        }

                        if (pc.useFlareType > 0) {
                            plane.useFlare(pc.useFlareType);
                        }

                        if (pc.openGui) {
                            plane.openGui(player);
                        }

                        if (pc.switchHatch > 0) {
                            if (plane.getAcInfo().haveHatch()) {
                                plane.foldHatch(pc.switchHatch == 2);
                            } else {
                                plane.foldWing(pc.switchHatch == 2);
                            }
                        }

                        if (pc.switchFreeLook > 0) {
                            plane.switchFreeLookMode(pc.switchFreeLook == 1);
                        }

                        if (pc.switchGear == 1) {
                            plane.foldLandingGear();
                        }

                        if (pc.switchGear == 2) {
                            plane.unfoldLandingGear();
                        }

                        if (pc.putDownRack == 1) {
                            plane.mountEntityToRack();
                        }

                        if (pc.putDownRack == 2) {
                            plane.unmountEntityFromRack();
                        }

                        if (pc.putDownRack == 3) {
                            plane.rideRack();
                        }

                        if (pc.isUnmount == 3) {
                            plane.unmountAircraft();
                        }

                        if (pc.switchGunnerStatus) {
                            plane.setGunnerStatus(!plane.getGunnerStatus());
                        }
                    }
                }
            });
        }
    }
}
