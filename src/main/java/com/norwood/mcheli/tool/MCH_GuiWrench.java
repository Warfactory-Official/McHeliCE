package com.norwood.mcheli.tool;

import com.norwood.mcheli.MCH_ClientProxy;
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

    private static final int COLOR_HP_HEALTHY = 0xFF28A745;
    private static final int COLOR_HP_LOW = 0xFFDF4758;
    private static final int COLOR_BACKGROUND = 0xFF14161C;

    private static final float LOW_HEALTH_THRESHOLD = 0.3F;

    private static final int BAR_WIDTH = 40;
    private static final int BAR_HEIGHT = 5;
    private static final int BAR_PADDING = 1;

    private static final int OFFSET_Y_BASE = 20;
    private static final int OFFSET_Y_BAR = 21;
    private static final int OFFSET_Y_TEXT = 30;

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
                MCH_EntityAircraft ac = MCH_ClientProxy.getMouseOverAircraft(player);
                if (ac != null && ac.getMaxHP() > 0) {
                    float healthPercentage = (float) ac.getHP() / ac.getMaxHP();
                    int color = healthPercentage > LOW_HEALTH_THRESHOLD ? COLOR_HP_HEALTHY : COLOR_HP_LOW;

                    this.drawHP(color, ac.getHP(), ac.getMaxHP());
                }
            }
        }
    }

    void drawHP(int color, int hp, int hpmax) {
        int posX = this.centerX;
        int posY = this.centerY + OFFSET_Y_BASE;

        int barTop = posY + OFFSET_Y_BAR;
        int barBottom = barTop + BAR_HEIGHT;
        int barLeft = posX - (BAR_WIDTH / 2);
        int barRight = posX + (BAR_WIDTH / 2);

        drawRect(barLeft, barTop, barRight, barBottom, MCH_GuiWrench.COLOR_BACKGROUND);

        if (hp > hpmax) {
            hp = hpmax;
        }

        float hpp = (float) hp / hpmax;
        int innerBarWidth = (int) ((BAR_WIDTH - (BAR_PADDING * 2)) * hpp);

        int innerLeft = barLeft + BAR_PADDING;
        int innerTop = barTop + BAR_PADDING;
        int innerRight = innerLeft + innerBarWidth;
        int innerBottom = barBottom - BAR_PADDING;

        drawRect(innerLeft, innerTop, innerRight, innerBottom, color);

        int hppn = (int) (hpp * 100.0F);
        if (hp < hpmax && hppn >= 100) {
            hppn = 99;
        }

        this.drawCenteredString(String.format("%d %%", hppn), posX, posY + OFFSET_Y_TEXT, color);
    }
}

