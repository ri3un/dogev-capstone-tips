package com.gaebalhagae.place.service;

import com.gaebalhagae.dog.domain.Dog;
import com.gaebalhagae.dog.domain.DogSize;
import com.gaebalhagae.place.review.ConditionKey;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** "우리 강아지 기준" 맞춤 판정 한 줄: OK / 확인 필요 / 어려움 (멘토 피드백 2-3) */
public final class PlaceVerdict {

    private PlaceVerdict() {
    }

    public enum Level {
        OK("OK"), CHECK("확인 필요"), HARD("어려움");

        private final String label;

        Level(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public record DogInfo(DogSize size, BigDecimal weightKg, boolean hasCarrier, boolean hasStroller) {

        public static DogInfo of(Dog dog) {
            return new DogInfo(dog.size(), dog.getWeightKg(), dog.isHasCarrier(), dog.isHasStroller());
        }
    }

    public record Cond(String value, Integer weightLimitKg) {
    }

    public record Verdict(Level level, String label, List<String> reasons) {
    }

    public static Verdict judge(DogInfo dog, Map<String, Cond> conds) {
        List<String> hard = new ArrayList<>();
        List<String> check = new ArrayList<>();

        Cond size = conds.get(ConditionKey.SIZE_ALLOWED.key());
        DogSize allowed = size == null ? null : switch (size.value()) {
            case "small_only" -> DogSize.SMALL;
            case "up_to_medium" -> DogSize.MEDIUM;
            case "up_to_large" -> DogSize.LARGE;
            default -> null;
        };
        if (allowed == null) {
            check.add("입장 가능한 크기 정보가 확실하지 않아요");
        } else if (dog.size().ordinal() > allowed.ordinal()) {
            hard.add(allowed == DogSize.SMALL ? "소형견만 입장할 수 있어요" : "중형견까지만 입장할 수 있어요");
        }
        if (size != null && size.weightLimitKg() != null
                && dog.weightKg().compareTo(BigDecimal.valueOf(size.weightLimitKg())) > 0) {
            hard.add("몸무게 " + size.weightLimitKg() + "kg 이하만 입장할 수 있어요");
        }

        Cond carrier = conds.get(ConditionKey.CARRIER_REQUIRED.key());
        if (carrier != null && "yes".equals(carrier.value()) && !dog.hasCarrier() && !dog.hasStroller()) {
            check.add("이동장이나 유모차가 필요해요");
        }

        Level level = !hard.isEmpty() ? Level.HARD : !check.isEmpty() ? Level.CHECK : Level.OK;
        List<String> reasons = new ArrayList<>(hard);
        reasons.addAll(check);
        if (reasons.isEmpty()) {
            reasons.add("우리 강아지와 함께 갈 수 있어요");
        }
        return new Verdict(level, level.label(), reasons);
    }
}
