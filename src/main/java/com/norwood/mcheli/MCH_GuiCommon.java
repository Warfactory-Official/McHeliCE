package com.norwood.mcheli;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;

import com.norwood.mcheli.aircraft.MCH_AircraftCommonGui;

@SideOnly(Side.CLIENT)
public class MCH_GuiCommon extends MCH_AircraftCommonGui {

    public int hitCount = 0;

    public MCH_GuiCommon(Minecraft minecraft) {
        super(minecraft);
    }

    @Override
    public boolean isDrawGui(EntityPlayer player) {
        return true;
    }

    @Override
    public void drawGui(EntityPlayer player, boolean isThirdPersonView) {
        GL11.glLineWidth(scaleFactor);
        this.drawHitMarker(this.hitCount, 15, -805306369);
    }

    @Override
    public void onTick() {
        super.onTick();
        if (this.hitCount > 0) {
            this.hitCount--;
        }
    }

    public void hitBullet() {
        this.hitCount = 15;
    }
}
