package com.sibam.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class LocalArtifactStorageTest {

    @TempDir
    Path tempDir;

    private LocalArtifactStorage storage() {
        return new LocalArtifactStorage(tempDir);
    }

    // --- exists ---

    @Test
    void existsReturnsFalseForMissingFile() {
        assertThat(storage().exists("missing.bin")).isFalse();
    }

    @Test
    void existsReturnsTrueAfterWrite() throws IOException {
        LocalArtifactStorage s = storage();
        s.write("model.onnx", new byte[]{1, 2, 3});
        assertThat(s.exists("model.onnx")).isTrue();
    }

    // --- write + read ---

    @Test
    void writeAndReadRoundtrip() throws IOException {
        byte[] data = {10, 20, 30, 40};
        LocalArtifactStorage s = storage();
        s.write("data.bin", data);
        assertThat(s.read("data.bin")).isEqualTo(data);
    }

    @Test
    void writeCreatesParentDirectoriesAutomatically() throws IOException {
        storage().write("subdir/nested/model.onnx", new byte[]{1});
        assertThat(Files.exists(tempDir.resolve("subdir/nested/model.onnx"))).isTrue();
    }

    @Test
    void writeOverwritesExistingFile() throws IOException {
        LocalArtifactStorage s = storage();
        s.write("file.bin", new byte[]{1, 2, 3});
        s.write("file.bin", new byte[]{9, 8});
        assertThat(s.read("file.bin")).isEqualTo(new byte[]{9, 8});
    }

    // --- delete ---

    @Test
    void deleteRemovesExistingFile() throws IOException {
        LocalArtifactStorage s = storage();
        s.write("file.bin", new byte[]{1});
        s.delete("file.bin");
        assertThat(s.exists("file.bin")).isFalse();
    }

    @Test
    void deleteMissingFileDoesNotThrow() {
        assertThatCode(() -> storage().delete("nonexistent.bin")).doesNotThrowAnyException();
    }

    // --- validate ---

    @Test
    void validateReturnsFalseForMissingFile() {
        assertThat(storage().validate("missing.bin")).isFalse();
    }

    @Test
    void validateReturnsFalseForEmptyFile() throws IOException {
        LocalArtifactStorage s = storage();
        s.write("empty.bin", new byte[0]);
        assertThat(s.validate("empty.bin")).isFalse();
    }

    @Test
    void validateReturnsTrueForNonEmptyFile() throws IOException {
        LocalArtifactStorage s = storage();
        s.write("model.onnx", new byte[]{1, 2, 3});
        assertThat(s.validate("model.onnx")).isTrue();
    }

    // --- resolve + root ---

    @Test
    void resolveReturnsPathUnderRoot() {
        Path resolved = storage().resolve("models/model.onnx");
        assertThat(resolved.startsWith(tempDir)).isTrue();
        assertThat(resolved.getFileName().toString()).isEqualTo("model.onnx");
    }

    @Test
    void rootReturnsConfiguredDirectory() {
        assertThat(storage().root()).isEqualTo(tempDir);
    }
}
