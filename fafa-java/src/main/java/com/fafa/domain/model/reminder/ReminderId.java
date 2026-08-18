package com.fafa.domain.model.reminder;

import lombok.Data;

import java.io.Serializable;

/**
 * 提醒 ID 值对象
 *
 * @author FaFa
 * @since 2026-08-18
 */
@Data
public class ReminderId implements Serializable {
    
    private Long value;
    
    public ReminderId() {
    }
    
    public ReminderId(Long value) {
        this.value = value;
    }
    
    public static ReminderId of(Long id) {
        ReminderId reminderId = new ReminderId();
        reminderId.setValue(id);
        return reminderId;
    }
}
