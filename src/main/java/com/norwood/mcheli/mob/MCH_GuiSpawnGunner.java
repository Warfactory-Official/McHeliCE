package com.norwood.mcheli.mob;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import com.norwood.mcheli.gui.MCH_Gui;

@SideOnly(Side.CLIENT)
public class MCH_GuiSpawnGunner extends MCH_Gui {

    public MCH_GuiSpawnGunner(Minecraft minecraft) {
        super(minecraft);
    }

    @Override
    public void initGui() {
        super.initGui();
    }

    @Override
    public boolean isDrawGui(EntityPlayer player) {
        return player != null && player.world != null && !player.getHeldItemMainhand().isEmpty() &&
                player.getHeldItemMainhand().getItem() instanceof MCH_ItemSpawnGunner;
    }

    @Override
    public void drawGui(EntityPlayer player, boolean isThirdPersonView) {
        if (!isThirdPersonView) {
            if (this.isDrawGui(player)) {
                GL11.glLineWidth(scaleFactor);
                GlStateManager.disableBlend();
                this.draw(player, this.searchTarget(player));
            }
        }
    }

    private Entity searchTarget(EntityPlayer player) {
        float f = 1.0F;
        float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * f;
        float yaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * f;
        double dx = player.prevPosX + (player.posX - player.prevPosX) * f;
        double dy = player.prevPosY + (player.posY - player.prevPosY) * f + player.getEyeHeight();
        double dz = player.prevPosZ + (player.posZ - player.prevPosZ) * f;
        Vec3d vec3 = new Vec3d(dx, dy, dz);
        float f3 = MathHelper.cos(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f4 = MathHelper.sin(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f5 = -MathHelper.cos(-pitch * (float) (Math.PI / 180.0));
        float f6 = MathHelper.sin(-pitch * (float) (Math.PI / 180.0));
        float f7 = f4 * f5;
        float f8 = f3 * f5;
        double d3 = 5.0;
        Vec3d vec31 = vec3.add(f7 * d3, f6 * d3, f8 * d3);
        Entity target = null;
        List<MCH_EntityGunner> list = player.world.getEntitiesWithinAABB(MCH_EntityGunner.class,
                player.getEntityBoundingBox().grow(5.0, 5.0, 5.0));

        for (MCH_EntityGunner gunner : list) {
            if (gunner.getEntityBoundingBox().calculateIntercept(vec3, vec31) != null &&
                    (target == null || player.getDistanceSq(gunner) < player.getDistanceSq(target))) {
                target = gunner;
            }
        }

        if (target == null) {
            MCH_ItemSpawnGunner item = (MCH_ItemSpawnGunner) player.getHeldItemMainhand().getItem();
            if (item.targetType == 1 && !player.world.isRemote) {
                player.getTeam();
            }
            List<MCH_EntitySeat> list1 = player.world.getEntitiesWithinAABB(MCH_EntitySeat.class,
                    player.getEntityBoundingBox().grow(5.0, 5.0, 5.0));

            for (MCH_EntitySeat seat : list1) {
                if (seat.getParent() != null && seat.getParent().getAcInfo() != null &&
                        seat.getEntityBoundingBox().calculateIntercept(vec3, vec31) != null &&
                        (target == null || player.getDistanceSq(seat) < player.getDistanceSq(target))) {
                    if (seat.getRiddenByEntity() instanceof MCH_EntityGunner) {
                        target = seat.getRiddenByEntity();
                    } else {
                        target = seat;
                    }
                }
            }

            if (target == null) {
                List<MCH_EntityAircraft> list2 = player.world
                        .getEntitiesWithinAABB(MCH_EntityAircraft.class,
                                player.getEntityBoundingBox().grow(5.0, 5.0, 5.0));

                for (MCH_EntityAircraft ac : list2) {
                    if (!ac.isUAV() && ac.getAcInfo() != null &&
                            ac.getEntityBoundingBox().calculateIntercept(vec3, vec31) != null &&
                            (target == null || player.getDistanceSq(ac) < player.getDistanceSq(target))) {
                        if (ac.getRiddenByEntity() instanceof MCH_EntityGunner) {
                            target = ac.getRiddenByEntity();
                        } else {
                            target = ac;
                        }
                    }
                }
            }

        }
        return target;
    }

    void draw(EntityPlayer player, Entity entity) {
        if (entity != null) {
            GlStateManager.enableBlend();
            GlStateManager.color(0.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            double size = 512.0;

            while (size < this.width || size < this.height) {
                size *= 2.0;
            }

            GlStateManager.disableBlend();
            double factor = size / 512.0;
            double SCALE_FACTOR = scaleFactor * factor;
            double CX = (double) this.mc.displayWidth / 2;
            double CY = (double) this.mc.displayHeight / 2;
            double px = (CX - 0.0) / SCALE_FACTOR;
            double py = (CY + 0.0) / SCALE_FACTOR;
            GlStateManager.pushMatrix();
            if (entity instanceof MCH_EntityGunner gunner) {
                String seatName = "";
                if (gunner.getRidingEntity() instanceof MCH_EntitySeat) {
                    seatName = "(seat " + (((MCH_EntitySeat) gunner.getRidingEntity()).seatID + 2) + ")";
                } else if (gunner.getRidingEntity() instanceof MCH_EntityAircraft) {
                    seatName = "(seat 1)";
                }

                this.drawCenteredString(gunner.getTeamName() + " Gunner " + seatName, (int) px, (int) py + 20,
                        -8355840);
                int S = 10;
                this.drawLine(new double[] { px - S, py - S, px + S, py - S, px + S, py + S, px - S, py + S }, -8355840,
                        2);
            } else if (entity instanceof MCH_EntitySeat seat) {
                if (seat.getRiddenByEntity() == null) {
                    this.drawCenteredString("seat " + (seat.seatID + 2), (int) px, (int) py + 20, -16711681);
                    int S = 10;
                    this.drawLine(new double[] { px - S, py - S, px + S, py - S, px + S, py + S, px - S, py + S },
                            -16711681, 2);
                } else {
                    this.drawCenteredString("seat " + (seat.seatID + 2), (int) px, (int) py + 20, -65536);
                    int S = 10;
                    this.drawLine(new double[] { px - S, py - S, px + S, py - S, px + S, py + S, px - S, py + S },
                            -65536, 2);
                    this.drawLine(new double[] { px - S, py - S, px + S, py + S }, -65536);
                    this.drawLine(new double[] { px + S, py - S, px - S, py + S }, -65536);
                }
            } else if (entity instanceof MCH_EntityAircraft ac) {
                if (ac.getRiddenByEntity() == null) {
                    this.drawCenteredString("seat 1", (int) px, (int) py + 20, -16711681);
                    int S = 10;
                    this.drawLine(new double[] { px - S, py - S, px + S, py - S, px + S, py + S, px - S, py + S },
                            -16711681, 2);
                } else {
                    this.drawCenteredString("seat 1", (int) px, (int) py + 20, -65536);
                    int S = 10;
                    this.drawLine(new double[] { px - S, py - S, px + S, py - S, px + S, py + S, px - S, py + S },
                            -65536, 2);
                    this.drawLine(new double[] { px - S, py - S, px + S, py + S }, -65536);
                    this.drawLine(new double[] { px + S, py - S, px - S, py + S }, -65536);
                }
            }

            GlStateManager.popMatrix();
        }
    }
}
