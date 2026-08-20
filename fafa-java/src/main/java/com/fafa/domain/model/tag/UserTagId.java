package com.fafa.domain.model.tag;

import lombok.Value;

import java.io.Serializable;

/**
 * 用户标签ID值对象
 *
 * @author FaFa
 * @since 2026-08-20
 */
@Value
public class UserTagId implements Serializable {

    Long value;

    public static UserTagId of(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("标签ID无效");
        }
        return new UserTagId(value);
    }
}
