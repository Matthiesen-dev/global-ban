package dev.matthiesen.global_ban.common.utils;

import com.google.gson.Gson;

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

    private static void example() {
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
}
