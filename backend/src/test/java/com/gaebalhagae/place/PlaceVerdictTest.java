package com.gaebalhagae.place;

import static org.assertj.core.api.Assertions.assertThat;

import com.gaebalhagae.dog.domain.DogSize;
import com.gaebalhagae.place.service.PlaceVerdict;
import com.gaebalhagae.place.service.PlaceVerdict.DogInfo;
import com.gaebalhagae.place.service.PlaceVerdict.Level;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PlaceVerdictTest {

    private final DogInfo small = new DogInfo(DogSize.SMALL, new BigDecimal("4"), false, false);
    private final DogInfo large = new DogInfo(DogSize.LARGE, new BigDecimal("30"), false, false);

    private static PlaceVerdict.Cond c(String value) {
        return new PlaceVerdict.Cond(value, null);
    }

    @Test
    void 크기가_맞지_않으면_어려움() {
        var v = PlaceVerdict.judge(large, Map.of("size_allowed", c("small_only")));
        assertThat(v.level()).isEqualTo(Level.HARD);
        assertThat(v.reasons()).isNotEmpty();
    }

    @Test
    void 몸무게_제한을_넘으면_어려움() {
        var v = PlaceVerdict.judge(small, Map.of("size_allowed", new PlaceVerdict.Cond("up_to_large", 3)));
        assertThat(v.level()).isEqualTo(Level.HARD);
    }

    @Test
    void 크기_정보가_없거나_엇갈리면_확인_필요() {
        assertThat(PlaceVerdict.judge(small, Map.of()).level()).isEqualTo(Level.CHECK);
        assertThat(PlaceVerdict.judge(small, Map.of("size_allowed", c("NEEDS_CHECK"))).level()).isEqualTo(Level.CHECK);
    }

    @Test
    void 이동장이_필요한데_없으면_확인_필요() {
        var v = PlaceVerdict.judge(small, Map.of("size_allowed", c("small_only"), "carrier_required", c("yes")));
        assertThat(v.level()).isEqualTo(Level.CHECK);
    }

    @Test
    void 조건이_맞으면_OK() {
        var withCarrier = new DogInfo(DogSize.SMALL, new BigDecimal("4"), true, false);
        assertThat(PlaceVerdict.judge(withCarrier,
                Map.of("size_allowed", c("small_only"), "carrier_required", c("yes"))).level()).isEqualTo(Level.OK);
    }
}
