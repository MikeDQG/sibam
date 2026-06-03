package com.sibam.cache;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ArtifactCacheServiceTest {

    private final LocalArtifactStorage localStorage = mock(LocalArtifactStorage.class);
    private final SupabaseArtifactStorage supabaseStorage = mock(SupabaseArtifactStorage.class);
    private final ArtifactCacheService service = new ArtifactCacheService(localStorage, supabaseStorage);

    private static final byte[] VALID_BYTES = {1, 2, 3};
    private static final byte[] OTHER_BYTES = {4, 5, 6};

    // --- ensure() ---

    @Test
    void returnsLocalWhenLocalExistsAndValid() throws IOException {
        when(localStorage.exists("model.onnx")).thenReturn(true);
        when(localStorage.read("model.onnx")).thenReturn(VALID_BYTES);

        ArtifactCacheService.ArtifactResult result = service.ensure(
                "model.onnx", MediaType.APPLICATION_OCTET_STREAM,
                bytes -> bytes.length > 0, () -> OTHER_BYTES, ArtifactSource.GENERATED
        );

        assertThat(result.source()).isEqualTo(ArtifactSource.LOCAL);
        assertThat(result.bytes()).isEqualTo(VALID_BYTES);
        verifyNoInteractions(supabaseStorage);
    }

    @Test
    void fallsBackToSupabaseWhenLocalInvalid() throws IOException {
        when(localStorage.exists("model.onnx")).thenReturn(true);
        when(localStorage.read("model.onnx")).thenReturn(new byte[0]);
        when(supabaseStorage.download("model.onnx")).thenReturn(VALID_BYTES);

        ArtifactCacheService.ArtifactResult result = service.ensure(
                "model.onnx", MediaType.APPLICATION_OCTET_STREAM,
                bytes -> bytes.length > 0, () -> OTHER_BYTES, ArtifactSource.GENERATED
        );

        assertThat(result.source()).isEqualTo(ArtifactSource.SUPABASE);
        assertThat(result.bytes()).isEqualTo(VALID_BYTES);
        verify(localStorage).write("model.onnx", VALID_BYTES);
    }

    @Test
    void writesRemoteToLocalWhenFetchedFromSupabase() throws IOException {
        when(localStorage.exists("model.onnx")).thenReturn(false);
        when(supabaseStorage.download("model.onnx")).thenReturn(VALID_BYTES);

        service.ensure(
                "model.onnx", MediaType.APPLICATION_OCTET_STREAM,
                bytes -> bytes.length > 0, () -> OTHER_BYTES, ArtifactSource.GENERATED
        );

        verify(localStorage).write("model.onnx", VALID_BYTES);
    }

    @Test
    void generatesWhenBothStoragesMiss() throws IOException {
        when(localStorage.exists("model.onnx")).thenReturn(false);
        when(supabaseStorage.download("model.onnx")).thenReturn(null);

        ArtifactCacheService.ArtifactResult result = service.ensure(
                "model.onnx", MediaType.APPLICATION_OCTET_STREAM,
                bytes -> bytes.length > 0, () -> VALID_BYTES, ArtifactSource.GENERATED
        );

        assertThat(result.source()).isEqualTo(ArtifactSource.GENERATED);
        assertThat(result.bytes()).isEqualTo(VALID_BYTES);
        verify(localStorage).write("model.onnx", VALID_BYTES);
        verify(supabaseStorage).upload("model.onnx", VALID_BYTES, MediaType.APPLICATION_OCTET_STREAM);
    }

    @Test
    void throwsWhenGeneratedBytesAreInvalid() {
        when(localStorage.exists("model.onnx")).thenReturn(false);
        when(supabaseStorage.download("model.onnx")).thenReturn(null);

        assertThatThrownBy(() -> service.ensure(
                "model.onnx", MediaType.APPLICATION_OCTET_STREAM,
                bytes -> bytes.length > 0, () -> new byte[0], ArtifactSource.GENERATED
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("model.onnx");
    }

    // --- restoreFromSupabase() ---

    @Test
    void restoreReturnsFalseWhenRemoteIsNull() throws IOException {
        when(supabaseStorage.download("model.onnx")).thenReturn(null);

        assertThat(service.restoreFromSupabase("model.onnx")).isFalse();
        verifyNoMoreInteractions(localStorage);
    }

    @Test
    void restoreReturnsFalseWhenRemoteIsEmpty() throws IOException {
        when(supabaseStorage.download("model.onnx")).thenReturn(new byte[0]);

        assertThat(service.restoreFromSupabase("model.onnx")).isFalse();
        verifyNoMoreInteractions(localStorage);
    }

    @Test
    void restoreReturnsTrueAndWritesWhenRemoteHasData() throws IOException {
        when(supabaseStorage.download("model.onnx")).thenReturn(VALID_BYTES);

        assertThat(service.restoreFromSupabase("model.onnx")).isTrue();
        verify(localStorage).write("model.onnx", VALID_BYTES);
    }

    // --- ArtifactResult equals/hashCode/toString ---

    @Test
    void artifactResultEqualsByContentNotReference() {
        byte[] a = {1, 2, 3};
        byte[] b = {1, 2, 3};
        ArtifactCacheService.ArtifactResult r1 = new ArtifactCacheService.ArtifactResult("p", a, ArtifactSource.LOCAL);
        ArtifactCacheService.ArtifactResult r2 = new ArtifactCacheService.ArtifactResult("p", b, ArtifactSource.LOCAL);

        assertThat(r1).isEqualTo(r2);
    }

    @Test
    void artifactResultNotEqualWhenBytesDiffer() {
        ArtifactCacheService.ArtifactResult r1 = new ArtifactCacheService.ArtifactResult("p", new byte[]{1}, ArtifactSource.LOCAL);
        ArtifactCacheService.ArtifactResult r2 = new ArtifactCacheService.ArtifactResult("p", new byte[]{2}, ArtifactSource.LOCAL);

        assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    void artifactResultHashCodeConsistentWithEquals() {
        byte[] a = {1, 2, 3};
        byte[] b = {1, 2, 3};
        ArtifactCacheService.ArtifactResult r1 = new ArtifactCacheService.ArtifactResult("p", a, ArtifactSource.LOCAL);
        ArtifactCacheService.ArtifactResult r2 = new ArtifactCacheService.ArtifactResult("p", b, ArtifactSource.LOCAL);

        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
    }

    @Test
    void artifactResultToStringIsReadable() {
        ArtifactCacheService.ArtifactResult r = new ArtifactCacheService.ArtifactResult("p", new byte[]{1, 2}, ArtifactSource.LOCAL);
        String str = r.toString();

        assertThat(str).contains("p").contains("LOCAL").doesNotContain("[B@");
    }
}
