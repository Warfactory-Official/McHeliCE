package com.norwood.mcheli.aircraft;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_KeyName;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.gui.MCH_Gui;
import com.norwood.mcheli.hud.MCH_Hud;
import com.norwood.mcheli.weapon.*;
import com.norwood.mcheli.wrapper.W_McClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.ActiveRenderInfo;
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
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

import java.nio.FloatBuffer;

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
        this.drawHitMarker(ac.weaponSystem.getHitStatus(), ac.getMaxHitStatus(), color);
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
        float a = (float) (argb >> 24 & 255) / 255.0F;
        float r = (float) (argb >> 16 & 255) / 255.0F;
        float g = (float) (argb >> 8 & 255) / 255.0F;
        float b = (float) (argb & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.color(r, g, b, a);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();

        builder.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);

        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            builder.pos(x + (CIRCLE_COS[i] * radius), y + (CIRCLE_SIN[i] * radius), 0.0D).endVertex();
        }

        tessellator.draw();


        GlStateManager.enableTexture2D();
    }

    protected void drawTurretBallistics(MCH_EntityAircraft aircraft, EntityPlayer player){
        GlStateManager.pushMatrix();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        ScaledResolution res = new ScaledResolution(this.mc);
        double centerX = res.getScaledWidth_double() / 2.0;
        double centerY = res.getScaledHeight_double() / 2.0;
        this.drawLine(new double[]{
                centerX - 2, centerY, centerX + 2, centerY,
                centerX, centerY - 2, centerX, centerY + 2
        }, 0xFF00FF00);

        this.drawDetachedTurretDot(aircraft, player);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();

    }

    protected void drawDetachedTurretDot(MCH_EntityAircraft aircraft, EntityPlayer player) {
        MCH_WeaponSet weaponSet = aircraft.getCurrentWeapon(player);
        MCH_WeaponBase weapon = weaponSet != null ? weaponSet.getCurrentWeapon() : null;
        if (weapon == null || weapon.getInfo() == null) return;

        float yaw = aircraft.getCurrentWeaponShotYaw(player, this.smoothCamPartialTicks);
        float pitch = aircraft.getCurrentWeaponShotPitch(player, this.smoothCamPartialTicks);
        Vec3d look = MCH_Lib.RotVec3(0.0, 0.0, 1.0, -yaw, -pitch, 0.0F).normalize();

        Vec3d target = getWeaponTrajectoryTarget(aircraft, player, weapon, look, this.smoothCamPartialTicks);

        Entity camera = this.mc.getRenderViewEntity();
        if (camera == null) return;

        double camX = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * this.smoothCamPartialTicks;
        double camY = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * this.smoothCamPartialTicks + camera.getEyeHeight();
        double camZ = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * this.smoothCamPartialTicks;
        Vec3d cameraPos = new Vec3d(camX, camY, camZ);

        Vec3d camLookVec = camera.getLook(this.smoothCamPartialTicks);
        Vec3d dirToTarget = target.subtract(cameraPos).normalize();
        boolean isInFront = camLookVec.dotProduct(dirToTarget) > 0;

        FloatBuffer screenCoords = BufferUtils.createFloatBuffer(3);
        boolean result = Project.gluProject(
                (float) (target.x - camX), (float) (target.y - camY), (float) (target.z - camZ),
                ActiveRenderInfo.MODELVIEW, ActiveRenderInfo.PROJECTION, ActiveRenderInfo.VIEWPORT, screenCoords
        );

        if (result && isInFront) {
            ScaledResolution res = new ScaledResolution(this.mc);
            int scaleFactor = res.getScaleFactor();
            double screenX = screenCoords.get(0) / scaleFactor;
            double screenY = (ActiveRenderInfo.VIEWPORT.get(3) - screenCoords.get(1)) / scaleFactor;

            float focalY = ActiveRenderInfo.PROJECTION.get(5);
            double distanceToCamera = cameraPos.distanceTo(target);
            double physicalRadius = distanceToCamera * Math.tan(Math.toRadians(weapon.getInfo().accuracy * 0.5F));
            double viewportHeight = ActiveRenderInfo.VIEWPORT.get(3);

            double screenRadius = (physicalRadius * (focalY * (viewportHeight / 2.0))) / distanceToCamera;
            double finalRadius = Math.max(screenRadius / scaleFactor, 2.0);

            drawSpreadCircle(screenX, screenY, finalRadius, 0xCCffffff);

            if (finalRadius > 2.0) {
                this.drawLine(new double[]{
                        screenX - 2, screenY, screenX + 2, screenY,
                        screenX, screenY - 2, screenX, screenY + 2
                }, 0xCCFFFFFF);
            }
        }
    }




    protected Vec3d getWeaponTrajectoryTarget(MCH_EntityAircraft aircraft, EntityPlayer player, MCH_WeaponBase weapon, Vec3d look, float partialTicks) {
        MCH_WeaponInfo info = weapon.getInfo();
        double interX = aircraft.lastTickPosX + (aircraft.posX - aircraft.lastTickPosX) * partialTicks;
        double interY = aircraft.lastTickPosY + (aircraft.posY - aircraft.lastTickPosY) * partialTicks;
        double interZ = aircraft.lastTickPosZ + (aircraft.posZ - aircraft.lastTickPosZ) * partialTicks;

        Vec3d currentPos;
        if (aircraft instanceof com.norwood.mcheli.vehicle.MCH_EntityVehicle vehicle && aircraft.weaponSystem.isDetachedWeaponAimActive()) {
            currentPos = vehicle.getCurrentWeaponShotPos(weapon.position, player, partialTicks).add(interX, interY, interZ);
        } else {
            currentPos = weapon.getShotPos(aircraft).add(interX, interY, interZ);
        }

        double mx, my, mz;
        if (weapon instanceof MCH_WeaponBomb) {
            mx = aircraft.motionX;
            my = aircraft.motionY;
            mz = aircraft.motionZ;
        } else if (weapon instanceof MCH_WeaponTorpedo) {
            mx = look.x * info.acceleration + aircraft.motionX;
            my = look.y * info.acceleration + aircraft.motionY;
            mz = look.z * info.acceleration + aircraft.motionZ;
        } else {
            double accel = Math.min(weapon.acceleration, 3.9F);
            mx = look.x * accel;
            my = look.y * accel;
            mz = look.z * accel;
        }

        double accelFactor = (info.acceleration > 4.0F && (weapon instanceof MCH_WeaponMachineGun1 || weapon instanceof MCH_WeaponMachineGun2 || weapon instanceof MCH_WeaponRocket))
                ? info.acceleration / 4.0F : 1.0;

        if (weapon instanceof MCH_WeaponMachineGun1 || weapon instanceof MCH_WeaponMachineGun2) {
            currentPos = currentPos.add(mx * 0.5, my * 0.5, mz * 0.5);
        }

        Vec3d apexPos = currentPos;
        double highestY = currentPos.y;
        int maxSteps = (info.timeFuse > 0) ? Math.min(256, info.timeFuse) : 256;

        for (int step = 0; step < maxSteps; step++) {
            var blockPos = new net.minecraft.util.math.BlockPos(currentPos.x, currentPos.y, currentPos.z);
            boolean inWater = aircraft.world.getBlockState(blockPos).getMaterial() == net.minecraft.block.material.Material.WATER;

            if (currentPos.y > highestY) {
                highestY = currentPos.y;
                apexPos = currentPos;
            }

            my += inWater ? info.gravityInWater : info.gravity;
            if (inWater && info.velocityInWater > 0.0F) {
                mx *= info.velocityInWater; my *= info.velocityInWater; mz *= info.velocityInWater;
            }

            Vec3d nextPos = new Vec3d(currentPos.x + mx * accelFactor, currentPos.y + my * accelFactor, currentPos.z + mz * accelFactor);
            RayTraceResult hit = aircraft.world.rayTraceBlocks(currentPos, nextPos, false, true, false);

            if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK) {
                return hit.hitVec;
            }
            currentPos = nextPos;
        }


        return (my >= 0 || look.y > 0) ? apexPos : currentPos;
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
