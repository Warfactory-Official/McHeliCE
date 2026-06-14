package com.norwood.mcheli.wingman.mission;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** 任務種別。複数選択可（排他種別を除く）。 */
public enum MissionType {
    CAP,     // 制空哨戒：旋回しながら対空Mob迎撃
    CAS,     // 近接航空支援：旋回しながら対地Mob攻撃
    STRIKE,  // 打撃：N回攻撃パス→即RTB（排他）
    ESCORT,  // 護衛：プレイヤー随伴+Mob迎撃
    RECON,   // 偵察：エリア一周→Mob数報告→即RTB
    FERRY;   // 輸送：戦闘なし移動（排他）

    /** STRIKEまたはFERRYは他と組み合わせ不可 */
    public boolean isExclusive() {
        return this == STRIKE || this == FERRY;
    }

    /** この任務種別のデフォルト武装セット */
    public Set<String> defaultWeapons() {
        switch (this) {
            case CAP:    return new HashSet<>(Arrays.asList("aamissile", "gun"));
            case CAS:    return new HashSet<>(Arrays.asList("cas", "rocket", "gun"));
            case STRIKE: return new HashSet<>(Arrays.asList("asmissile", "bomb", "gun"));
            case ESCORT: return new HashSet<>(Arrays.asList("aamissile", "gun"));
            case RECON:  return new HashSet<>(Arrays.asList("gun"));
            case FERRY:  return new HashSet<>();
            default:     return new HashSet<>();
        }
    }

    public String displayName() {
        switch (this) {
            case CAP:    return "CAP";
            case CAS:    return "CAS";
            case STRIKE: return "Strike";
            case ESCORT: return "Escort";
            case RECON:  return "Recon";
            case FERRY:  return "Ferry";
            default:     return name();
        }
    }
}
