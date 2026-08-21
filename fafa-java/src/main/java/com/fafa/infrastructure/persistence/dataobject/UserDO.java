package com.fafa.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表 DO
 */
@Data
@TableName("`user`")
public class UserDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String openid;

    private String nickname;

    private String avatarUrl;

    private String phone;

    private Integer gender;

    private String city;

    private Integer memberLevel;

    private LocalDateTime memberExpireAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
