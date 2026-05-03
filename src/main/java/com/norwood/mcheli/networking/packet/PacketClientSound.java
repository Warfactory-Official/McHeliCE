package com.norwood.mcheli.networking.packet;

import hohserg.elegant.networking.api.ElegantPacket;
import hohserg.elegant.networking.api.ServerToClientPacket;
import lombok.AllArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Random;

@ElegantPacket
@AllArgsConstructor
public class PacketClientSound implements ServerToClientPacket {



    final public static Random rand = new Random();
    final public float posX, posY, posZ;
    final public String sound;
    final public boolean distort, silenced;

    public PacketClientSound(double x, double y, double z, String s)
    {
        this(x, y, z, s, false);
    }

    public PacketClientSound(double x, double y, double z, String s, boolean distort)
    {
        this(x, y, z, s, distort, false);
    }

    public PacketClientSound(double x, double y, double z, String s, boolean distort, boolean silenced)
    {
        posX = (float)x; posY = (float)y; posZ = (float)z;
        sound = s;
        this.distort = distort;
        this.silenced = silenced;
    }


    public static void sendSoundPacket(double x, double y, double z, double range, World dimension, String s, boolean distort)
    {
        sendSoundPacket(x, y, z, range, dimension, s, distort, false);
    }

    public static void sendSoundPacket(double x, double y, double z, double range, World dimension, String s, boolean distort, boolean silenced)
    {
        if(s!=null && !s.isEmpty())
        {
            new PacketClientSound(x, y, z, s, distort, silenced).sendPacketToAllAround(dimension,x,y,z,range);
        }
    }


    @Override
    public void onReceive(Minecraft mc) {
        SoundEvent snd  = ForgeRegistries.SOUND_EVENTS.getValue((new ResourceLocation(sound)));
        if(snd == null) return;
        FMLClientHandler.instance().getClient().getSoundHandler().playSound(new PositionedSoundRecord(snd, SoundCategory.MASTER, silenced ? 50F : 100F, (distort ? 1.0F / (rand.nextFloat() * 0.4F + 0.8F) : 1.0F) * (silenced ? 2F : 1F), posX, posY, posZ));
    }
}
