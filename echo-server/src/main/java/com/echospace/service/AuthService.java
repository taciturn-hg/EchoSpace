package com.echospace.service;

import com.echospace.dto.LoginDTO;
import com.echospace.dto.RefreshTokenDTO;
import com.echospace.dto.RegisterDTO;
import com.echospace.vo.LoginVO;
import com.echospace.vo.UserInfoVO;

/**
 * 认证服务接口：注册、登录、刷新 Token
 *
 * @Author: taciturn-hg
 */
public interface AuthService {

    /**
     * 用户注册
     * <p>校验两次密码一致性、用户名/手机号/邮箱唯一性，通过后写入数据库。</p>
     *
     * @param registerDTO 注册请求参数
     * @throws com.echospace.common.BusinessException 用户名/手机号/邮箱已存在时抛出 409
     */
    void register(RegisterDTO registerDTO);

    /**
     * 用户登录
     * <p>支持用户名 / 邮箱 / 手机号三种账号形式，验证通过后签发 AccessToken + RefreshToken。</p>
     *
     * @param loginDTO 登录请求参数
     * @return 包含 accessToken、refreshToken、expiresIn 的响应 VO
     * @throws com.echospace.common.BusinessException 用户不存在或密码错误时抛出 400
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 刷新 Token
     * <p>验证 refreshToken 有效性和类型，通过后重新签发 Token 对。</p>
     *
     * @param refreshTokenDTO 包含 refreshToken 的请求参数
     * @return 新的 accessToken、refreshToken、expiresIn
     * @throws com.echospace.common.BusinessException refreshToken 过期或无效时抛出 400
     */
    LoginVO refresh(RefreshTokenDTO refreshTokenDTO);

    /**
     * 获取当前登录用户信息
     * <p>从 SecurityContext 中取出 userId，查库后返回脱敏的用户信息 VO。</p>
     *
     * @return 当前用户信息 VO
     * @throws com.echospace.common.BusinessException 用户不存在时抛出 404
     */
    UserInfoVO getMe();
}
