package com.fafa.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 带逻辑删除的 DO 基类
 *
 * 仅用户、宠物、照片、提醒等需要回溯的表继承；
 * 行为流水类表按需继承 {@link BaseDO}。
 *
 * @author FaFa Team
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class SoftDeleteDO extends BaseDO {

    /** 逻辑删除标记：0 未删除，1 已删除 */
    @TableLogic
    private Integer isDeleted;
}
