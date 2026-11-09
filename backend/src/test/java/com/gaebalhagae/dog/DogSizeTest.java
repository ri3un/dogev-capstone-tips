package com.gaebalhagae.dog;

import static org.assertj.core.api.Assertions.assertThat;

import com.gaebalhagae.dog.domain.DogSize;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DogSizeTest {

    @ParameterizedTest
    @CsvSource({"3, SMALL", "9.99, SMALL", "10, MEDIUM", "24.9, MEDIUM", "25, LARGE", "40, LARGE"})
    void 몸무게로_크기를_나눈다(String kg, DogSize expected) {
        assertThat(DogSize.of(new BigDecimal(kg))).isEqualTo(expected);
    }
}
