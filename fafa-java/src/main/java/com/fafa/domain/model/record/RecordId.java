package com.fafa.domain.model.record;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 记录 ID 值对象（通用）
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecordId implements Serializable {
    
    private Long value;
    
    public static RecordId of(Long id) {
        return new RecordId(id);
    }
}
