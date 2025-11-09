package com.norwood.mcheli;

import java.io.File;
import java.util.Iterator;
import java.util.Map.Entry;

import net.minecraft.command.CommandHandler;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.world.World;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import org.apache.logging.log4j.Logger;

import com.norwood.mcheli.aircraft.*;
import com.norwood.mcheli.block.MCH_DraftingTableBlock;
import com.norwood.mcheli.block.MCH_DraftingTableTileEntity;
import com.norwood.mcheli.chain.MCH_EntityChain;
import com.norwood.mcheli.chain.MCH_ItemChain;
import com.norwood.mcheli.command.MCH_Command;
import com.norwood.mcheli.compat.ModCompatManager;
import com.norwood.mcheli.compat.oneprobe.AircraftInfoProvider;
import com.norwood.mcheli.container.MCH_EntityContainer;
import com.norwood.mcheli.container.MCH_ItemContainer;
import com.norwood.mcheli.flare.MCH_EntityFlare;
import com.norwood.mcheli.gltd.MCH_EntityGLTD;
import com.norwood.mcheli.gltd.MCH_ItemGLTD;
import com.norwood.mcheli.gui.MCH_GuiCommonHandler;
import com.norwood.mcheli.helicopter.MCH_EntityHeli;
import com.norwood.mcheli.helicopter.MCH_HeliInfo;
import com.norwood.mcheli.helicopter.MCH_ItemHeli;
import com.norwood.mcheli.helper.*;
import com.norwood.mcheli.helper.info.ContentRegistries;
import com.norwood.mcheli.item.MCH_Item;
import com.norwood.mcheli.item.MCH_ItemInfo;
import com.norwood.mcheli.item.MCH_ItemInfoManager;
import com.norwood.mcheli.lweapon.MCH_ItemLightWeaponBase;
import com.norwood.mcheli.lweapon.MCH_ItemLightWeaponBullet;
import com.norwood.mcheli.mob.MCH_EntityGunner;
import com.norwood.mcheli.mob.MCH_ItemSpawnGunner;
import com.norwood.mcheli.parachute.MCH_EntityParachute;
import com.norwood.mcheli.parachute.MCH_ItemParachute;
import com.norwood.mcheli.plane.MCH_EntityPlane;
import com.norwood.mcheli.plane.MCH_PlaneInfo;
import com.norwood.mcheli.plane.MCP_ItemPlane;
import com.norwood.mcheli.ship.MCH_EntityShip;
import com.norwood.mcheli.ship.MCH_ItemShip;
import com.norwood.mcheli.ship.MCH_ShipInfo;
import com.norwood.mcheli.tank.MCH_EntityTank;
import com.norwood.mcheli.tank.MCH_ItemTank;
import com.norwood.mcheli.tank.MCH_TankInfo;
import com.norwood.mcheli.throwable.MCH_EntityThrowable;
import com.norwood.mcheli.throwable.MCH_ItemThrowable;
import com.norwood.mcheli.throwable.MCH_ThrowableInfo;
import com.norwood.mcheli.tool.MCH_ItemWrench;
import com.norwood.mcheli.tool.rangefinder.MCH_ItemRangeFinder;
import com.norwood.mcheli.uav.MCH_EntityUavStation;
import com.norwood.mcheli.uav.MCH_ItemUavStation;
import com.norwood.mcheli.vehicle.MCH_EntityVehicle;
import com.norwood.mcheli.vehicle.MCH_ItemVehicle;
import com.norwood.mcheli.vehicle.MCH_VehicleInfo;
import com.norwood.mcheli.weapon.*;
import com.norwood.mcheli.wrapper.W_Item;
import com.norwood.mcheli.wrapper.W_LanguageRegistry;
import com.norwood.mcheli.wrapper.W_NetworkRegistry;

@Mod(
     modid = "mcheli",
     name = "MC Helicopter MOD",
     dependencies = "required-after:elegant_networking;after:hbm")
public class MCH_MOD {

    public static final String MOD_ID = Tags.MODID;
    @Deprecated
    public static final String DOMAIN = MOD_ID;
    public static final String ADDON_FOLDER_NAME = "mcheli_addons";
    public static String VER = "";
    @Instance("mcheli")
    public static MCH_MOD instance;
    @SidedProxy(
                clientSide = "com.norwood.mcheli.MCH_ClientProxy",
                serverSide = "com.norwood.mcheli.MCH_CommonProxy")
    public static MCH_CommonProxy proxy;
    public static MCH_Config config;
    public static String sourcePath;
    public static MCH_InvisibleItem invisibleItem;
    public static MCH_ItemGLTD itemGLTD;
    public static MCH_ItemLightWeaponBullet itemStingerBullet;
    public static MCH_ItemLightWeaponBase itemStinger;
    public static MCH_ItemLightWeaponBullet itemJavelinBullet;
    public static MCH_ItemLightWeaponBase itemJavelin;
    public static MCH_ItemUavStation[] itemUavStation;
    public static MCH_ItemParachute itemParachute;
    public static MCH_ItemContainer itemContainer;
    public static MCH_ItemChain itemChain;
    public static MCH_ItemFuel itemFuel;
    public static MCH_ItemWrench itemWrench;
    public static MCH_ItemRangeFinder itemRangeFinder;
    public static MCH_ItemSpawnGunner itemSpawnGunnerVsPlayer;
    public static MCH_ItemSpawnGunner itemSpawnGunnerVsMonster;
    public static MCH_CreativeTabs creativeTabs;
    public static MCH_CreativeTabs creativeTabsHeli;
    public static MCH_CreativeTabs creativeTabsPlane;
    public static MCH_CreativeTabs creativeTabsShip;
    public static MCH_CreativeTabs creativeTabsItem;
    public static MCH_CreativeTabs creativeTabsTank;
    public static MCH_CreativeTabs creativeTabsVehicle;
    public static MCH_DraftingTableBlock blockDraftingTable;
    public static MCH_DraftingTableBlock blockDraftingTableLit;
    private static File sourceFile;
    private static File addonDir;

    public static void registerItem(W_Item item, String name, MCH_CreativeTabs ct) {
        item.setTranslationKey("mcheli:" + name);
        if (ct != null) {
            item.setCreativeTab(ct);
            ct.addIconItem(item);
        }

        MCH_Items.register(item, name);
    }

    public static void registerItemCustom() {
        System.out.println("[mcheli.MCH_MOD:registerItemCustom] Starting custom item registration...");

        Iterator<String> i$ = MCH_ItemInfoManager.getKeySet().iterator();

        while (i$.hasNext()) {
            String name = i$.next();
            System.out.println("[mcheli.MCH_MOD:registerItemCustom] Processing item: " + name);

            // Get the item info for the current item
            MCH_ItemInfo info = MCH_ItemInfoManager.get(name);

            // Check if item info is null
            if (info == null) {
                System.out.println(
                        "[mcheli.MCH_MOD:registerItemCustom] Error: Item info for " + name + " is null! Skipping...");
                continue;
            }

            // Separate logic for throwable items (grenades)
            if (isThrowableItem(name)) {
                // Skip registering the throwable item in the normal item registration logic
                System.out.println("[mcheli.MCH_MOD:registerItemCustom] Skipping throwable item: " + name);
                continue;
            }

            // Register as a normal item (non-throwable)
            info.item = new MCH_Item(info.itemID);
            info.item.setMaxStackSize(info.stackSize);
            registerItem(info.item, name, creativeTabsItem);
            info.itemID = W_Item.getIdFromItem(info.item) - 256;
            W_LanguageRegistry.addName(info.item, info.displayName);

            // Register item names in multiple languages
            // for (String lang : info.displayNameLang.keySet()) {
            // W_LanguageRegistry.addNameForObject(info.item, (Object) lang, info.displayNameLang.get(lang));
            // }
            // let's get one thing fucking clear before I split you in two. The lang is Fucking Working.
        }
    }

    private static boolean isThrowableItem(String name) {
        return name.toLowerCase().contains("grenade");
        // worst method in the world award
    }

    public static void registerItemThrowable() {
        for (Entry<String, MCH_ThrowableInfo> entry : ContentRegistries.throwable().entries()) {
            MCH_ThrowableInfo info = entry.getValue();
            info.item = new MCH_ItemThrowable(info.itemID);
            info.item.setMaxStackSize(info.stackSize);
            registerItem(info.item, entry.getKey(), creativeTabs);
            MCH_ItemThrowable.registerDispenseBehavior(info.item);
            info.itemID = W_Item.getIdFromItem(info.item) - 256;
            W_LanguageRegistry.addName(info.item, info.displayName);

            for (String lang : info.displayNameLang.keySet()) {
                W_LanguageRegistry.addNameForObject(info.item, lang, info.displayNameLang.get(lang));
            }
        }
    }

    public static void registerItemAircraft() {
        for (Entry<String, MCH_HeliInfo> entry : ContentRegistries.heli().entries()) {
            MCH_HeliInfo info = entry.getValue();
            info.item = new MCH_ItemHeli(info.itemID);
            info.item.setMaxDamage(info.maxHp);
            if (info.canRide || !(info.ammoSupplyRange > 0.0F) && !(info.fuelSupplyRange > 0.0F)) {
                registerItem(info.item, entry.getKey(), creativeTabsHeli);
            } else {
                registerItem(info.item, entry.getKey(), creativeTabs);
            }

            MCH_ItemAircraft.registerDispenseBehavior(info.item);
            info.itemID = W_Item.getIdFromItem(info.item) - 256;
            W_LanguageRegistry.addName(info.item, info.displayName);

            for (String lang : info.displayNameLang.keySet()) {
                W_LanguageRegistry.addNameForObject(info.item, lang, info.displayNameLang.get(lang));
            }
        }

        for (Entry<String, MCH_PlaneInfo> entry : ContentRegistries.plane().entries()) {
            MCH_PlaneInfo info = entry.getValue();
            info.item = new MCP_ItemPlane(info.itemID);
            info.item.setMaxDamage(info.maxHp);
            if (info.canRide || !(info.ammoSupplyRange > 0.0F) && !(info.fuelSupplyRange > 0.0F)) {
                registerItem(info.item, entry.getKey(), creativeTabsPlane);
            } else {
                registerItem(info.item, entry.getKey(), creativeTabs);
            }

            MCH_ItemAircraft.registerDispenseBehavior(info.item);
            info.itemID = W_Item.getIdFromItem(info.item) - 256;
            W_LanguageRegistry.addName(info.item, info.displayName);

            for (String lang : info.displayNameLang.keySet()) {
                W_LanguageRegistry.addNameForObject(info.item, lang, info.displayNameLang.get(lang));
            }
        }

        for (Entry<String, MCH_ShipInfo> entry : ContentRegistries.ship().entries()) {
            MCH_ShipInfo info = entry.getValue();
            info.item = new MCH_ItemShip(info.itemID);
            info.item.setMaxDamage(info.maxHp);
            if (info.canRide || !(info.ammoSupplyRange > 0.0F) && !(info.fuelSupplyRange > 0.0F)) {
                registerItem(info.item, entry.getKey(), creativeTabsShip);
            } else {
                registerItem(info.item, entry.getKey(), creativeTabs);
            }

            MCH_ItemAircraft.registerDispenseBehavior(info.item);
            info.itemID = W_Item.getIdFromItem(info.item) - 256;
            W_LanguageRegistry.addName(info.item, info.displayName);

            for (String lang : info.displayNameLang.keySet()) {
                W_LanguageRegistry.addNameForObject(info.item, lang, info.displayNameLang.get(lang));
            }
        }

        for (Entry<String, MCH_TankInfo> entry : ContentRegistries.tank().entries()) {
            MCH_TankInfo info = entry.getValue();
            info.item = new MCH_ItemTank(info.itemID);
            info.item.setMaxDamage(info.maxHp);
            if (info.canRide || !(info.ammoSupplyRange > 0.0F) && !(info.fuelSupplyRange > 0.0F)) {
                registerItem(info.item, entry.getKey(), creativeTabsTank);
            } else {
                registerItem(info.item, entry.getKey(), creativeTabs);
            }

            MCH_ItemAircraft.registerDispenseBehavior(info.item);
            info.itemID = W_Item.getIdFromItem(info.item) - 256;
            W_LanguageRegistry.addName(info.item, info.displayName);

            for (String lang : info.displayNameLang.keySet()) {
                W_LanguageRegistry.addNameForObject(info.item, lang, info.displayNameLang.get(lang));
            }
        }

        for (Entry<String, MCH_VehicleInfo> entry : ContentRegistries.vehicle().entries()) {
            MCH_VehicleInfo info = entry.getValue();
            info.item = new MCH_ItemVehicle(info.itemID);
            info.item.setMaxDamage(info.maxHp);
            if (info.canRide || !(info.ammoSupplyRange > 0.0F) && !(info.fuelSupplyRange > 0.0F)) {
                registerItem(info.item, entry.getKey(), creativeTabsVehicle);
            } else {
                registerItem(info.item, entry.getKey(), creativeTabs);
            }

            MCH_ItemAircraft.registerDispenseBehavior(info.item);
            info.itemID = W_Item.getIdFromItem(info.item) - 256;
            W_LanguageRegistry.addName(info.item, info.displayName);

            for (String lang : info.displayNameLang.keySet()) {
                W_LanguageRegistry.addNameForObject(info.item, lang, info.displayNameLang.get(lang));
            }
        }
    }

    @Deprecated
    public static Logger getLogger() {
        return MCH_Logger.get();
    }

    public static File getSource() {
        return sourceFile;
    }

    public static File getAddonDir() {
        return addonDir;
    }

    @EventHandler
    public void PreInit(FMLPreInitializationEvent evt) {
        MCH_Logger.setLogger(evt.getModLog());
        VER = Loader.instance().activeModContainer().getVersion();
        MCH_Lib.init();
        MCH_Lib.Log("MC Ver:1.12.2 MOD Ver:" + VER);
        MCH_Lib.Log("Start load...");
        sourcePath = evt.getSourceFile().getPath();
        sourceFile = evt.getSourceFile();
        addonDir = new File(evt.getModConfigurationDirectory().getParentFile(), ADDON_FOLDER_NAME);
        MCH_Lib.Log("SourcePath: " + sourcePath);
        MCH_Lib.Log("CurrentDirectory:" + new File(".").getAbsolutePath());
        proxy.init();
        creativeTabs = new MCH_CreativeTabs("MCHeli CE Items");
        creativeTabsHeli = new MCH_CreativeTabs("MCHeli CE Helicopters");
        creativeTabsPlane = new MCH_CreativeTabs("MCHeli CE Planes");
        creativeTabsShip = new MCH_CreativeTabs("MCHeli CE Ships");
        creativeTabsTank = new MCH_CreativeTabs("MCHeli CE Tanks");
        creativeTabsVehicle = new MCH_CreativeTabs("MCHeli CE Vehicles");
        creativeTabsItem = new MCH_CreativeTabs("MCHeli CE Recipe Items");
        proxy.loadConfig("config/mcheli.cfg");
        config = proxy.config;
        ContentRegistries.loadContents(addonDir);
        MCH_Lib.Log("Register item");
        this.registerItemSpawnGunner();
        this.registerItemRangeFinder();
        this.registerItemWrench();
        this.registerItemFuel();
        this.registerItemGLTD();
        this.registerItemChain();
        this.registerItemParachute();
        this.registerItemContainer();
        this.registerItemUavStation();
        this.registerItemInvisible();
        registerItemThrowable();
        registerItemCustom();
        this.registerItemLightWeaponBullet();
        this.registerItemLightWeapon();
        registerItemAircraft();
        blockDraftingTable = new MCH_DraftingTableBlock(MCH_Config.BlockID_DraftingTableOFF.prmInt, false);
        blockDraftingTable.setTranslationKey("drafting_table");
        blockDraftingTable.setCreativeTab(creativeTabs);
        blockDraftingTableLit = new MCH_DraftingTableBlock(MCH_Config.BlockID_DraftingTableON.prmInt, true);
        blockDraftingTableLit.setTranslationKey("lit_drafting_table");
        MCH_Blocks.register(blockDraftingTable, "drafting_table");
        MCH_Blocks.register(blockDraftingTableLit, "lit_drafting_table");
        MCH_Items.registerBlock(blockDraftingTable);
        W_LanguageRegistry.addName(blockDraftingTable, "Drafting Table");
        W_LanguageRegistry.addNameForObject(blockDraftingTable, "ja_jp", "製図台");
        MCH_CriteriaTriggers.registerTriggers();
        MCH_Lib.Log("Register system");
        MinecraftForge.EVENT_BUS.register(new MCH_EventHook());
        proxy.registerClientTick();
        W_NetworkRegistry.registerGuiHandler(this, new MCH_GuiCommonHandler());
        MCH_Lib.Log("Register entity");
        this.registerEntity();
        MCH_Lib.Log("Register renderer");
        proxy.registerRenderer();
        MCH_Lib.Log("Register Sounds");
        proxy.registerSounds();
        proxy.updateGeneratedLanguage();
        MCH_Lib.Log("End load");
    }

    @EventHandler
    public void init(FMLInitializationEvent evt) {
        GameRegistry.registerTileEntity(MCH_DraftingTableTileEntity.class, MCH_Utils.suffix("drafting_table"));
        if (World.MAX_ENTITY_RADIUS < 5)
            World.MAX_ENTITY_RADIUS = 5;
        proxy.registerBlockRenderer();
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent evt) {
        creativeTabs.setFixedIconItem(MCH_Config.CreativeTabIcon.prmString);
        creativeTabsHeli.setFixedIconItem(MCH_Config.CreativeTabIconHeli.prmString);
        creativeTabsPlane.setFixedIconItem(MCH_Config.CreativeTabIconPlane.prmString);
        creativeTabsShip.setFixedIconItem(MCH_Config.CreativeTabIconShip.prmString);
        creativeTabsTank.setFixedIconItem(MCH_Config.CreativeTabIconTank.prmString);
        creativeTabsVehicle.setFixedIconItem(MCH_Config.CreativeTabIconVehicle.prmString);
        proxy.readClientModList();
        proxy.postInit(evt);
    }

    @EventHandler
    public void onLoadComplete(FMLLoadCompleteEvent evt) {
        MCH_WeaponInfoManager.setRoundItems();
        ContentRegistries.weapon().values().parallelStream().filter(mchWeaponInfo -> mchWeaponInfo.useHBM)
                .forEach(MCH_WeaponInfo::loadNTMFunctionality);
        if (ModCompatManager.isLoaded(ModCompatManager.MODID_TOP))
            AircraftInfoProvider.register();
    }

    @EventHandler
    public void onStartServer(FMLServerStartingEvent event) {
        proxy.registerServerTick();
    }

    public void registerEntity() {
        MCH_Entities.register(MCH_EntitySeat.class, "MCH.E.Seat", 100, this, 600, 10, true);
        MCH_Entities.register(MCH_EntityHeli.class, "MCH.E.Heli", 101, this, 600, 10, true);
        MCH_Entities.register(MCH_EntityGLTD.class, "MCH.E.GLTD", 102, this, 600, 10, true);
        MCH_Entities.register(MCH_EntityPlane.class, "MCH.E.Plane", 103, this, 600, 10, true);
        MCH_Entities.register(MCH_EntityShip.class, "MCH.E.Ship", 403, this, 600, 10, true);

        MCH_Entities.register(MCH_EntityChain.class, "MCH.E.Chain", 104, this, 600, 10, true);
        MCH_Entities.register(MCH_EntityHitBox.class, "MCH.E.PSeat", 105, this, 200, 10, true);
        MCH_Entities.register(MCH_EntityParachute.class, "MCH.E.Parachute", 106, this, 200, 10, true);
        MCH_Entities.register(MCH_EntityContainer.class, "MCH.E.Container", 107, this, 200, 10, true);
        MCH_Entities.register(MCH_EntityVehicle.class, "MCH.E.Vehicle", 108, this, 600, 10, true);
        MCH_Entities.register(MCH_EntityUavStation.class, "MCH.E.UavStation", 109, this, 400, 10, true);
        MCH_Entities.register(MCH_EntityHitBox.class, "MCH.E.HitBox", 110, this, 200, 10, true);
        MCH_Entities.register(MCH_EntityHide.class, "MCH.E.Hide", 111, this, 200, 10, true);
        MCH_Entities.register(MCH_EntityTank.class, "MCH.E.Tank", 112, this, 400, 10, true);
        MCH_Entities.register(MCH_EntityRocket.class, "MCH.E.Rocket", 200, this, 530, 5, true);
        MCH_Entities.register(MCH_EntityTvMissile.class, "MCH.E.TvMissle", 201, this, 530, 5, true);
        MCH_Entities.register(MCH_EntityBullet.class, "MCH.E.Bullet", 202, this, 530, 5, true);
        MCH_Entities.register(MCH_EntityA10.class, "MCH.E.A10", 203, this, 530, 5, true);
        MCH_Entities.register(MCH_EntityAAMissile.class, "MCH.E.AAM", 204, this, 530, 5, true);
        MCH_Entities.register(MCH_EntityASMissile.class, "MCH.E.ASM", 205, this, 530, 5, true);
        MCH_Entities.register(MCH_EntityTorpedo.class, "MCH.E.Torpedo", 206, this, 530, 5, true);
        MCH_Entities.register(MCH_EntityATMissile.class, "MCH.E.ATMissle", 207, this, 530, 5, true);
        MCH_Entities.register(MCH_EntityBomb.class, "MCH.E.Bomb", 208, this, 530, 5, true);
        MCH_Entities.register(MCH_EntityMarkerRocket.class, "MCH.E.MkRocket", 209, this, 530, 5, true);
        MCH_Entities.register(MCH_EntityDispensedItem.class, "MCH.E.DispItem", 210, this, 530, 5, true);
        MCH_Entities.register(MCH_EntityFlare.class, "MCH.E.Flare", 300, this, 330, 10, true);
        MCH_Entities.register(MCH_EntityThrowable.class, "MCH.E.Throwable", 400, this, 330, 10, true);
        MCH_Entities.register(MCH_EntityGunner.class, "MCH.E.Gunner", 500, this, 530, 5, true);
    }

    @EventHandler
    public void registerCommand(FMLServerStartedEvent e) {
        CommandHandler handler = (CommandHandler) FMLCommonHandler.instance().getSidedDelegate().getServer()
                .getCommandManager();
        handler.registerCommand(new MCH_Command());
    }

    private void registerItemSpawnGunner() {
        String name = "spawn_gunner_vs_monster";
        MCH_ItemSpawnGunner item = new MCH_ItemSpawnGunner();
        item.targetType = 0;
        item.primaryColor = 12632224;
        item.secondaryColor = 12582912;
        itemSpawnGunnerVsMonster = item;
        registerItem(item, name, creativeTabs);
        W_LanguageRegistry.addName(item, "Gunner (vs Monster)");
        W_LanguageRegistry.addNameForObject(item, "ja_jp", "対モンスター 射撃手");
        name = "spawn_gunner_vs_player";
        item = new MCH_ItemSpawnGunner();
        item.targetType = 1;
        item.primaryColor = 12632224;
        item.secondaryColor = 49152;
        itemSpawnGunnerVsPlayer = item;
        registerItem(item, name, creativeTabs);
        W_LanguageRegistry.addName(item, "Gunner (vs Player of other team)");
        W_LanguageRegistry.addNameForObject(item, "ja_jp", "対他チームプレイヤー 射撃手");
    }

    private void registerItemRangeFinder() {
        String name = "rangefinder";
        MCH_ItemRangeFinder item = new MCH_ItemRangeFinder(MCH_Config.ItemID_RangeFinder.prmInt);
        itemRangeFinder = item;
        registerItem(item, name, creativeTabs);
        W_LanguageRegistry.addName(item, "Laser Rangefinder");
        W_LanguageRegistry.addNameForObject(item, "ja_jp", "レーザー レンジ ファインダー");
    }

    private void registerItemWrench() {
        String name = "wrench";
        MCH_ItemWrench item = new MCH_ItemWrench(MCH_Config.ItemID_Wrench.prmInt, ToolMaterial.IRON);
        itemWrench = item;
        registerItem(item, name, creativeTabs);
        W_LanguageRegistry.addName(item, "Wrench");
        W_LanguageRegistry.addNameForObject(item, "ja_jp", "レンチ");
    }

    public void registerItemInvisible() {
        String name = "internal";
        MCH_InvisibleItem item = new MCH_InvisibleItem(MCH_Config.ItemID_InvisibleItem.prmInt);
        invisibleItem = item;
        registerItem(item, name, null);
    }

    public void registerItemUavStation() {
        String[] dispName = new String[] { "UAV Station", "Portable UAV Controller" };
        String[] localName = new String[] { "UAVステーション", "携帯UAV制御端末" };
        itemUavStation = new MCH_ItemUavStation[MCH_ItemUavStation.UAV_STATION_KIND_NUM];
        String name = "uav_station";

        for (int i = 0; i < itemUavStation.length; i++) {
            String nn = i > 0 ? "" + (i + 1) : "";
            MCH_ItemUavStation item = new MCH_ItemUavStation(MCH_Config.ItemID_UavStation[i].prmInt, 1 + i);
            itemUavStation[i] = item;
            registerItem(item, name + nn, creativeTabs);
            W_LanguageRegistry.addName(item, dispName[i]);
            W_LanguageRegistry.addNameForObject(item, "ja_jp", localName[i]);
        }
    }

    public void registerItemParachute() {
        String name = "parachute";
        MCH_ItemParachute item = new MCH_ItemParachute(MCH_Config.ItemID_Parachute.prmInt);
        itemParachute = item;
        registerItem(item, name, creativeTabs);
        W_LanguageRegistry.addName(item, "Parachute");
        W_LanguageRegistry.addNameForObject(item, "ja_jp", "パラシュート");
    }

    public void registerItemContainer() {
        String name = "container";
        MCH_ItemContainer item = new MCH_ItemContainer(MCH_Config.ItemID_Container.prmInt);
        itemContainer = item;
        registerItem(item, name, creativeTabs);
        W_LanguageRegistry.addName(item, "Container");
        W_LanguageRegistry.addNameForObject(item, "ja_jp", "コンテナ");
    }

    public void registerItemLightWeapon() {
        String name = "fim92";
        MCH_ItemLightWeaponBase item = new MCH_ItemLightWeaponBase(MCH_Config.ItemID_Stinger.prmInt, itemStingerBullet);
        itemStinger = item;
        registerItem(item, name, creativeTabs);
        W_LanguageRegistry.addName(item, "FIM-92 Stinger");
        name = "fgm148";
        item = new MCH_ItemLightWeaponBase(MCH_Config.ItemID_Stinger.prmInt, itemJavelinBullet);
        itemJavelin = item;
        registerItem(item, name, creativeTabs);
        W_LanguageRegistry.addName(item, "FGM-148 Javelin");
    }

    public void registerItemLightWeaponBullet() {
        String name = "fim92_bullet";
        MCH_ItemLightWeaponBullet item = new MCH_ItemLightWeaponBullet(MCH_Config.ItemID_StingerMissile.prmInt);
        itemStingerBullet = item;
        registerItem(item, name, creativeTabs);
        W_LanguageRegistry.addName(item, "FIM-92 Stinger missile");
        name = "fgm148_bullet";
        item = new MCH_ItemLightWeaponBullet(MCH_Config.ItemID_StingerMissile.prmInt);
        itemJavelinBullet = item;
        registerItem(item, name, creativeTabs);
        W_LanguageRegistry.addName(item, "FGM-148 Javelin missile");
    }

    public void registerItemChain() {
        String name = "chain";
        MCH_ItemChain item = new MCH_ItemChain(MCH_Config.ItemID_Chain.prmInt);
        itemChain = item;
        registerItem(item, name, creativeTabs);
        W_LanguageRegistry.addName(item, "Chain");
        W_LanguageRegistry.addNameForObject(item, "ja_jp", "鎖");
    }

    public void registerItemFuel() {
        String name = "fuel";
        MCH_ItemFuel item = new MCH_ItemFuel(MCH_Config.ItemID_Fuel.prmInt);
        itemFuel = item;
        registerItem(item, name, creativeTabs);
        W_LanguageRegistry.addName(item, "Fuel");
        W_LanguageRegistry.addNameForObject(item, "ja_jp", "燃料");
    }

    public void registerItemGLTD() {
        String name = "gltd";
        MCH_ItemGLTD item = new MCH_ItemGLTD(MCH_Config.ItemID_GLTD.prmInt);
        itemGLTD = item;
        registerItem(item, name, creativeTabs);
        W_LanguageRegistry.addName(item, "GLTD:Target Designator");
        W_LanguageRegistry.addNameForObject(item, "ja_jp", "GLTD:レーザー目標指示装置");
    }

    @SubscribeEvent
    public void onTextureStitch(TextureStitchEvent.Pre event) {
        proxy.registerParticleTextures(event);
    }

    @SubscribeEvent
    public void onTextureStitchPost(TextureStitchEvent.Post event) {
        proxy.registerShaders(event);
    }
}
