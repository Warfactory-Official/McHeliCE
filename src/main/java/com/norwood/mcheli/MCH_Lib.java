package com.norwood.mcheli;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemMapBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import com.norwood.mcheli.wrapper.*;

public class MCH_Lib {

    public static final String[] AZIMUTH_8 = new String[] { "S", "SW", "W", "NW", "N", "NE", "E", "SE" };
    public static final int AZIMUTH_8_ANG = 360 / AZIMUTH_8.length;
    private static final HashMap<String, Material> mapMaterial = new HashMap<>();

    public static void init() {
        mapMaterial.clear();
        mapMaterial.put("air", Material.AIR);
        mapMaterial.put("grass", Material.GRASS);
        mapMaterial.put("ground", Material.GROUND);
        mapMaterial.put("wood", Material.WOOD);
        mapMaterial.put("rock", Material.ROCK);
        mapMaterial.put("iron", Material.IRON);
        mapMaterial.put("anvil", Material.ANVIL);
        mapMaterial.put("water", Material.WATER);
        mapMaterial.put("lava", Material.LAVA);
        mapMaterial.put("leaves", Material.LEAVES);
        mapMaterial.put("plants", Material.PLANTS);
        mapMaterial.put("vine", Material.VINE);
        mapMaterial.put("sponge", Material.SPONGE);
        mapMaterial.put("cloth", Material.CLOTH);
        mapMaterial.put("fire", Material.FIRE);
        mapMaterial.put("sand", Material.SAND);
        mapMaterial.put("circuits", Material.CIRCUITS);
        mapMaterial.put("carpet", Material.CARPET);
        mapMaterial.put("glass", Material.GLASS);
        mapMaterial.put("redstoneLight", Material.REDSTONE_LIGHT);
        mapMaterial.put("tnt", Material.TNT);
        mapMaterial.put("coral", Material.CORAL);
        mapMaterial.put("ice", Material.ICE);
        mapMaterial.put("packedIce", Material.PACKED_ICE);
        mapMaterial.put("snow", Material.SNOW);
        mapMaterial.put("craftedSnow", Material.CRAFTED_SNOW);
        mapMaterial.put("cactus", Material.CACTUS);
        mapMaterial.put("clay", Material.CLAY);
        mapMaterial.put("gourd", Material.GOURD);
        mapMaterial.put("dragonEgg", Material.DRAGON_EGG);
        mapMaterial.put("portal", Material.PORTAL);
        mapMaterial.put("cake", Material.CAKE);
        mapMaterial.put("web", Material.WEB);
        mapMaterial.put("piston", Material.PISTON);
    }

    public static Material getMaterialFromName(String name) {
        return mapMaterial.getOrDefault(name, null);
    }

    public static Vec3d calculateFaceNormal(Vec3d[] vertices) {
        Vec3d v1 = new Vec3d(vertices[1].x - vertices[0].x, vertices[1].y - vertices[0].y,
                vertices[1].z - vertices[0].z);
        Vec3d v2 = new Vec3d(vertices[2].x - vertices[0].x, vertices[2].y - vertices[0].y,
                vertices[2].z - vertices[0].z);
        return v1.crossProduct(v2).normalize();
    }

    public static double parseDouble(String s) {
        return s == null ? 0.0 : Double.parseDouble(s.replace(',', '.'));
    }

    public static float RNG(float a, float min, float max) {
        return a > max ? max : (Math.max(a, min));
    }

    public static double RNG(double a, double min, double max) {
        return a > max ? max : (Math.max(a, min));
    }

    public static float smooth(float rot, float prevRot, float tick) {
        return prevRot + (rot - prevRot) * tick;
    }

    public static float smoothRot(float rot, float prevRot, float tick) {
        if (rot - prevRot < -180.0F) {
            prevRot -= 360.0F;
        } else if (prevRot - rot < -180.0F) {
            prevRot += 360.0F;
        }

        return prevRot + (rot - prevRot) * tick;
    }

    public static double getRotateDiff(double base, double target) {
        base = getRotate360(base);
        target = getRotate360(target);
        if (target - base < -180.0) {
            target += 360.0;
        } else if (target - base > 180.0) {
            base += 360.0;
        }

        return target - base;
    }

    public static float getPosAngle(double tx, double tz, double cx, double cz) {
        double length_A = Math.sqrt(tx * tx + tz * tz);
        double length_B = Math.sqrt(cx * cx + cz * cz);
        double cos_sita = (tx * cx + tz * cz) / (length_A * length_B);
        double sita = Math.acos(cos_sita);
        return (float) (sita * 180.0 / Math.PI);
    }

    public static void applyEntityHurtResistantTimeConfig(Entity entity) {
        if (entity instanceof EntityLivingBase elb) {
            double h_time = MCH_Config.HurtResistantTime.prmDouble * elb.hurtResistantTime;
            elb.hurtResistantTime = (int) h_time;
        }
    }

    public static int round(double d) {
        return (int) (d + 0.5);
    }

    public static Vec3d Rot2Vec3(float yaw, float pitch) {
        return new Vec3d(
                -MathHelper.sin(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI),
                -MathHelper.sin(pitch / 180.0F * (float) Math.PI),
                MathHelper.cos(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI));
    }

    public static Vec3d RotVec3(double x, double y, double z, float yaw, float pitch) {
        Vec3d v = new Vec3d(x, y, z);
        v = v.rotatePitch(pitch / 180.0F * (float) Math.PI);
        return v.rotateYaw(yaw / 180.0F * (float) Math.PI);
    }

    public static Vec3d RotVec3(double x, double y, double z, float yaw, float pitch, float roll) {
        Vec3d v = new Vec3d(x, y, z);
        v = W_Vec3.rotateRoll(roll / 180.0F * (float) Math.PI, v);
        v = v.rotatePitch(pitch / 180.0F * (float) Math.PI);
        return v.rotateYaw(yaw / 180.0F * (float) Math.PI);
    }

    public static Vec3d RotVec3(Vec3d vin, float yaw, float pitch) {
        Vec3d v = new Vec3d(vin.x, vin.y, vin.z);
        v = v.rotatePitch(pitch / 180.0F * (float) Math.PI);
        return v.rotateYaw(yaw / 180.0F * (float) Math.PI);
    }

    public static Vec3d RotVec3(Vec3d vin, float yaw, float pitch, float roll) {
        Vec3d v = new Vec3d(vin.x, vin.y, vin.z);
        v = W_Vec3.rotateRoll(roll / 180.0F * (float) Math.PI, v);
        v = v.rotatePitch(pitch / 180.0F * (float) Math.PI);
        return v.rotateYaw(yaw / 180.0F * (float) Math.PI);
    }

    public static Vec3d _Rot2Vec3(float yaw, float pitch, float roll) {
        return new Vec3d(
                -MathHelper.sin(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI),
                -MathHelper.sin(pitch / 180.0F * (float) Math.PI),
                MathHelper.cos(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI));
    }

    public static double getRotate360(double r) {
        r %= 360.0;
        return r >= 0.0 ? r : r + 360.0;
    }

    public static void Log(String format, Object... data) {
        String side = MCH_MOD.proxy.isRemote() ? "[Client]" : "[Server]";
        System.out.printf("[" + getTime() + "][mcheli]" + side + " " + format + "\n", data);
    }

    public static void Log(World world, String format, Object... data) {
        if (world != null) {
            Log((world.isRemote ? "[ClientWorld]" : "[ServerWorld]") + " " + format, data);
        } else {
            Log("[UnknownWorld]" + format, data);
        }
    }

    public static void Log(Entity entity, String format, Object... data) {
        if (entity != null) {
            Log(entity.world, format, data);
        } else {
            Log((World) null, format, data);
        }
    }

    public static void DbgLog(boolean isRemote, String format, Object... data) {
        if (MCH_Config.DebugLog) {
            if (isRemote) {
                if (getClientPlayer() instanceof EntityPlayer) {}

                System.out.printf((format) + "%n", data);
            } else {
                System.out.printf((format) + "%n", data);
            }
        }
    }

    public static void DbgLog(World w, String format, Object... data) {
        DbgLog(w.isRemote, format, data);
    }

    public static String getTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss:SSS");
        return sdf.format(new Date());
    }

    public static String getAzimuthStr8(int dir) {
        dir %= 360;
        if (dir < 0) {
            dir += 360;
        }

        dir /= AZIMUTH_8_ANG;
        return AZIMUTH_8[dir];
    }

    public static void rotatePoints(double[] points, float r) {
        r = r / 180.0F * (float) Math.PI;

        for (int i = 0; i + 1 < points.length; i += 2) {
            double x = points[i];
            double y = points[i + 1];
            points[i] = x * MathHelper.cos(r) - y * MathHelper.sin(r);
            points[i + 1] = x * MathHelper.sin(r) + y * MathHelper.cos(r);
        }
    }

    public static void rotatePoints(ArrayList<MCH_Vector2> points, float r) {
        r = r / 180.0F * (float) Math.PI;

        for (int i = 0; i + 1 < points.size(); i += 2) {
            double x = points.get(i).x;
            double y = points.get(i).y;
            points.get(i).x = x * MathHelper.cos(r) - y * MathHelper.sin(r);
            points.get(i).y = x * MathHelper.sin(r) + y * MathHelper.cos(r);
        }
    }

    public static String[] listupFileNames(String path) {
        File dir = new File(path);
        return dir.list();
    }

    public static boolean isBlockInWater(World w, int x, int y, int z) {
        int[][] offset = new int[][] { { 0, -1, 0 }, { 0, 0, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { -1, 0, 0 }, { 1, 0, 0 },
                { 0, 1, 0 } };
        if (y > 0) {
            for (int[] o : offset) {
                if (W_WorldFunc.isBlockWater(w, x + o[0], y + o[1], z + o[2])) {
                    return true;
                }
            }

        }
        return false;
    }

    public static int getBlockIdY(World w, double posX, double posY, double posZ, int size, int lenY,
                                  boolean canColliableOnly) {
        Block block = getBlockY(w, posX, posY, posZ, size, lenY, canColliableOnly);
        return block == null ? 0 : W_Block.getIdFromBlock(block);
    }

    public static int getBlockIdY(Entity entity, int size, int lenY) {
        return getBlockIdY(entity, size, lenY, true);
    }

    public static int getBlockIdY(Entity entity, int size, int lenY, boolean canColliableOnly) {
        Block block = getBlockY(entity, size, lenY, canColliableOnly);
        return block == null ? 0 : W_Block.getIdFromBlock(block);
    }

    public static Block getBlockY(Entity entity, int size, int lenY, boolean canColliableOnly) {
        return getBlockY(entity.world, entity.posX, entity.posY, entity.posZ, size, lenY, canColliableOnly);
    }

    public static Block getBlockY(World world, Vec3d pos, int size, int lenY, boolean canColliableOnly) {
        return getBlockY(world, pos.x, pos.y, pos.z, size, lenY, canColliableOnly);
    }

    public static Block getBlockY(World world, double posX, double posY, double posZ, int size, int lenY,
                                  boolean canColliableOnly) {
        if (lenY != 0) {
            int px = (int) (posX + 0.5);
            int py = (int) (posY + 0.5);
            int pz = (int) (posZ + 0.5);
            int cntY = lenY > 0 ? lenY : -lenY;

            for (int y = 0; y < cntY; y++) {
                if (py + y < 0 || py + y > 255) {
                    return Blocks.AIR;
                }

                for (int x = -size / 2; x <= size / 2; x++) {
                    for (int z = -size / 2; z <= size / 2; z++) {
                        IBlockState iblockstate = world
                                .getBlockState(new BlockPos(px + x, py + (lenY > 0 ? y : -y), pz + z));
                        Block block = W_WorldFunc.getBlock(world, px + x, py + (lenY > 0 ? y : -y), pz + z);
                        if (block != Blocks.AIR) {
                            if (!canColliableOnly) {
                                return block;
                            }

                            if (block.canCollideCheck(iblockstate, true)) {
                                return block;
                            }
                        }
                    }
                }
            }

        }
        return Blocks.AIR;
    }

    public static Vec3d getYawPitchFromVec(Vec3d v) {
        return getYawPitchFromVec(v.x, v.y, v.z);
    }

    public static Vec3d getYawPitchFromVec(double x, double y, double z) {
        double p = MathHelper.sqrt(x * x + z * z);
        float yaw = (float) (Math.atan2(z, x) * 180.0 / Math.PI);
        float roll = (float) (Math.atan2(y, p) * 180.0 / Math.PI);
        return new Vec3d(0.0, yaw, roll);
    }

    public static float getAlpha(int argb) {
        return (argb >> 24) / 255.0F;
    }

    public static float getRed(int argb) {
        return (argb >> 16 & 0xFF) / 255.0F;
    }

    public static float getGreen(int argb) {
        return (argb >> 8 & 0xFF) / 255.0F;
    }

    public static float getBlue(int argb) {
        return (argb & 0xFF) / 255.0F;
    }

    public static void enableFirstPersonItemRender() {
        switch (MCH_Config.DisableItemRender.prmInt) {
            case 2 -> MCH_ItemRendererDummy.disableDummyItemRenderer();
            case 3 -> W_Reflection.restoreCameraZoom();
            default -> {}
        }
    }

    public static void disableFirstPersonItemRender(ItemStack itemStack) {
        if (!itemStack.isEmpty() || !(itemStack.getItem() instanceof ItemMapBase) ||
                W_McClient.getRenderEntity() instanceof MCH_ViewEntityDummy) {
            disableFirstPersonItemRender();
        }
    }

    public static void disableFirstPersonItemRender() {
        switch (MCH_Config.DisableItemRender.prmInt) {
            case 1:
                W_Reflection.setItemRendererMainHand(new ItemStack(MCH_MOD.invisibleItem));
                break;
            case 2:
                MCH_ItemRendererDummy.enableDummyItemRenderer();
                break;
            case 3:
                W_Reflection.setCameraZoom(1.01F);
        }
    }

    public static Entity getClientPlayer() {
        return MCH_MOD.proxy.getClientPlayer();
    }

    public static void setRenderViewEntity(EntityLivingBase entity) {
        if (MCH_Config.ReplaceRenderViewEntity.prmBool) {
            W_McClient.setRenderEntity(entity);
        }
    }

    public static boolean isFlansModEntity(Entity entity) {
        if (entity == null) {
            return false;
        } else {
            String className = entity.getClass().getName();
            return className.contains("EntityVehicle") || className.contains("EntityPlane") ||
                    className.contains("EntityMecha") || className.contains("EntityAAGun");
        }
    }
}
