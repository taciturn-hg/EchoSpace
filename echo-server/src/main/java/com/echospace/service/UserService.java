package com.echospace.service;

import com.echospace.dto.ChangePasswordDTO;
import com.echospace.dto.UpdateProfileDTO;
import com.echospace.dto.UpdateSettingsDTO;
import com.echospace.vo.FollowItemVO;
import com.echospace.vo.FollowVO;
import com.echospace.vo.PageVO;
import com.echospace.vo.PublicUserVO;
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
     * 查询指定用户的公开信息（个人主页），对应 API 2.1
     * <p>
     * 查询内容包括：用户基本信息、发帖总数、粉丝数、关注数。
     * 若当前用户已登录，还会标记 isFollowed（当前用户是否已关注该目标用户）；
     * 未登录时 isFollowed 恒为 false。
     * </p>
     *
     * @param userId 目标用户 ID
     * @return 公开用户信息 VO
     * @throws com.echospace.common.BusinessException 目标用户不存在时抛出 404
     */
    PublicUserVO getPublicProfile(Long userId);

    /**
     * 获取当前登录用户的资料设置信息
     * <p>用于资料设置页面表单回显，仅返回 avatar / nickname / bio。</p>
     *
     * @return 当前用户资料 VO
     * @throws com.echospace.common.BusinessException 用户不存在时抛出 404
     */
    UserProfileVO getMyProfile();

    /**
     * 获取当前登录用户的账号设置信息
     * <p>用于账号设置页面回显手机号与邮箱，直接返回原始值。</p>
     *
     * @return 当前用户账号设置 VO（手机号、邮箱）
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

    /**
     * 关注或取消关注指定用户（toggle 模式），对应 API 2.8
     * <p>
     * 已关注则删除关注关系，返回 followed=false；
     * 未关注则创建关注关系，返回 followed=true。
     * 不允许关注自己，调用时抛出 400。
     * </p>
     *
     * @param followedId 被关注的用户 ID
     * @return 关注状态 VO
     * @throws com.echospace.common.BusinessException 目标用户不存在时抛出 404，关注自己时抛出 400
     */
    FollowVO followUser(Long followedId);

    /**
     * 分页查询指定用户的粉丝列表，对应 API 2.9
     * <p>按关注时间倒序排列，未登录用户也可访问。</p>
     *
     * @param userId  目标用户 ID
     * @param current 页码，默认 1
     * @param size    每页条数，默认 10
     * @return 页码分页的粉丝列表
     * @throws com.echospace.common.BusinessException 目标用户不存在时抛出 404
     */
    PageVO<FollowItemVO> listFollowers(Long userId, int current, int size);

    /**
     * 分页查询指定用户关注的人的列表，对应 API 2.10
     * <p>按关注时间倒序排列，未登录用户也可访问。</p>
     *
     * @param userId  目标用户 ID
     * @param current 页码，默认 1
     * @param size    每页条数，默认 10
     * @return 页码分页的关注列表
     * @throws com.echospace.common.BusinessException 目标用户不存在时抛出 404
     */
    PageVO<FollowItemVO> listFollowing(Long userId, int current, int size);
}
