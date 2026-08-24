package com.fafa.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pet_emergency_profile")
public class PetEmergencyProfilePO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long petId;
    private String bloodType;
    private String allergies;
    private String chronicConditions;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String primaryHospitalName;
    private String primaryHospitalPhone;
    private String primaryHospitalAddress;
    private String notes;
    private Integer version;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
