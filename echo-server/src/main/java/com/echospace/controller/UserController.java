package com.echospace.controller;

import com.echospace.common.Result;
import com.echospace.dto.ChangePasswordDTO;
import com.echospace.dto.UpdateProfileDTO;
import com.echospace.dto.UpdateSettingsDTO;
import com.echospace.security.SecurityUtil;
import com.echospace.service.UserService;
import com.echospace.vo.UserProfileVO;
import com.echospace.vo.UserSettingsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户模块控制器：当前登录用户的资料设置、账号设置、修改密码
 * <p>
 * 所有接口均要求请求头携带有效的 {@code Authorization: Bearer <accessToken>}，
 * 不在 Security 白名单内，未携带 / 已过期会被 JwtAuthFilter 拦截并返回 401。
 * 这里只承担参数绑定 + 委派 Service 的职责，业务规则全部下沉到 UserService 实现。
 * </p>
 *
 * @Author: taciturn-hg
 */
@Slf4j
@Tag(name = "用户模块", description = "当前登录用户的资料设置、账号设置、修改密码")
@RestController
@RequestMapping("/users/me")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 获取当前登录用户的资料设置信息
     * <p>对应接口文档 2.2：用于资料设置页表单回显（avatar / nickname / bio）。</p>
     *
     * @return 资料设置 VO
     */
    @Operation(summary = "获取资料设置信息", description = "用于资料设置页表单回显（头像 / 昵称 / 简介）")
    @GetMapping("/profile")
    public Result<UserProfileVO> getMyProfile() {
        log.info("获取资料设置信息请求 userId={}", SecurityUtil.getCurrentUserId());
        return Result.success(userService.getMyProfile());
    }

    /**
     * 获取当前登录用户的账号设置信息
     * <p>对应接口文档 2.3：直接返回手机号和邮箱原始值，用于账号设置页回显。</p>
     *
     * @return 账号设置 VO（手机号 / 邮箱）
     */
    @Operation(summary = "获取账号设置信息", description = "返回手机号和邮箱，用于账号设置页回显")
    @GetMapping("/settings")
    public Result<UserSettingsVO> getMySettings() {
        log.info("获取账号设置信息请求 userId={}", SecurityUtil.getCurrentUserId());
        return Result.success(userService.getMySettings());
    }

    /**
     * 更新当前登录用户的资料设置
     * <p>对应接口文档 2.4：仅更新请求体中非 null 的字段，未传字段保持原值。</p>
     *
     * @param dto 资料更新参数（avatar / nickname / bio 全部可选）
     * @return 提示信息，data 为 null
     */
    @Operation(summary = "更新资料设置", description = "更新头像 / 昵称 / 简介，仅更新提交的字段")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UpdateProfileDTO dto) {
        log.info("更新资料设置请求 userId={}, avatarChanged={}, nicknameChanged={}, bioChanged={}",
                SecurityUtil.getCurrentUserId(),
                dto.getAvatar() != null, dto.getNickname() != null, dto.getBio() != null);
        userService.updateProfile(dto);
        log.info("更新资料设置成功 userId={}", SecurityUtil.getCurrentUserId());
        return Result.success("更新成功");
    }

    /**
     * 更新当前登录用户的账号设置
     * <p>
     * 对应接口文档 2.5：手机号 / 邮箱均为可选，新值需通过格式校验且与库中其他用户唯一不冲突，
     * 与原值相同则跳过该字段更新。
     * </p>
     *
     * @param dto 账号更新参数（phone / email 全部可选）
     * @return 提示信息，data 为 null
     */
    @Operation(summary = "更新账号设置", description = "更新手机号 / 邮箱，需通过唯一性校验")
    @PutMapping("/settings")
    public Result<Void> updateSettings(@Valid @RequestBody UpdateSettingsDTO dto) {
        log.info("更新账号设置请求 userId={}, phoneChanged={}, emailChanged={}",
                SecurityUtil.getCurrentUserId(),
                dto.getPhone() != null, dto.getEmail() != null);
        userService.updateSettings(dto);
        log.info("更新账号设置成功 userId={}", SecurityUtil.getCurrentUserId());
        return Result.success("更新成功");
    }

    /**
     * 修改当前登录用户的密码
     * <p>
     * 对应接口文档 2.6：需提供原密码、新密码、确认新密码。
     * Service 层会校验：原密码匹配、两次新密码一致、新旧密码不同。
     * </p>
     *
     * @param dto 修改密码参数
     * @return 提示信息，data 为 null
     */
    @Operation(summary = "修改密码", description = "需提供原密码 + 新密码 + 确认新密码，BCrypt 加密后写库")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        log.info("修改密码请求 userId={}", SecurityUtil.getCurrentUserId());
        userService.changePassword(dto);
        log.info("修改密码成功 userId={}", SecurityUtil.getCurrentUserId());
        return Result.success("密码修改成功");
    }
}
