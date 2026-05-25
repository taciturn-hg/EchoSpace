package com.echospace.vo;

import com.echospace.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 账号设置回显 VO
 * <p>对应接口：GET /api/users/me/settings</p>
 * <p>
 * 出于隐私保护考虑，手机号与邮箱在返回前需进行脱敏处理：
 * 手机号保留前 3 位与后 4 位，中间用 4 个 * 代替（138****8000）；
 * 邮箱保留 @ 前的前 3 位与完整域名，中间用 *** 代替（zha***@example.com）。
 * </p>
 *
 * @Author: taciturn-hg
 */
@Data
@Schema(description = "账号设置信息（已脱敏）")
public class UserSettingsVO {

    @Schema(description = "手机号（脱敏）", example = "138****8000")
    private String phone;

    @Schema(description = "邮箱（脱敏）", example = "zha***@example.com")
    private String email;

    /**
     * 从 User 实体构建脱敏后的账号设置 VO
     *
     * @param user 数据库用户实体
     * @return 包含脱敏手机号与邮箱的 VO
     */
    public static UserSettingsVO from(User user) {
        UserSettingsVO vo = new UserSettingsVO();
        vo.setPhone(maskPhone(user.getPhone()));
        vo.setEmail(maskEmail(user.getEmail()));
        return vo;
    }

    /**
     * 手机号脱敏：保留前 3 位与后 4 位，中间用 4 个 * 代替
     * <p>非 11 位的输入直接原样返回，避免误处理异常数据。</p>
     *
     * @param phone 原始手机号
     * @return 脱敏后的手机号
     */
    private static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * 邮箱脱敏：保留 @ 前的前 3 位与完整域名，中间用 *** 代替
     * <p>本地名长度不足 3 位时全部保留，避免脱敏后无法辨识。</p>
     *
     * @param email 原始邮箱
     * @return 脱敏后的邮箱
     */
    private static String maskEmail(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return email;
        }
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 3) {
            return local + domain;
        }
        return local.substring(0, 3) + "***" + domain;
    }
}
