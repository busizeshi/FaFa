package com.fafa.application.dto.emergency;

import java.time.LocalDateTime;

public record EmergencyProfileDTO(Long petId, String bloodType, String allergies,
                                  String chronicConditions, String emergencyContactName,
                                  String emergencyContactPhone, String primaryHospitalName,
                                  String primaryHospitalPhone, String primaryHospitalAddress,
                                  String notes, int version, LocalDateTime updatedAt) {
}
