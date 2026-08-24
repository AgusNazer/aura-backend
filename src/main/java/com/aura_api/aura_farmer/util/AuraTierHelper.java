package com.aura_api.aura_farmer.util;

public final class AuraTierHelper {

    private AuraTierHelper() {}

    public static String getTierTitle(Long auraPercentage) {
        if (auraPercentage == null || auraPercentage == 0L) {
            return "Fantasma / NPC";
        } else if (auraPercentage < 10L) {
            return "Intento de Ser Humano";
        } else if (auraPercentage < 50L) {
            return "Presencia Leve";
        } else if (auraPercentage < 100L) {
            return "Respetable";
        } else if (auraPercentage < 500L) {
            return "Mirada Pesada";
        } else if (auraPercentage < 1000L) {
            return "Aura Completa";
        } else {
            return "Entidad Todopoderosa";
        }
    }
}