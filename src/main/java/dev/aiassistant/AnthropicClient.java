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

    public AnthropicClient(AIAssistantPlugin plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = new Gson();
    }

    public void askAsync(String playerName, String question,
                         Consumer<String> onSuccess, Consumer<String> onError) {

        String apiKey = plugin.getConfig().getString("api-key", "");
        String model = plugin.getConfig().getString("model", "gemini-2.0-flash");
        String systemPrompt = plugin.getConfig().getString("server-description", "");
        int maxTokens = plugin.getConfig().getInt("max-tokens", 300);

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    String response = callGeminiAPI(apiKey, model, systemPrompt, playerName, question, maxTokens);
                    new BukkitRunnable() {
                        @Override
                        public void run() { onSuccess.accept(response); }
                    }.runTask(plugin);
                } catch (Exception e) {
                    plugin.getLogger().warning("API Hatası: " + e.getMessage());
                    new BukkitRunnable() {
                        @Override
                        public void run() { onError.accept(e.getMessage()); }
                    }.runTask(plugin);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private String callGeminiAPI(String apiKey, String model, String systemPrompt,
                                  String playerName, String question, int maxTokens)
            throws IOException, InterruptedException {

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;

        JsonArray sysParts = new JsonArray();
        JsonObject sysPart = new JsonObject();
        sysPart.addProperty("text", systemPrompt);
        sysParts.add(sysPart);
        JsonObject systemInstruction = new JsonObject();
        systemInstruction.add("parts", sysParts);

        JsonObject userPart = new JsonObject();
        userPart.addProperty("text", "Oyuncu adı: " + playerName + "\nSoru: " + question);
        JsonArray userParts = new JsonArray();
        userParts.add(userPart);
        JsonObject userContent = new JsonObject();
        userContent.addProperty("role", "user");
        userContent.add("parts", userParts);
        JsonArray contents = new JsonArray();
        contents.add(userContent);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("maxOutputTokens", maxTokens);

        JsonObject body = new JsonObject();
        body.add("system_instruction", systemInstruction);
        body.add("contents", contents);
        body.add("generationConfig", generationConfig);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("API Hata Kodu: " + response.statusCode() + " | " + response.body());
        }

        JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
        return responseJson
                .getAsJsonArray("candidates")
                .get(0).getAsJsonObject()
                .getAsJsonObject("content")
                .getAsJsonArray("parts")
                .get(0).getAsJsonObject()
                .get("text").getAsString()
                .trim();
    }
}
