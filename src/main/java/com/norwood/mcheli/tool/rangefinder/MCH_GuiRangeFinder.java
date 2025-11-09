package com.norwood.mcheli.tool.rangefinder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_KeyName;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.gui.MCH_Gui;
import com.norwood.mcheli.wrapper.W_McClient;

@SideOnly(Side.CLIENT)
public class MCH_GuiRangeFinder extends MCH_Gui {

    public MCH_GuiRangeFinder(Minecraft minecraft) {
        super(minecraft);
    }

    @Override
    public void initGui() {
        super.initGui();
    }

    @Override
    public boolean isDrawGui(EntityPlayer player) {
        return MCH_ItemRangeFinder.canUse(player);
    }

    @Override
    public void drawGui(EntityPlayer player, boolean isThirdPersonView) {
        if (!isThirdPersonView) {
            GL11.glLineWidth(scaleFactor);
            if (this.isDrawGui(player)) {
                GlStateManager.disableBlend();
                if (MCH_ItemRangeFinder.isUsingScope(player)) {
                    this.drawRF(player);
                }
            }
        }
    }

    void drawRF(EntityPlayer player) {
        GlStateManager.enableBlend();
        GlStateManager.color(0.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        W_McClient.MOD_bindTexture("textures/gui/rangefinder.png");
        double size = 512.0;

        while (size < this.width || size < this.height) {
            size *= 2.0;
        }

        this.drawTexturedModalRectRotate(-(size - this.width) / 2.0, -(size - this.height) / 2.0, size, size, 0.0, 0.0,
                256.0, 256.0, 0.0F);
        GlStateManager.disableBlend();
        double factor = size / 512.0;
        double SCALE_FACTOR = scaleFactor * factor;
        double CX = (double) this.mc.displayWidth / 2;
        double CY = (double) this.mc.displayHeight / 2;
        double px = (CX - 80.0 * SCALE_FACTOR) / SCALE_FACTOR;
        double py = (CY + 55.0 * SCALE_FACTOR) / SCALE_FACTOR;
        GlStateManager.pushMatrix();
        GlStateManager.scale(factor, factor, factor);
        ItemStack item = player.getHeldItemMainhand();
        int damage = (int) ((double) (item.getMaxDamage() - item.getMetadata()) / item.getMaxDamage() * 100.0);
        this.drawDigit(String.format("%3d", damage), (int) px, (int) py, 13, damage > 0 ? -15663328 : -61424);
        if (damage <= 0) {
            this.drawString("Please craft", (int) px + 40, (int) py, -65536);
            this.drawString("redstone", (int) px + 40, (int) py + 10, -65536);
        }

        px = (CX - 20.0 * SCALE_FACTOR) / SCALE_FACTOR;
        if (damage > 0) {
            Vec3d vs = new Vec3d(player.posX, player.posY + player.getEyeHeight(), player.posZ);
            Vec3d ve = MCH_Lib.Rot2Vec3(player.rotationYaw, player.rotationPitch);
            ve = vs.add(ve.x * 300.0, ve.y * 300.0, ve.z * 300.0);
            RayTraceResult mop = player.world.rayTraceBlocks(vs, ve, true);
            if (mop != null && mop.typeOfHit != Type.MISS) {
                int range = (int) player.getDistance(mop.hitVec.x, mop.hitVec.y, mop.hitVec.z);
                this.drawDigit(String.format("%4d", range), (int) px, (int) py, 13, -15663328);
            } else {
                this.drawDigit("----", (int) px, (int) py, 13, -61424);
            }
        }

        py -= 4.0;
        px -= 80.0;
        drawRect((int) px, (int) py, (int) px + 30, (int) py + 2, -15663328);
        drawRect((int) px, (int) py, (int) px + MCH_ItemRangeFinder.rangeFinderUseCooldown / 2, (int) py + 2, -61424);
        this.drawString(String.format("x%.1f", MCH_ItemRangeFinder.zoom), (int) px, (int) py - 20, -1);
        px += 130.0;
        int mode = MCH_ItemRangeFinder.mode;
        this.drawString(">", (int) px, (int) py - 30 + mode * 10, -1);
        px += 10.0;
        this.drawString("Players/Vehicles", (int) px, (int) py - 30, mode == 0 ? -1 : -12566464);
        this.drawString("Monsters/Mobs", (int) px, (int) py - 20, mode == 1 ? -1 : -12566464);
        this.drawString("Mark Point", (int) px, (int) py - 10, mode == 2 ? -1 : -12566464);
        GlStateManager.popMatrix();
        px = (CX - 160.0 * SCALE_FACTOR) / scaleFactor;
        py = (CY - 100.0 * SCALE_FACTOR) / scaleFactor;
        if (px < 10.0) {
            px = 10.0;
        }

        if (py < 10.0) {
            py = 10.0;
        }

        String s = "Spot      : " + MCH_KeyName.getDescOrName(MCH_Config.KeyAttack.prmInt);
        this.drawString(s, (int) px, (int) py, -1);
        s = "Zoom in   : " + MCH_KeyName.getDescOrName(MCH_Config.KeyZoom.prmInt);
        this.drawString(s, (int) px, (int) py + 10, MCH_ItemRangeFinder.zoom < 10.0F ? -1 : -12566464);
        s = "Zoom out : " + MCH_KeyName.getDescOrName(MCH_Config.KeySwWeaponMode.prmInt);
        this.drawString(s, (int) px, (int) py + 20, MCH_ItemRangeFinder.zoom > 1.2F ? -1 : -12566464);
        s = "Mode      : " + MCH_KeyName.getDescOrName(MCH_Config.KeyFlare.prmInt);
        this.drawString(s, (int) px, (int) py + 30, -1);
    }
}
