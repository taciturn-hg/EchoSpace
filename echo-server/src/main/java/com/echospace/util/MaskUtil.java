package com.echospace.util;

/**
 * 敏感数据脱敏工具：用于日志输出前对手机号、邮箱、密码、Token 等做掩码处理。
 * <p>
 * 所有方法均为纯函数（无副作用），null 输入返回 null，线程安全。
 * </p>
 *
 * @Author: taciturn-hg
 */
public final class MaskUtil {

    private MaskUtil() {
        // 工具类禁止实例化
    }

    /**
     * 手机号脱敏：保留前 3 后 4，中间 4 位替换为 ****
     * <p>例：13800138000 → 138****8000</p>
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 邮箱脱敏：本地部分保留前 3 位 + ***，域名保留完整
     * <p>例：zhangsan@example.com → zha***@example.com，ab@x.com → ab***@x.com</p>
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        String prefix = local.length() <= 3 ? local : local.substring(0, 3);
        return prefix + "***" + domain;
    }

    /**
     * 密码掩码：始终返回固定占位符，不在日志中保留任何密码信息
     */
    public static String maskPassword(String password) {
        if (password == null) return null;
        return "******";
    }

    /**
     * Token 脱敏：保留前 8 个字符 + ***，避免 Token 完整泄露
     * <p>例：eyJhbGciOiJIUzI1NiJ9.xxx... → eyJhbGci***</p>
     */
    public static String maskToken(String token) {
        if (token == null || token.length() <= 8) return token;
        return token.substring(0, 8) + "***";
    }

    /**
     * 登录账号脱敏：自动识别账号类型并调用对应的掩码方法。
     * <ul>
     *   <li>含 {@code @} → 按邮箱脱敏</li>
     *   <li>11 位 1 开头的纯数字 → 按手机号脱敏</li>
     *   <li>其他 → 保持原样（视为用户名，无需脱敏）</li>
     * </ul>
     */
    public static String maskAccount(String account) {
        if (account == null) return null;
        if (account.contains("@")) return maskEmail(account);
        if (account.matches("^1[3-9]\\d{9}$")) return maskPhone(account);
        return account;
    }
}
