package com.sibam.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Oddaljena cache shramba prek Supabase Storage REST API-ja.
 *
 * Uporablja service-role ključ in se tiho degradira, kadar cache ni omogočen ali
 * je oddaljeni prenos neuspešen.
 */
@Component
public class SupabaseArtifactStorage {

    private static final Logger log = LoggerFactory.getLogger(SupabaseArtifactStorage.class);

    private final WebClient webClient;
    private final String bucket;
    private final String serviceRoleKey;
    private final boolean enabled;

    public SupabaseArtifactStorage(
            WebClient.Builder webClientBuilder,
            @Value("${supabase.url:}") String supabaseUrl,
            @Value("${supabase.service-role-key:${supabase.service-key:}}") String serviceRoleKey,
            @Value("${supabase.cache.bucket:cache}") String bucket,
            @Value("${supabase.cache.enabled:false}") boolean enabled
    ) {
        this.webClient = webClientBuilder.baseUrl(supabaseUrl == null ? "" : supabaseUrl).build();
        this.serviceRoleKey = serviceRoleKey == null ? "" : serviceRoleKey;
        this.bucket = bucket;
        this.enabled = enabled;
    }

    public String bucket() {
        return bucket;
    }

    /**
     * Preveri, ali je Supabase cache operativno omogočen.
     *
     * @return true, če je v konfiguraciji omogočen in ima service-role ključ
     */
    public boolean enabled() {
        return enabled && !serviceRoleKey.isBlank();
    }

    public boolean exists(String path) {
        return download(path) != null;
    }

    /**
     * Prenese artefakt iz Supabase cache bucketa.
     *
     * @param path pot objekta znotraj bucketa
     * @return bajti artefakta ali null, če ni na voljo
     */
    public byte[] download(String path) {
        if (!enabled()) {
            return null;
        }

        try {
            return webClient.get()
                    .uri("/storage/v1/object/" + bucket + "/" + path)
                    .headers(this::authorize)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            return null;
        } catch (RuntimeException e) {
            log.warn("Failed to download Supabase cache object {}/{}: {}", bucket, path, e.getMessage());
            return null;
        }
    }

    /**
     * Naloži artefakt v Supabase cache bucket.
     *
     * Če oddaljena vsebina že ima enak SHA-256 hash, upload preskoči.
     */
    public void upload(String path, byte[] bytes, MediaType contentType) {
        if (!enabled()) {
            return;
        }

        byte[] existing = download(path);
        if (existing != null && Sha256.hex(existing).equals(Sha256.hex(bytes))) {
            return;
        }

        try {
            webClient.put()
                    .uri("/storage/v1/object/" + bucket + "/" + path)
                    .headers(headers -> {
                        authorize(headers);
                        headers.setContentType(contentType);
                        headers.set("x-upsert", "true");
                    })
                    .bodyValue(bytes)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (RuntimeException e) {
            log.warn("Failed to upload Supabase cache object {}/{}: {}", bucket, path, e.getMessage());
        }
    }

    /**
     * Izbriše artefakt iz Supabase cache bucketa, če je cache omogočen.
     *
     * @param path pot objekta znotraj bucketa
     */
    public void delete(String path) {
        if (!enabled()) {
            return;
        }

        try {
            webClient.delete()
                    .uri("/storage/v1/object/" + bucket + "/" + path)
                    .headers(this::authorize)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException.NotFound ignored) {
        } catch (RuntimeException e) {
            log.warn("Failed to delete Supabase cache object {}/{}: {}", bucket, path, e.getMessage());
        }
    }

    private void authorize(org.springframework.http.HttpHeaders headers) {
        headers.setBearerAuth(serviceRoleKey);
        headers.set("apikey", serviceRoleKey);
    }
}
