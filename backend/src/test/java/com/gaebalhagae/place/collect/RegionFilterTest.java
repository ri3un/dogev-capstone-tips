package com.gaebalhagae.place.collect;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RegionFilterTest {

    @Test
    void 안양_지명이_있으면_다른_지역명이_같이_있어도_남긴다() {
        assertThat(RegionFilter.isLocalOrUnknown("서울에서 범계까지 강아지랑 왔어요")).isTrue();
    }

    @Test
    void 다른_지역명만_있으면_뺀다() {
        assertThat(RegionFilter.isLocalOrUnknown("해운대 카페A 애견동반 후기")).isFalse();
    }

    @Test
    void 지역명이_없으면_남긴다() {
        assertThat(RegionFilter.isLocalOrUnknown("카페A 강아지랑 다녀왔어요")).isTrue();
    }
}
