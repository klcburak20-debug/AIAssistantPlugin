package dev.aiassistant;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AICommand implements CommandExecutor {

    private final AIAssistantPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public AICommand(AIAssistantPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        // Sadece oyuncular kullanabilir
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Bu komut sadece oyuncular tarafından kullanılabilir.");
            return true;
        }

        // İzin kontrolü
        if (!player.hasPermission("aiassistant.use")) {
            sendMsg(player, getMsg("no-permission"));
            return true;
        }

        // Boş soru kontrolü
        if (args.length == 0) {
            sendMsg(player, getMsg("empty-question"));
            return true;
        }

        // Soruyu birleştir
        String question = String.join(" ", args);

        // Maksimum uzunluk kontrolü
        int maxLen = plugin.getConfig().getInt("max-question-length", 200);
        if (question.length() > maxLen) {
            sendMsg(player, getMsg("too-long").replace("{max}", String.valueOf(maxLen)));
            return true;
        }

        // Cooldown kontrolü
        if (!player.hasPermission("aiassistant.bypass-cooldown")) {
            int remaining = plugin.getCooldownManager().getRemainingCooldown(player.getUniqueId());
            if (remaining > 0) {
                sendMsg(player, getMsg("cooldown").replace("{saniye}", String.valueOf(remaining)));
                return true;
            }
        }

        // Cooldown'ı hemen başlat
        plugin.getCooldownManager().setCooldown(player.getUniqueId());

        // "Düşünüyor..." mesajı gönder
        sendMsg(player, getMsg("thinking"));

        // API'ye asenkron istek at
        plugin.getAnthropicClient().askAsync(
                player.getName(),
                question,

                // Başarı durumu
                (answer) -> {
                    String prefix = plugin.getConfig().getString("messages.prefix", "&8[&bAI&8] ");
                    String answerColor = plugin.getConfig().getString("answer-color", "&f");
                    String playerColor = plugin.getConfig().getString("player-color", "&e");

                    // Oyuncuya cevabı gönder
                    player.sendMessage(colorize(prefix + playerColor + player.getName()
                            + "&7: " + "&7" + question));
                    player.sendMessage(colorize(prefix + answerColor + answer));

                    // Konsola logla
                    plugin.getLogger().info("[AI] " + player.getName()
                            + " sordu: " + question);
                    plugin.getLogger().info("[AI] Cevap: " + answer);
                },

                // Hata durumu
                (error) -> {
                    sendMsg(player, getMsg("error"));
                    // Hata durumunda cooldown'ı geri ver (isterse tekrar sorsun)
                    plugin.getCooldownManager().resetCooldown(player.getUniqueId());
                    plugin.getLogger().severe("[AI] API Hatası: " + error);
                }
        );

        return true;
    }

    private String getMsg(String key) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&bAI&8] ");
        String msg = plugin.getConfig().getString("messages." + key, "&cMesaj bulunamadı: " + key);
        return prefix + msg;
    }

    private void sendMsg(Player player, String message) {
        player.sendMessage(colorize(message));
    }

    // &a, &b gibi renk kodlarını dönüştür
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
