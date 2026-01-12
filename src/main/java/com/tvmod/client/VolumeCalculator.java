package com.tvmod.client;

public class VolumeCalculator {

    public static final double MAX_DISTANCE = 32.0;

    public static float calculateVolume(double distance, float baseVolume) {
        baseVolume = Math.max(0.0f, Math.min(1.0f, baseVolume));

        if (distance <= 0) {
            return baseVolume;
        }

        if (distance >= MAX_DISTANCE) {
            return 0.0f;
        }

        float attenuation = (float) (1.0 - (distance / MAX_DISTANCE));
        return baseVolume * attenuation;
    }

    public static float calculateVolume(double distance, float baseVolume, float playerMasterVolume) {
        float spatialVolume = calculateVolume(distance, baseVolume);
        return spatialVolume * Math.max(0.0f, Math.min(1.0f, playerMasterVolume));
    }
}
