package com.gaebalhagae.global.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class GeoTest {

    @Test
    void 위도_0_001도는_약_111m() {
        assertThat(Geo.distanceM(37.0, 127.0, 37.001, 127.0)).isCloseTo(111, within(1));
    }

    @Test
    void 같은_점은_0m() {
        assertThat(Geo.distanceM(37.39, 126.92, 37.39, 126.92)).isZero();
    }

    @Test
    void 걷는_시간은_시속_4km() {
        assertThat(Geo.walkMinutes(1000)).isEqualTo(15);
        assertThat(Geo.walkMinutes(0)).isZero();
    }
}
