package com.norwood.mcheli.helper.criterion;

import com.google.common.collect.Maps;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.advancements.ICriterionTrigger;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class MCH_SimpleTrigger implements ICriterionTrigger<MCH_SimpleListeners.SimpleInstance> {

    private final Map<PlayerAdvancements, MCH_SimpleListeners> listeners = Maps.newHashMap();
    private final ResourceLocation id;

    public MCH_SimpleTrigger(ResourceLocation id) {
        this.id = id;
    }

    public @NotNull ResourceLocation getId() {
        return this.id;
    }

    public void addListener(@NotNull PlayerAdvancements playerAdvancementsIn,
                            @NotNull Listener<MCH_SimpleListeners.SimpleInstance> listener) {
        MCH_SimpleListeners listeners = this.listeners.get(playerAdvancementsIn);
        if (listeners == null) {
            listeners = new MCH_SimpleListeners(playerAdvancementsIn);
            this.listeners.put(playerAdvancementsIn, listeners);
        }

        listeners.add(listener);
    }

    public void removeListener(@NotNull PlayerAdvancements playerAdvancementsIn,
                               @NotNull Listener<MCH_SimpleListeners.SimpleInstance> listener) {
        MCH_SimpleListeners listeners = this.listeners.get(playerAdvancementsIn);
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                this.listeners.remove(playerAdvancementsIn);
            }
        }
    }

    public void removeAllListeners(@NotNull PlayerAdvancements playerAdvancementsIn) {
        this.listeners.remove(playerAdvancementsIn);
    }

    public MCH_SimpleListeners.@NotNull SimpleInstance deserializeInstance(@NotNull JsonObject json,
                                                                           @NotNull JsonDeserializationContext context) {
        return new MCH_SimpleListeners.SimpleInstance(this.id);
    }

    public void trigger(EntityPlayerMP player) {
        MCH_SimpleListeners listener = this.listeners.get(player.getAdvancements());
        if (listener != null) {
            listener.trigger();
        }
    }
}
