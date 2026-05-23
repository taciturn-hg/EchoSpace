package com.echospace.controller;

import com.echospace.common.Result;
import com.echospace.dto.LoginDTO;
import com.echospace.dto.RefreshTokenDTO;
import com.echospace.dto.RegisterDTO;
import com.echospace.service.AuthService;
import com.echospace.vo.LoginVO;
import com.echospace.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证模块控制器：注册、登录、刷新 Token
 * <p>以下接口均在 Security 白名单内，无需携带 Authorization Header。</p>
 *
 * @Author: taciturn-hg
 */
@Tag(name = "认证模块", description = "注册、登录、刷新 Token")
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 用户注册
     *
     * @param registerDTO 注册请求参数（用户名、手机号、邮箱、密码、确认密码）
     * @return 注册成功提示，data 为 null
     */
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO registerDTO) {
        authService.register(registerDTO);
        return Result.success("注册成功");
    }

    /**
     * 用户登录
     *
     * @param loginDTO 登录请求参数（account 支持用户名 / 邮箱 / 手机号）
     * @return accessToken、refreshToken 及过期时间（秒）
     */
    @Operation(summary = "用户登录", description = "支持用户名 / 邮箱 / 手机号登录，返回 JWT Token 对")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return Result.success(authService.login(loginDTO));
    }

    /**
     * 刷新 Token
     *
     * @param refreshTokenDTO 包含 refreshToken 的请求体
     * @return 新的 accessToken、refreshToken 及过期时间（秒）
     */
    @Operation(summary = "刷新 Token", description = "用 refreshToken 换取新的 accessToken 和 refreshToken")
    @PostMapping("/refresh")
    public Result<LoginVO> refresh(@Valid @RequestBody RefreshTokenDTO refreshTokenDTO) {
        return Result.success(authService.refresh(refreshTokenDTO));
    }

    /**
     * 获取当前登录用户信息
     * <p>需要在请求头携带有效的 accessToken：{@code Authorization: Bearer <token>}</p>
     *
     * @return 当前用户的公开信息（不含密码等敏感字段）
     */
    @Operation(summary = "获取当前用户信息", description = "需携带 Authorization: Bearer <accessToken>，返回当前登录用户的公开信息")
    @GetMapping("/me")
    public Result<UserInfoVO> me() {
        return Result.success(authService.getMe());
    }
}
