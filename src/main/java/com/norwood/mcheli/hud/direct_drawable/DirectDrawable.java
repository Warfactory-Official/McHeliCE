package com.norwood.mcheli.hud.direct_drawable;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Tuple;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

public interface DirectDrawable {

    // Manual override: use a non-canceled overlay pass so direct drawables still render.
    boolean FORCE_OVERLAY_POST_OVERRIDE = true;
    RenderGameOverlayEvent.ElementType FORCED_OVERLAY_POST_TYPE = RenderGameOverlayEvent.ElementType.CHAT;

    void renderHud(RenderGameOverlayEvent.Post event, Tuple<EntityPlayer, MCH_EntityAircraft> ctx);

    DirectDrawable getInstance();

    static boolean shouldRender(RenderGameOverlayEvent.Post event) {
        return FORCE_OVERLAY_POST_OVERRIDE
                ? event.getType() == FORCED_OVERLAY_POST_TYPE
                : event.getType() == RenderGameOverlayEvent.ElementType.ALL;
    }
}
