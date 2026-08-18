package com.fafa.domain.model.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户 ID 值对象
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserId implements Serializable {
    
    private Long value;
    
    public static UserId of(Long id) {
        return new UserId(id);
    }
}
