package com.fafa.domain.model.media;

import com.fafa.domain.model.tag.TagCategory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Phase 4 枚举解析测试。 */
class MediaEnumsTest {

    @Test
    void fromCode_shouldIgnoreCase() {
        assertThat(MediaType.fromCode("PHOTO")).isEqualTo(MediaType.PHOTO);
        assertThat(MediaAnalysisStatus.fromCode("completed")).isEqualTo(MediaAnalysisStatus.COMPLETED);
        assertThat(TagCategory.fromCode("pet")).isEqualTo(TagCategory.PET);
    }

    @Test
    void fromCode_shouldRejectUnknownValue() {
        assertThatThrownBy(() -> MediaType.fromCode("audio")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MediaAnalysisStatus.fromCode("unknown")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TagCategory.fromCode("unknown")).isInstanceOf(IllegalArgumentException.class);
    }
}
