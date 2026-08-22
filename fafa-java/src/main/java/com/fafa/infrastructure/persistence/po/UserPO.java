package com.fafa.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户持久化对象
 *
 * @author FaFa Team
 * @since 1.0
 */
@Data
@TableName("user")
public class UserPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String openId;

    private String nickname;

    private String phone;

    private String gender;

    private Integer age;

    private String avatarUrl;

    private String city;

    @TableField("is_vip")
    private Boolean isVip;

    private LocalDateTime vipExpireTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean deleted;
}
