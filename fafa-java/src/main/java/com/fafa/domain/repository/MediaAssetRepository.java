package com.fafa.domain.repository;

import com.fafa.domain.model.media.MediaAsset;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 媒体素材仓储接口
 *
 * @author FaFa Team
 * @since 4.0
 */
public interface MediaAssetRepository {

    Optional<MediaAsset> findById(Long id);

    MediaAsset save(MediaAsset asset);

    /**
     * 逻辑删除
     */
    void deleteById(Long id);

    /**
     * 确定性检索（tags 任一命中，OR 语义），按 takenAt 倒序
     *
     * @param petTagName 宠物名称标签过滤，可空。媒体归属以 tags 为唯一业务标记
     * @param tags      标签过滤（任一命中），可空
     * @param startDate 拍摄日期起（含），可空
     * @param endDate   拍摄日期止（含），可空
     * @param offset    偏移
     * @param limit     条数
     */
    List<MediaAsset> search(Long userId, String petTagName, List<String> tags,
                            LocalDate startDate, LocalDate endDate, int offset, int limit);

    /**
     * 确定性检索总数（配合 search 分页）
     */
    long countSearch(Long userId, String petTagName, List<String> tags, LocalDate startDate, LocalDate endDate);

    /**
     * 语义检索结果批量补全（过滤归属与逻辑删除，免疫 Qdrant 残留 point）
     */
    List<MediaAsset> findByIds(Long userId, List<Long> ids);

    /**
     * 待确认归属列表（petId 非空且未确认）
     */
    List<MediaAsset> findUnconfirmed(Long userId, int limit);

    /**
     * 宠物删除级联：批量清空归属（素材保留）
     *
     * @return 受影响行数
     */
    int clearPetReference(Long petId);

    /**
     * 宠物改名后批量迁移该用户媒体中的宠物名称标签。
     *
     * @return 受影响素材数量
     */
    int replacePetNameTag(Long userId, String oldName, String newName);

}
