package com.norwood.mcheli.core;


import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.weapon.MCH_EntityBaseBullet;
import com.norwood.mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;

import java.util.Collection;

@SuppressWarnings("unused")
public class WorldUnloadHook {

    /**
     * Filters the entities the client is about to drop because their chunk unloaded.
     *
     * <p>Long-distance rendering deliberately keeps MCHeli entities (aircraft / UAVs) on the client
     * even when their chunk unloads, so the far-render pass can still draw them past the view
     * distance — which means their <em>only</em> remaining removal path is an explicit server
     * {@code SPacketDestroyEntities}.
     *
     * <p>Transient projectiles must <b>not</b> get that treatment. A TV missile (or any bullet)
     * streamed far past the view distance that survives a missed/late server destroy would otherwise
     * become a permanent client ghost — still drawn by the far pass and still emitting its trajectory
     * smoke every tick (the "still makes noise / persists after it explodes" bug). Letting a bullet
     * unload together with its chunk restores the vanilla self-healing safety net; while the missile
     * is alive its chunk stays streamed (so it never unloads underneath it), and the chunk only
     * unloads once {@code UavChunkStreamer} stops streaming around it — i.e. after it has died.
     *
     * <p>No-op on the server (vanilla unloading is left untouched there).
     */
    public static Collection<Entity> onEntityUnload(Collection<Entity> entities) {
        if (!MCH_MOD.proxy.isRemote()) {
            return entities;
        }
        return entities.stream()
                .filter(e -> !(e instanceof W_Entity) || e instanceof MCH_EntityBaseBullet)
                .toList();
    }


}
