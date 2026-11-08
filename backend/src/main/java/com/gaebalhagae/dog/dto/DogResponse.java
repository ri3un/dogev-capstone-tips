package com.gaebalhagae.dog.dto;

import com.gaebalhagae.dog.domain.ActivityLevel;
import com.gaebalhagae.dog.domain.Dog;
import com.gaebalhagae.dog.domain.DogSize;
import com.gaebalhagae.dog.domain.Sex;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DogResponse(Long id, String name, String breed, BigDecimal weightKg, DogSize size, String birthYm,
                          int ageMonths, Sex sex, boolean neutered, boolean hasCarrier, boolean hasStroller,
                          ActivityLevel activityLevel, List<String> diseases, List<String> allergies,
                          Integer foodKcalPerKg, String photoUrl, Long createdBy) {

    public static DogResponse of(Dog d, LocalDate today) {
        return new DogResponse(d.getId(), d.getName(), d.getBreed(), d.getWeightKg(), d.size(), d.getBirthYm(),
                d.ageMonths(today), d.getSex(), d.isNeutered(), d.isHasCarrier(), d.isHasStroller(),
                d.getActivityLevel(), List.copyOf(d.getDiseases()), List.copyOf(d.getAllergies()),
                d.getFoodKcalPerKg(), d.getPhotoUrl(), d.getCreatedBy());
    }
}
