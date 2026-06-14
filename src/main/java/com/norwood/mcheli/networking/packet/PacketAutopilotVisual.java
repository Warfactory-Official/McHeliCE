package com.norwood.mcheli.networking.packet;

import hohserg.elegant.networking.api.ElegantPacket;
import hohserg.elegant.networking.api.ServerToClientPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;

/**
 * Server → Client: autonomous-flight target heading and throttle.
 * The client re-applies the value in {@code ClientAutopilotHandler} on
 * {@code Phase.END}, cancelling McHeli's key-input based override in
 * {@code onUpdateAircraft()}.
 *
 * <p>{@code targetYaw == Float.NaN} clears the yaw override;
 * {@code targetThrottle == Float.NaN} clears the throttle override.
 */
@ElegantPacket
public class PacketAutopilotVisual implements ServerToClientPacket {

    public int   entityId;
    public float targetYaw;      // Float.NaN = clear yaw
    public float targetThrottle; // Float.NaN = clear throttle

    // ─── Client-side storage (client VM only) ────────────────────────────────
    @SideOnly(Side.CLIENT)
    public static final Map<Integer, Float> CLIENT_HEADINGS  = new HashMap<>();
    @SideOnly(Side.CLIENT)
    public static final Map<Integer, Float> CLIENT_THROTTLES = new HashMap<>();

    public PacketAutopilotVisual() {}

    public PacketAutopilotVisual(int entityId, float targetYaw, float targetThrottle) {
        this.entityId       = entityId;
        this.targetYaw      = targetYaw;
        this.targetThrottle = targetThrottle;
    }

    @Override
    public void onReceive(Minecraft mc) {
        if (Float.isNaN(this.targetYaw)) {
            CLIENT_HEADINGS.remove(this.entityId);
        } else {
            CLIENT_HEADINGS.put(this.entityId, this.targetYaw);
        }
        if (Float.isNaN(this.targetThrottle)) {
            CLIENT_THROTTLES.remove(this.entityId);
        } else {
            CLIENT_THROTTLES.put(this.entityId, this.targetThrottle);
        }
    }
}
