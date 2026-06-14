package com.norwood.mcheli.wingman.mission;
//WINGMAN — file introduced for the McHeli Wingman feature merge

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.norwood.mcheli.wingman.McHeliWingman;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

/**
 * 名前付きミッションプランのレジストリ。
 * サーバー起動時に configDir/wingman_routes.json から読み込み、変更時に書き戻す。
 * GUIからも読み書きできるよう Map<String, List<String>> をそのまま公開する。
 */
public class MissionPlan {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File saveFile;

    /** ルート名 → シリアライズ済みノードリスト */
    private static final Map<String, List<String>> routes = new LinkedHashMap<>();

    public static void init(File configDir) {
        saveFile = new File(configDir, "wingman_routes.json");
        load();
    }

    // ─── CRUD ────────────────────────────────────────────────────────────────

    public static void put(String name, List<MissionNode> nodes) {
        List<String> serialized = new ArrayList<>();
        for (MissionNode n : nodes) serialized.add(n.serialize());
        routes.put(name, serialized);
        save();
    }

    public static List<MissionNode> get(String name) {
        List<String> raw = routes.get(name);
        if (raw == null) return null;
        List<MissionNode> nodes = new ArrayList<>();
        for (String s : raw) nodes.add(MissionNode.parse(s));
        return nodes;
    }

    public static boolean remove(String name) {
        if (!routes.containsKey(name)) return false;
        routes.remove(name);
        save();
        return true;
    }

    public static Set<String> names() { return routes.keySet(); }

    public static boolean exists(String name) { return routes.containsKey(name); }

    // ─── Persistence ─────────────────────────────────────────────────────────

    private static void load() {
        if (saveFile == null || !saveFile.exists()) return;
        try (Reader r = new FileReader(saveFile)) {
            Type t = new TypeToken<Map<String, List<String>>>(){}.getType();
            Map<String, List<String>> loaded = GSON.fromJson(r, t);
            if (loaded != null) routes.putAll(loaded);
            McHeliWingman.logger.info("[MissionPlan] Loaded {} route(s) from {}", routes.size(), saveFile.getName());
        } catch (Exception e) {
            McHeliWingman.logger.warn("[MissionPlan] Failed to load routes: {}", e.getMessage());
        }
    }

    private static void save() {
        if (saveFile == null) return;
        try (Writer w = new FileWriter(saveFile)) {
            GSON.toJson(routes, w);
        } catch (Exception e) {
            McHeliWingman.logger.warn("[MissionPlan] Failed to save routes: {}", e.getMessage());
        }
    }
}
