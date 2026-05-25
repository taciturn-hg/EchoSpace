package com.echospace.service;

import com.echospace.dto.ChangePasswordDTO;
import com.echospace.dto.UpdateProfileDTO;
import com.echospace.dto.UpdateSettingsDTO;
import com.echospace.vo.UserProfileVO;
import com.echospace.vo.UserSettingsVO;

/**
 * 用户模块服务接口：当前登录用户的资料设置、账号设置、修改密码
 * <p>
 * 与 {@link AuthService} 的边界划分：
 * AuthService 负责"登录态产生"环节（注册、登录、刷新 Token、读取当前用户基础信息）；
 * UserService 负责"登录态之后的自我管理"环节（修改资料、修改账号信息、修改密码）。
 * </p>
 *
 * @Author: taciturn-hg
 */
public interface UserService {

    /**
     * 获取当前登录用户的资料设置信息
     * <p>用于资料设置页面表单回显，仅返回 avatar / nickname / bio。</p>
     *
     * @return 当前用户资料 VO
     * @throws com.echospace.common.BusinessException 用户不存在时抛出 404
     */
    UserProfileVO getMyProfile();

    /**
     * 获取当前登录用户的账号设置信息（脱敏）
     * <p>用于账号设置页面回显手机号与邮箱，返回前已做脱敏处理。</p>
     *
     * @return 当前用户账号设置 VO（手机号、邮箱已脱敏）
     * @throws com.echospace.common.BusinessException 用户不存在时抛出 404
     */
    UserSettingsVO getMySettings();

    /**
     * 更新当前登录用户的资料设置
     * <p>仅更新 DTO 中非 null 的字段（avatar / nickname / bio），未传字段保持原值不变。</p>
     *
     * @param dto 更新请求参数
     * @throws com.echospace.common.BusinessException 用户不存在时抛出 404
     */
    void updateProfile(UpdateProfileDTO dto);

    /**
     * 更新当前登录用户的账号设置（手机号 / 邮箱）
     * <p>
     * 仅更新 DTO 中非 null 的字段；新值需通过格式校验，并与库中其他用户唯一不冲突。
     * 与原值相同时跳过该字段的唯一性校验，避免误判为冲突。
     * </p>
     *
     * @param dto 更新请求参数
     * @throws com.echospace.common.BusinessException 手机号或邮箱已被其他用户占用时抛出 409
     */
    void updateSettings(UpdateSettingsDTO dto);

    /**
     * 修改当前登录用户的密码
     * <p>
     * 流程：
     * 1) 读取当前用户实体；
     * 2) 校验 oldPassword 与库中 BCrypt 哈希一致；
     * 3) 校验 newPassword 与 confirmPassword 一致；
     * 4) 校验 newPassword 与 oldPassword 不同；
     * 5) 用 BCrypt 重新加密新密码并写库。
     * </p>
     *
     * @param dto 修改密码请求参数
     * @throws com.echospace.common.BusinessException 校验失败时抛出 400
     */
    void changePassword(ChangePasswordDTO dto);
}
