package com.example.myshopapp.points;

/**
 * 会员等级：按累计获得积分划分，等级只升不降，用于权益展示与运营标识。
 */
public enum MemberLevel {

    BRONZE("青铜会员", 0),
    SILVER("白银会员", 1000),
    GOLD("黄金会员", 5000),
    PLATINUM("铂金会员", 20000),
    DIAMOND("钻石会员", 50000);

    private final String label;
    private final int threshold;

    MemberLevel(String label, int threshold) {
        this.label = label;
        this.threshold = threshold;
    }

    public String label() {
        return label;
    }

    public int threshold() {
        return threshold;
    }

    /** 累计积分对应的等级 */
    public static MemberLevel of(int totalEarned) {
        MemberLevel level = BRONZE;
        for (MemberLevel candidate : values()) {
            if (totalEarned >= candidate.threshold) {
                level = candidate;
            }
        }
        return level;
    }

    /** 下一等级，已是最高等级时返回 null */
    public MemberLevel next() {
        MemberLevel[] all = values();
        return ordinal() + 1 < all.length ? all[ordinal() + 1] : null;
    }
}
