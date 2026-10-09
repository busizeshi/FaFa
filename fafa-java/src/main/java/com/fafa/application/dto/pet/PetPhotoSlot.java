package com.fafa.application.dto.pet;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

/**
 * 三视图槽位（位1头像 / 位2正面照 / 位3侧面照）
 * <p>
 * 每一位要么传新文件（file），要么引用宠物当前已有的照片 URL（reuseUrl，仅调整位序时免重复上传），
 * 二者必须且只能提供其一。
 *
 * @author FaFa Team
 * @since 4.0
 */
@Schema(description = "三视图槽位：新文件与已有照片引用二选一")
public record PetPhotoSlot(
    @Schema(description = "新照片文件")
    MultipartFile file,
    @Schema(description = "复用宠物当前已有照片的 URL（仅调位序时使用）")
    String reuseUrl
) {

    /**
     * 是否提供了新文件
     */
    public boolean hasFile() {
        return file != null && !file.isEmpty();
    }

    /**
     * 是否提供了复用引用
     */
    public boolean hasReuseUrl() {
        return reuseUrl != null && !reuseUrl.isBlank();
    }
}
