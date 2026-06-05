package com.sibam.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Lokalna datotečna shramba za cache artefakte.
 *
 * Zapisi potekajo prek začasne datoteke in atomskega premika, da cache ne ostane
 * v delno zapisani obliki.
 */
@Component
public class LocalArtifactStorage {

    private final Path root;

    @Autowired
    public LocalArtifactStorage(@Value("${cache.local-root:data/cache}") String localRoot) {
        this(Path.of(localRoot));
    }

    LocalArtifactStorage(Path root) {
        this.root = root;
    }

    public Path root() {
        return root;
    }

    /**
     * Razreši relativno cache pot glede na lokalni root.
     *
     * @param path relativna pot artefakta
     * @return normalizirana datotečna pot
     */
    public Path resolve(String path) {
        return root.resolve(path).normalize();
    }

    public boolean exists(String path) {
        return Files.exists(resolve(path));
    }

    /**
     * Prebere artefakt iz lokalnega cache-a.
     *
     * @param path relativna pot artefakta
     * @return vsebina datoteke
     */
    public byte[] read(String path) throws IOException {
        return Files.readAllBytes(resolve(path));
    }

    /**
     * Zapiše artefakt v lokalni cache prek začasne datoteke.
     *
     * @param path relativna pot artefakta
     * @param bytes vsebina za zapis
     */
    public void write(String path, byte[] bytes) throws IOException {
        Path target = resolve(path);
        Files.createDirectories(target.getParent());
        Path tmp = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
        Files.write(tmp, bytes);
        moveAtomically(tmp, target);
    }

    public void delete(String path) throws IOException {
        Files.deleteIfExists(resolve(path));
    }

    /**
     * Preveri, ali lokalni artefakt obstaja in ni prazen.
     *
     * @param path relativna pot artefakta
     * @return true, če je datoteka uporabna
     */
    public boolean validate(String path) {
        Path target = resolve(path);
        try {
            return Files.isRegularFile(target) && Files.size(target) > 0;
        } catch (IOException e) {
            return false;
        }
    }

    private void moveAtomically(Path tmp, Path target) throws IOException {
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
