package com.shane.raspberryjuicereload.manager;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PotionEffectManager {
    private final PotionEffectType effect;
    private final int time;
    private final int strength;
    private final boolean particles;


    public PotionEffectManager(String[] args) {
        this(args[0], args[1], args[2], args[4]);
    }

    public PotionEffectManager(String effect, String time, String strength, String particles) {
        this(PotionEffectType.getByName(effect), Integer.parseInt(time), Integer.parseInt(strength), Boolean.parseBoolean(particles));
    }

    public PotionEffectManager(PotionEffectType effect, int time, int strength, boolean particles) {
        this.effect = effect;
        this.time = time;
        this.strength = strength;
        this.particles = particles;
    }

    public PotionEffect getPotionEffect() {
        return new PotionEffect(this.effect, this.time, this.strength, this.particles);
    }
}
