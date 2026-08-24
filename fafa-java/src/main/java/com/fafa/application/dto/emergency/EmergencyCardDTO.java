package com.fafa.application.dto.emergency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmergencyCardDTO(PetSummary pet, ProfileSummary profile, Contact emergencyContact,
                               Hospital primaryHospital, String sourceStatus, LocalDateTime updatedAt) {
    public record PetSummary(Long id, String name, String type, String breed, String gender,
                             LocalDate birthDate, BigDecimal weight) { }
    public record ProfileSummary(String bloodType, String allergies, String chronicConditions, String notes) { }
    public record Contact(String name, String phone) { }
    public record Hospital(String name, String phone, String address) { }
}
