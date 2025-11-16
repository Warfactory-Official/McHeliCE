package com.norwood.mcheli;

import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_SoundUpdater;
import com.norwood.mcheli.helicopter.MCH_HeliInfo;
import com.norwood.mcheli.helper.MCH_SoundEvents;
import com.norwood.mcheli.helper.MCH_Utils;
import com.norwood.mcheli.helper.addon.AddonManager;
import com.norwood.mcheli.helper.addon.AddonPack;
import com.norwood.mcheli.helper.addon.AddonResourceLocation;
import com.norwood.mcheli.helper.info.ContentRegistries;
import com.norwood.mcheli.helper.info.ContentType;
import com.norwood.mcheli.plane.MCH_PlaneInfo;
import com.norwood.mcheli.ship.MCH_ShipInfo;
import com.norwood.mcheli.tank.MCH_TankInfo;
import com.norwood.mcheli.vehicle.MCH_VehicleInfo;
import com.norwood.mcheli.weapon.MCH_WeaponInfo;
import com.norwood.mcheli.wrapper.W_LanguageRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;

import java.io.File;
import java.util.List;

public class MCH_CommonProxy {

    public MCH_Config config = null;
    public String lastConfigFileName;

    public void postInit(FMLPostInitializationEvent postEvent) {}

    public String getDataDir() {
        return MCH_Utils.getServer().getFolderName();
    }

    public void registerRenderer() {}

    public void registerBlockRenderer() {}

    public void registerModels() {}

    public void registerModelsHeli(MCH_HeliInfo info, boolean reload) {}

    public void registerModelsPlane(MCH_PlaneInfo info, boolean reload) {}

    public void registerModelsShip(MCH_ShipInfo info, boolean reload) {}

    public void registerModelsVehicle(MCH_VehicleInfo info, boolean reload) {}

    public void registerModelsTank(MCH_TankInfo info, boolean reload) {}

    public void registerClientTick() {}

    public void registerServerTick() {}

    public boolean isRemote() {
        return false;
    }

    public String side() {
        return "Server";
    }

    public MCH_SoundUpdater CreateSoundUpdater(MCH_EntityAircraft aircraft) {
        return null;
    }

    public void registerSounds() {
        MCH_SoundEvents.registerSoundEventName("alert");
        MCH_SoundEvents.registerSoundEventName("locked");
        MCH_SoundEvents.registerSoundEventName("gltd");
        MCH_SoundEvents.registerSoundEventName("zoom");
        MCH_SoundEvents.registerSoundEventName("ng");
        MCH_SoundEvents.registerSoundEventName("a-10_snd");
        MCH_SoundEvents.registerSoundEventName("gau-8_snd");
        MCH_SoundEvents.registerSoundEventName("hit");
        MCH_SoundEvents.registerSoundEventName("helidmg");
        MCH_SoundEvents.registerSoundEventName("heli");
        MCH_SoundEvents.registerSoundEventName("plane");
        MCH_SoundEvents.registerSoundEventName("plane_cc");
        MCH_SoundEvents.registerSoundEventName("plane_cv");
        MCH_SoundEvents.registerSoundEventName("chain");
        MCH_SoundEvents.registerSoundEventName("chain_ct");
        MCH_SoundEvents.registerSoundEventName("eject_seat");
        MCH_SoundEvents.registerSoundEventName("fim92_snd");
        MCH_SoundEvents.registerSoundEventName("fim92_reload");
        MCH_SoundEvents.registerSoundEventName("lockon");
        MCH_SoundEvents.registerSoundEventName("wrench");

        for (MCH_WeaponInfo info : ContentRegistries.weapon().values()) {
            MCH_SoundEvents.registerSoundEventName(info.soundFileName);
        }

        for (MCH_AircraftInfo info : ContentRegistries.plane().values()) {
            if (!info.soundMove.isEmpty()) {
                MCH_SoundEvents.registerSoundEventName(info.soundMove);
            }
        }

        for (MCH_AircraftInfo infox : ContentRegistries.heli().values()) {
            if (!infox.soundMove.isEmpty()) {
                MCH_SoundEvents.registerSoundEventName(infox.soundMove);
            }
        }

        for (MCH_AircraftInfo infoxx : ContentRegistries.tank().values()) {
            if (!infoxx.soundMove.isEmpty()) {
                MCH_SoundEvents.registerSoundEventName(infoxx.soundMove);
            }
        }

        for (MCH_AircraftInfo infoxxx : ContentRegistries.vehicle().values()) {
            if (!infoxxx.soundMove.isEmpty()) {
                MCH_SoundEvents.registerSoundEventName(infoxxx.soundMove);
            }
        }
    }

    public void loadConfig(String fileName) {
        this.lastConfigFileName = fileName;
        this.config = new MCH_Config("./", fileName);
        this.config.load();
        this.config.write();
    }

    public void reconfig() {
        MCH_Lib.DbgLog(false, "MCH_CommonProxy.reconfig()");
        this.loadConfig(this.lastConfigFileName);
    }

    public void save() {
        MCH_Lib.DbgLog(false, "MCH_CommonProxy.save()");
        this.config.write();
    }

    public void reloadHUD() {}

    public Entity getClientPlayer() {
        return null;
    }

    public void setCreativeDigDelay(int n) {}

    public void init() {}

    public boolean isFirstPerson() {
        return false;
    }

    public boolean isSinglePlayer() {
        return MCH_Utils.getServer().isSinglePlayer();
    }

    public void readClientModList() {}

    public void printChatMessage(ITextComponent chat, int showTime, int pos) {}

    public void hitBullet() {}

    public void clientLocked() {}

    public void setRenderEntityDistanceWeight(double renderDistWeight) {}

    public List<AddonPack> loadAddonPacks(File addonDir) {
        return AddonManager.loadAddons(addonDir);
    }

    public void onLoadStartAddons(int addonSize) {}

    public void onLoadStepAddon(String addonDomain) {
        MCH_Utils.logger().debug("addon({}) loading start.", addonDomain);
    }

    public void onLoadFinishAddons() {}

    public void onLoadStartContents(String typeName, int fileSize) {
        MCH_Utils.logger().debug("content type({}) loading start. steps:{}", typeName, fileSize);
    }

    public void onLoadFinishContents(String typeName) {}

    public void onParseStartFile(AddonResourceLocation location) {
        MCH_Utils.logger().debug("content file({}) loading start.", location);
    }

    public void onParseFinishFile(AddonResourceLocation location) {}

    public boolean canLoadContentDirName(String dir) {
        return ContentType.validateDirName(dir);
    }

    public void updateGeneratedLanguage() {
        W_LanguageRegistry.clear();
    }

    @Deprecated
    public void updateSoundsJson() {}

    public void registerParticleTextures(TextureStitchEvent.Pre event) {}

    public void registerShaders(TextureStitchEvent.Post event) {}
}
