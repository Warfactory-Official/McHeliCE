package com.norwood.mcheli;

import com.norwood.mcheli.helicopter.MCH_EntityHeli;
import com.norwood.mcheli.plane.MCH_EntityPlane;
import com.norwood.mcheli.tank.MCH_EntityTank;
import com.norwood.mcheli.vehicle.MCH_EntityVehicle;
import com.norwood.mcheli.wrapper.W_Block;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class MCH_Config {

    public static final List<Block> dummyBreakableBlocks = new ArrayList<>();
    public static final List<Material> dummyBreakableMaterials = new ArrayList<>();
    public static String mcPath;
    public static String configFilePath;
    public static boolean DebugLog;
    public static String configVer;
    public static int hitMarkColorRGB;
    public static float hitMarkColorAlpha;
    public static List<Block> bulletBreakableBlocks;
    public static List<Block> carNoBreakableBlocks;
    public static List<Block> carBreakableBlocks;
    public static List<Material> carBreakableMaterials;
    public static List<Block> tankNoBreakableBlocks;
    public static List<Block> tankBreakableBlocks;
    public static List<Material> tankBreakableMaterials;
    public static MCH_ConfigPrm KeyUp;
    public static MCH_ConfigPrm waitForModels;
    public static MCH_ConfigPrm KeyDown;
    public static MCH_ConfigPrm KeyRight;
    public static MCH_ConfigPrm KeyLeft;
    public static MCH_ConfigPrm KeySwitchMode;
    public static MCH_ConfigPrm KeySwitchHovering;
    public static MCH_ConfigPrm KeyAttack;
    public static MCH_ConfigPrm KeyUseWeapon;
    public static MCH_ConfigPrm KeySwitchWeapon1;
    public static MCH_ConfigPrm KeySwitchWeapon2;
    public static MCH_ConfigPrm KeySwWeaponMode;
    public static MCH_ConfigPrm KeyZoom;
    public static MCH_ConfigPrm KeyCameraMode;
    public static MCH_ConfigPrm KeyUnmount;
    public static MCH_ConfigPrm KeyFlare;
    public static MCH_ConfigPrm KeyExtra;
    public static MCH_ConfigPrm KeyCameraDistUp;
    public static MCH_ConfigPrm KeyCameraDistDown;
    public static MCH_ConfigPrm KeyFreeLook;
    public static MCH_ConfigPrm KeyGUI;
    public static MCH_ConfigPrm KeyGearUpDown;
    public static MCH_ConfigPrm KeyPutToRack;
    public static MCH_ConfigPrm KeyDownFromRack;
    public static MCH_ConfigPrm KeyScoreboard;
    public static MCH_ConfigPrm KeyMultiplayManager;
    public static List<MCH_ConfigPrm> DamageVs;
    public static List<String> IgnoreBulletHitList;
    public static MCH_ConfigPrm IgnoreBulletHitItem;
    public static MCH_Config.DamageFactor[] DamageFactorList;
    public static MCH_Config.DamageFactor DamageVsEntity;
    public static MCH_Config.DamageFactor DamageVsLiving;
    public static MCH_Config.DamageFactor DamageVsPlayer;
    public static MCH_Config.DamageFactor DamageVsMCHeliAircraft;
    public static MCH_Config.DamageFactor DamageVsMCHeliTank;
    public static MCH_Config.DamageFactor DamageVsMCHeliVehicle;
    public static MCH_Config.DamageFactor DamageVsMCHeliOther;
    public static MCH_Config.DamageFactor DamageAircraftByExternal;
    public static MCH_Config.DamageFactor DamageTankByExternal;
    public static MCH_Config.DamageFactor DamageVehicleByExternal;
    public static MCH_Config.DamageFactor DamageOtherByExternal;
    public static List<MCH_ConfigPrm> CommandPermission;
    public static List<MCH_Config.CommandPermission> CommandPermissionList;
    public static MCH_ConfigPrm TestMode;
    public static MCH_ConfigPrm __TextureAlpha;
    public static MCH_ConfigPrm EnableCommand;
    public static MCH_ConfigPrm PlaceableOnSpongeOnly;
    public static MCH_ConfigPrm HideKeybind;
    public static MCH_ConfigPrm ItemDamage;
    public static MCH_ConfigPrm ItemFuel;
    public static MCH_ConfigPrm AutoRepairHP;
    public static MCH_ConfigPrm Collision_DestroyBlock;
    public static MCH_ConfigPrm Explosion_DestroyBlock;
    public static MCH_ConfigPrm Explosion_FlamingBlock;
    public static MCH_ConfigPrm BulletBreakableBlock;
    public static MCH_ConfigPrm Collision_Car_BreakableBlock;
    public static MCH_ConfigPrm Collision_Car_NoBreakableBlock;
    public static MCH_ConfigPrm Collision_Car_BreakableMaterial;
    public static MCH_ConfigPrm Collision_Tank_BreakableBlock;
    public static MCH_ConfigPrm Collision_Tank_NoBreakableBlock;
    public static MCH_ConfigPrm Collision_Tank_BreakableMaterial;
    public static MCH_ConfigPrm Collision_EntityDamage;
    public static MCH_ConfigPrm Collision_EntityTankDamage;
    public static MCH_ConfigPrm LWeaponAutoFire;
    public static MCH_ConfigPrm DismountAll;
    public static MCH_ConfigPrm MountMinecartHeli;
    public static MCH_ConfigPrm MountMinecartPlane;
    public static MCH_ConfigPrm MountMinecartVehicle;
    public static MCH_ConfigPrm MountMinecartTank;
    public static MCH_ConfigPrm AutoThrottleDownHeli;
    public static MCH_ConfigPrm AutoThrottleDownPlane;
    public static MCH_ConfigPrm AutoThrottleDownTank;
    public static MCH_ConfigPrm DisableItemRender;
    public static MCH_ConfigPrm RenderDistanceWeight;
    public static MCH_ConfigPrm MobRenderDistanceWeight;
    public static MCH_ConfigPrm CreativeTabIcon;
    public static MCH_ConfigPrm CreativeTabIconHeli;
    public static MCH_ConfigPrm CreativeTabIconPlane;
    public static MCH_ConfigPrm CreativeTabIconShip;
    public static MCH_ConfigPrm CreativeTabIconTank;
    public static MCH_ConfigPrm CreativeTabIconVehicle;
    public static MCH_ConfigPrm DisableShader;
    public static MCH_ConfigPrm AliveTimeOfCartridge;
    public static MCH_ConfigPrm InfinityAmmo;
    public static MCH_ConfigPrm InfinityFuel;
    public static MCH_ConfigPrm HitMarkColor;
    public static MCH_ConfigPrm SmoothShading;
    public static MCH_ConfigPrm EnableModEntityRender;
    public static MCH_ConfigPrm DisableRenderLivingSpecials;
    public static MCH_ConfigPrm PreventingBroken;
    public static MCH_ConfigPrm DropItemInCreativeMode;
    public static MCH_ConfigPrm BreakableOnlyPickaxe;
    public static MCH_ConfigPrm InvertMouse;
    public static MCH_ConfigPrm MouseSensitivity;
    public static MCH_ConfigPrm MouseControlStickModeHeli;
    public static MCH_ConfigPrm MouseControlStickModePlane;
    public static MCH_ConfigPrm MouseControlFlightSimMode;
    public static MCH_ConfigPrm SwitchWeaponWithMouseWheel;
    public static MCH_ConfigPrm AllPlaneSpeed;

    public static MCH_ConfigPrm AllShipSpeed;
    public static MCH_ConfigPrm AllHeliSpeed;
    public static MCH_ConfigPrm AllTankSpeed;
    public static MCH_ConfigPrm HurtResistantTime;
    public static MCH_ConfigPrm DisplayHUDThirdPerson;
    public static MCH_ConfigPrm DisableCameraDistChange;
    public static MCH_ConfigPrm EnableReplaceTextureManager;
    public static MCH_ConfigPrm DisplayEntityMarker;
    public static MCH_ConfigPrm EntityMarkerSize;
    public static MCH_ConfigPrm BlockMarkerSize;
    public static MCH_ConfigPrm DisplayMarkThroughWall;
    public static MCH_ConfigPrm ReplaceRenderViewEntity;
    public static MCH_ConfigPrm StingerLockRange;
    public static MCH_ConfigPrm DefaultExplosionParticle;
    public static MCH_ConfigPrm RangeFinderSpotDist;
    public static MCH_ConfigPrm RangeFinderSpotTime;
    public static MCH_ConfigPrm RangeFinderConsume;
    public static MCH_ConfigPrm EnablePutRackInFlying;
    public static MCH_ConfigPrm EnableDebugBoundingBox;
    public static MCH_ConfigPrm DespawnCount;
    public static MCH_ConfigPrm HitBoxDelayTick;
    public static MCH_ConfigPrm EnableRotationLimit;
    public static MCH_ConfigPrm PitchLimitMax;
    public static MCH_ConfigPrm PitchLimitMin;
    public static MCH_ConfigPrm RollLimit;
    public static MCH_ConfigPrm RangeOfGunner_VsMonster_Vertical;
    public static MCH_ConfigPrm RangeOfGunner_VsMonster_Horizontal;
    public static MCH_ConfigPrm RangeOfGunner_VsPlayer_Vertical;
    public static MCH_ConfigPrm RangeOfGunner_VsPlayer_Horizontal;
    public static MCH_ConfigPrm FixVehicleAtPlacedPoint;
    public static MCH_ConfigPrm KillPassengersWhenDestroyed;
    public static MCH_ConfigPrm ItemID_Fuel;
    public static MCH_ConfigPrm ItemID_GLTD;
    public static MCH_ConfigPrm ItemID_Chain;
    public static MCH_ConfigPrm ItemID_Parachute;
    public static MCH_ConfigPrm ItemID_Container;
    public static MCH_ConfigPrm ItemID_Stinger;
    public static MCH_ConfigPrm ItemID_StingerMissile;
    public static MCH_ConfigPrm[] ItemID_UavStation;
    public static MCH_ConfigPrm ItemID_InvisibleItem;
    public static MCH_ConfigPrm ItemID_DraftingTable;
    public static MCH_ConfigPrm ItemID_Wrench;
    public static MCH_ConfigPrm ItemID_RangeFinder;
    public static MCH_ConfigPrm BlockID_DraftingTableOFF;
    public static MCH_ConfigPrm BlockID_DraftingTableON;
    public static MCH_ConfigPrm[] KeyConfig;
    public static MCH_ConfigPrm[] General;
    public final String destroyBlockNames = "glass_pane, stained_glass_pane, tallgrass, double_plant, yellow_flower, red_flower, vine, wheat, reeds, waterlily";

    public MCH_Config(String minecraftPath, String cfgFile) {
        mcPath = minecraftPath;
        configFilePath = mcPath + cfgFile;
        DebugLog = false;
        waitForModels = new MCH_ConfigPrm("WaitForModels", true);
        configVer = "0.0.0";
        bulletBreakableBlocks = new ArrayList<>();
        carBreakableBlocks = new ArrayList<>();
        carNoBreakableBlocks = new ArrayList<>();
        carBreakableMaterials = new ArrayList<>();
        tankBreakableBlocks = new ArrayList<>();
        tankNoBreakableBlocks = new ArrayList<>();
        tankBreakableMaterials = new ArrayList<>();
        KeyUp = new MCH_ConfigPrm("KeyUp", 17);
        KeyDown = new MCH_ConfigPrm("KeyDown", 31);
        KeyRight = new MCH_ConfigPrm("KeyRight", 32);
        KeyLeft = new MCH_ConfigPrm("KeyLeft", 30);
        KeySwitchMode = new MCH_ConfigPrm("KeySwitchGunner", 35);
        KeySwitchHovering = new MCH_ConfigPrm("KeySwitchHovering", 57);
        KeyAttack = new MCH_ConfigPrm("KeyAttack", -100);
        KeyUseWeapon = new MCH_ConfigPrm("KeyUseWeapon", -99);
        KeySwitchWeapon1 = new MCH_ConfigPrm("KeySwitchWeapon1", -98);
        KeySwitchWeapon2 = new MCH_ConfigPrm("KeySwitchWeapon2", 34);
        KeySwWeaponMode = new MCH_ConfigPrm("KeySwitchWeaponMode", 45);
        KeyZoom = new MCH_ConfigPrm("KeyZoom", 44);
        KeyCameraMode = new MCH_ConfigPrm("KeyCameraMode", 46);
        KeyUnmount = new MCH_ConfigPrm("KeyUnmountMob", 21);
        KeyFlare = new MCH_ConfigPrm("KeyFlare", 47);
        KeyExtra = new MCH_ConfigPrm("KeyExtra", 33);
        KeyCameraDistUp = new MCH_ConfigPrm("KeyCameraDistanceUp", 201);
        KeyCameraDistDown = new MCH_ConfigPrm("KeyCameraDistanceDown", 209);
        KeyFreeLook = new MCH_ConfigPrm("KeyFreeLook", 29);
        KeyGUI = new MCH_ConfigPrm("KeyGUI", 19);
        KeyGearUpDown = new MCH_ConfigPrm("KeyGearUpDown", 48);
        KeyPutToRack = new MCH_ConfigPrm("KeyPutToRack", 36);
        KeyDownFromRack = new MCH_ConfigPrm("KeyDownFromRack", 22);
        KeyScoreboard = new MCH_ConfigPrm("KeyScoreboard", 38);
        KeyMultiplayManager = new MCH_ConfigPrm("KeyMultiplayManager", 50);
        KeyConfig = new MCH_ConfigPrm[] {
                KeyUp,
                KeyDown,
                KeyRight,
                KeyLeft,
                KeySwitchMode,
                KeySwitchHovering,
                KeySwitchWeapon1,
                KeySwitchWeapon2,
                KeySwWeaponMode,
                KeyZoom,
                KeyCameraMode,
                KeyUnmount,
                KeyFlare,
                KeyExtra,
                KeyCameraDistUp,
                KeyCameraDistDown,
                KeyFreeLook,
                KeyGUI,
                KeyGearUpDown,
                KeyPutToRack,
                KeyDownFromRack,
                KeyScoreboard,
                KeyMultiplayManager
        };
        DamageVs = new ArrayList<>();
        CommandPermission = new ArrayList<>();
        CommandPermissionList = new ArrayList<>();
        IgnoreBulletHitList = new ArrayList<>();
        IgnoreBulletHitItem = new MCH_ConfigPrm("IgnoreBulletHit", "");
        TestMode = new MCH_ConfigPrm("TestMode", false);
        __TextureAlpha = new MCH_ConfigPrm("__TextureAlphaDebug", 1.0);
        EnableCommand = new MCH_ConfigPrm("EnableCommand", true);
        PlaceableOnSpongeOnly = new MCH_ConfigPrm("PlaceableOnSpongeOnly", false);
        HideKeybind = new MCH_ConfigPrm("HideKeybind", false);
        ItemDamage = new MCH_ConfigPrm("ItemDamage", true);
        ItemFuel = new MCH_ConfigPrm("ItemFuel", true);
        AutoRepairHP = new MCH_ConfigPrm("AutoRepairHP", 0.5);
        Collision_DestroyBlock = new MCH_ConfigPrm("Collision_DestroyBlock", true);
        Explosion_DestroyBlock = new MCH_ConfigPrm("Explosion_DestroyBlock", true);
        Explosion_FlamingBlock = new MCH_ConfigPrm("Explosion_FlamingBlock", true);
        Collision_Car_BreakableBlock = new MCH_ConfigPrm("Collision_Car_BreakableBlock",
                "double_plant, glass_pane,stained_glass_pane");
        Collision_Car_NoBreakableBlock = new MCH_ConfigPrm("Collision_Car_NoBreakBlock", "torch");
        Collision_Car_BreakableMaterial = new MCH_ConfigPrm("Collision_Car_BreakableMaterial",
                "cactus, cake, gourd, leaves, vine, plants");
        Collision_Tank_BreakableBlock = new MCH_ConfigPrm("Collision_Tank_BreakableBlock", "nether_brick_fence");
        Collision_Tank_BreakableBlock.validVer = "1.0.0";
        Collision_Tank_NoBreakableBlock = new MCH_ConfigPrm("Collision_Tank_NoBreakBlock", "torch, glowstone");
        Collision_Tank_BreakableMaterial = new MCH_ConfigPrm(
                "Collision_Tank_BreakableMaterial",
                "cactus, cake, carpet, circuits, glass, gourd, leaves, vine, wood, plants");
        Collision_EntityDamage = new MCH_ConfigPrm("Collision_EntityDamage", true);
        Collision_EntityTankDamage = new MCH_ConfigPrm("Collision_EntityTankDamage", false);
        LWeaponAutoFire = new MCH_ConfigPrm("LWeaponAutoFire", false);
        DismountAll = new MCH_ConfigPrm("DismountAll", false);
        MountMinecartHeli = new MCH_ConfigPrm("MountMinecartHeli", true);
        MountMinecartPlane = new MCH_ConfigPrm("MountMinecartPlane", true);
        MountMinecartVehicle = new MCH_ConfigPrm("MountMinecartVehicle", false);
        MountMinecartTank = new MCH_ConfigPrm("MountMinecartTank", true);
        AutoThrottleDownHeli = new MCH_ConfigPrm("AutoThrottleDownHeli", true);
        AutoThrottleDownPlane = new MCH_ConfigPrm("AutoThrottleDownPlane", false);
        AutoThrottleDownTank = new MCH_ConfigPrm("AutoThrottleDownTank", false);
        DisableItemRender = new MCH_ConfigPrm("DisableItemRender", 1);
        DisableItemRender.desc = ";DisableItemRender = 0 ~ 3 (1 = Recommended)";
        RenderDistanceWeight = new MCH_ConfigPrm("RenderDistanceWeight", 10.0);
        MobRenderDistanceWeight = new MCH_ConfigPrm("MobRenderDistanceWeight", 1.0);
        CreativeTabIcon = new MCH_ConfigPrm("CreativeTabIconItem", "fuel");
        CreativeTabIconHeli = new MCH_ConfigPrm("CreativeTabIconHeli", "ah-64");
        CreativeTabIconPlane = new MCH_ConfigPrm("CreativeTabIconPlane", "f22a");
        CreativeTabIconShip = new MCH_ConfigPrm("CreativeTabIconShip", "project1204");
        CreativeTabIconTank = new MCH_ConfigPrm("CreativeTabIconTank", "merkava_mk4");
        CreativeTabIconVehicle = new MCH_ConfigPrm("CreativeTabIconVehicle", "mk15");
        DisableShader = new MCH_ConfigPrm("DisableShader", false);
        AliveTimeOfCartridge = new MCH_ConfigPrm("AliveTimeOfCartridge", 200);
        InfinityAmmo = new MCH_ConfigPrm("InfinityAmmo", false);
        InfinityFuel = new MCH_ConfigPrm("InfinityFuel", false);
        HitMarkColor = new MCH_ConfigPrm("HitMarkColor", "255, 255, 0, 0");
        HitMarkColor.desc = ";HitMarkColor = Alpha, Red, Green, Blue";
        SmoothShading = new MCH_ConfigPrm("SmoothShading", true);
        BulletBreakableBlock = new MCH_ConfigPrm(
                "BulletBreakableBlocks",
                "glass_pane, stained_glass_pane, tallgrass, double_plant, yellow_flower, red_flower, vine, wheat, reeds, waterlily");
        BulletBreakableBlock.validVer = "0.10.4";
        EnableModEntityRender = new MCH_ConfigPrm("EnableModEntityRender", true);
        DisableRenderLivingSpecials = new MCH_ConfigPrm("DisableRenderLivingSpecials", true);
        PreventingBroken = new MCH_ConfigPrm("PreventingBroken", false);
        DropItemInCreativeMode = new MCH_ConfigPrm("DropItemInCreativeMode", false);
        BreakableOnlyPickaxe = new MCH_ConfigPrm("BreakableOnlyPickaxe", false);
        InvertMouse = new MCH_ConfigPrm("InvertMouse", false);
        MouseSensitivity = new MCH_ConfigPrm("MouseSensitivity", 10.0);
        MouseControlStickModeHeli = new MCH_ConfigPrm("MouseControlStickModeHeli", false);
        MouseControlStickModePlane = new MCH_ConfigPrm("MouseControlStickModePlane", false);
        MouseControlFlightSimMode = new MCH_ConfigPrm("MouseControlFlightSimMode", false);
        MouseControlFlightSimMode.desc = ";MouseControlFlightSimMode = true ( Yaw:key, Roll=mouse )";
        SwitchWeaponWithMouseWheel = new MCH_ConfigPrm("SwitchWeaponWithMouseWheel", true);
        AllHeliSpeed = new MCH_ConfigPrm("AllHeliSpeed", 1.0);
        AllPlaneSpeed = new MCH_ConfigPrm("AllPlaneSpeed", 1.0);
        AllShipSpeed = new MCH_ConfigPrm("AllShipSpeed", 1.0);
        AllTankSpeed = new MCH_ConfigPrm("AllTankSpeed", 1.0);
        HurtResistantTime = new MCH_ConfigPrm("HurtResistantTime", 0.0);
        DisplayHUDThirdPerson = new MCH_ConfigPrm("DisplayHUDThirdPerson", false);
        DisableCameraDistChange = new MCH_ConfigPrm("DisableThirdPersonCameraDistChange", false);
        EnableReplaceTextureManager = new MCH_ConfigPrm("EnableReplaceTextureManager", true);
        DisplayEntityMarker = new MCH_ConfigPrm("DisplayEntityMarker", true);
        DisplayMarkThroughWall = new MCH_ConfigPrm("DisplayMarkThroughWall", true);
        EntityMarkerSize = new MCH_ConfigPrm("EntityMarkerSize", 10.0);
        BlockMarkerSize = new MCH_ConfigPrm("BlockMarkerSize", 10.0);
        ReplaceRenderViewEntity = new MCH_ConfigPrm("ReplaceRenderViewEntity", true);
        StingerLockRange = new MCH_ConfigPrm("StingerLockRange", 320.0);
        StingerLockRange.validVer = "1.0.0";
        DefaultExplosionParticle = new MCH_ConfigPrm("DefaultExplosionParticle", false);
        RangeFinderSpotDist = new MCH_ConfigPrm("RangeFinderSpotDist", 400);
        RangeFinderSpotTime = new MCH_ConfigPrm("RangeFinderSpotTime", 15);
        RangeFinderConsume = new MCH_ConfigPrm("RangeFinderConsume", true);
        EnablePutRackInFlying = new MCH_ConfigPrm("EnablePutRackInFlying", true);
        EnableDebugBoundingBox = new MCH_ConfigPrm("EnableDebugBoundingBox", true);
        DespawnCount = new MCH_ConfigPrm("DespawnCount", 25);
        HitBoxDelayTick = new MCH_ConfigPrm("HitBoxDelayTick", 0);
        EnableRotationLimit = new MCH_ConfigPrm("EnableRotationLimit", false);
        PitchLimitMax = new MCH_ConfigPrm("PitchLimitMax", 10);
        PitchLimitMin = new MCH_ConfigPrm("PitchLimitMin", -10);
        RollLimit = new MCH_ConfigPrm("RollLimit", 35);
        RangeOfGunner_VsMonster_Horizontal = new MCH_ConfigPrm("RangeOfGunner_VsMonster_Horizontal", 80);
        RangeOfGunner_VsMonster_Vertical = new MCH_ConfigPrm("RangeOfGunner_VsMonster_Vertical", 160);
        RangeOfGunner_VsPlayer_Horizontal = new MCH_ConfigPrm("RangeOfGunner_VsPlayer_Horizontal", 200);
        RangeOfGunner_VsPlayer_Vertical = new MCH_ConfigPrm("RangeOfGunner_VsPlayer_Vertical", 300);
        FixVehicleAtPlacedPoint = new MCH_ConfigPrm("FixVehicleAtPlacedPoint", true);
        KillPassengersWhenDestroyed = new MCH_ConfigPrm("KillPassengersWhenDestroyed", false);
        hitMarkColorAlpha = 1.0F;
        hitMarkColorRGB = 16711680;
        ItemID_GLTD = new MCH_ConfigPrm("ItemID_GLTD", 28799);
        ItemID_Chain = new MCH_ConfigPrm("ItemID_Chain", 28798);
        ItemID_Parachute = new MCH_ConfigPrm("ItemID_Parachute", 28797);
        ItemID_Container = new MCH_ConfigPrm("ItemID_Container", 28796);
        ItemID_UavStation = new MCH_ConfigPrm[] { new MCH_ConfigPrm("ItemID_UavStation", 28795),
                new MCH_ConfigPrm("ItemID_UavStation2", 28790) };
        ItemID_InvisibleItem = new MCH_ConfigPrm("ItemID_Internal", 28794);
        ItemID_Fuel = new MCH_ConfigPrm("ItemID_Fuel", 28793);
        ItemID_DraftingTable = new MCH_ConfigPrm("ItemID_DraftingTable", 28792);
        ItemID_Wrench = new MCH_ConfigPrm("ItemID_Wrench", 28791);
        ItemID_RangeFinder = new MCH_ConfigPrm("ItemID_RangeFinder", 28789);
        ItemID_Stinger = new MCH_ConfigPrm("ItemID_Stinger", 28900);
        ItemID_StingerMissile = new MCH_ConfigPrm("ItemID_StingerMissile", 28901);
        BlockID_DraftingTableOFF = new MCH_ConfigPrm("BlockID_DraftingTable", 3450);
        BlockID_DraftingTableON = new MCH_ConfigPrm("BlockID_DraftingTableON", 3451);
        General = new MCH_ConfigPrm[] {
                TestMode,
                __TextureAlpha,
                EnableCommand,
                null,
                PlaceableOnSpongeOnly,
                ItemDamage,
                ItemFuel,
                AutoRepairHP,
                Explosion_DestroyBlock,
                Explosion_FlamingBlock,
                BulletBreakableBlock,
                Collision_DestroyBlock,
                Collision_Car_BreakableBlock,
                Collision_Car_BreakableMaterial,
                Collision_Tank_BreakableBlock,
                Collision_Tank_BreakableMaterial,
                Collision_EntityDamage,
                Collision_EntityTankDamage,
                InfinityAmmo,
                InfinityFuel,
                DismountAll,
                MountMinecartHeli,
                MountMinecartPlane,
                MountMinecartVehicle,
                MountMinecartTank,
                PreventingBroken,
                DropItemInCreativeMode,
                BreakableOnlyPickaxe,
                AllHeliSpeed,
                AllPlaneSpeed,
                AllShipSpeed,
                AllTankSpeed,
                HurtResistantTime,
                StingerLockRange,
                RangeFinderSpotDist,
                RangeFinderSpotTime,
                RangeFinderConsume,
                EnablePutRackInFlying,
                EnableDebugBoundingBox,
                DespawnCount,
                HitBoxDelayTick,
                EnableRotationLimit,
                PitchLimitMax,
                PitchLimitMin,
                RollLimit,
                RangeOfGunner_VsMonster_Horizontal,
                RangeOfGunner_VsMonster_Vertical,
                RangeOfGunner_VsPlayer_Horizontal,
                RangeOfGunner_VsPlayer_Vertical,
                FixVehicleAtPlacedPoint,
                KillPassengersWhenDestroyed,
                null,
                InvertMouse,
                MouseSensitivity,
                MouseControlStickModeHeli,
                MouseControlStickModePlane,
                MouseControlFlightSimMode,
                AutoThrottleDownHeli,
                AutoThrottleDownPlane,
                AutoThrottleDownTank,
                SwitchWeaponWithMouseWheel,
                LWeaponAutoFire,
                DisableItemRender,
                HideKeybind,
                RenderDistanceWeight,
                MobRenderDistanceWeight,
                CreativeTabIcon,
                CreativeTabIconHeli,
                CreativeTabIconPlane,
                CreativeTabIconShip,
                CreativeTabIconTank,
                CreativeTabIconVehicle,
                DisableShader,
                DefaultExplosionParticle,
                AliveTimeOfCartridge,
                HitMarkColor,
                SmoothShading,
                EnableModEntityRender,
                DisableRenderLivingSpecials,
                DisplayHUDThirdPerson,
                DisableCameraDistChange,
                EnableReplaceTextureManager,
                DisplayEntityMarker,
                EntityMarkerSize,
                BlockMarkerSize,
                ReplaceRenderViewEntity,
                null
        };
        DamageVsEntity = new DamageFactor(this, "DamageVsEntity");
        DamageVsLiving = new DamageFactor(this, "DamageVsLiving");
        DamageVsPlayer = new DamageFactor(this, "DamageVsPlayer");
        DamageVsMCHeliAircraft = new DamageFactor(this, "DamageVsMCHeliAircraft");
        DamageVsMCHeliTank = new DamageFactor(this, "DamageVsMCHeliTank");
        DamageVsMCHeliVehicle = new DamageFactor(this, "DamageVsMCHeliVehicle");
        DamageVsMCHeliOther = new DamageFactor(this, "DamageVsMCHeliOther");
        DamageAircraftByExternal = new DamageFactor(this, "DamageMCHeliAircraftByExternal");
        DamageTankByExternal = new DamageFactor(this, "DamageMCHeliTankByExternal");
        DamageVehicleByExternal = new DamageFactor(this, "DamageMCHeliVehicleByExternal");
        DamageOtherByExternal = new DamageFactor(this, "DamageMCHeliOtherByExternal");
        DamageFactorList = new MCH_Config.DamageFactor[] {
                DamageVsEntity,
                DamageVsLiving,
                DamageVsPlayer,
                DamageVsMCHeliAircraft,
                DamageVsMCHeliTank,
                DamageVsMCHeliVehicle,
                DamageVsMCHeliOther,
                DamageAircraftByExternal,
                DamageTankByExternal,
                DamageVehicleByExternal,
                DamageOtherByExternal
        };
    }

    public static float applyDamageByExternal(Entity target, DamageSource ds, float damage) {
        List<MCH_Config.DamageEntity> list;
        if (target instanceof MCH_EntityHeli || target instanceof MCH_EntityPlane) {
            list = DamageAircraftByExternal.list;
        } else if (target instanceof MCH_EntityTank) {
            list = DamageTankByExternal.list;
        } else if (target instanceof MCH_EntityVehicle) {
            list = DamageVehicleByExternal.list;
        } else {
            list = DamageOtherByExternal.list;
        }

        Entity attacker = ds.getTrueSource();
        Entity attackerSource = ds.getImmediateSource();

        for (MCH_Config.DamageEntity de : list) {
            if (de.name.isEmpty() || attacker != null && attacker.getClass().toString().indexOf(de.name) > 0 ||
                    attackerSource != null && attackerSource.getClass().toString().indexOf(de.name) > 0) {
                damage = (float) (damage * de.factor);
            }
        }

        return damage;
    }

    public static float applyDamageVsEntity(Entity target, DamageSource ds, float damage) {
        if (target != null) {
            String targetName = target.getClass().toString();
            List<DamageEntity> list;
            if (target instanceof MCH_EntityHeli || target instanceof MCH_EntityPlane) {
                list = DamageVsMCHeliAircraft.list;
            } else if (target instanceof MCH_EntityTank) {
                list = DamageVsMCHeliTank.list;
            } else if (target instanceof MCH_EntityVehicle) {
                list = DamageVsMCHeliVehicle.list;
            } else if (targetName.indexOf("com.norwood.mcheli.") > 0) {
                list = DamageVsMCHeliOther.list;
            } else if (target instanceof EntityPlayer) {
                list = DamageVsPlayer.list;
            } else if (target instanceof EntityLivingBase) {
                list = DamageVsLiving.list;
            } else {
                list = DamageVsEntity.list;
            }

            for (DamageEntity de : list) {
                if (de.name.isEmpty() || targetName.indexOf(de.name) > 0) {
                    damage = (float) (damage * de.factor);
                }
            }

        }
        return damage;
    }

    public static List<Block> getBreakableBlockListFromType(int n) {
        if (n == 2) {
            return tankBreakableBlocks;
        } else {
            return n == 1 ? carBreakableBlocks : dummyBreakableBlocks;
        }
    }

    public static List<Block> getNoBreakableBlockListFromType(int n) {
        if (n == 2) {
            return tankNoBreakableBlocks;
        } else {
            return n == 1 ? carNoBreakableBlocks : dummyBreakableBlocks;
        }
    }

    public static List<Material> getBreakableMaterialListFromType(int n) {
        if (n == 2) {
            return tankBreakableMaterials;
        } else {
            return n == 1 ? carBreakableMaterials : dummyBreakableMaterials;
        }
    }

    public void setBlockListFromString(List<Block> list, String str) {
        list.clear();
        String[] s = str.split("\\s*,\\s*");

        for (String blockName : s) {
            Block b = W_Block.getBlockFromName(blockName);
            if (b != null) {
                list.add(b);
            }
        }
    }

    public void setMaterialListFromString(List<Material> list, String str) {
        list.clear();
        String[] s = str.split("\\s*,\\s*");

        for (String name : s) {
            Material m = MCH_Lib.getMaterialFromName(name);
            if (m != null) {
                list.add(m);
            }
        }
    }

    public void correctionParameter() {
        String[] s = HitMarkColor.prmString.split("\\s*,\\s*");
        if (s.length == 4) {
            hitMarkColorAlpha = this.toInt255(s[0]) / 255.0F;
            hitMarkColorRGB = this.toInt255(s[1]) << 16 | this.toInt255(s[2]) << 8 | this.toInt255(s[3]);
        }

        AllHeliSpeed.prmDouble = MCH_Lib.RNG(AllHeliSpeed.prmDouble, 0.0, 1000.0);
        AllPlaneSpeed.prmDouble = MCH_Lib.RNG(AllPlaneSpeed.prmDouble, 0.0, 1000.0);
        AllShipSpeed.prmDouble = MCH_Lib.RNG(AllShipSpeed.prmDouble, 0.0, 1000.0);
        AllTankSpeed.prmDouble = MCH_Lib.RNG(AllTankSpeed.prmDouble, 0.0, 1000.0);
        this.setBlockListFromString(bulletBreakableBlocks, BulletBreakableBlock.prmString);
        this.setBlockListFromString(carBreakableBlocks, Collision_Car_BreakableBlock.prmString);
        this.setBlockListFromString(carNoBreakableBlocks, Collision_Car_NoBreakableBlock.prmString);
        this.setMaterialListFromString(carBreakableMaterials, Collision_Car_BreakableMaterial.prmString);
        this.setBlockListFromString(tankBreakableBlocks, Collision_Tank_BreakableBlock.prmString);
        this.setBlockListFromString(tankNoBreakableBlocks, Collision_Tank_NoBreakableBlock.prmString);
        this.setMaterialListFromString(tankBreakableMaterials, Collision_Tank_BreakableMaterial.prmString);
        if (EntityMarkerSize.prmDouble < 0.0) {
            EntityMarkerSize.prmDouble = 0.0;
        }

        if (BlockMarkerSize.prmDouble < 0.0) {
            BlockMarkerSize.prmDouble = 0.0;
        }

        if (HurtResistantTime.prmDouble < 0.0) {
            HurtResistantTime.prmDouble = 0.0;
        }

        if (HurtResistantTime.prmDouble > 10000.0) {
            HurtResistantTime.prmDouble = 10000.0;
        }

        if (MobRenderDistanceWeight.prmDouble < 0.1) {
            MobRenderDistanceWeight.prmDouble = 0.1;
        } else if (MobRenderDistanceWeight.prmDouble > 10.0) {
            MobRenderDistanceWeight.prmDouble = 10.0;
        }

        for (MCH_ConfigPrm p : CommandPermission) {
            MCH_Config.CommandPermission cpm = new CommandPermission(this, p.prmString);
            if (!cpm.name.isEmpty()) {
                CommandPermissionList.add(cpm);
            }
        }

        if (IgnoreBulletHitList.isEmpty()) {
            IgnoreBulletHitList.add("flansmod.common.guns.EntityBullet");
            IgnoreBulletHitList.add("flansmod.common.guns.EntityGrenade");
        }

        boolean isNoDamageVsSetting = DamageVs.isEmpty();

        for (MCH_ConfigPrm px : DamageVs) {
            for (MCH_Config.DamageFactor df : DamageFactorList) {
                if (px.name.equals(df.itemName)) {
                    df.list.add(this.newDamageEntity(px.prmString));
                }
            }
        }

        for (MCH_Config.DamageFactor dfx : DamageFactorList) {
            if (dfx.list.isEmpty()) {
                DamageVs.add(new MCH_ConfigPrm(dfx.itemName, "1.0"));
            } else {
                boolean foundCommon = false;

                for (MCH_Config.DamageEntity n : dfx.list) {
                    if (n.name.isEmpty()) {
                        foundCommon = true;
                        break;
                    }
                }

                if (!foundCommon) {
                    DamageVs.add(new MCH_ConfigPrm(dfx.itemName, "1.0"));
                }
            }
        }

        if (DespawnCount.prmInt <= 0) {
            DespawnCount.prmInt = 1;
        }

        if (HitBoxDelayTick.prmInt < 0) {
            HitBoxDelayTick.prmInt = 0;
        }

        if (HitBoxDelayTick.prmInt > 50) {
            HitBoxDelayTick.prmInt = 50;
        }

        PitchLimitMax.prmInt = PitchLimitMax.prmInt < 0 ? 0 : (Math.min(PitchLimitMax.prmInt, 80));
        PitchLimitMin.prmInt = PitchLimitMin.prmInt > 0 ? 0 : (Math.max(PitchLimitMin.prmInt, -80));
        RollLimit.prmInt = RollLimit.prmInt < 0 ? 0 : (Math.min(RollLimit.prmInt, 80));
        if (isNoDamageVsSetting) {
            DamageVs.add(new MCH_ConfigPrm("DamageVsEntity", "3.0, flansmod"));
            DamageVs.add(new MCH_ConfigPrm("DamageMCHeliAircraftByExternal", "0.5, flansmod"));
            DamageVs.add(new MCH_ConfigPrm("DamageMCHeliVehicleByExternal", "0.5, flansmod"));
        }
    }

    public MCH_Config.DamageEntity newDamageEntity(String s) {
        String[] splt = s.split("\\s*,\\s*");
        if (splt.length == 1) {
            return new DamageEntity(this, Double.parseDouble(splt[0]), "");
        } else {
            return splt.length == 2 ? new DamageEntity(this, Double.parseDouble(splt[0]), splt[1]) :
                    new DamageEntity(this, 1.0, "");
        }
    }

    public int toInt255(String s) {
        int a = Integer.parseInt(s);
        return a > 255 ? 255 : (Math.max(a, 0));
    }

    public void load() {
        MCH_InputFile file = new MCH_InputFile();
        if (file.open(configFilePath)) {
            for (String str = file.readLine(); str != null; str = file.readLine()) {
                if (str.trim().equalsIgnoreCase("McHeliOutputDebugLog")) {
                    DebugLog = true;
                } else {
                    this.readConfigData(str);
                }
            }

            file.close();
            MCH_Lib.Log("loaded " + file.file.getAbsolutePath());
        } else {
            MCH_Lib.Log(new File(configFilePath).getAbsolutePath() + " not found.");
        }

        this.correctionParameter();
    }

    private void readConfigData(String str) {
        String[] s = str.split("=");
        if (s.length == 2) {
            s[0] = s[0].trim();
            s[1] = s[1].trim();
            if (s[0].equalsIgnoreCase("MOD_Version")) {
                configVer = s[1];
            } else {
                if (s[0].equalsIgnoreCase("CommandPermission")) {
                    CommandPermission.add(new MCH_ConfigPrm("CommandPermission", s[1]));
                }

                for (MCH_Config.DamageFactor item : DamageFactorList) {
                    if (item.itemName.equalsIgnoreCase(s[0])) {
                        DamageVs.add(new MCH_ConfigPrm(item.itemName, s[1]));
                    }
                }

                if (IgnoreBulletHitItem.compare(s[0])) {
                    IgnoreBulletHitList.add(s[1]);
                }

                for (MCH_ConfigPrm p : KeyConfig) {
                    if (p != null && p.compare(s[0]) && p.isValidVer(configVer)) {
                        p.setPrm(s[1]);
                        return;
                    }
                }

                for (MCH_ConfigPrm px : General) {
                    if (px != null && px.compare(s[0]) && px.isValidVer(configVer)) {
                        px.setPrm(s[1]);
                        return;
                    }
                }
            }
        }
    }

    public void write() {
        MCH_OutputFile file = new MCH_OutputFile();
        if (file.open(configFilePath)) {
            this.writeConfigData(file.pw);
            file.close();
            MCH_Lib.Log("update " + file.file.getAbsolutePath());
        } else {
            MCH_Lib.Log(new File(configFilePath).getAbsolutePath() + " cannot open.");
        }
    }

    private void writeConfigData(PrintWriter pw) {
        pw.println("[General]");
        pw.println("MOD_Name = mcheli");
        pw.println("MOD_Version = " + MCH_MOD.VER);
        pw.println("MOD_MC_Version = 1.12.2");
        pw.println();
        if (DebugLog) {
            pw.println("McHeliOutputDebugLog");
            pw.println();
        }

        for (MCH_ConfigPrm p : General) {
            if (p != null) {
                if (!p.desc.isEmpty()) {
                    pw.println(p.desc);
                }

                pw.println(p.name + " = " + p);
            } else {
                pw.println("");
            }
        }

        pw.println();

        for (MCH_ConfigPrm px : DamageVs) {
            pw.println(px.name + " = " + px);
        }

        pw.println();

        for (String s : IgnoreBulletHitList) {
            pw.println(IgnoreBulletHitItem.name + " = " + s);
        }

        pw.println();
        pw.println(
                ";CommandPermission = commandName(eg, modlist, status, fill...):playerName1, playerName2, playerName3...");
        if (CommandPermission.isEmpty()) {
            pw.println(";CommandPermission = modlist :example1, example2");
            pw.println(";CommandPermission = status :  example2");
        }

        for (MCH_ConfigPrm px : CommandPermission) {
            pw.println(px.name + " = " + px);
        }

        pw.println();
        pw.println();
        pw.println("[Key config]");
        pw.println("https://minecraft.gamepedia.com/Key_codes");
        pw.println();

        for (MCH_ConfigPrm px : KeyConfig) {
            pw.println(px.name + " = " + px);
        }
    }

    public static class CommandPermission {

        public final String name;
        public final String[] players;

        public CommandPermission(MCH_Config arg1, String param) {
            String[] s = param.split(":");
            if (s.length == 2) {
                this.name = s[0].toLowerCase().trim();
                this.players = s[1].trim().split("\\s*,\\s*");
            } else {
                this.name = "";
                this.players = new String[0];
            }
        }
    }

    public static class DamageEntity {

        public final double factor;
        public final String name;

        public DamageEntity(MCH_Config paramMCH_Config, double factor, String name) {
            this.factor = factor;
            this.name = name;
        }
    }

    public static class DamageFactor {

        public final String itemName;
        public final List<MCH_Config.DamageEntity> list;

        public DamageFactor(MCH_Config paramMCH_Config, String itemName) {
            this.itemName = itemName;
            this.list = new ArrayList<>();
        }
    }
}
