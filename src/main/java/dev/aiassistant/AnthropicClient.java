package dev.aiassistant;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Consumer;

public class AnthropicClient {

    private final AIAssistantPlugin plugin;
    private final HttpClient httpClient;
    private final Gson gson;

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    public AnthropicClient(AIAssistantPlugin plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = new Gson();
    }

    /**
     * Asenkron olarak Anthropic API'sine istek atar.
     * Cevap gelince onSuccess çalışır, hata olursa onError çalışır.
     */
    public void askAsync(String playerName, String question,
                         Consumer<String> onSuccess, Consumer<String> onError) {

        String apiKey = plugin.getConfig().getString("api-key", "");
        String model = plugin.getConfig().getString("model", "claude-3-5-haiku-20241022");
        String systemPrompt = plugin.getConfig().getString("server-description", "");
        int maxTokens = plugin.getConfig().getInt("max-tokens", 300);

        // API isteğini asenkron thread'de yap (sunucuyu bloke etmemek için)
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    String response = callAPI(apiKey, model, systemPrompt,
                            playerName, question, maxTokens);

                    // Cevabı ana thread'de işle
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            onSuccess.accept(response);
                        }
                    }.runTask(plugin);

                } catch (Exception e) {
                    plugin.getLogger().warning("API Hatası: " + e.getMessage());
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            onError.accept(e.getMessage());
                        }
                    }.runTask(plugin);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private String callAPI(String apiKey, String model, String systemPrompt,
                           String playerName, String question, int maxTokens)
            throws IOException, InterruptedException {

        // JSON gövdesini oluştur
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("max_tokens", maxTokens);
        body.addProperty("system", systemPrompt);

        // Kullanıcı mesajı
        JsonArray messages = new JsonArray();
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content",
                "Oyuncu adı: " + playerName + "\nSoru: " + question);
        messages.add(userMessage);
        body.add("messages", messages);

        // HTTP isteği oluştur
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .timeout(Duration.ofSeconds(30))
                .build();

        // İsteği gönder
        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("API Hata Kodu: " + response.statusCode()
                    + " | " + response.body());
        }

        // Cevabı parse et
        JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
        return responseJson
                .getAsJsonArray("content")
                .get(0)
                .getAsJsonObject()
                .get("text")
                .getAsString()
                .trim();
    }
}
