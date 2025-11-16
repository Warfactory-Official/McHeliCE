package com.norwood.mcheli.aircraft;

import com.norwood.mcheli.MCH_Vector2;
import com.norwood.mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class MCH_Radar {

    private final World worldObj;
    private final ArrayList<MCH_Vector2> entityList = new ArrayList<>();
    private final ArrayList<MCH_Vector2> enemyList = new ArrayList<>();

    public MCH_Radar(World world) {
        this.worldObj = world;
    }

    public ArrayList<MCH_Vector2> getEntityList() {
        return this.entityList;
    }

    public ArrayList<MCH_Vector2> getEnemyList() {
        return this.enemyList;
    }

    public void clear() {
        this.entityList.clear();
        this.enemyList.clear();
    }

    public void updateXZ(Entity centerEntity, int range) {
        if (this.worldObj.isRemote) {
            this.clear();
            List<Entity> list = centerEntity.world
                    .getEntitiesWithinAABBExcludingEntity(centerEntity,
                            centerEntity.getEntityBoundingBox().grow(range, range, range));

            for (Entity entity : list) {
                if (entity instanceof EntityLiving) {
                    double x = entity.posX - centerEntity.posX;
                    double z = entity.posZ - centerEntity.posZ;
                    if (x * x + z * z < range * range) {
                        int y = 1 + (int) entity.posY;
                        if (y < 0) {
                            y = 1;
                        }

                        int blockCnt;
                        for (blockCnt = 0; y < 200; y++) {
                            if (W_WorldFunc.getBlockId(this.worldObj, (int) entity.posX, y, (int) entity.posZ) != 0) {
                                if (++blockCnt >= 5) {
                                    break;
                                }
                            }
                        }

                        if (blockCnt < 5) {
                            if (entity instanceof EntityMob) {
                                this.enemyList.add(new MCH_Vector2(x, z));
                            } else {
                                this.entityList.add(new MCH_Vector2(x, z));
                            }
                        }
                    }
                }
            }
        }
    }
}
