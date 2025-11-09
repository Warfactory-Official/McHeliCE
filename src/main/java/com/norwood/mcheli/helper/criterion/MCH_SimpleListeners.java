package com.norwood.mcheli.helper.criterion;

import java.util.Set;

import net.minecraft.advancements.ICriterionTrigger.Listener;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.advancements.critereon.AbstractCriterionInstance;
import net.minecraft.util.ResourceLocation;

import com.google.common.collect.Sets;

public class MCH_SimpleListeners {

    private final Set<Listener<MCH_SimpleListeners.SimpleInstance>> listeners = Sets.newHashSet();
    private final PlayerAdvancements playerAdvancements;

    public MCH_SimpleListeners(PlayerAdvancements playerAdvancements) {
        this.playerAdvancements = playerAdvancements;
    }

    public boolean isEmpty() {
        return this.listeners.isEmpty();
    }

    public void add(Listener<MCH_SimpleListeners.SimpleInstance> listener) {
        this.listeners.add(listener);
    }

    public void remove(Listener<MCH_SimpleListeners.SimpleInstance> listener) {
        this.listeners.remove(listener);
    }

    public void trigger() {
        for (Listener<MCH_SimpleListeners.SimpleInstance> listener : this.listeners) {
            listener.grantCriterion(this.playerAdvancements);
        }
    }

    public static class SimpleInstance extends AbstractCriterionInstance {

        public SimpleInstance(ResourceLocation criterionIn) {
            super(criterionIn);
        }
    }
}
