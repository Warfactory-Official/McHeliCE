package com.norwood.mcheli.wingman.block;
//WINGMAN — file introduced for the McHeli Wingman feature merge

/** WingmanMarkerBlock のモード。NBTに文字列で保存。 */
public enum MarkerType implements net.minecraft.util.IStringSerializable {
    BASE,       // 基地（親マーカー — 子マーカーをまとめるアンカー）
    PARKING,    // 駐機場
    RUNWAY_A,   // 滑走路端A（離陸起点 / 着陸終点）
    RUNWAY_B,   // 滑走路端B（離陸終点 / 着陸起点）
    WAYPOINT,   // 空中巡航ウェイポイント（XZ座標のみ使用）
    HELIPAD,    // ヘリ・VTOL機専用垂直離着陸スポット（1基地に複数設置可）
    HELIPAD_B;  // ヘリパッド方向指示マーカー。HELIPAD → HELIPAD_B の方向が
                // VTOL 離着陸時の機首向き（固定翼滑走路の RUNWAY_A/B と同じ考え方）。
                // HELIPAD と同じ baseId を付けて配置する。着陸スポットではない。

    @Override
    public String getName() { return name().toLowerCase(); }

    public MarkerType next() {
        MarkerType[] v = values();
        return v[(ordinal() + 1) % v.length];
    }

    public String displayName() {
        switch (this) {
            case BASE:      return "§6[Base]";
            case PARKING:   return "§e[Parking]";
            case RUNWAY_A:  return "§a[Runway-A]";
            case RUNWAY_B:  return "§b[Runway-B]";
            case WAYPOINT:  return "§d[Waypoint]";
            case HELIPAD:   return "§9[Helipad]";
            case HELIPAD_B: return "§3[Helipad-B]";
            default:        return name();
        }
    }

    /** GUI ボタン用の短い名称 */
    public String shortName() {
        switch (this) {
            case BASE:      return "Base";
            case PARKING:   return "Parking";
            case RUNWAY_A:  return "Runway-A";
            case RUNWAY_B:  return "Runway-B";
            case WAYPOINT:  return "Waypoint";
            case HELIPAD:   return "Helipad";
            case HELIPAD_B: return "Helipad-B";
            default:        return name();
        }
    }

    /** 着陸スポットか（HELIPAD_B は方向指示のみ、スポットではない） */
    public boolean isHelipad() {
        return this == HELIPAD;
    }

    /** 滑走路として機能するか（離着陸ルート端点） */
    public boolean isRunwayEndpoint() {
        return this == RUNWAY_A || this == RUNWAY_B || this == HELIPAD;
    }
}
