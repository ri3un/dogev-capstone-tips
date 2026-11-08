package com.gaebalhagae.dog.dto;

import com.gaebalhagae.dog.domain.ActivityLevel;
import com.gaebalhagae.dog.domain.Dog;
import com.gaebalhagae.dog.domain.Sex;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/** 필수 6개(이름·견종·몸무게·생년월·성별·중성화) + 선택 (결정 7). 수정(PATCH)도 화면 전체 값을 보냅니다 */
public record DogRequest(
        @NotBlank @Size(max = 30) String name,
        @NotBlank @Size(max = 50) String breed,
        @NotNull @DecimalMin("0.5") @DecimalMax("100") BigDecimal weightKg,
        @NotBlank @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "YYYY-MM 형식이어야 합니다") String birthYm,
        @NotNull Sex sex,
        @NotNull Boolean neutered,
        Boolean hasCarrier,
        Boolean hasStroller,
        ActivityLevel activityLevel,
        @Size(max = 10) List<@NotBlank @Size(max = 30) String> diseases,
        @Size(max = 10) List<@NotBlank @Size(max = 30) String> allergies,
        @Min(1000) @Max(10000) Integer foodKcalPerKg
) {

    public Dog.Profile toProfile() {
        return new Dog.Profile(name, breed, weightKg, birthYm, sex, neutered,
                Boolean.TRUE.equals(hasCarrier), Boolean.TRUE.equals(hasStroller), activityLevel,
                diseases, allergies, foodKcalPerKg);
    }
}
