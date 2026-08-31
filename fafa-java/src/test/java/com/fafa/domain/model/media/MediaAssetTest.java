package com.fafa.domain.model.media;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** MediaAsset 领域规则测试。 */
class MediaAssetTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 23, 10, 0);

    @Test
    @DisplayName("照片创建为 pending，标签去重去空，指定宠物直接确认归属")
    void createPhoto_shouldNormalizeTagsAndConfirmPet() {
        MediaAsset asset = MediaAsset.create(1L, MediaType.PHOTO, "https://media/photo.jpg", "photo.jpg",
                100L, "image/jpeg", List.of(" 晒太阳 ", "晒太阳", ""), 8L, NOW, NOW);

        assertThat(asset.getStatus()).isEqualTo(MediaAnalysisStatus.PENDING);
        assertThat(asset.getTags()).containsExactly("晒太阳");
        assertThat(asset.getPetId()).isEqualTo(8L);
        assertThat(asset.isRecognitionConfirmed()).isTrue();
    }

    @Test
    @DisplayName("照片和视频创建为 pending，分析回调可回填疑似归属")
    void analysis_shouldCompleteAndRecognizePet() {
        MediaAsset asset = MediaAsset.create(1L, MediaType.PHOTO, "https://media/photo.jpg", "photo.jpg",
                100L, "image/jpeg", null, null, NOW, NOW);
        asset.markAnalysisCompleted("一只猫", List.of("猫", "阳台"), 8L, new BigDecimal("0.91"));

        assertThat(asset.getStatus()).isEqualTo(MediaAnalysisStatus.COMPLETED);
        assertThat(asset.getPetId()).isEqualTo(8L);
        assertThat(asset.isRecognitionConfirmed()).isFalse();
        assertThat(asset.getAiTags()).containsExactly("猫", "阳台");

        MediaAsset video = MediaAsset.create(1L, MediaType.VIDEO, "https://media/video.mp4", "video.mp4",
                100L, "video/mp4", null, null, NOW, NOW);
        assertThat(video.getStatus()).isEqualTo(MediaAnalysisStatus.PENDING);
        video.markAnalysisCompleted("一只猫在视频中玩耍", List.of("玩耍"), null, null);
        assertThat(video.getStatus()).isEqualTo(MediaAnalysisStatus.COMPLETED);
    }

    @Test
    @DisplayName("终态回调幂等，确认/清除归属符合规则")
    void terminalAndPetConfirmation_shouldBeIdempotent() {
        MediaAsset asset = MediaAsset.create(1L, MediaType.PHOTO, "https://media/photo.jpg", "photo.jpg",
                100L, "image/jpeg", null, null, NOW, NOW);
        asset.markAnalysisCompleted("首次", null, 8L, new BigDecimal("0.90"));
        asset.markAnalysisCompleted("重复", null, 9L, new BigDecimal("0.99"));
        assertThat(asset.getAiDescription()).isEqualTo("首次");
        assertThat(asset.getPetId()).isEqualTo(8L);

        asset.confirmPet(9L);
        assertThat(asset.isRecognitionConfirmed()).isTrue();
        asset.confirmPet(null);
        assertThat(asset.getPetId()).isNull();
        assertThat(asset.isRecognitionConfirmed()).isFalse();

        asset.clearPetReference();
        assertThat(asset.getPetId()).isNull();
    }

    @Test
    @DisplayName("重新分析会清理旧 AI 结果并回到 pending")
    void requestAnalysis_shouldResetAnalysisState() {
        MediaAsset asset = MediaAsset.create(1L, MediaType.VIDEO, "https://media/video.mp4", "video.mp4",
                100L, "video/mp4", null, null, NOW, NOW);
        asset.markAnalysisCompleted("旧描述", List.of("旧标签"), 8L, new BigDecimal("0.91"));

        asset.requestAnalysis(NOW.plusMinutes(1));

        assertThat(asset.getStatus()).isEqualTo(MediaAnalysisStatus.PENDING);
        assertThat(asset.getAiDescription()).isNull();
        assertThat(asset.getAiTags()).isNull();
        assertThat(asset.getPetId()).isNull();
        assertThat(asset.getRecognitionScore()).isNull();
    }

    @Test
    @DisplayName("素材标签数量和长度超限时拒绝")
    void create_shouldRejectInvalidTags() {
        assertThatThrownBy(() -> MediaAsset.create(1L, MediaType.PHOTO, "https://media/photo.jpg", "photo.jpg",
                100L, "image/jpeg", List.of("a".repeat(33)), null, NOW, NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MediaAsset.create(1L, MediaType.PHOTO, "https://media/photo.jpg", "photo.jpg",
                100L, "image/jpeg", java.util.stream.IntStream.range(0, 11).mapToObj(String::valueOf).toList(),
                null, NOW, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("自动宠物标签可以超过手工标签上限，并支持多宠物结果")
    void vectorization_shouldAppendAllPetTags() {
        MediaAsset asset = MediaAsset.create(1L, MediaType.PHOTO, "https://media/photo.jpg", "photo.jpg",
                100L, "image/jpeg", java.util.stream.IntStream.range(0, 10)
                        .mapToObj(String::valueOf).toList(), null, NOW, NOW);

        assertThat(asset.markVectorizationCompleted(List.of(8L, 9L), new BigDecimal("0.92"), NOW.plusMinutes(1)))
                .isTrue();
        asset.addTags(List.of("豆包", "团子"));

        assertThat(asset.getPetId()).isNull();
        assertThat(asset.getTags()).contains("豆包", "团子");
        assertThat(asset.getTags()).hasSize(12);
        assertThat(asset.getAiDescription()).isNull();
        assertThat(asset.getAiTags()).isNull();
    }
}
