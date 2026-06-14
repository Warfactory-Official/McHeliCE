package com.norwood.mcheli.wingman.mission;
//WINGMAN — file introduced for the McHeli Wingman feature merge

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * タキシールート定義。
 * 駐機スポット → WPリスト（順序付き）→ 滑走路A端 を表す。
 * 帰還時（着陸後）は arrivalWaypointIds（設定時）または waypointIdsReversed() を使う。
 *
 * runwayId  = RUNWAY_A マーカー ID（タキシー終点 = 離陸開始点）
 *             帰還専用ルート（着陸後 B 端スタート）では RUNWAY_B を設定する場合もある。
 * runwayBId = RUNWAY_B マーカー ID（離陸終点）。空文字 = 自動検索。
 */
public class TaxiRoute {

    public final String routeId;
    public final String baseId;
    public final String parkingId;     // PARKING マーカー ID
    public final String runwayId;      // RUNWAY_A（または帰還専用ルートでは RUNWAY_B）マーカー ID
    public final String runwayBId;     // RUNWAY_B マーカー ID（離陸終点）。空文字 = 自動検索。
    public final List<String> waypointIds; // WAYPOINT マーカー ID リスト（出発順）

    /**
     * 着陸後 TAXI_IN 用の WP リスト（着陸時専用の通過順序）。
     * 空リストの場合は waypointIdsReversed()（waypointIds の逆順）で代替（後方互換）。
     */
    public final List<String> arrivalWaypointIds;

    /**
     * 着陸エントリー端点 ID（着陸時に最初に向かうマーカー）。
     * 空文字の場合は runwayId を使用（後方互換）。
     * RT_B 端からの着陸や、反対側のヘリパッドからの進入に使用。
     */
    public final String arrivalRunwayId;

    /**
     * 駐機完了時の機首方位。
     * -1 = 任意（設定なし）
     *  0 = 北（Minecraft yaw 180°）
     *  1 = 東（Minecraft yaw -90°）
     *  2 = 南（Minecraft yaw 0°）
     *  3 = 西（Minecraft yaw 90°）
     */
    public final int parkingHeading;

    /** runwayBId なし（後方互換）コンストラクタ */
    public TaxiRoute(String routeId, String baseId, String parkingId,
                     String runwayId, List<String> waypointIds) {
        this(routeId, baseId, parkingId, runwayId, "", waypointIds, new ArrayList<>(), -1);
    }

    public TaxiRoute(String routeId, String baseId, String parkingId,
                     String runwayId, String runwayBId, List<String> waypointIds) {
        this(routeId, baseId, parkingId, runwayId, runwayBId, waypointIds, new ArrayList<>(), -1);
    }

    public TaxiRoute(String routeId, String baseId, String parkingId,
                     String runwayId, String runwayBId, List<String> waypointIds,
                     int parkingHeading) {
        this(routeId, baseId, parkingId, runwayId, runwayBId, waypointIds, new ArrayList<>(), parkingHeading);
    }

    public TaxiRoute(String routeId, String baseId, String parkingId,
                     String runwayId, String runwayBId, List<String> waypointIds,
                     List<String> arrivalWaypointIds, int parkingHeading) {
        this(routeId, baseId, parkingId, runwayId, runwayBId, waypointIds,
             arrivalWaypointIds, parkingHeading, "");
    }

    public TaxiRoute(String routeId, String baseId, String parkingId,
                     String runwayId, String runwayBId, List<String> waypointIds,
                     List<String> arrivalWaypointIds, int parkingHeading, String arrivalRunwayId) {
        this.routeId             = routeId;
        this.baseId              = baseId;
        this.parkingId           = parkingId;
        this.runwayId            = runwayId;
        this.runwayBId           = (runwayBId != null) ? runwayBId : "";
        this.waypointIds         = Collections.unmodifiableList(new ArrayList<>(waypointIds));
        this.arrivalWaypointIds  = Collections.unmodifiableList(
                                       (arrivalWaypointIds != null) ? new ArrayList<>(arrivalWaypointIds)
                                                                     : new ArrayList<>());
        this.parkingHeading      = parkingHeading;
        this.arrivalRunwayId     = (arrivalRunwayId != null) ? arrivalRunwayId : "";
    }

    /** 帰還用WPリスト（逆順） */
    public List<String> waypointIdsReversed() {
        List<String> rev = new ArrayList<>(waypointIds);
        Collections.reverse(rev);
        return rev;
    }

    /** 出発順の全経由点リスト（parking→wp…→runway） */
    public List<String> fullDeparture() {
        List<String> all = new ArrayList<>();
        all.add(parkingId);
        all.addAll(waypointIds);
        all.add(runwayId);
        return all;
    }

    /**
     * 帰還順の全経由点リスト（runway→arrival_wps or reversed_departure_wps→parking）。
     * arrivalWaypointIds が設定されている場合はそれを使用。
     * 空の場合は waypointIdsReversed()（後方互換）。
     */
    public List<String> fullArrival() {
        List<String> all = new ArrayList<>();
        // arrivalRunwayId が設定されていればそちらを使用（RT_B 端着陸など）
        String entry = (arrivalRunwayId != null && !arrivalRunwayId.isEmpty())
                       ? arrivalRunwayId : runwayId;
        all.add(entry);
        if (!arrivalWaypointIds.isEmpty()) {
            all.addAll(arrivalWaypointIds);
        } else {
            all.addAll(waypointIdsReversed());
        }
        all.add(parkingId);
        return all;
    }

    /** 着陸エントリー端点 ID（arrivalRunwayId が設定されていればそちら、なければ runwayId） */
    public String effectiveArrivalEntry() {
        return (arrivalRunwayId != null && !arrivalRunwayId.isEmpty()) ? arrivalRunwayId : runwayId;
    }
}
