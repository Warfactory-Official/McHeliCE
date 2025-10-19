package com.norwood.mcheli.ship;


import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_KeyName;
import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.aircraft.MCH_AircraftCommonGui;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.gui.MCH_Gui;
import com.norwood.mcheli.plane.MCP_PlaneInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_GuiShip extends MCH_AircraftCommonGui {

    public MCH_GuiShip(Minecraft minecraft) {
        super(minecraft);
    }

    public boolean isDrawGui(EntityPlayer player) {
        return MCH_EntityAircraft.getAircraft_RiddenOrControl(player) instanceof MCH_EntityShip;
    }

    public void drawGui(EntityPlayer player, boolean isThirdPersonView) {
        MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(player);
        if(ac instanceof MCH_EntityShip && !ac.isDestroyed()) {
            MCH_EntityShip plane = (MCH_EntityShip)ac;
            int seatID = ac.getSeatIdByEntity(player);
            GL11.glLineWidth((float)MCH_Gui.scaleFactor);
            if(plane.getCameraMode(player) == 1) {
                this.drawNightVisionNoise();
            }

            MCH_Config var10000;
            label50: {
                if(isThirdPersonView) {
                    var10000 = MCH_MOD.config;
                    if(!MCH_Config.DisplayHUDThirdPerson.prmBool) {
                        break label50;
                    }
                }

                if(seatID == 0 && plane.getIsGunnerMode(player)) {
                    this.drawHud(ac, player, 1);
                } else {
                    this.drawHud(ac, player, seatID);
                }
            }

            label51: {
                this.drawDebugtInfo(plane);
                if(isThirdPersonView) {
                    var10000 = MCH_MOD.config;
                    if(!MCH_Config.DisplayHUDThirdPerson.prmBool) {
                        break label51;
                    }
                }

                if(plane.getTVMissile() != null && (plane.getIsGunnerMode(player) || plane.isUAV())) {
                    this.drawTvMissileNoise(plane, plane.getTVMissile());
                } else {
                    this.drawKeybind(plane, player, seatID);
                }
            }

            this.drawHitMarker(plane, -14101432, seatID);
        }
    }

    public void drawKeybind(MCH_EntityShip plane, EntityPlayer player, int seatID) {
        MCH_Config var10000 = MCH_MOD.config;
        if(!MCH_Config.HideKeybind.prmBool) {
            MCH_ShipInfo info = plane.getPlaneInfo();
            if(info != null) {
                int colorActive = -1342177281;
                int colorInactive = -1349546097;
                int RX = super.centerX + 120;
                int LX = super.centerX - 200;
                this.drawKeyBind(plane, info, player, seatID, RX, LX, colorActive, colorInactive);
                String msg;
                StringBuilder var12;
                MCH_Config var10001;
                if(seatID == 0 && info.isEnableGunnerMode) {
                    var10000 = MCH_MOD.config;
                    if(!Keyboard.isKeyDown(MCH_Config.KeyFreeLook.prmInt)) {
                        int c = plane.isHoveringMode()?colorInactive:colorActive;
                        var12 = (new StringBuilder()).append(plane.getIsGunnerMode(player)?"Normal":"Gunner").append(" : ");
                        var10001 = MCH_MOD.config;
                        msg = var12.append(MCH_KeyName.getDescOrName(MCH_Config.KeySwitchMode.prmInt)).toString();
                        this.drawString(msg, RX, super.centerY - 70, c);
                    }
                }

                if(seatID > 0 && plane.canSwitchGunnerModeOtherSeat(player)) {
                    var12 = (new StringBuilder()).append(plane.getIsGunnerMode(player)?"Normal":"Camera").append(" : ");
                    var10001 = MCH_MOD.config;
                    msg = var12.append(MCH_KeyName.getDescOrName(MCH_Config.KeySwitchMode.prmInt)).toString();
                    this.drawString(msg, RX, super.centerY - 40, colorActive);
                }

                if(seatID == 0 && info.isEnableVtol) {
                    var10000 = MCH_MOD.config;
                    if(!Keyboard.isKeyDown(MCH_Config.KeyFreeLook.prmInt)) {
                        int stat = plane.getVtolMode();
                        if(stat != 1) {
                            //give the submarine a slow gradual descent gravity for diving.
                            //if(plane.getShipInfo().isSubmarine) {
                            //plane.motionY -= 0.01;
                            //plane.get
                            //}
                            var12 = (new StringBuilder()).append(stat == 0?"DIVING : ":"Normal : ");
                            var10001 = MCH_MOD.config;
                            msg = var12.append(MCH_KeyName.getDescOrName(MCH_Config.KeyExtra.prmInt)).toString();
                            this.drawString(msg, RX, super.centerY - 60, colorActive);
                        }
                    }
                }

                if(plane.canEjectSeat(player)) {
                    var12 = (new StringBuilder()).append("Eject seat: ");
                    var10001 = MCH_MOD.config;
                    msg = var12.append(MCH_KeyName.getDescOrName(MCH_Config.KeySwitchHovering.prmInt)).toString();
                    this.drawString(msg, RX, super.centerY - 30, colorActive);
                }

                if(plane.getIsGunnerMode(player) && info.cameraZoom > 1) {
                    var12 = (new StringBuilder()).append("Zoom : ");
                    var10001 = MCH_MOD.config;
                    msg = var12.append(MCH_KeyName.getDescOrName(MCH_Config.KeyZoom.prmInt)).toString();
                    this.drawString(msg, LX, super.centerY - 80, colorActive);
                } else if(seatID == 0) {
                    if(!plane.canFoldWing() && !plane.canUnfoldWing()) {
                        if(plane.canFoldHatch() || plane.canUnfoldHatch()) {
                            var12 = (new StringBuilder()).append("OpenHatch : ");
                            var10001 = MCH_MOD.config;
                            msg = var12.append(MCH_KeyName.getDescOrName(MCH_Config.KeyZoom.prmInt)).toString();
                            this.drawString(msg, LX, super.centerY - 80, colorActive);
                        }
                    } else {
                        var12 = (new StringBuilder()).append("FoldWing : ");
                        var10001 = MCH_MOD.config;
                        msg = var12.append(MCH_KeyName.getDescOrName(MCH_Config.KeyZoom.prmInt)).toString();
                        this.drawString(msg, LX, super.centerY - 80, colorActive);
                    }
                }

            }
        }
    }
}
