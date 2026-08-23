package com.fafa.domain.model.tag;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** UserTag 领域规则测试。 */
class UserTagTest {

    @Test
    @DisplayName("创建标签会 trim，touch 增加热度并刷新时间")
    void createAndTouch_shouldUpdateUsage() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 23, 10, 0);
        LocalDateTime usedAt = createdAt.plusHours(1);
        UserTag tag = UserTag.create(1L, " 晒太阳 ", TagCategory.ACTION, createdAt).touch(usedAt);

        assertThat(tag.getTagName()).isEqualTo("晒太阳");
        assertThat(tag.getCategory()).isEqualTo(TagCategory.ACTION);
        assertThat(tag.getUsageCount()).isEqualTo(1);
        assertThat(tag.getLastUsedAt()).isEqualTo(usedAt);
    }

    @Test
    @DisplayName("标签名为空或超长时拒绝")
    void create_shouldRejectInvalidName() {
        assertThatThrownBy(() -> UserTag.create(1L, " ", null, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> UserTag.create(1L, "a".repeat(51), null, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
