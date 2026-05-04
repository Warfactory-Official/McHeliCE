package com.norwood.mcheli.weapon;

import java.util.List;

public class MCH_BulletDecaySegmented implements MCH_IBulletDecay {

    private final List<DecaySegment> segments;

    public MCH_BulletDecaySegmented(List<DecaySegment> segments) {
        this.segments = segments;
    }

    @Override
    public float calculateDecayFactor(float distanceTraveled) {
        float decayFactor = 1.0F;
        for (DecaySegment segment : this.segments) {
            if (distanceTraveled > segment.startDistance) {
                decayFactor = segment.damageMultiplier;
            }
        }
        return decayFactor;
    }

    @Override
    public float calculateDamage(float initialDamage, float distanceTraveled) {
        return initialDamage * this.calculateDecayFactor(distanceTraveled);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Segmented ");
        for (DecaySegment segment : this.segments) {
            builder.append(segment);
        }
        return builder.toString();
    }

    public static class DecaySegment {

        public final float startDistance;
        public final float damageMultiplier;

        public DecaySegment(float startDistance, float damageMultiplier) {
            this.startDistance = startDistance;
            this.damageMultiplier = damageMultiplier;
        }

        @Override
        public String toString() {
            return "| >" + this.startDistance + "m-x" + this.damageMultiplier + " ";
        }
    }
}
