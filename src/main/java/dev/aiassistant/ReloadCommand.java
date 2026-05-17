package dev.aiassistant;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ReloadCommand implements CommandExecutor {

    private final AIAssistantPlugin plugin;

    public ReloadCommand(AIAssistantPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        if (!sender.hasPermission("aiassistant.reload")) {
            sender.sendMessage("§cBu komutu kullanma iznin yok.");
            return true;
        }

        plugin.reloadPlugin();

        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§bAI§8] ");
        String successMsg = plugin.getConfig().getString("messages.reload-success",
                "§aAyarlar başarıyla yeniden yüklendi.");
        sender.sendMessage(colorize(prefix + successMsg));

        return true;
    }

    private String colorize(String text) {
        return text.replace("&0", "§0").replace("&1", "§1")
                .replace("&2", "§2").replace("&3", "§3")
                .replace("&4", "§4").replace("&5", "§5")
                .replace("&6", "§6").replace("&7", "§7")
                .replace("&8", "§8").replace("&9", "§9")
                .replace("&a", "§a").replace("&b", "§b")
                .replace("&c", "§c").replace("&d", "§d")
                .replace("&e", "§e").replace("&f", "§f")
                .replace("&l", "§l").replace("&o", "§o")
                .replace("&n", "§n").replace("&m", "§m")
                .replace("&k", "§k").replace("&r", "§r");
    }
}
