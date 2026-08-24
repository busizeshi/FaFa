package com.fafa.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pet_member")
public class PetMemberPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long petId;
    private Long userId;
    private String role;
    private String status;
    private Long grantedBy;
    private LocalDateTime joinedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
