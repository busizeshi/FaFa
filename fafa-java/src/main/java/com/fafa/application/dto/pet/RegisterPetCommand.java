package com.fafa.application.dto.pet;

import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 注册宠物命令
 * <p>
 * photos 为按位序上传的三张照片（位1=头像、位2=正面照、位3=侧面照），创建宠物时必传。
 *
 * @author FaFa Team
 * @since 1.0
 */
public record RegisterPetCommand(
    String name,
    String type,
    String breed,
    String gender,
    LocalDate birthDate,
    Boolean neutered,
    LocalDate adoptDate,
    BigDecimal weight,
    List<MultipartFile> photos
) {
}
