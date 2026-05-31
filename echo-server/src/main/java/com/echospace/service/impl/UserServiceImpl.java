package com.echospace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echospace.common.BusinessException;
import com.echospace.dto.ChangePasswordDTO;
import com.echospace.dto.UpdateProfileDTO;
import com.echospace.dto.UpdateSettingsDTO;
import com.echospace.entity.Post;
import com.echospace.entity.User;
import com.echospace.entity.UserFollow;
import com.echospace.mapper.PostMapper;
import com.echospace.mapper.UserFollowMapper;
import com.echospace.mapper.UserMapper;
import com.echospace.security.SecurityUtil;
import com.echospace.service.UserService;
import com.echospace.util.MaskUtil;
import com.echospace.vo.FollowItemVO;
import com.echospace.vo.FollowVO;
import com.echospace.vo.PageVO;
import com.echospace.vo.PublicUserVO;
import com.echospace.vo.UserProfileVO;
import com.echospace.vo.UserSettingsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserFollowMapper userFollowMapper;

    /**
     * 密码加密器：与注册/登录保持一致，避免哈希算法/盐策略不同导致校验失败
     */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 查询指定用户的公开信息（个人主页），对应 API 2.1
     * <p>
     * 未登录用户也可访问，此时 isFollowed 恒为 false。
     * 统计口径：postCount = status=1 的帖子数，followerCount = 粉丝数，followingCount = 关注数。
     * </p>
     *
     * @param userId 目标用户 ID
     * @return 公开用户信息 VO
     * @throws BusinessException 目标用户不存在或已注销时抛出 404
     */
    @Override
    public PublicUserVO getPublicProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("目标用户不存在 userId={}", userId);
            throw BusinessException.notFound("用户不存在");
        }

        Long currentUserId = SecurityUtil.getCurrentUserId();

        // 统计发帖数（仅正常状态帖子）
        Long postCount = postMapper.selectCount(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getUserId, userId)
                        .eq(Post::getStatus, 1)
        );

        // 统计粉丝数（关注该用户的人数）
        Long followerCount = userFollowMapper.selectCount(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowedId, userId)
        );

        // 统计关注数（该用户关注的人数）
        Long followingCount = userFollowMapper.selectCount(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, userId)
        );

        // 仅当前用户已登录时检查是否已关注
        boolean isFollowed = false;
        if (currentUserId != null) {
            isFollowed = userFollowMapper.exists(
                    new LambdaQueryWrapper<UserFollow>()
                            .eq(UserFollow::getFollowerId, currentUserId)
                            .eq(UserFollow::getFollowedId, userId)
            );
        }

        log.debug("公开用户信息查询完成 targetUserId={}, postCount={}, followerCount={}, followingCount={}, isFollowed={}",
                userId, postCount, followerCount, followingCount, isFollowed);

        return PublicUserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .bio(user.getBio())
                .postCount(postCount.intValue())
                .followerCount(followerCount.intValue())
                .followingCount(followingCount.intValue())
                .isFollowed(isFollowed)
                .createdAt(user.getCreatedAt())
                .build();
    }

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
        User current = requireCurrentUser();
        Long userId = current.getId();

        User update = new User();
        update.setId(userId);
        boolean changed = false;

        if (dto.getAvatar() != null && !Objects.equals(dto.getAvatar(), current.getAvatar())) {
            update.setAvatar(dto.getAvatar());
            changed = true;
        }
        if (dto.getNickname() != null && !Objects.equals(dto.getNickname(), current.getNickname())) {
            update.setNickname(dto.getNickname());
            changed = true;
        }
        if (dto.getBio() != null && !Objects.equals(dto.getBio(), current.getBio())) {
            update.setBio(dto.getBio());
            changed = true;
        }

        if (changed) {
            userMapper.updateById(update);
            log.info("资料设置更新完成 userId={}, avatarUpdated={}, nicknameUpdated={}, bioUpdated={}",
                    userId, update.getAvatar() != null, update.getNickname() != null, update.getBio() != null);
        } else {
            log.info("资料设置无变化，跳过更新 userId={}", userId);
        }
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

    /**
     * 关注或取消关注指定用户（toggle 模式），对应 API 2.8
     * <p>
     * 已关注则删除关注关系并返回 followed=false；
     * 未关注则创建关注关系并返回 followed=true。
     * 不允许关注自己。
     * </p>
     */
    @Override
    public FollowVO followUser(Long followedId) {
        Long userId = currentUserIdOrThrow();

        if (userId.equals(followedId)) {
            log.warn("关注失败：不能关注自己 userId={}", userId);
            throw new BusinessException("不能关注自己");
        }

        User target = userMapper.selectById(followedId);
        if (target == null) {
            log.warn("关注失败：目标用户不存在 followedId={}", followedId);
            throw BusinessException.notFound("用户不存在");
        }

        UserFollow existing = userFollowMapper.selectOne(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, userId)
                        .eq(UserFollow::getFollowedId, followedId)
        );

        if (existing != null) {
            userFollowMapper.deleteById(existing.getId());
            log.info("已取消关注 userId={}, followedId={}", userId, followedId);
            return new FollowVO(false);
        }

        UserFollow follow = new UserFollow();
        follow.setFollowerId(userId);
        follow.setFollowedId(followedId);
        userFollowMapper.insert(follow);
        log.info("关注成功 userId={}, followedId={}", userId, followedId);
        return new FollowVO(true);
    }

    /**
     * 分页查询指定用户的粉丝列表，对应 API 2.9
     * <p>
     * 按关注时间倒序排列，JOIN user 表取粉丝信息。
     * 未登录用户也可访问（不依赖 SecurityContext）。
     * </p>
     */
    @Override
    public PageVO<FollowItemVO> listFollowers(Long userId, int current, int size) {
        if (userMapper.selectById(userId) == null) {
            log.warn("查询粉丝列表失败：用户不存在 userId={}", userId);
            throw BusinessException.notFound("用户不存在");
        }

        IPage<UserFollow> page = userFollowMapper.selectPage(
                new Page<>(current, size),
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowedId, userId)
                        .orderByDesc(UserFollow::getCreatedAt)
        );

        List<FollowItemVO> records = page.getRecords().stream().map(uf -> {
            User follower = userMapper.selectById(uf.getFollowerId());
            return FollowItemVO.builder()
                    .id(follower.getId())
                    .username(follower.getUsername())
                    .nickname(follower.getNickname())
                    .avatar(follower.getAvatar())
                    .followedAt(uf.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());

        log.debug("粉丝列表查询完成 targetUserId={}, current={}, size={}, total={}",
                userId, current, size, page.getTotal());
        return new PageVO<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 分页查询指定用户关注的人的列表，对应 API 2.10
     * <p>
     * 按关注时间倒序排列，JOIN user 表取被关注用户信息。
     * 未登录用户也可访问（不依赖 SecurityContext）。
     * </p>
     */
    @Override
    public PageVO<FollowItemVO> listFollowing(Long userId, int current, int size) {
        if (userMapper.selectById(userId) == null) {
            log.warn("查询关注列表失败：用户不存在 userId={}", userId);
            throw BusinessException.notFound("用户不存在");
        }

        IPage<UserFollow> page = userFollowMapper.selectPage(
                new Page<>(current, size),
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, userId)
                        .orderByDesc(UserFollow::getCreatedAt)
        );

        List<FollowItemVO> records = page.getRecords().stream().map(uf -> {
            User followed = userMapper.selectById(uf.getFollowedId());
            return FollowItemVO.builder()
                    .id(followed.getId())
                    .username(followed.getUsername())
                    .nickname(followed.getNickname())
                    .avatar(followed.getAvatar())
                    .followedAt(uf.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());

        log.debug("关注列表查询完成 targetUserId={}, current={}, size={}, total={}",
                userId, current, size, page.getTotal());
        return new PageVO<>(records, page.getTotal(), page.getCurrent(), page.getSize());
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
            log.warn("更新账号设置失败：手机号被占用 phone={}, userId={}", MaskUtil.maskPhone(phone), currentUserId);
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
            log.warn("更新账号设置失败：邮箱被占用 email={}, userId={}", MaskUtil.maskEmail(email), currentUserId);
            throw BusinessException.conflict("邮箱已被注册");
        }
    }
}
