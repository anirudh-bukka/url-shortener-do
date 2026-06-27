package com.example.urlshortener.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Fast, dependency-free unit tests for the Base62 codec. */
class Base62Test {

    @Test
    void encodesKnownValues() {
        assertThat(Base62.encode(0)).isEqualTo("0");
        assertThat(Base62.encode(61)).isEqualTo("z");
        assertThat(Base62.encode(62)).isEqualTo("10");
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 61L, 62L, 1000L, 1_000_000L, Long.MAX_VALUE})
    void encodeThenDecodeIsLossless(long value) {
        assertThat(Base62.decode(Base62.encode(value))).isEqualTo(value);
    }

    @Test
    void rejectsNegativeInput() {
        assertThatThrownBy(() -> Base62.encode(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
