package com.norwood.mcheli.weapon;

public interface MCH_IBulletDecay {

    float calculateDecayFactor(float distanceTraveled);

    float calculateDamage(float initialDamage, float distanceTraveled);
}
