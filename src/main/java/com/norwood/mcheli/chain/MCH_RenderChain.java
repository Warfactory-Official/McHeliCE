package com.norwood.mcheli.chain;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.MCH_ModelManager;
import com.norwood.mcheli.wrapper.W_Render;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

@SideOnly(Side.CLIENT)
public class MCH_RenderChain extends W_Render<MCH_EntityChain> {

    public static final IRenderFactory<MCH_EntityChain> FACTORY = MCH_RenderChain::new;

    public MCH_RenderChain(RenderManager renderManager) {
        super(renderManager);
    }

    public void doRender(@NotNull MCH_EntityChain e, double posX, double posY, double posZ, float par8, float par9) {
        if (e.towedEntity != null && e.towEntity != null) {
            GlStateManager.pushMatrix();
            GlStateManager.enableCull();
            GlStateManager.color(0.5F, 0.5F, 0.5F, 1.0F);
            GlStateManager.translate(
                    e.towedEntity.lastTickPosX - TileEntityRendererDispatcher.staticPlayerX,
                    e.towedEntity.lastTickPosY - TileEntityRendererDispatcher.staticPlayerY,
                    e.towedEntity.lastTickPosZ - TileEntityRendererDispatcher.staticPlayerZ);
            this.bindTexture("textures/chain.png");
            double dx = e.towEntity.lastTickPosX - e.towedEntity.lastTickPosX;
            double dy = e.towEntity.lastTickPosY - e.towedEntity.lastTickPosY;
            double dz = e.towEntity.lastTickPosZ - e.towedEntity.lastTickPosZ;
            double diff = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double x = dx * 0.95F / diff;
            double y = dy * 0.95F / diff;

            for (double z = dz * 0.95F / diff; diff > 0.95F; diff -= 0.95F) {
                GlStateManager.translate(x, y, z);
                GlStateManager.pushMatrix();
                Vec3d v = MCH_Lib.getYawPitchFromVec(x, y, z);
                GlStateManager.rotate((float) v.y, 0.0F, -1.0F, 0.0F);
                GlStateManager.rotate((float) v.z, 0.0F, 0.0F, 1.0F);
                MCH_ModelManager.render("chain");
                GlStateManager.popMatrix();
            }

            GlStateManager.popMatrix();
        }
    }

    protected ResourceLocation getEntityTexture(@NotNull MCH_EntityChain entity) {
        return TEX_DEFAULT;
    }
}
