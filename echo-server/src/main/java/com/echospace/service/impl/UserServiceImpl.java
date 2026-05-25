package com.echospace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.echospace.common.BusinessException;
import com.echospace.dto.ChangePasswordDTO;
import com.echospace.dto.UpdateProfileDTO;
import com.echospace.dto.UpdateSettingsDTO;
import com.echospace.entity.User;
import com.echospace.mapper.UserMapper;
import com.echospace.security.SecurityUtil;
import com.echospace.service.UserService;
import com.echospace.vo.UserProfileVO;
import com.echospace.vo.UserSettingsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 用户模块服务实现：资料设置、账号设置、修改密码
 * <p>
 * 当前用户 ID 统一从 {@link SecurityUtil#getCurrentUserId()} 获取，
 * 任何路径走到这里说明 JwtAuthFilter 已通过；若仍取不到则视为登录态异常，抛 401。
 * </p>
 *
 * @Author: taciturn-hg
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 密码加密器：与注册/登录保持一致，避免哈希算法/盐策略不同导致校验失败
     */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 获取当前登录用户的资料设置信息
     * <p>从 SecurityContext 取出 userId，按主键查库后转换为 VO（仅 avatar / nickname / bio）。</p>
     */
    @Override
    public UserProfileVO getMyProfile() {
        User user = requireCurrentUser();
        log.debug("查询资料设置回显完成 userId={}", user.getId());
        return UserProfileVO.from(user);
    }

    /**
     * 获取当前登录用户的账号设置信息
     */
    @Override
    public UserSettingsVO getMySettings() {
        User user = requireCurrentUser();
        log.debug("查询账号设置回显完成 userId={}", user.getId());
        return UserSettingsVO.from(user);
    }

    /**
     * 更新当前登录用户的资料设置
     * <p>
     * 仅写入 DTO 中非 null 的字段：MyBatis-Plus 默认 FieldStrategy=NOT_NULL，
     * updateById 会自动跳过 null 字段，无需手动过滤；这里仍构造一个新的 User 仅设置变更项，
     * 既清晰表达"未传则不更新"的意图，也避免误用入参中的其他字段。
     * </p>
     */
    @Override
    public void updateProfile(UpdateProfileDTO dto) {
        Long userId = currentUserIdOrThrow();

        User update = new User();
        update.setId(userId);
        update.setAvatar(dto.getAvatar());
        update.setNickname(dto.getNickname());
        update.setBio(dto.getBio());

        userMapper.updateById(update);
        log.info("资料设置更新完成 userId={}, avatarUpdated={}, nicknameUpdated={}, bioUpdated={}",
                userId, dto.getAvatar() != null, dto.getNickname() != null, dto.getBio() != null);
    }

    /**
     * 更新当前登录用户的账号设置（手机号 / 邮箱）
     * <p>
     * 流程：
     * 1) 读取当前用户实体；
     * 2) 对每个非 null 字段单独做"是否与原值相同"判断，相同则不做唯一性校验也不更新；
     * 3) 不同则查表确认未被其他用户占用（排除当前用户自身）；
     * 4) 通过校验后写库。
     * </p>
     */
    @Override
    public void updateSettings(UpdateSettingsDTO dto) {
        User current = requireCurrentUser();

        User update = new User();
        update.setId(current.getId());
        boolean changed = false;

        if (dto.getPhone() != null && !Objects.equals(dto.getPhone(), current.getPhone())) {
            ensurePhoneAvailable(dto.getPhone(), current.getId());
            update.setPhone(dto.getPhone());
            changed = true;
        }

        if (dto.getEmail() != null && !Objects.equals(dto.getEmail(), current.getEmail())) {
            ensureEmailAvailable(dto.getEmail(), current.getId());
            update.setEmail(dto.getEmail());
            changed = true;
        }

        // 没有任何字段实际发生变化时直接返回，避免无意义的 UPDATE
        if (changed) {
            userMapper.updateById(update);
            log.info("账号设置更新完成 userId={}, phoneUpdated={}, emailUpdated={}",
                    current.getId(), update.getPhone() != null, update.getEmail() != null);
        } else {
            log.info("账号设置无变化，跳过更新 userId={}", current.getId());
        }
    }

    /**
     * 修改当前登录用户的密码
     * <p>
     * 校验顺序按"成本由低到高"组织：先做 DTO 内字段一致性 / 等价性判断（不查库），
     * 再做密码匹配（BCrypt 校验有计算成本），最后才写库，尽早失败减少无效开销。
     * </p>
     */
    @Override
    public void changePassword(ChangePasswordDTO dto) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            log.warn("修改密码失败：两次新密码不一致 userId={}", SecurityUtil.getCurrentUserId());
            throw new BusinessException("两次输入的新密码不一致");
        }
        if (dto.getOldPassword().equals(dto.getNewPassword())) {
            log.warn("修改密码失败：新旧密码相同 userId={}", SecurityUtil.getCurrentUserId());
            throw new BusinessException("新密码不能与原密码相同");
        }

        User current = requireCurrentUser();
        if (!passwordEncoder.matches(dto.getOldPassword(), current.getPassword())) {
            log.warn("修改密码失败：原密码错误 userId={}", current.getId());
            throw new BusinessException("原密码错误");
        }

        User update = new User();
        update.setId(current.getId());
        update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(update);
        log.info("密码修改完成 userId={}", current.getId());
    }

    // ---------- 内部辅助 ----------

    /**
     * 取当前登录用户实体；token 有效但库中已无对应用户时视为账号被删除
     *
     */
    private User requireCurrentUser() {
        Long userId = currentUserIdOrThrow();
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("当前用户不存在 userId={}", userId);
            throw BusinessException.notFound("用户不存在");
        }
        return user;
    }

    /**
     * 从 SecurityContext 取当前用户 ID，取不到说明登录态异常
     */
    private Long currentUserIdOrThrow() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            log.warn("未登录或登录已过期");
            throw BusinessException.unauthorized("未登录或登录已过期");
        }
        return userId;
    }

    /**
     * 校验手机号未被其他用户占用（排除自己）
     */
    private void ensurePhoneAvailable(String phone, Long currentUserId) {
        boolean exists = userMapper.exists(
                new LambdaQueryWrapper<User>()
                        .eq(User::getPhone, phone)
                        .ne(User::getId, currentUserId)
        );
        if (exists) {
            log.warn("更新账号设置失败：手机号被占用 phone={}, userId={}", phone, currentUserId);
            throw BusinessException.conflict("手机号已被注册");
        }
    }

    /**
     * 校验邮箱未被其他用户占用（排除自己）
     */
    private void ensureEmailAvailable(String email, Long currentUserId) {
        boolean exists = userMapper.exists(
                new LambdaQueryWrapper<User>()
                        .eq(User::getEmail, email)
                        .ne(User::getId, currentUserId)
        );
        if (exists) {
            log.warn("更新账号设置失败：邮箱被占用 email={}, userId={}", email, currentUserId);
            throw BusinessException.conflict("邮箱已被注册");
        }
    }
}
