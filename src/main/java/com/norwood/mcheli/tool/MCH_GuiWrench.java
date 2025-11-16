package com.norwood.mcheli.tool;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.gui.MCH_Gui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_GuiWrench extends MCH_Gui {

    public MCH_GuiWrench(Minecraft minecraft) {
        super(minecraft);
    }

    @Override
    public void initGui() {
        super.initGui();
    }

    @Override
    public boolean isDrawGui(EntityPlayer player) {
        return player != null && player.world != null && !player.getHeldItemMainhand().isEmpty() &&
                player.getHeldItemMainhand().getItem() instanceof MCH_ItemWrench;
    }

    @Override
    public void drawGui(EntityPlayer player, boolean isThirdPersonView) {
        if (!isThirdPersonView) {
            GL11.glLineWidth(scaleFactor);
            if (this.isDrawGui(player)) {
                GlStateManager.disableBlend();
                MCH_EntityAircraft ac = ((MCH_ItemWrench) player.getHeldItemMainhand().getItem())
                        .getMouseOverAircraft(player);
                if (ac != null && ac.getMaxHP() > 0) {
                    int color = (float) ac.getHP() / ac.getMaxHP() > 0.3 ? -14101432 : -2161656;
                    this.drawHP(color, -15433180, ac.getHP(), ac.getMaxHP());
                }
            }
        }
    }

    void drawHP(int color, int colorBG, int hp, int hpmax) {
        int posX = this.centerX;
        int posY = this.centerY + 20;
        drawRect(posX - 20, posY + 20 + 1, posX - 20 + 40, posY + 20 + 1 + 1 + 3 + 1, colorBG);
        if (hp > hpmax) {
            hp = hpmax;
        }

        float hpp = (float) hp / hpmax;
        drawRect(posX - 20 + 1, posY + 20 + 1 + 1, posX - 20 + 1 + (int) (38.0 * hpp), posY + 20 + 1 + 1 + 3, color);
        int hppn = (int) (hpp * 100.0F);
        if (hp < hpmax && hppn >= 100) {
            hppn = 99;
        }

        this.drawCenteredString(String.format("%d %%", hppn), posX, posY + 30, color);
    }
}
