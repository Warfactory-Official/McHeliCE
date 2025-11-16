package com.norwood.mcheli.mob;

import com.norwood.mcheli.Tags;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelBiped.ArmPose;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_RenderGunner extends RenderLivingBase<MCH_EntityGunner> {

    public static final IRenderFactory<MCH_EntityGunner> FACTORY = MCH_RenderGunner::new;
    private static final ResourceLocation steveTextures = new ResourceLocation(Tags.MODID,
            "textures/mob/heligunner.png");
    public final ModelBiped modelBipedMain = (ModelBiped) this.mainModel;
    public final ModelBiped modelArmorChestplate = new ModelBiped(1.0F);
    public final ModelBiped modelArmor = new ModelBiped(0.5F);

    public MCH_RenderGunner(RenderManager renderManager) {
        super(renderManager, new ModelBiped(0.0F), 0.5F);
    }

    protected int shouldRenderPass(EntityLivingBase p_77032_1_, int p_77032_2_, float p_77032_3_) {
        return -1;
    }

    protected boolean canRenderName(MCH_EntityGunner targetEntity) {
        return targetEntity.getTeam() != null;
    }

    public void doRender(MCH_EntityGunner entity, double x, double y, double z, float entityYaw, float partialTicks) {
        // EntityLivingBase was MCH_EntityGunner
        GL11.glColor3f(1.0F, 1.0F, 1.0F);
        this.modelArmorChestplate.isSneak = this.modelArmor.isSneak = this.modelBipedMain.isSneak = entity.isSneaking();
        double d3 = y - entity.getMountedYOffset();
        if (entity.isSneaking()) {
            d3 -= 0.125;
        }

        MCH_EntityAircraft ac = ((MCH_EntityGunner) entity).getAc();
        if (ac != null && ac.getAcInfo() != null && (!ac.getAcInfo().hideEntity || !ac.isPilot(entity))) {
            super.doRender((MCH_EntityGunner) entity, x, d3, z, entityYaw, partialTicks);
        }

        this.modelArmorChestplate.isSneak = this.modelArmor.isSneak = this.modelBipedMain.isSneak = false;
        this.modelArmorChestplate.rightArmPose = this.modelArmor.rightArmPose = this.modelBipedMain.rightArmPose = ArmPose.EMPTY;
    }

    /**
     * 1.7.10 method
     * public void doRender(EntityLivingBase p_76986_1_, double p_76986_2_, double p_76986_4_, double p_76986_6_, float
     * p_76986_8_, float p_76986_9_) {
     * GL11.glColor3f(1.0F, 1.0F, 1.0F);
     * this.modelBipedMain.isSneak = p_76986_1_.isSneaking();
     * double d3 = p_76986_4_ - p_76986_1_.yOffset;
     * if (p_76986_1_.isSneaking() && !(p_76986_1_ instanceof net.minecraft.client.entity.EntityPlayerSP))
     * d3 -= 0.125D;
     * MCH_EntityAircraft ac = ((MCH_EntityGunner)p_76986_1_).getAc();
     * if (ac != null && ac.getAcInfo() != null && (!(ac.getAcInfo()).hideEntity || !ac.isPilot((Entity)p_76986_1_)))
     * super.doRender(p_76986_1_, p_76986_2_, d3, p_76986_6_, p_76986_8_, p_76986_9_);
     * this.modelBipedMain.aimedBow = false;
     * this.modelBipedMain.isSneak = false;
     * this.modelBipedMain.heldItemRight = 0;
     * }
     */

    protected void preRenderCallback(@NotNull MCH_EntityGunner entitylivingbaseIn, float partialTickTime) {
        float f1 = 0.9375F;
        GlStateManager.scale(f1, f1, f1);
    }

    public void renderFirstPersonArm(EntityPlayer p_82441_1_) {
        float f = 1.0F;
        GL11.glColor3f(f, f, f);
        this.modelBipedMain.swingProgress = 0.0F;
        this.modelBipedMain.setRotationAngles(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, p_82441_1_);
        this.modelBipedMain.bipedRightArm.render(0.0625F);
    }

    protected void _renderOffsetLivingLabel(
                                            MCH_EntityGunner p_96449_1_, double p_96449_2_, double p_96449_4_,
                                            double p_96449_6_, String p_96449_8_, float p_96449_9_,
                                            double p_96449_10_) {}

    protected void _preRenderCallback(MCH_EntityGunner entitylivingbaseIn, float partialTickTime) {
        this.preRenderCallback(entitylivingbaseIn, partialTickTime);
    }

    protected int _shouldRenderPass(MCH_EntityGunner p_77032_1_, int p_77032_2_, float p_77032_3_) {
        return this.shouldRenderPass(p_77032_1_, p_77032_2_, p_77032_3_);
    }

    protected void _renderEquippedItems(MCH_EntityGunner p_77029_1_, float p_77029_2_) {}

    protected void _rotateCorpse(MCH_EntityGunner entityLiving, float p_77043_2_, float rotationYaw,
                                 float partialTicks) {
        this.applyRotations(entityLiving, p_77043_2_, rotationYaw, partialTicks);
    }

    protected void _renderLivingAt(MCH_EntityGunner entityLivingBaseIn, double x, double y, double z) {
        this.renderLivingAt(entityLivingBaseIn, x, y, z);
    }

    public void _doRender(MCH_EntityGunner entity, double x, double y, double z, float entityYaw, float partialTicks) {
        this.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    protected ResourceLocation getEntityTexture(@NotNull MCH_EntityGunner entity) {
        return steveTextures;
    }

    public void _doRender2(MCH_EntityGunner entity, double x, double y, double z, float entityYaw, float partialTicks) {
        this.doRender(entity, x, y, z, entityYaw, partialTicks);
    }
}
