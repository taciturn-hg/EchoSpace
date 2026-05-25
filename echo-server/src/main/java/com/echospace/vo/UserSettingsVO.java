package com.echospace.vo;

import com.echospace.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 账号设置回显 VO
 * <p>对应接口：GET /api/users/me/settings</p>
 *
 * @Author: taciturn-hg
 */
@Data
@Schema(description = "账号设置信息")
public class UserSettingsVO {

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    public static UserSettingsVO from(User user) {
        UserSettingsVO vo = new UserSettingsVO();
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        return vo;
    }
}
