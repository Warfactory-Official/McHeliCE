package com.norwood.mcheli.compat.hbm;


import lombok.Builder;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Builder
public class VNTSettingContainer {

    @Nullable public  String blockAllocatorPath;
    @Nullable public  String  entityProcessorPath;
    @Nullable public  String blockProcessorPath;
    @Nullable public  String  playerProcessorPath;
    @Nullable public  String  rangeMutatorPath;
    @Nullable public  String  blockMutatorPath;
    @Nullable public  List<String> explosionSFXPath;
    @Nullable public  String customDamageHandlerPath;
    @Nullable public String fortuneMutatorPath;
    @Nullable public String dropChanceMutatorPatch;

    //Allocator possible variables
    int allocatorResolution = 16;
    int allocatorMaximum = -1;

    //Entity Possible variables


    //ONLY ASSIGNABLE AT RUNTIME
        private Object blockAllocator;
        private Object entityProcessor;
        private Object playerProcessor;
        private Object[] sfx;
        private Object damageHandler;
        private Object rangeMutator;

        private Object blockProcessor;

        private Object blockMutator;
        private Object fortuneMutator;
        private Object dropChanceMutator;


    @Optional.Method(modid = "hbm")
    public void loadRuntimeInstances() {
        blockAllocator = loadInstance(blockAllocatorPath,  com.hbm.explosion.vanillant.interfaces.IBlockAllocator.class);
        entityProcessor = loadInstance(entityProcessorPath,  com.hbm.explosion.vanillant.interfaces.IEntityProcessor.class);
        blockProcessor = loadInstance(blockProcessorPath, com.hbm.explosion.vanillant.interfaces.IBlockProcessor.class);
        playerProcessor = loadInstance(playerProcessorPath, com.hbm.explosion.vanillant.interfaces.IPlayerProcessor.class);
        rangeMutator = loadInstance(rangeMutatorPath, com.hbm.explosion.vanillant.interfaces.IEntityRangeMutator.class);
        blockMutator = loadInstance(blockMutatorPath, com.hbm.explosion.vanillant.interfaces.IBlockMutator.class);
        damageHandler = loadInstance(customDamageHandlerPath, com.hbm.explosion.vanillant.interfaces.ICustomDamageHandler.class);
        fortuneMutator = loadInstance(fortuneMutatorPath, com.hbm.explosion.vanillant.interfaces.IFortuneMutator.class);

        if (explosionSFXPath != null) {
            List<com.hbm.explosion.vanillant.interfaces.IExplosionSFX> list = new ArrayList<>();
            for (String sfxPath : explosionSFXPath) {
                com.hbm.explosion.vanillant.interfaces.IExplosionSFX s = loadInstance(sfxPath, com.hbm.explosion.vanillant.interfaces.IExplosionSFX.class);
                if (s != null) list.add(s);
            }
            sfx = list.toArray(new com.hbm.explosion.vanillant.interfaces.IExplosionSFX[0]);
        } else {
            sfx = new com.hbm.explosion.vanillant.interfaces.IExplosionSFX[0];
        }
    }
    public Object buildExplosion(World world, double x, double y, double z, float size, Entity exploder  ){
        var vnt = new com.hbm.explosion.vanillant.ExplosionVNT(world,x,y,z,size,exploder);


        return vnt;
    }


    @SuppressWarnings("unchecked")
    @Nullable
    private static <T> T loadInstance(@Nullable String path, Class<T> iface) {
        if (path == null || path.isEmpty()) return null;
        try {
            Class<?> clazz = Class.forName(path);
            if (!iface.isAssignableFrom(clazz)) {
                System.err.println("[VNTCompat] " + path + " does not implement " + iface.getSimpleName());
                return null;
            }
            Constructor<?>  constructor = getLongestConstructor(clazz);
            if(constructor==null) return  null;
            constructor.setAccessible(true);

            return (T) constructor.newInstance();
        } catch (ClassNotFoundException e) {
            System.err.println("[VNTCompat] Missing class: " + path);
        } catch (Throwable t) {
            System.err.println("[VNTCompat] Failed to instantiate " + path + ": " + t);
        }
        return null;
    }

    public static Constructor<?> getLongestConstructor(Class<?> clazz) {
        Constructor<?>[] ctors = clazz.getDeclaredConstructors();
        if (ctors.length == 0) {
            // fallback
            return null;
        }
        return Arrays.stream(ctors)
                .max(Comparator.comparingInt(Constructor::getParameterCount))
                .orElse(ctors[0]);
    }



}
