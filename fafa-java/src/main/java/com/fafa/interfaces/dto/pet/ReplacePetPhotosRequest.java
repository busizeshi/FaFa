package com.fafa.interfaces.dto.pet;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 替换宠物三视图请求（multipart/form-data）
 * <p>
 * 每个槽位二选一：上传新文件（avatarFile/frontFile/sideFile），
 * 或传入现有照片 URL（avatarUrl/frontUrl/sideUrl，仅调位序时免重传）。
 * 位1=头像、位2=正面照、位3=侧面照，三张缺一不可。
 *
 * @author FaFa Team
 * @since 1.0
 */
@Data
@Schema(description = "替换宠物三视图请求")
public class ReplacePetPhotosRequest {

    @Schema(description = "位1头像新文件")
    private MultipartFile avatarFile;

    @Schema(description = "位1头像复用的现有照片URL")
    private String avatarUrl;

    @Schema(description = "位2正面照新文件")
    private MultipartFile frontFile;

    @Schema(description = "位2正面照复用的现有照片URL")
    private String frontUrl;

    @Schema(description = "位3侧面照新文件")
    private MultipartFile sideFile;

    @Schema(description = "位3侧面照复用的现有照片URL")
    private String sideUrl;
}
