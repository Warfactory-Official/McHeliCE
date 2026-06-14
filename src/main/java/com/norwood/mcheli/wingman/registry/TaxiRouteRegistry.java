package com.norwood.mcheli.wingman.registry;
//WINGMAN — file introduced for the McHeli Wingman feature merge

import com.norwood.mcheli.wingman.McHeliWingman;
import com.norwood.mcheli.wingman.mission.TaxiRoute;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

import java.util.*;

/** タキシールートをワールド単位で永続管理する。 */
public class TaxiRouteRegistry extends WorldSavedData {

    private static final String KEY = "wingman_taxi_routes";
    private final Map<String, TaxiRoute> routes = new LinkedHashMap<>();

    public TaxiRouteRegistry()            { super(KEY); }
    public TaxiRouteRegistry(String name) { super(name); }

    private static TaxiRouteRegistry get(World world) {
        MapStorage ms = world.getPerWorldStorage();
        TaxiRouteRegistry r = (TaxiRouteRegistry) ms.getOrLoadData(TaxiRouteRegistry.class, KEY);
        if (r == null) { r = new TaxiRouteRegistry(); ms.setData(KEY, r); }
        return r;
    }

    // ─── API ─────────────────────────────────────────────────────────────────

    public static void save(World world, TaxiRoute route) {
        TaxiRouteRegistry reg = get(world);
        reg.routes.put(route.routeId, route);
        reg.markDirty();
        McHeliWingman.logger.info("[TaxiRoute] SAVE routeId={} baseId={} parkingId={} runwayId={} runwayBId={} wps={} heading={}",
            route.routeId, route.baseId, route.parkingId, route.runwayId, route.runwayBId, route.waypointIds, route.parkingHeading);
    }

    public static void delete(World world, String routeId) {
        TaxiRouteRegistry reg = get(world);
        reg.routes.remove(routeId);
        reg.markDirty();
    }

    /** 指定基地のタキシールート一覧 */
    public static List<TaxiRoute> getForBase(World world, String baseId) {
        TaxiRouteRegistry reg = get(world);
        McHeliWingman.logger.info("[TaxiRoute] getForBase baseId={} totalRoutes={}", baseId, reg.routes.size());
        for (TaxiRoute r : reg.routes.values()) {
            McHeliWingman.logger.info("[TaxiRoute]   stored: routeId={} baseId={}", r.routeId, r.baseId);
        }
        List<TaxiRoute> result = new ArrayList<>();
        for (TaxiRoute r : reg.routes.values()) {
            if (baseId.equals(r.baseId)) result.add(r);
        }
        McHeliWingman.logger.info("[TaxiRoute] getForBase result={} routes", result.size());
        return result;
    }

    /** 駐機スポットIDからルートを検索 */
    public static TaxiRoute findByParking(World world, String parkingId) {
        for (TaxiRoute r : get(world).routes.values()) {
            if (parkingId.equals(r.parkingId)) return r;
        }
        return null;
    }

    /**
     * 滑走路端 / ヘリパッド ID からルートを検索。
     * VTOL 着陸後の TAXI_IN 用: ヘリパッドID (= route.runwayId) でルートを逆引きする。
     */
    public static TaxiRoute findByRunway(World world, String runwayId) {
        for (TaxiRoute r : get(world).routes.values()) {
            if (runwayId.equals(r.runwayId)) return r;
        }
        return null;
    }

    /** ルートIDから直接取得 */
    public static TaxiRoute findById(World world, String routeId) {
        return get(world).routes.get(routeId);
    }

    /** 全ルートのスナップショット */
    public static List<TaxiRoute> snapshot(World world) {
        return new ArrayList<>(get(world).routes.values());
    }

    // ─── NBT ─────────────────────────────────────────────────────────────────

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        routes.clear();
        NBTTagList list = tag.getTagList("routes", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound c = list.getCompoundTagAt(i);
            List<String> wps = new ArrayList<>();
            NBTTagList wpList = c.getTagList("waypoints", Constants.NBT.TAG_STRING);
            for (int j = 0; j < wpList.tagCount(); j++) wps.add(wpList.getStringTagAt(j));
            String runwayBId     = c.hasKey("runwayBId") ? c.getString("runwayBId") : "";
            int    parkingHeading = c.hasKey("parkingHeading") ? c.getInteger("parkingHeading") : -1;
            List<String> arrWps = new ArrayList<>();
            if (c.hasKey("arrivalWaypoints")) {
                NBTTagList arrList = c.getTagList("arrivalWaypoints", Constants.NBT.TAG_STRING);
                for (int j = 0; j < arrList.tagCount(); j++) arrWps.add(arrList.getStringTagAt(j));
            }
            String arrivalRunwayId = c.hasKey("arrivalRunwayId") ? c.getString("arrivalRunwayId") : "";
            TaxiRoute r = new TaxiRoute(
                c.getString("routeId"), c.getString("baseId"),
                c.getString("parkingId"), c.getString("runwayId"), runwayBId, wps,
                arrWps, parkingHeading, arrivalRunwayId);
            routes.put(r.routeId, r);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        NBTTagList list = new NBTTagList();
        for (TaxiRoute r : routes.values()) {
            NBTTagCompound c = new NBTTagCompound();
            c.setString("routeId",   r.routeId);
            c.setString("baseId",    r.baseId);
            c.setString("parkingId", r.parkingId);
            c.setString("runwayId",  r.runwayId);
            c.setString("runwayBId", r.runwayBId);
            c.setInteger("parkingHeading", r.parkingHeading);
            NBTTagList wpList = new NBTTagList();
            for (String wp : r.waypointIds) wpList.appendTag(new NBTTagString(wp));
            c.setTag("waypoints", wpList);
            NBTTagList arrList = new NBTTagList();
            for (String wp : r.arrivalWaypointIds) arrList.appendTag(new NBTTagString(wp));
            c.setTag("arrivalWaypoints", arrList);
            if (!r.arrivalRunwayId.isEmpty()) c.setString("arrivalRunwayId", r.arrivalRunwayId);
            list.appendTag(c);
        }
        tag.setTag("routes", list);
        return tag;
    }
}
