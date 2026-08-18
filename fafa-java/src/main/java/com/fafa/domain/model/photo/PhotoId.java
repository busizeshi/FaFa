package com.fafa.domain.model.photo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 照片ID值对象
 *
 * @author FaFa
 * @since 2026-08-18
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhotoId implements Serializable {

    private Long value;

    public static PhotoId of(Long id) {
        return new PhotoId(id);
    }
}
