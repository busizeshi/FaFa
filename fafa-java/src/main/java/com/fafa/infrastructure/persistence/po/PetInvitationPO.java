package com.fafa.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pet_invitation")
public class PetInvitationPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long petId;
    private Long createdBy;
    private String role;
    private String tokenHash;
    private LocalDateTime expiresAt;
    private LocalDateTime memberExpiresAt;
    private Integer maxUses;
    private Integer usedCount;
    private String status;
    private Long acceptedBy;
    private LocalDateTime acceptedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
