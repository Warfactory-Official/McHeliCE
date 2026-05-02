package com.norwood.mcheli.aircraft;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_KeyName;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.gui.MCH_Gui;
import com.norwood.mcheli.hud.MCH_Hud;
import com.norwood.mcheli.weapon.MCH_EntityTvMissile;
import com.norwood.mcheli.weapon.MCH_WeaponBase;
import com.norwood.mcheli.weapon.MCH_WeaponSet;
import com.norwood.mcheli.wrapper.W_McClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public abstract class MCH_AircraftCommonGui extends MCH_Gui {

    protected int currentLeftY;
    protected int currentRightY;

    public MCH_AircraftCommonGui(Minecraft minecraft) {
        super(minecraft);
    }

    public void drawHud(MCH_EntityAircraft aircraft, EntityPlayer player, int seatId) {
        MCH_AircraftInfo info = aircraft.getAcInfo();
        if (info == null) return;

        if (aircraft.isMissileCameraMode(player) && aircraft.getTVMissile() != null && info.hudTvMissile != null) {
            info.hudTvMissile.draw(aircraft, player, this.smoothCamPartialTicks);
            return;
        }

        if (seatId >= 0 && seatId < info.hudList.size()) {
            MCH_Hud hud = info.hudList.get(seatId);
            if (hud != null) {
                hud.draw(aircraft, player, this.smoothCamPartialTicks);
            }
        }
    }

    public void drawDebugtInfo(MCH_EntityAircraft ac) {
        if (!MCH_Config.DebugLog) {
            return;
        }

        EntityPlayer player = this.mc.player;
        if (player == null) {
            return;
        }

        int x = this.centerX - 145;
        int y = this.centerY;
        int color = -1342177281;
        this.drawString(String.format("X: %+.1f", ac.posX), x, y, color);
        this.drawString(String.format("Y: %+.1f", ac.posY), x, y + 10, color);
        this.drawString(String.format("Z: %+.1f", ac.posZ), x, y + 20, color);
        this.drawString(String.format("AX: %+.1f", player.rotationYaw), x, y + 40, color);
        this.drawString(String.format("AY: %+.1f", player.rotationPitch), x, y + 50, color);

        MCH_WeaponSet weaponSet = ac.getCurrentWeapon(player);
        if (weaponSet != null && weaponSet.getCurrentWeapon() != null) {
            float barrelYaw = ac.getCurrentWeaponShotYaw(player);
            float barrelPitch = ac.getCurrentWeaponShotPitch(player);
            this.drawString(String.format("BX: %+.1f", barrelYaw), x, y + 70, color);
            this.drawString(String.format("BY: %+.1f", barrelPitch), x, y + 80, color);
        }
    }

    public void drawNightVisionNoise() {
        GlStateManager.enableBlend();
        GlStateManager.color(0.0F, 1.0F, 0.0F, 0.3F);
        GlStateManager.blendFunc(GL11.GL_ONE, GL11.GL_ONE);
        W_McClient.MOD_bindTexture("textures/gui/alpha.png");
        this.drawTexturedModalRectRotate(0.0, 0.0, this.width, this.height, this.rand.nextInt(256),
                this.rand.nextInt(256), 256.0, 256.0, 0.0F);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
    }

    public void drawHitMarker(int hitStrength, int maxHitStrength, int baseColor) {
        if (hitStrength <= 0) return;

        final int centerX = this.centerX;
        final int centerY = this.centerY;
        final int outerOffset = 10;
        final int innerOffset = 5;

        // Define crosshair-style marker line segments (4 lines from edges to center cross)
        double[] markerLines = {
                centerX - outerOffset, centerY - outerOffset,
                centerX - innerOffset, centerY - innerOffset,

                centerX - outerOffset, centerY + outerOffset,
                centerX - innerOffset, centerY + innerOffset,

                centerX + outerOffset, centerY - outerOffset,
                centerX + innerOffset, centerY - innerOffset,

                centerX + outerOffset, centerY + outerOffset,
                centerX + innerOffset, centerY + innerOffset,
        };

        int alpha = hitStrength * (256 / maxHitStrength);
        int finalColor = (int) (MCH_Config.hitMarkColorAlpha * alpha) << 24 | MCH_Config.hitMarkColorRGB;// FIXME:
        // basecolor
        // ignored...?

        this.drawLine(markerLines, finalColor);
    }

    public void drawHitMarker(MCH_EntityAircraft ac, int color, int seatID) {
        this.drawHitMarker(ac.getHitStatus(), ac.getMaxHitStatus(), color);
    }

    protected void drawTvMissileNoise(MCH_EntityAircraft ac, MCH_EntityTvMissile tvmissile) {
        GlStateManager.enableBlend();
        GlStateManager.color(0.5F, 0.5F, 0.5F, 0.4F);
        GlStateManager.blendFunc(GL11.GL_ONE, GL11.GL_ONE);
        W_McClient.MOD_bindTexture("textures/gui/noise.png");
        this.drawTexturedModalRectRotate(0.0, 0.0, this.width, this.height, this.rand.nextInt(256),
                this.rand.nextInt(256), 256.0, 256.0, 0.0F);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
    }



    private void drawSpreadCircle(double x, double y, double radius, int argb) {
        float a = (float)(argb >> 24 & 255) / 255.0F;
        float r = (float)(argb >> 16 & 255) / 255.0F;
        float g = (float)(argb >> 8 & 255) / 255.0F;
        float b = (float)(argb & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(r, g, b, a);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();

        builder.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);

        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            builder.pos(x + (CIRCLE_COS[i] * radius), y + (CIRCLE_SIN[i] * radius), 0.0D).endVertex();
        }

        tessellator.draw();

        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    protected void drawDetachedTurretDot(MCH_EntityAircraft aircraft, EntityPlayer player) {
        if (!aircraft.isDetachedWeaponAimActive()) {
            return;
        }

        MCH_WeaponSet weaponSet = aircraft.getCurrentWeapon(player);
        if (weaponSet == null || weaponSet.getCurrentWeapon() == null) {
            return;
        }

        MCH_WeaponBase currentWeapon = weaponSet.getCurrentWeapon();


        double acX = aircraft.lastTickPosX + (aircraft.posX - aircraft.lastTickPosX) * this.smoothCamPartialTicks;
        double acY = aircraft.lastTickPosY + (aircraft.posY - aircraft.lastTickPosY) * this.smoothCamPartialTicks;
        double acZ = aircraft.lastTickPosZ + (aircraft.posZ - aircraft.lastTickPosZ) * this.smoothCamPartialTicks;

        Vec3d start = currentWeapon.getShotPos(aircraft).add(acX, acY, acZ);


        float yaw = aircraft.prevLastRiderYaw + (aircraft.getLastRiderYaw() - aircraft.prevLastRiderYaw) * this.smoothCamPartialTicks;
        float pitch = aircraft.prevLastRiderPitch + (aircraft.getLastRiderPitch() - aircraft.prevLastRiderPitch) * this.smoothCamPartialTicks;

        Vec3d direction = MCH_Lib.RotVec3(0.0, 0.0, 1.0, -yaw, -pitch, 0.0F).normalize();
        Vec3d end = start.add(direction.scale(512.0));
        RayTraceResult hit = aircraft.world.rayTraceBlocks(start, end, false, true, false);
        Vec3d target = hit != null && hit.hitVec != null ? hit.hitVec : end;

        Entity camera = this.mc.getRenderViewEntity();
        if (camera == null) {
            return;
        }

        double camX = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * this.smoothCamPartialTicks;
        double camY = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * this.smoothCamPartialTicks;
        double camZ = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * this.smoothCamPartialTicks;
        float camYaw = camera.prevRotationYaw + (camera.rotationYaw - camera.prevRotationYaw) * this.smoothCamPartialTicks;
        float camPitch = camera.prevRotationPitch + (camera.rotationPitch - camera.prevRotationPitch) * this.smoothCamPartialTicks;

        double yawRad = Math.toRadians(camYaw);
        double pitchRad = Math.toRadians(camPitch);
        Vec3d forward = new Vec3d(
                -Math.sin(yawRad) * Math.cos(pitchRad),
                -Math.sin(pitchRad),
                Math.cos(yawRad) * Math.cos(pitchRad));
        Vec3d right = new Vec3d(Math.cos(yawRad), 0.0, Math.sin(yawRad));
        Vec3d up = forward.crossProduct(right);
        Vec3d relative = target.subtract(camX, camY, camZ);

        double viewX = relative.dotProduct(right);
        double viewY = relative.dotProduct(up);
        double viewZ = relative.dotProduct(forward);

        if (viewZ <= 0.01) {
            return;
        }


        float acRoll = aircraft.prevRotationRoll + (aircraft.rotationRoll - aircraft.prevRotationRoll) * this.smoothCamPartialTicks;
        double rollRad = Math.toRadians(acRoll);

        double rotatedViewX = viewX * Math.cos(rollRad) - viewY * Math.sin(rollRad);
        double rotatedViewY = viewX * Math.sin(rollRad) + viewY * Math.cos(rollRad);


        double fov = this.mc.gameSettings.fovSetting;
        double focal = this.centerY / Math.tan(Math.toRadians(fov * 0.5));

        double screenX = this.centerX - rotatedViewX * focal / viewZ;
        double screenY = this.centerY - rotatedViewY * focal / viewZ;
        float baseSpread = currentWeapon.getInfo().accuracy * 0.5F;


        this.drawLine(new double[] {
                screenX - 1.5, screenY, screenX + 1.5, screenY,
                screenX, screenY - 1.5, screenX, screenY + 1.5
        }, 0xFF00FF00);
        drawSpreadCircle(screenX, screenY, baseSpread, 0xFF00FF00);

    }

    public void drawAircraftKeyBinds(MCH_EntityAircraft aircraft, MCH_AircraftInfo info, EntityPlayer player, int seatID, LayoutTheme theme) {
        if (seatID == 0) {
            drawLeftConditional(aircraft.canPutToRack(), "PutRack", MCH_Config.KeyPutToRack.prmInt, theme.leftX(), theme.active());
            drawLeftConditional(aircraft.canDownFromRack(), "DownRack", MCH_Config.KeyDownFromRack.prmInt, theme.leftX(), theme.active());
            drawLeftConditional(aircraft.canRideRack(), "RideRack", MCH_Config.KeyPutToRack.prmInt, theme.leftX(), theme.active());
            drawLeftConditional(aircraft.getRidingEntity() != null, "DismountRack", MCH_Config.KeyDownFromRack.prmInt, theme.leftX(), theme.active());
        }

        var multipleSeats = aircraft.getSeatNum() > 1;
        var freeLookPressed = Keyboard.isKeyDown(MCH_Config.KeyFreeLook.prmInt);

        if ((seatID > 0 && multipleSeats) || freeLookPressed) {
            var seatColor = seatID == 0 ? 'Ｐ' : theme.active();
            var prefix = seatID == 0 ? MCH_KeyName.getDescOrName(MCH_Config.KeyFreeLook.prmInt) + " + " : "";

            var nextMsg = "NextSeat : " + prefix + MCH_KeyName.getDescOrName(MCH_Config.KeyGUI.prmInt);
            drawString(nextMsg, theme.rightX(), currentRightY, seatColor);
            currentRightY += 10;

            var prevMsg = "PrevSeat : " + prefix + MCH_KeyName.getDescOrName(MCH_Config.KeyExtra.prmInt);
            drawString(prevMsg, theme.rightX(), currentRightY, seatColor);
            currentRightY += 10;
        }

        var gunnerStatus = aircraft.getGunnerStatus() ? "ON" : "OFF";
        var gunnerMsg = "Gunner %s : %s + %s".formatted(
                gunnerStatus,
                MCH_KeyName.getDescOrName(MCH_Config.KeyFreeLook.prmInt),
                MCH_KeyName.getDescOrName(MCH_Config.KeyCameraMode.prmInt)
        );
        drawString(gunnerMsg, theme.leftX(), currentLeftY, theme.active());
        currentLeftY += 10;

        if (seatID >= 0 && seatID <= 1 && aircraft.haveFlare()) {
            var flareColor = aircraft.isFlarePreparation() ? theme.inactive() : theme.active();
            drawRightKey("Flare", MCH_Config.KeyFlare.prmInt, theme.rightX(), flareColor);
        }

        if (seatID == 0 && info.haveLandingGear()) {
            if (aircraft.canFoldLandingGear()) {
                drawRightKey("Gear Up", MCH_Config.KeyGearUpDown.prmInt, theme.rightX(), theme.active());
            } else if (aircraft.canUnfoldLandingGear()) {
                drawRightKey("Gear Down", MCH_Config.KeyGearUpDown.prmInt, theme.rightX(), theme.active());
            }
        }

        var weaponSet = aircraft.getCurrentWeapon(player);
        if (aircraft.getWeaponNum() > 1) {
            drawLeftKey("Weapon", MCH_Config.KeySwitchWeapon2.prmInt, theme.leftX(), theme.active());
        }

        if (weaponSet != null && weaponSet.getCurrentWeapon() != null && weaponSet.getCurrentWeapon().numMode > 0) {
            drawLeftKey("WeaponMode", MCH_Config.KeySwWeaponMode.prmInt, theme.leftX(), theme.active());
        }

        if (aircraft.canSwitchSearchLight(player)) {
            drawLeftKey("SearchLight", MCH_Config.KeyCameraMode.prmInt, theme.leftX(), theme.active());
        } else if (aircraft.canSwitchCameraMode(seatID)) {
            drawLeftKey("CameraMode", MCH_Config.KeyCameraMode.prmInt, theme.leftX(), theme.active());
        }

        if (seatID == 0 && aircraft.getSeatNum() >= 1) {
            var isParachuting = info.isEnableParachuting && MCH_Lib.getBlockIdY(aircraft, 3, -10) == 0;
            var isRepelling = aircraft.canStartRepelling();

            var dismountLabel = isParachuting ? "Parachuting" : (isRepelling ? "Repelling" : "Dismount");
            var dismountColor = isRepelling ? 0x00FF00 : theme.active();

            drawLeftKey(dismountLabel, MCH_Config.KeyUnmount.prmInt, theme.leftX(), dismountColor);
        }

        var canFreeLook = (seatID == 0 && aircraft.canSwitchFreeLook()) ||
                (seatID > 0 && aircraft.canSwitchGunnerModeOtherSeat(player));
        drawLeftConditional(canFreeLook, "FreeLook", MCH_Config.KeyFreeLook.prmInt, theme.leftX(), theme.active());
    }

    protected void drawLeftKey(String label, int key, int x, int color) {
        var keyName = MCH_KeyName.getDescOrName(key);
        drawString(label + " : " + keyName, x, currentLeftY, color);
        currentLeftY += 10;
    }

    protected void drawRightKey(String label, int key, int x, int color) {
        var keyName = MCH_KeyName.getDescOrName(key);
        drawString(label + " : " + keyName, x, currentRightY, color);
        currentRightY += 10;
    }

    protected void drawLeftConditional(boolean condition, String label, int key, int x, int color) {
        if (condition) drawLeftKey(label, key, x, color);
    }

    public static record LayoutTheme(int leftX, int rightX, int active, int inactive) {
    }
}
