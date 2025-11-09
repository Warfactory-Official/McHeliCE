package com.norwood.mcheli.multiplay;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import net.minecraft.command.server.CommandSummon;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.helper.MCH_Utils;

import lombok.Getter;

@Getter
public class MultiplayerHandler {

    public static EntityPlayer modListRequestPlayer = null;
    public static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
    public static byte[] imageData = null;
    public static String lastPlayerName = "";
    public static double lastDataPercent = 0.0;
    public static int playerInfoId = 0;

    public static void destoryAllAircraft(EntityPlayer player) {
        CommandSummon cmd = new CommandSummon();
        if (cmd.checkPermission(MCH_Utils.getServer(), player)) {
            for (Entity e : new ArrayList<>(player.world.loadedEntityList)) {
                if (e instanceof MCH_EntityAircraft) {
                    e.setDead();
                }
            }
        }
    }

    // Helpers
    public static void resetState() {
        imageData = null;
        lastPlayerName = "";
        lastDataPercent = 0.0;
    }

    public static void saveScreenshot(String playerName, byte[] data) throws IOException {
        String dt = dateFormat.format(new Date());
        File dir = new File("screenshots_op");
        dir.mkdir();

        File file = new File(dir, playerName + "_" + dt + ".png");
        try (FileOutputStream fos = new FileOutputStream(file);
                DataOutputStream dos = new DataOutputStream(fos)) {
            dos.write(data);
        }
        LogInfo("[mcheli]Screenshot saved: %s", file.getAbsolutePath());
    }

    public static void LogInfo(String format, Object... args) {
        MCH_Lib.Log(String.format(format, args));
    }

    public static void LogError(String format, Object... args) {
        MCH_Lib.Log(String.format(format, args));
    }

    public static int getPlayerInfoId(EntityPlayer player) {
        modListRequestPlayer = player;
        playerInfoId++;
        if (playerInfoId > 1000000) {
            playerInfoId = 1;
        }

        return playerInfoId;
    }
}
