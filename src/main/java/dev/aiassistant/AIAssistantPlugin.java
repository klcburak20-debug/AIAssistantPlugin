package dev.aiassistant;

import org.bukkit.plugin.java.JavaPlugin;

public class AIAssistantPlugin extends JavaPlugin {

    private static AIAssistantPlugin instance;
    private AnthropicClient anthropicClient;
    private CooldownManager cooldownManager;

    @Override
    public void onEnable() {
        instance = this;

        // Config dosyasını kaydet
        saveDefaultConfig();

        // Bileşenleri başlat
        this.anthropicClient = new AnthropicClient(this);
        this.cooldownManager = new CooldownManager(this);

        // Komutları kaydet
        AICommand aiCommand = new AICommand(this);
        getCommand("ai").setExecutor(aiCommand);
        getCommand("aireload").setExecutor(new ReloadCommand(this));

        getLogger().info("AI Asistan Plugin başlatıldı! ✔");
        getLogger().info("Model: " + getConfig().getString("model"));

        if (getConfig().getString("api-key", "").equals("BURAYA_API_ANAHTARINI_YAZ")) {
            getLogger().warning("⚠ API anahtarı ayarlanmamış! config.yml dosyasını düzenle.");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("AI Asistan Plugin kapatıldı.");
    }

    public static AIAssistantPlugin getInstance() {
        return instance;
    }

    public AnthropicClient getAnthropicClient() {
        return anthropicClient;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public void reloadPlugin() {
        reloadConfig();
        this.anthropicClient = new AnthropicClient(this);
        this.cooldownManager = new CooldownManager(this);
    }
}
