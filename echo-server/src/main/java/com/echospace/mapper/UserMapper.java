package com.echospace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echospace.entity.User;

/**
 * 用户模块 Mapper：负责 user 表的 CRUD。
 * <p>
 * 与 {@link AuthMapper} 同样面向 {@link User} 表，但归口拆分为：
 * AuthMapper 用于注册/登录/Token 等认证场景，UserMapper 用于资料设置、账号设置、修改密码等用户自管理场景，
 * 便于按业务模块组织 SQL 与扩展。
 * </p>
 *
 * @Author: taciturn-hg
 */
public interface UserMapper extends BaseMapper<User> {
}
