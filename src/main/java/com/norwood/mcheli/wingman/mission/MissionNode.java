package com.norwood.mcheli.wingman.mission;

/**
 * ミッションの1ステップ。
 * GUIからも生成できるよう immutable value object として設計。
 */
public class MissionNode {

    public enum Type {
        FLY_TO,       // (x, y, z) へ飛行
        TAKEOFF,      // runwayId の滑走路から離陸
        LAND,         // runwayId の滑走路へ着陸
        ATTACK,       // 現在位置周辺 radius ブロック内の敵を攻撃して次へ
        LOITER,       // durationTicks 間その場で旋回
        PARK          // parkingId の駐機場へ地上滑走して停止
    }

    public final Type   type;

    // FLY_TO
    public final double x, y, z;

    // TAKEOFF / LAND
    public final String runwayId;

    // ATTACK
    public final double attackRadius;

    // LOITER
    public final int durationTicks;

    // PARK
    public final String parkingId;

    // ─── Factories ───────────────────────────────────────────────────────────

    public static MissionNode flyTo(double x, double y, double z) {
        return new MissionNode(Type.FLY_TO, x, y, z, null, 200.0, 0, null);
    }

    public static MissionNode takeoff(String runwayId) {
        return new MissionNode(Type.TAKEOFF, 0, 0, 0, runwayId, 0, 0, null);
    }

    public static MissionNode land(String runwayId) {
        return new MissionNode(Type.LAND, 0, 0, 0, runwayId, 0, 0, null);
    }

    public static MissionNode attack(double radius) {
        return new MissionNode(Type.ATTACK, 0, 0, 0, null, radius, 0, null);
    }

    public static MissionNode loiter(int ticks) {
        return new MissionNode(Type.LOITER, 0, 0, 0, null, 0, ticks, null);
    }

    public static MissionNode park(String parkingId) {
        return new MissionNode(Type.PARK, 0, 0, 0, null, 0, 0, parkingId);
    }

    private MissionNode(Type type, double x, double y, double z,
                        String runwayId, double attackRadius,
                        int durationTicks, String parkingId) {
        this.type          = type;
        this.x             = x;
        this.y             = y;
        this.z             = z;
        this.runwayId      = runwayId;
        this.attackRadius  = attackRadius;
        this.durationTicks = durationTicks;
        this.parkingId     = parkingId;
    }

    /** コマンド文字列から1ノードをパース。書式: "flyto:x,y,z" "takeoff:id" "land:id" "attack:r" "loiter:t" "park:id" */
    public static MissionNode parse(String token) {
        String[] parts = token.split(":", 2);
        String cmd = parts[0].toLowerCase();
        String arg = parts.length > 1 ? parts[1] : "";
        switch (cmd) {
            case "flyto": {
                String[] xyz = arg.split(",");
                return flyTo(Double.parseDouble(xyz[0]),
                             Double.parseDouble(xyz[1]),
                             Double.parseDouble(xyz[2]));
            }
            case "takeoff": return takeoff(arg);
            case "land":    return land(arg);
            case "attack":  return attack(arg.isEmpty() ? 200.0 : Double.parseDouble(arg));
            case "loiter":  return loiter(arg.isEmpty() ? 600 : Integer.parseInt(arg));
            case "park":    return park(arg);
            default: throw new IllegalArgumentException("Unknown node type: " + cmd);
        }
    }

    /** コマンド文字列へシリアライズ（JSON保存用）。 */
    public String serialize() {
        switch (type) {
            case FLY_TO:  return "flyto:" + (int)x + "," + (int)y + "," + (int)z;
            case TAKEOFF: return "takeoff:" + runwayId;
            case LAND:    return "land:" + runwayId;
            case ATTACK:  return "attack:" + (int)attackRadius;
            case LOITER:  return "loiter:" + durationTicks;
            case PARK:    return "park:" + parkingId;
            default:      return type.name().toLowerCase();
        }
    }

    @Override
    public String toString() { return serialize(); }
}
