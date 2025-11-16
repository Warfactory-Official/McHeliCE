package com.norwood.mcheli.weapon;

import com.norwood.mcheli.wrapper.W_Entity;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class MCH_RenderNone extends MCH_RenderBulletBase<W_Entity> {

    public static final IRenderFactory<W_Entity> FACTORY = MCH_RenderNone::new;

    protected MCH_RenderNone(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    public void renderBullet(W_Entity entity, double posX, double posY, double posZ, float yaw,
                             float partialTickTime) {}

    protected ResourceLocation getEntityTexture(W_Entity entity) {
        return TEX_DEFAULT;
    }
}
