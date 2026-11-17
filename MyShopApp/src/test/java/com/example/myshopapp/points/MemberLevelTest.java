package com.example.myshopapp.points;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 会员等级纯逻辑回归：阈值边界与等级晋升。
 */
class MemberLevelTest {

    @Test
    @DisplayName("累计积分按阈值映射会员等级，边界值归入较高等级")
    void mapTotalEarnedToLevel() {
        assertThat(MemberLevel.of(0)).isEqualTo(MemberLevel.BRONZE);
        assertThat(MemberLevel.of(999)).isEqualTo(MemberLevel.BRONZE);
        assertThat(MemberLevel.of(1000)).isEqualTo(MemberLevel.SILVER);
        assertThat(MemberLevel.of(4999)).isEqualTo(MemberLevel.SILVER);
        assertThat(MemberLevel.of(5000)).isEqualTo(MemberLevel.GOLD);
        assertThat(MemberLevel.of(19999)).isEqualTo(MemberLevel.GOLD);
        assertThat(MemberLevel.of(20000)).isEqualTo(MemberLevel.PLATINUM);
        assertThat(MemberLevel.of(50000)).isEqualTo(MemberLevel.DIAMOND);
        assertThat(MemberLevel.of(Integer.MAX_VALUE)).isEqualTo(MemberLevel.DIAMOND);
    }

    @Test
    @DisplayName("下一等级：最高等级没有下一级")
    void nextLevel() {
        assertThat(MemberLevel.BRONZE.next()).isEqualTo(MemberLevel.SILVER);
        assertThat(MemberLevel.SILVER.next()).isEqualTo(MemberLevel.GOLD);
        assertThat(MemberLevel.GOLD.next()).isEqualTo(MemberLevel.PLATINUM);
        assertThat(MemberLevel.PLATINUM.next()).isEqualTo(MemberLevel.DIAMOND);
        assertThat(MemberLevel.DIAMOND.next()).isNull();
    }

    @Test
    @DisplayName("等级名称与阈值保持稳定，避免前端展示与运营配置漂移")
    void levelMetadata() {
        assertThat(MemberLevel.BRONZE.label()).isEqualTo("青铜会员");
        assertThat(MemberLevel.BRONZE.threshold()).isZero();
        assertThat(MemberLevel.SILVER.threshold()).isEqualTo(1000);
        assertThat(MemberLevel.GOLD.threshold()).isEqualTo(5000);
        assertThat(MemberLevel.PLATINUM.threshold()).isEqualTo(20000);
        assertThat(MemberLevel.DIAMOND.threshold()).isEqualTo(50000);
    }
}
