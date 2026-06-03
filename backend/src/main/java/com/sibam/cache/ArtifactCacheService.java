package com.sibam.cache;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.Arrays;
import java.util.Objects;

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

    public boolean restoreFromSupabase(String path) throws IOException {
        byte[] remote = supabaseStorage.download(path);
        if (remote == null || remote.length == 0) {
            return false;
        }

        localStorage.write(path, remote);
        return true;
    }

    public void upload(String path, byte[] bytes, MediaType contentType) {
        supabaseStorage.upload(path, bytes, contentType);
    }

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
