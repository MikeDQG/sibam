package com.sibam.cache;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.Arrays;
import java.util.Objects;

/**
 * Koordinira lokalni in Supabase cache za generirane artefakte.
 *
 * Najprej uporabi veljaven lokalni artefakt, nato poskusi Supabase, šele nato
 * sproži generator in rezultat zapiše v oba cache sloja.
 */
@Service
public class ArtifactCacheService {

    private final LocalArtifactStorage localStorage;
    private final SupabaseArtifactStorage supabaseStorage;

    public ArtifactCacheService(LocalArtifactStorage localStorage, SupabaseArtifactStorage supabaseStorage) {
        this.localStorage = localStorage;
        this.supabaseStorage = supabaseStorage;
    }

    public LocalArtifactStorage localStorage() {
        return localStorage;
    }

    public SupabaseArtifactStorage supabaseStorage() {
        return supabaseStorage;
    }

    /**
     * Zagotovi veljaven artefakt na podani poti.
     *
     * @param path pot artefakta znotraj cache-a
     * @param contentType MIME tip pri nalaganju v Supabase
     * @param validator validator bajtov artefakta
     * @param generator generator, če lokalni in oddaljeni cache nista uporabna
     * @param generatedSource oznaka vira za novo generiran artefakt
     * @return bajti artefakta in vir, iz katerega so prišli
     */
    public ArtifactResult ensure(
            String path,
            MediaType contentType,
            Predicate<byte[]> validator,
            Supplier<byte[]> generator,
            ArtifactSource generatedSource
    ) throws IOException {
        if (localStorage.exists(path)) {
            byte[] bytes = localStorage.read(path);
            if (validator.test(bytes)) {
                return new ArtifactResult(path, bytes, ArtifactSource.LOCAL);
            }
        }

        byte[] remote = supabaseStorage.download(path);
        if (remote != null && validator.test(remote)) {
            localStorage.write(path, remote);
            return new ArtifactResult(path, remote, ArtifactSource.SUPABASE);
        }

        byte[] generated = generator.get();
        if (!validator.test(generated)) {
            throw new IllegalStateException("Generated artifact is invalid: " + path);
        }

        localStorage.write(path, generated);
        supabaseStorage.upload(path, generated, contentType);
        return new ArtifactResult(path, generated, generatedSource);
    }

    /**
     * Poskusi obnoviti artefakt iz Supabase v lokalni cache.
     *
     * @param path pot artefakta
     * @return true, če je bil artefakt najden in zapisan lokalno
     */
    public boolean restoreFromSupabase(String path) throws IOException {
        byte[] remote = supabaseStorage.download(path);
        if (remote == null || remote.length == 0) {
            return false;
        }

        localStorage.write(path, remote);
        return true;
    }

    /**
     * Naloži artefakt v Supabase cache, če je oddaljeni cache omogočen.
     *
     * @param path pot artefakta
     * @param bytes vsebina artefakta
     * @param contentType MIME tip vsebine
     */
    public void upload(String path, byte[] bytes, MediaType contentType) {
        supabaseStorage.upload(path, bytes, contentType);
    }

    /**
     * Rezultat pridobivanja cache artefakta.
     *
     * @param path pot artefakta
     * @param bytes vsebina artefakta
     * @param source vir podatkov, ki je bil uporabljen
     */
    public record ArtifactResult(
        String path,
        byte[] bytes,
        ArtifactSource source
    ) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ArtifactResult that = (ArtifactResult) o;
            return source == that.source &&
                    Objects.equals(path, that.path) &&
                    Arrays.equals(bytes, that.bytes);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(path, source);
            result = 31 * result + Arrays.hashCode(bytes);
            return result;
        }

        @Override
        public String toString() {
            return "ArtifactResult{" +
                    "path='" + path + '\'' +
                    ", bytes=" + Arrays.toString(bytes) +
                    ", source=" + source +
                    '}';
        }
    }

}
