package com.norwood.mcheli.helper;

import com.google.common.collect.Sets;
import com.norwood.mcheli.MCH_Lib;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Objects;
import java.util.Set;

@EventBusSubscriber(
                    modid = "mcheli")
public class MCH_SoundEvents {

    private static final Set<ResourceLocation> registryWrapper = Sets.newLinkedHashSet();

    @SubscribeEvent
    static void onSoundEventRegisterEvent(Register<SoundEvent> event) {
        for (ResourceLocation soundLocation : registryWrapper) {
            event.getRegistry().register(new SoundEvent(soundLocation).setRegistryName(soundLocation));
        }
    }

    public static void registerSoundEventName(String name) {
        registerSoundEventName(MCH_Utils.suffix(name));
    }

    public static void registerSoundEventName(ResourceLocation name) {
        registryWrapper.add(name);
    }

    public static void playSound(World w, double x, double y, double z, String name, float volume, float pitch) {
        SoundEvent sound = getSound(name);
        w.playSound(null, x, y, z, sound, SoundCategory.MASTER, volume, pitch);
    }

    public static SoundEvent getSound(String name) {
        SoundEvent sound = getSound(new ResourceLocation(name));
        return Objects.requireNonNull(sound);
    }

    // THIS IS NOT FUCKING NULLABLE
    public static SoundEvent getSound(ResourceLocation location) {
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(location);
        if (sound == null) {
            MCH_Lib.Log("[WARNING] Sound event does not found. event name= " + location);
            return SoundEvents.BLOCK_STONE_BREAK;
        }
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT && sound.getSoundName() == null) {
            MCH_Lib.Log("[WARNING] Sound event is empty. event name= " + location);
            return SoundEvents.BLOCK_STONE_BREAK;
        }

        return sound;
    }
}
