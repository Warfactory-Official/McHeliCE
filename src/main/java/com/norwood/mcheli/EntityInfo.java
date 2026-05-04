package com.norwood.mcheli;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import hohserg.elegant.networking.api.ElegantSerializable;
import hohserg.elegant.networking.api.IByteBufSerializable;
import io.netty.buffer.ByteBuf;
import lombok.*;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.network.ByteBufUtils;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class EntityInfo implements IByteBufSerializable {

    public int entityId;
    public String worldName;
    public String entityName;
    public String entityClassName;
    public double x;
    public double y;
    public double z;
    public double prevX;
    public double prevY;
    public double prevZ;
    public long lastUpdateTime;

    public EntityInfo(int entityId, String worldName, String entityName, String entityClassName, double posX,
                      double posY, double posZ, double lastTickPosX, double lastTickPosY, double lastTickPosZ) {
        this.entityId = entityId;
        this.worldName = worldName;
        this.entityName = entityName;
        this.entityClassName = entityClassName;
        this.x = posX;
        this.y = posY;
        this.z = posZ;
        this.prevX = lastTickPosX;
        this.prevY = lastTickPosY;
        this.prevZ = lastTickPosZ;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    @SuppressWarnings("unused")
    public EntityInfo(ByteBuf buf) {
        this.entityId = buf.readInt();
        this.worldName = ByteBufUtils.readUTF8String(buf);
        this.entityName = ByteBufUtils.readUTF8String(buf);
        this.entityClassName = ByteBufUtils.readUTF8String(buf);

        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.prevX = buf.readDouble();
        this.prevY = buf.readDouble();
        this.prevZ = buf.readDouble();

        this.lastUpdateTime = buf.readLong();
    }

    public void serialize(ByteBuf buf) {
        buf.writeInt(this.entityId);
        ByteBufUtils.writeUTF8String(buf, this.worldName);
        ByteBufUtils.writeUTF8String(buf, this.entityName);
        ByteBufUtils.writeUTF8String(buf, this.entityClassName);

        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeDouble(this.prevX);
        buf.writeDouble(this.prevY);
        buf.writeDouble(this.prevZ);

        buf.writeLong(this.lastUpdateTime);
    }

    public static EntityInfo createInfo(Entity e) {
        String name = String.valueOf(e.getCommandSenderEntity());
        if (e instanceof MCH_EntityAircraft) {
            MCH_EntityAircraft ac = (MCH_EntityAircraft) e;
            if (ac.getAcInfo() != null) {
                name = ac.getAcInfo().name;
            }
        }
        return new EntityInfo(e.getEntityId(),
                e.world.getWorldInfo().getWorldName(),
                name,
                e.getClass().getName(),
                e.posX, e.posY, e.posZ,
                e.lastTickPosX, e.lastTickPosY, e.lastTickPosZ);
    }

    public double getDistanceToEntity(Entity e) {
        return Math.sqrt((e.posX - x) * (e.posX - x) + (e.posY - y) * (e.posY - y) + (e.posZ - z) * (e.posZ - z));
    }

    public double getDistanceSqToEntity(Entity e) {
        return (e.posX - x) * (e.posX - x) + (e.posY - y) * (e.posY - y) + (e.posZ - z) * (e.posZ - z);
    }

    public double getHorizonalDistanceSqToEntity(Entity e) {
        return (e.posX - x) * (e.posX - x) + (e.posZ - z) * (e.posZ - z);
    }
}
