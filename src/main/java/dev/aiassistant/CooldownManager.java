package dev.aiassistant;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final AIAssistantPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public CooldownManager(AIAssistantPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Oyuncunun cooldown'da olup olmadığını kontrol eder.
     * @return Kalan saniye (0 ise cooldown bitti / yok)
     */
    public int getRemainingCooldown(UUID playerId) {
        if (!cooldowns.containsKey(playerId)) return 0;

        int cooldownSeconds = plugin.getConfig().getInt("cooldown-seconds", 15);
        long lastUsed = cooldowns.get(playerId);
        long elapsed = (System.currentTimeMillis() - lastUsed) / 1000;

        if (elapsed >= cooldownSeconds) {
            cooldowns.remove(playerId);
            return 0;
        }

        return (int) (cooldownSeconds - elapsed);
    }

    /**
     * Oyuncuyu cooldown'a alır (komut kullanıldığında çağır).
     */
    public void setCooldown(UUID playerId) {
        cooldowns.put(playerId, System.currentTimeMillis());
    }

    /**
     * Oyuncunun cooldown'ını sıfırlar (hata durumunda geri ver).
     */
    public void resetCooldown(UUID playerId) {
        cooldowns.remove(playerId);
    }
}
