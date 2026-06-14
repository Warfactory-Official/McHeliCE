package com.norwood.mcheli.wingman.registry;

import com.norwood.mcheli.wingman.McHeliWingman;
import com.norwood.mcheli.wingman.block.MarkerType;
import com.norwood.mcheli.wingman.block.WingmanMarkerTileEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

import java.util.*;

/**
 * ワールドごとに WingmanMarkerBlock の位置・種別・ID・ベースIDを永続管理する。
 * WorldSavedData として保存されるためサーバー再起動後も維持される。
 *
 * 親子モデル:
 *   BASE マーカー (id="alpha") ← 親
 *   ├─ RUNWAY_A (baseId="alpha")
 *   ├─ RUNWAY_B (baseId="alpha")
 *   └─ PARKING  (baseId="alpha") × n
 *
 * ルートノードの解決:
 *   takeoff:alpha → resolveRunway(RUNWAY_A/B, "alpha")
 *     1. BASE マーカー id="alpha" を検索
 *     2. baseId="alpha" の RUNWAY_A/B を返す
 *     3. 見つからなければ直接 findById(RUNWAY_A, "alpha") にフォールバック
 */
public class MarkerRegistry extends WorldSavedData {

    private static final String KEY = "wingman_markers";

    /** マーカー情報 */
    public static class MarkerInfo {
        public final BlockPos   pos;
        public final MarkerType type;
        public final String     id;
        public final String     baseId;        // 所属ベースのID（BASE種別は空文字）
        public final int        parkingHeading; // PARKING のみ: -1=任意, 0=N, 1=E, 2=S, 3=W

        public MarkerInfo(BlockPos pos, MarkerType type, String id, String baseId) {
            this(pos, type, id, baseId, -1);
        }

        public MarkerInfo(BlockPos pos, MarkerType type, String id, String baseId, int parkingHeading) {
            this.pos            = pos;
            this.type           = type;
            this.id             = id;
            this.baseId         = baseId != null ? baseId : "";
            this.parkingHeading = parkingHeading;
        }
    }

    private final List<MarkerInfo> markers = new ArrayList<>();

    public MarkerRegistry() { super(KEY); }
    public MarkerRegistry(String name) { super(name); }

    // ─── Static access ───────────────────────────────────────────────────────

    private static MarkerRegistry get(World world) {
        MapStorage storage = world.getPerWorldStorage();
        MarkerRegistry inst = (MarkerRegistry) storage.getOrLoadData(MarkerRegistry.class, KEY);
        if (inst == null) {
            inst = new MarkerRegistry();
            storage.setData(KEY, inst);
        }
        return inst;
    }

    public static void register(World world, BlockPos pos, WingmanMarkerTileEntity te) {
        MarkerRegistry reg = get(world);
        reg.markers.removeIf(m -> m.pos.equals(pos));
        reg.markers.add(new MarkerInfo(pos, te.getMarkerType(), te.getMarkerId(), te.getBaseId(), te.getParkingHeading()));
        reg.markDirty();
        McHeliWingman.logger.debug("[MarkerRegistry] registered {} {} id={} base={}",
            te.getMarkerType(), pos, te.getMarkerId(), te.getBaseId());
    }

    public static void unregister(World world, BlockPos pos) {
        MarkerRegistry reg = get(world);
        reg.markers.removeIf(m -> m.pos.equals(pos));
        reg.markDirty();
    }

    public static void setId(World world, BlockPos pos, String id) {
        MarkerRegistry reg = get(world);
        for (int i = 0; i < reg.markers.size(); i++) {
            MarkerInfo m = reg.markers.get(i);
            if (m.pos.equals(pos)) {
                reg.markers.set(i, new MarkerInfo(pos, m.type, id, m.baseId, m.parkingHeading));
                reg.markDirty();
                return;
            }
        }
    }

    /** 全マーカーのスナップショット。 */
    public static List<MarkerInfo> snapshot(World world) {
        return Collections.unmodifiableList(get(world).markers);
    }

    /** 直接 ID 検索（type も一致が必要）。 */
    public static MarkerInfo findById(World world, MarkerType type, String id) {
        return findById(world, type, id, "");
    }

    /**
     * 直接 ID 検索（type + id + baseId で検索）。
     * baseId が空でない場合、まず baseId が一致するものを返す（完全一致優先）。
     * 一致しなければ baseId を無視した最初のヒットを返す（後方互換フォールバック）。
     */
    public static MarkerInfo findById(World world, MarkerType type, String id, String baseId) {
        MarkerInfo fallback = null;
        for (MarkerInfo m : get(world).markers) {
            if (m.type != type || !id.equals(m.id)) continue;
            if (!baseId.isEmpty() && baseId.equals(m.baseId)) return m; // 完全一致
            if (fallback == null) fallback = m;                          // フォールバック候補
        }
        return fallback;
    }

    /** 特定タイプのマーカーを全て返す。 */
    public static List<MarkerInfo> findByType(World world, MarkerType type) {
        List<MarkerInfo> result = new ArrayList<>();
        for (MarkerInfo m : get(world).markers) {
            if (m.type == type) result.add(m);
        }
        return result;
    }

    /**
     * ベースIDで子マーカーを検索する（特定タイプのみ）。
     * ベース名 → そのベースに属する type のマーカーを返す。
     */
    public static MarkerInfo findByBase(World world, String baseId, MarkerType type) {
        for (MarkerInfo m : get(world).markers) {
            if (m.type == type && baseId.equals(m.baseId)) return m;
        }
        return null;
    }

    /**
     * ベースIDで子マーカーを全て返す。
     */
    public static List<MarkerInfo> findChildren(World world, String baseId) {
        List<MarkerInfo> result = new ArrayList<>();
        for (MarkerInfo m : get(world).markers) {
            if (!m.baseId.isEmpty() && baseId.equals(m.baseId)) result.add(m);
        }
        return result;
    }

    /** 指定基地の指定タイプのマーカーを全て返す（findByBase の複数版）。 */
    public static List<MarkerInfo> findAllByBase(World world, String baseId, MarkerType type) {
        List<MarkerInfo> result = new ArrayList<>();
        for (MarkerInfo m : get(world).markers) {
            if (m.type == type && baseId.equals(m.baseId)) result.add(m);
        }
        return result;
    }

    /**
     * 指定基地の空き駐機スポットを返す。
     * ロード済みエンティティリストに McHeli 機体が PARK_DIST ブロック以内にいないスポットを探す。
     * 空きがなければ null を返す。
     */
    public static MarkerInfo findAvailableParking(World world, String baseId,
            java.util.List<net.minecraft.entity.Entity> loadedEntities) {
        for (MarkerInfo m : get(world).markers) {
            if (m.type != MarkerType.PARKING || !baseId.equals(m.baseId)) continue;
            boolean occupied = false;
            double px = m.pos.getX() + 0.5, pz = m.pos.getZ() + 0.5;
            for (net.minecraft.entity.Entity e : loadedEntities) {
                if (!com.norwood.mcheli.wingman.util.McheliReflect.isAircraft(e)) continue;
                double dx = e.posX - px, dz = e.posZ - pz;
                if (dx * dx + dz * dz < 8.0 * 8.0) { occupied = true; break; }
            }
            if (!occupied) return m;
        }
        return null;
    }

    /**
     * 指定基地の空き駐機スポットを返す（PARKING → HELIPAD の順で検索）。
     * canUseHelipad = true のとき HELIPAD もフォールバック候補にする。
     */
    public static MarkerInfo findAvailableSpot(World world, String baseId,
            java.util.List<net.minecraft.entity.Entity> loadedEntities, boolean canUseHelipad) {
        // 1. 通常駐機スポット優先
        MarkerInfo parking = findAvailableParking(world, baseId, loadedEntities);
        if (parking != null) return parking;
        // 2. ヘリパッドフォールバック
        if (canUseHelipad) {
            for (MarkerInfo m : get(world).markers) {
                if (m.type != MarkerType.HELIPAD || !baseId.equals(m.baseId)) continue;
                boolean occupied = false;
                double px = m.pos.getX() + 0.5, pz = m.pos.getZ() + 0.5;
                for (net.minecraft.entity.Entity e : loadedEntities) {
                    if (!com.norwood.mcheli.wingman.util.McheliReflect.isAircraft(e)) continue;
                    double dx = e.posX - px, dz = e.posZ - pz;
                    if (dx * dx + dz * dz < 8.0 * 8.0) { occupied = true; break; }
                }
                if (!occupied) return m;
            }
        }
        return null;
    }

    /**
     * ルートノードの ID 解決。
     *
     * baseId="alpha" → BASE マーカー "alpha" を親として type の子を返す。
     * 直接 ID → findById のフォールバック。
     *
     * 例: resolveMarker(world, "alpha", RUNWAY_A)
     *   → BASE "alpha" の RUNWAY_A 子マーカー
     */
    public static MarkerInfo resolveMarker(World world, String nodeId, MarkerType type) {
        // 1. ベース名として解決を試みる
        MarkerInfo base = findById(world, MarkerType.BASE, nodeId);
        if (base != null) {
            MarkerInfo child = findByBase(world, nodeId, type);
            if (child != null) return child;
        }
        // 2. 直接IDフォールバック（従来の動作と後方互換）
        return findById(world, type, nodeId);
    }

    // ─── Serialization ───────────────────────────────────────────────────────

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        markers.clear();
        NBTTagList list = tag.getTagList("markers", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound c = list.getCompoundTagAt(i);
            BlockPos pos = new BlockPos(c.getInteger("x"), c.getInteger("y"), c.getInteger("z"));
            MarkerType type;
            try { type = MarkerType.valueOf(c.getString("type")); }
            catch (Exception e) { type = MarkerType.PARKING; }
            String id             = c.getString("id");
            String baseId         = c.hasKey("baseId") ? c.getString("baseId") : "";
            int    parkingHeading = c.hasKey("parkingHeading") ? c.getInteger("parkingHeading") : -1;
            markers.add(new MarkerInfo(pos, type, id, baseId, parkingHeading));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        NBTTagList list = new NBTTagList();
        for (MarkerInfo m : markers) {
            NBTTagCompound c = new NBTTagCompound();
            c.setInteger("x", m.pos.getX());
            c.setInteger("y", m.pos.getY());
            c.setInteger("z", m.pos.getZ());
            c.setString("type",   m.type.name());
            c.setString("id",     m.id);
            c.setString("baseId", m.baseId);
            if (m.parkingHeading >= 0) c.setInteger("parkingHeading", m.parkingHeading);
            list.appendTag(c);
        }
        tag.setTag("markers", list);
        return tag;
    }
}
