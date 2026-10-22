package com.fafa.biz.pet.domain;

import com.fafa.common.error.CommonErrorCode;
import com.fafa.common.error.FafaException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;
import jakarta.validation.constraints.*;

/** 宠物资料和生命周期值对象；体重由后续照护记录模块提供。 */
public final class PetModels {
    private PetModels() { }
    public enum Species { CAT, DOG, OTHER }
    public enum Sex { MALE, FEMALE, UNKNOWN }
    public enum Neutered { YES, NO, UNKNOWN }
    public enum Status { ACTIVE, ARCHIVED, DELETION_PENDING, PURGING, DELETED }
    public enum ClearField { BREED, BIRTH_DATE, ESTIMATED_BIRTH_MONTH, ARRIVED_ON }
    public record Profile(@NotBlank @Size(max = 30) String nickname, @NotNull Species species,
                          @Size(max = 64) String breedId, Sex sex, Neutered neuteredStatus,
                          LocalDate birthDate, @Pattern(regexp = "[0-9]{4}-[0-9]{2}") String estimatedBirthMonth,
                          LocalDate arrivedOn) {
        public Profile {
            nickname = nickname == null ? null : nickname.strip();
            sex = sex == null ? Sex.UNKNOWN : sex;
            neuteredStatus = neuteredStatus == null ? Neutered.UNKNOWN : neuteredStatus;
        }
        public void validate(LocalDate today) {
            if (nickname == null || nickname.isBlank() || nickname.codePointCount(0, nickname.length()) > 30
                    || species == null || (birthDate != null && estimatedBirthMonth != null)) invalid();
            if (birthDate != null && (birthDate.getYear() < 1 || birthDate.isAfter(today))) invalid();
            if (arrivedOn != null && (arrivedOn.getYear() < 1 || arrivedOn.isAfter(today))) invalid();
            if (birthDate != null && arrivedOn != null && arrivedOn.isBefore(birthDate)) invalid();
            if (estimatedBirthMonth != null) {
                try {
                    var month = YearMonth.parse(estimatedBirthMonth);
                    if (month.getYear() < 1 || month.isAfter(YearMonth.from(today))
                            || (arrivedOn != null && month.isAfter(YearMonth.from(arrivedOn)))) invalid();
                } catch (java.time.format.DateTimeParseException exception) { invalid(); }
            }
        }
    }
    public record Patch(@Positive long version, @Size(min = 1, max = 30) String nickname,
                        Species species, @Size(max = 64) String breedId, Sex sex, Neutered neuteredStatus,
                        LocalDate birthDate, @Pattern(regexp = "[0-9]{4}-[0-9]{2}") String estimatedBirthMonth,
                        LocalDate arrivedOn, Set<ClearField> clearFields) {
        public Patch { clearFields = clearFields == null ? Set.of() : Set.copyOf(clearFields); }
        public Profile merge(Profile old) {
            return new Profile(nickname == null ? old.nickname() : nickname, species == null ? old.species() : species,
                    choose(ClearField.BREED, breedId, old.breedId()), sex == null ? old.sex() : sex,
                    neuteredStatus == null ? old.neuteredStatus() : neuteredStatus,
                    choose(ClearField.BIRTH_DATE, birthDate, old.birthDate()),
                    choose(ClearField.ESTIMATED_BIRTH_MONTH, estimatedBirthMonth, old.estimatedBirthMonth()),
                    choose(ClearField.ARRIVED_ON, arrivedOn, old.arrivedOn()));
        }
        private <T> T choose(ClearField field, T input, T old) {
            if (clearFields.contains(field) && input != null) invalid();
            return clearFields.contains(field) ? null : input == null ? old : input;
        }
    }
    public record Pet(String id, String ownerUserId, String createdBy, Profile profile, Status status, long version) { }
    public record Preferences(String currentPetId, String defaultPetId, long version) { }
    public record PreferenceChange(@Positive long version, String currentPetId, String defaultPetId) { }
    public record Version(@Positive long version) { }
    public record Delete(@Positive long version, @AssertTrue boolean acknowledgement) { }
    private static void invalid() {
        throw new FafaException(CommonErrorCode.INVALID_ARGUMENT, "请核对宠物资料、日期及清空字段");
    }
}
