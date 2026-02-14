package com.norwood.mcheli.uav;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UAVTracker {


    @Nullable
    public static ChunkPos getUAVPos(@NotNull World world, @NotNull UUID id) {
        if (world.isRemote || !(world instanceof WorldServer serverWorld)) return null;
        var uav = serverWorld.getEntityFromUuid(id);
        if (uav != null) return new ChunkPos(uav.getPosition());
        return getChunkPosForUAV(world, id);
    }


    @Nullable
    private static ChunkPos getChunkPosForUAV(@NotNull World world, @NotNull UUID id) {
        if (world.isRemote) return null;
        return MCH_GlobalUAVData.get(world).entityLocations.get(id);
    }

    public static void saveUAVPos(@NotNull World world, @NotNull MCH_EntityAircraft aircraft) {
        if (world.isRemote) return;
        var data = MCH_GlobalUAVData.get(world);
        data.entityLocations.put(aircraft.getUniqueID(), new ChunkPos(aircraft.getPosition()));
        data.markDirty();
    }

    public static void delUAVPos(@NotNull World world, @NotNull MCH_EntityAircraft aircraft) {
        if (world.isRemote) return;
        var data = MCH_GlobalUAVData.get(world);
        data.entityLocations.remove(aircraft.getUniqueID());
        data.markDirty();
    }


    public static class MCH_GlobalUAVData extends WorldSavedData {
        private static final String DATA_NAME = "MCH_GlobalUAVData";
        protected Map<UUID, ChunkPos> entityLocations = new HashMap<>();

        public MCH_GlobalUAVData() {
            super(DATA_NAME);
        }

        @SuppressWarnings("unused")
        public MCH_GlobalUAVData(String name) {
            super(name);
        }

        public static MCH_GlobalUAVData get(net.minecraft.world.World world) {
            net.minecraft.world.storage.MapStorage storage = world.getMapStorage();
            var instance = (MCH_GlobalUAVData) storage.getOrLoadData(MCH_GlobalUAVData.class, DATA_NAME);

            if (instance == null) {
                instance = new MCH_GlobalUAVData();
                storage.setData(DATA_NAME, instance);
            }
            return instance;
        }

        @Override
        public void readFromNBT(NBTTagCompound nbt) {
            entityLocations.clear();
            NBTTagList list = nbt.getTagList("LocationList", Constants.NBT.TAG_COMPOUND);

            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound entry = list.getCompoundTagAt(i);
                UUID id = entry.getUniqueId("ID");
                int cx = entry.getInteger("CX");
                int cz = entry.getInteger("CZ");
                entityLocations.put(id, new ChunkPos(cx, cz));
            }
        }

        @Override
        public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
            NBTTagList list = new NBTTagList();

            for (Map.Entry<UUID, ChunkPos> entry : entityLocations.entrySet()) {
                NBTTagCompound compound = new NBTTagCompound();
                compound.setUniqueId("ID", entry.getKey());
                compound.setInteger("CX", entry.getValue().x);
                compound.setInteger("CZ", entry.getValue().z);
                list.appendTag(compound);
            }

            nbt.setTag("LocationList", list);
            return nbt;
        }
    }
}
