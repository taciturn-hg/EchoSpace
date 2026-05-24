package com.echospace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.echospace.common.BusinessException;
import com.echospace.dto.LoginDTO;
import com.echospace.dto.RefreshTokenDTO;
import com.echospace.dto.RegisterDTO;
import com.echospace.entity.User;
import com.echospace.mapper.AuthMapper;
import com.echospace.security.JwtUtil;
import com.echospace.security.SecurityUtil;
import com.echospace.service.AuthService;
import com.echospace.vo.LoginVO;
import com.echospace.vo.UserInfoVO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 认证服务实现：注册、登录、刷新 Token
 *
 * @Author: taciturn-hg
 */
@Service
public class AuthServiceImpl implements AuthService {

    @Value("${jwt.access-token-expiration:1800000}")
    private long accessExpire;

    @Autowired
    private AuthMapper authMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 用户注册
     * <p>
     * 流程：两次密码一致性 → 用户名/手机号/邮箱唯一性 → BCrypt 加密密码 → 写库。
     * nickname 默认由系统生成随机值（格式为 Echo_ + UUID 子串），用户后续可在设置页修改。
     * </p>
     */
    @Override
    public void register(RegisterDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("两次密码输入不一致");
        }

        if (authMapper.exists(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()))) {
            throw BusinessException.conflict("用户名已存在");
        }
        if (authMapper.exists(new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()))) {
            throw BusinessException.conflict("手机号已被注册");
        }
        if (authMapper.exists(new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail()))) {
            throw BusinessException.conflict("邮箱已被注册");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setNickname("Echo_" + UUID.randomUUID().toString().substring(0, 8));
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setStatus(1);
        authMapper.insert(user);
    }

    /**
     * 用户登录
     * <p>
     * account 字段由 MyBatis-Plus OR 条件同时匹配 username / email / phone，
     * 加 limit 1 避免极端情况下多行返回。
     * 密码通过 BCrypt matches 校验，不做明文比对。
     * expiresIn 以秒为单位返回，与接口文档保持一致。
     * </p>
     */
    @Override
    public LoginVO login(LoginDTO loginDTO) {
        User user = authMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, loginDTO.getAccount())
                        .or().eq(User::getEmail, loginDTO.getAccount())
                        .or().eq(User::getPhone, loginDTO.getAccount())
                        .last("limit 1")
        );
        if (user == null) {
            throw new BusinessException("账号或密码错误");
        }

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException("账号或密码错误");
        }

        String access = jwtUtil.generateAccessToken(user.getId().toString(), user.getUsername());
        String refresh = jwtUtil.generateRefreshToken(user.getId().toString(), user.getUsername());
        return new LoginVO(access, refresh, accessExpire / 1000);
    }

    /**
     * 刷新 Token
     * <p>
     * 解析 refreshToken → 校验类型为 refresh → 用原 userId/username 重新签发 Token 对。
     * 旧 refreshToken 在此实现中不做主动吊销（无状态），后续引入 Redis 黑名单时可在此处扩展。
     * </p>
     */
    @Override
    public LoginVO refresh(RefreshTokenDTO dto) {
        Claims claims;
        try {
            claims = jwtUtil.parseToken(dto.getRefreshToken());
        } catch (ExpiredJwtException e) {
            throw new BusinessException("refreshToken 已过期，请重新登录");
        } catch (JwtException e) {
            throw new BusinessException("refreshToken 无效");
        }

        String tokenType = claims.get("type", String.class);
        if (!JwtUtil.TokenType.REFRESH.claimValue().equals(tokenType)) {
            throw new BusinessException("Token 类型错误，请使用 refreshToken");
        }

        String userId = claims.getSubject();
        String username = claims.get("username", String.class);

        String newAccess = jwtUtil.generateAccessToken(userId, username);
        String newRefresh = jwtUtil.generateRefreshToken(userId, username);
        return new LoginVO(newAccess, newRefresh, accessExpire / 1000);
    }

    /**
     * 获取当前登录用户信息
     * <p>
     * 从 SecurityContext 取出 userId，按主键查库，转换为脱敏 VO 返回。
     * 正常情况下 token 有效则用户必然存在；若查不到则说明账号已被删除。
     * </p>
     */
    @Override
    public UserInfoVO getMe() {
        Long userId = SecurityUtil.getCurrentUserId();
        User user = authMapper.selectById(userId);
        if (userId == null) {
            throw BusinessException.unauthorized("未登录或登录已过期");
        }
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return UserInfoVO.from(user);
    }
}
