package com.norwood.mcheli.gltd;

import com.norwood.mcheli.MCH_Camera;
import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_KeyName;
import com.norwood.mcheli.gui.MCH_Gui;
import com.norwood.mcheli.wrapper.W_McClient;
import com.norwood.mcheli.wrapper.W_WorldFunc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_GuiGLTD extends MCH_Gui {

    public MCH_GuiGLTD(Minecraft minecraft) {
        super(minecraft);
    }

    @Override
    public void initGui() {
        super.initGui();
    }

    @Override
    public boolean isDrawGui(EntityPlayer player) {
        return player.getRidingEntity() != null && player.getRidingEntity() instanceof MCH_EntityGLTD;
    }

    @Override
    public void drawGui(EntityPlayer player, boolean isThirdPersonView) {
        if (!isThirdPersonView || MCH_Config.DisplayHUDThirdPerson.prmBool) {
            GL11.glLineWidth(scaleFactor);
            if (this.isDrawGui(player)) {
                MCH_EntityGLTD gltd = (MCH_EntityGLTD) player.getRidingEntity();
                if (gltd.camera.getMode(0) == 1) {
                    GlStateManager.enableBlend();
                    GlStateManager.color(0.0F, 1.0F, 0.0F, 0.3F);
                    GlStateManager.blendFunc(GL11.GL_ONE, GL11.GL_ONE);
                    W_McClient.MOD_bindTexture("textures/gui/alpha.png");
                    this.drawTexturedModalRectRotate(0.0, 0.0, this.width, this.height, this.rand.nextInt(256),
                            this.rand.nextInt(256), 256.0, 256.0, 0.0F);
                    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                    GlStateManager.disableBlend();
                }

                this.drawString(String.format("x%.1f", gltd.camera.getCameraZoom()), this.centerX - 70,
                        this.centerY + 10, -805306369);
                this.drawString(gltd.weaponCAS.getName(), this.centerX - 200, this.centerY + 65,
                        gltd.countWait == 0 ? -819986657 : -807468024);
                this.drawCommonPosition(gltd, -819986657);
                this.drawString(gltd.camera.getModeName(0), this.centerX + 30, this.centerY - 50, -819986657);
                this.drawSight(gltd.camera, -819986657);
                this.drawTargetPosition(gltd, -819986657, -807468024);
                this.drawKeyBind(gltd.camera, -805306369, -813727873);
            }
        }
    }

    public void drawKeyBind(MCH_Camera camera, int color, int colorCannotUse) {
        int OffX = this.centerX + 55;
        int OffY = this.centerY + 40;
        this.drawString("DISMOUNT :", OffX, OffY, color);
        this.drawString("CAM MODE :", OffX, OffY + 10, color);
        this.drawString("ZOOM IN   :", OffX, OffY + 20, camera.getCameraZoom() < 10.0F ? color : colorCannotUse);
        this.drawString("ZOOM OUT :", OffX, OffY + 30, camera.getCameraZoom() > 1.0F ? color : colorCannotUse);
        OffX += 60;
        this.drawString(
                MCH_KeyName.getDescOrName(42) + " or " + MCH_KeyName.getDescOrName(MCH_Config.KeyUnmount.prmInt), OffX,
                OffY, color);
        this.drawString(MCH_KeyName.getDescOrName(MCH_Config.KeyCameraMode.prmInt), OffX, OffY + 10, color);
        this.drawString(MCH_KeyName.getDescOrName(MCH_Config.KeyZoom.prmInt), OffX, OffY + 20,
                camera.getCameraZoom() < 10.0F ? color : colorCannotUse);
        this.drawString(MCH_KeyName.getDescOrName(MCH_Config.KeySwWeaponMode.prmInt), OffX, OffY + 30,
                camera.getCameraZoom() > 1.0F ? color : colorCannotUse);
    }

    public void drawCommonPosition(MCH_EntityGLTD gltd, int color) {
        Entity riddenByEntity = gltd.getRiddenByEntity();
        this.drawString(String.format("X: %+.1f", gltd.posX), this.centerX - 145, this.centerY, color);
        this.drawString(String.format("Y: %+.1f", gltd.posY), this.centerX - 145, this.centerY + 10, color);
        this.drawString(String.format("Z: %+.1f", gltd.posZ), this.centerX - 145, this.centerY + 20, color);
        this.drawString(String.format("AX: %+.1f", riddenByEntity.rotationYaw), this.centerX - 145, this.centerY + 40,
                color);
        this.drawString(String.format("AY: %+.1f", riddenByEntity.rotationPitch), this.centerX - 145, this.centerY + 50,
                color);
    }

    public void drawTargetPosition(MCH_EntityGLTD gltd, int color, int colorDanger) {
        Entity riddenByEntity = gltd.getRiddenByEntity();
        if (riddenByEntity != null) {
            World w = riddenByEntity.world;
            float yaw = riddenByEntity.rotationYaw;
            float pitch = riddenByEntity.rotationPitch;
            double tX = -MathHelper.sin(yaw / 180.0F * (float) Math.PI) *
                    MathHelper.cos(pitch / 180.0F * (float) Math.PI);
            double tZ = MathHelper.cos(yaw / 180.0F * (float) Math.PI) *
                    MathHelper.cos(pitch / 180.0F * (float) Math.PI);
            double tY = -MathHelper.sin(pitch / 180.0F * (float) Math.PI);
            double dist = MathHelper.sqrt(tX * tX + tY * tY + tZ * tZ);
            tX = tX * 80.0 / dist;
            tY = tY * 80.0 / dist;
            tZ = tZ * 80.0 / dist;
            MCH_Camera c = gltd.camera;
            Vec3d src = new Vec3d(c.posX, c.posY, c.posZ);
            Vec3d dst = new Vec3d(c.posX + tX, c.posY + tY, c.posZ + tZ);
            RayTraceResult m = W_WorldFunc.clip(w, src, dst);
            if (m != null) {
                this.drawString(String.format("X: %+.2fm", m.hitVec.x), this.centerX + 50, this.centerY - 5 - 15,
                        color);
                this.drawString(String.format("Y: %+.2fm", m.hitVec.y), this.centerX + 50, this.centerY - 5, color);
                this.drawString(String.format("Z: %+.2fm", m.hitVec.z), this.centerX + 50, this.centerY - 5 + 15,
                        color);
                double x = m.hitVec.x - c.posX;
                double y = m.hitVec.y - c.posY;
                double z = m.hitVec.z - c.posZ;
                double len = Math.sqrt(x * x + y * y + z * z);
                this.drawCenteredString(String.format("[%.2fm]", len), this.centerX, this.centerY + 30,
                        len > 20.0 ? color : colorDanger);
            } else {
                this.drawString("X: --.--m", this.centerX + 50, this.centerY - 5 - 15, color);
                this.drawString("Y: --.--m", this.centerX + 50, this.centerY - 5, color);
                this.drawString("Z: --.--m", this.centerX + 50, this.centerY - 5 + 15, color);
                this.drawCenteredString("[--.--m]", this.centerX, this.centerY + 30, colorDanger);
            }
        }
    }

    private void drawSight(MCH_Camera camera, int color) {
        double posX = this.centerX;
        double posY = this.centerY;
        double[] line2 = new double[] {
                posX - 30.0,
                posY - 10.0,
                posX - 30.0,
                posY - 20.0,
                posX - 30.0,
                posY - 20.0,
                posX - 10.0,
                posY - 20.0,
                posX - 30.0,
                posY + 10.0,
                posX - 30.0,
                posY + 20.0,
                posX - 30.0,
                posY + 20.0,
                posX - 10.0,
                posY + 20.0,
                posX + 30.0,
                posY - 10.0,
                posX + 30.0,
                posY - 20.0,
                posX + 30.0,
                posY - 20.0,
                posX + 10.0,
                posY - 20.0,
                posX + 30.0,
                posY + 10.0,
                posX + 30.0,
                posY + 20.0,
                posX + 30.0,
                posY + 20.0,
                posX + 10.0,
                posY + 20.0
        };
        this.drawLine(line2, color);
    }
}
