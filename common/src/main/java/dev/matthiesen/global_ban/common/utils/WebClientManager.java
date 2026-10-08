package dev.matthiesen.global_ban.common.utils;

import com.google.gson.Gson;
import dev.matthiesen.global_ban.common.GlobalBanCommon;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class WebClientManager {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final Gson GSON = new Gson();

    public static HttpRequest getRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "GlobalBan/1.0 (Matthiesen-Dev https://github.com/Matthiesen-dev/global-ban)")
                .GET()
                .build();
    }

    public static HttpRequest postRequest(String url, String jsonPayload) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "GlobalBan/1.0 (Matthiesen-Dev https://github.com/Matthiesen-dev/global-ban)")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
    }

    public static void getRequestExample() {
        HttpRequest request = getRequest("https://api.example.com/data");
        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return GSON.fromJson(response.body(), Object.class); // Replace Object with your desired class
                    } else {
                        throw new RuntimeException("Failed to fetch data: " + response.statusCode());
                    }
                })
                .thenAccept(data -> {
                    // Success
                })
                .exceptionally(throwable -> {
                    // Handle error
                    return null;
                });
    }

    public record ExamplePayload(String field1, int field2) {
        // Add any necessary methods or validation here
    }

    public static void postRequestExample(ExamplePayload payload) {
        String jsonPayload = GSON.toJson(payload);
        HttpRequest request = postRequest("https://api.example.com/submit", jsonPayload);

        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 201) {
                        // success
                        GlobalBanCommon.INSTANCE.createInfoLog("Successfully submitted data: " + response.body());
                    } else {
                        throw new RuntimeException("Failed to submit data: " + response.statusCode());
                    }
                })
                .exceptionally(throwable -> {
                    // Handle error
                    return null;
                });
    }
}
