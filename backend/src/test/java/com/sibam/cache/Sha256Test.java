package com.sibam.cache;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class Sha256Test {

    @Test
    void emptyBytesProducesKnownHash() {
        String hash = Sha256.hex(new byte[0]);
        assertThat(hash).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    void knownInputProducesKnownHash() {
        byte[] input = "hello".getBytes(StandardCharsets.UTF_8);
        String hash = Sha256.hex(input);
        assertThat(hash).isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
    }

    @Test
    void hashIsLowercase64HexChars() {
        String hash = Sha256.hex("test".getBytes(StandardCharsets.UTF_8));
        assertThat(hash).hasSize(64).matches("[0-9a-f]+");
    }

    @Test
    void sameInputProducesSameHash() {
        byte[] input = "consistent".getBytes(StandardCharsets.UTF_8);
        assertThat(Sha256.hex(input)).isEqualTo(Sha256.hex(input));
    }

    @Test
    void differentInputsProduceDifferentHashes() {
        assertThat(Sha256.hex("a".getBytes(StandardCharsets.UTF_8)))
                .isNotEqualTo(Sha256.hex("b".getBytes(StandardCharsets.UTF_8)));
    }
}
