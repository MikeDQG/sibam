package com.sibam.integration.supabase;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Odjemalec za prenos datotek iz Supabase Storage.
 * Uporablja Supabase Storage REST API za prenos modelov in drugih artefaktov iz določenega bucketa. Avtentikacija poteka s service-key žetonom.
 * Primer uporabe: prenos ONNX modelov iz bucketa {@code gold} ob zagonu aplikacije.
 */


@Component
public class SupabaseStorageClient {
    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-key}")
    private String serviceKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public byte[] download(String bucket, String path) {
        String url = supabaseUrl + "/storage/v1/object/" + bucket + "/" + path;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + serviceKey)
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "Failed to download " + bucket + "/" + path + " — HTTP " + response.statusCode());
            }
            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Error downloading from Supabase storage: " + path, e);
        }
    }
}