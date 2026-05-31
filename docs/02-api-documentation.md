# EchoSpace API 接口文档

## 通用说明

- **Base URL**：`http://localhost:8080/api`
- **认证方式**：除注册/登录/刷新Token外，所有接口请求头须携带 `Authorization: Bearer {accessToken}`
- **统一响应码**：`code` 为 1 代表成功，0 代表失败
- **分页响应**有两种格式：
  - **游标分页**（帖子列表、用户帖子列表、评论列表）：`data` 包含 `records`（数组）、`cursor`（下页游标，无更多时为 null）、`hasMore`（是否还有更多）、`count`（本次返回的记录数）
  - **页码分页**（粉丝列表、关注列表等）：`data` 包含 `records`（数组）、`total`（总记录数）、`current`（当前页）、`size`（每页条数）

---

## 1. 认证模块

### 1.1 用户注册

#### 1.1.1 基本信息

> 请求路径：/api/auth/register
>
> 请求方式：POST
>
> 接口描述：该接口用于新用户注册

#### 1.1.2 请求参数

格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| username | string | 必须 | 用户名，长度 2~50 |
| phone | string | 必须 | 手机号，需唯一 |
| email | string | 必须 | 邮箱，需唯一 |
| password | string | 必须 | 密码，长度 6~100 |
| confirmPassword | string | 必须 | 确认密码，须与 password 一致 |

请求参数样例：

```json
{
  "username": "zhangsan",
  "phone": "13800138000",
  "email": "zhangsan@example.com",
  "password": "123456",
  "confirmPassword": "123456"
}
```

#### 1.1.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据（注册成功时为 null） |

响应数据样例：

```json
{
  "code": 1,
  "msg": "注册成功",
  "data": null
}
```

---

### 1.2 用户登录

#### 1.2.1 基本信息

> 请求路径：/api/auth/login
>
> 请求方式：POST
>
> 接口描述：该接口用于用户登录（支持用户名/邮箱/手机号），返回 JWT 令牌

#### 1.2.2 请求参数

格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| account | string | 必须 | 用户名/邮箱/手机号 |
| password | string | 必须 | 密码 |

请求参数样例：

```json
{
  "account": "zhangsan",
  "password": "123456"
}
```

#### 1.2.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| \|- accessToken | string | 必须 | 访问令牌，有效期 30 分钟 |
| \|- refreshToken | string | 必须 | 刷新令牌，有效期 7 天 |
| \|- expiresIn | number | 必须 | accessToken 过期时间，单位秒 |

响应数据样例：

```json
{
  "code": 1,
  "msg": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 1800
  }
}
```

---

### 1.3 刷新 Token

#### 1.3.1 基本信息

> 请求路径：/api/auth/refresh
>
> 请求方式：POST
>
> 接口描述：该接口用于使用 refreshToken 换取新的 accessToken

#### 1.3.2 请求参数

格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| refreshToken | string | 必须 | 登录时获取的刷新令牌 |

请求参数样例：

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

#### 1.3.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| \|- accessToken | string | 必须 | 新的访问令牌 |
| \|- refreshToken | string | 必须 | 新的刷新令牌 |
| \|- expiresIn | number | 必须 | accessToken 过期时间，单位秒 |

响应数据样例：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 1800
  }
}
```

---

### 1.4 获取当前用户信息

#### 1.4.1 基本信息

> 请求路径：/api/auth/me
>
> 请求方式：GET
>
> 接口描述：该接口用于获取当前登录用户的个人信息

#### 1.4.2 请求参数

无

#### 1.4.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| \|- id | number | 必须 | 用户ID |
| \|- username | string | 必须 | 用户名 |
| \|- nickname | string | 必须 | 昵称 |
| \|- email | string | 必须 | 邮箱 |
| \|- phone | string | 必须 | 手机号 |
| \|- avatar | string | 非必须 | 头像 URL |
| \|- bio | string | 非必须 | 个人简介 |
| \|- createdAt | string | 必须 | 注册时间 |

响应数据样例：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 1,
    "username": "zhangsan",
    "nickname": "张三",
    "email": "zhangsan@example.com",
    "phone": "13800138000",
    "avatar": "http://localhost:9000/echospace/avatars/default.png",
    "bio": "这个人很懒，什么都没写",
    "createdAt": "2026-05-20 12:00:00"
  }
}
```

---

## 2. 用户模块

### 2.1 获取用户信息（个人主页）

#### 2.1.1 基本信息

> 请求路径：/api/users/{id}
>
> 请求方式：GET
>
> 接口描述：该接口用于查询指定用户的公开信息（个人主页展示）

#### 2.1.2 请求参数

参数格式：路径参数

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| id | number | 必须 | 用户ID |

请求参数样例：

```
/api/users/1
/api/users/3
```

#### 2.1.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| \|- id | number | 必须 | 用户ID |
| \|- nickname | string | 非必须 | 昵称（可为空） |
| \|- avatar | string | 非必须 | 头像 URL |
| \|- bio | string | 非必须 | 个人简介 |
| \|- postCount | number | 必须 | 发帖总数 |
| \|- followerCount | number | 必须 | 粉丝数 |
| \|- followingCount | number | 必须 | 关注数 |
| \|- createdAt | string | 必须 | 注册时间 |

响应数据样例：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 1,
    "nickname": "张三",
    "avatar": "http://localhost:9000/echospace/avatars/default.png",
    "bio": "这个人很懒，什么都没写",
    "postCount": 25,
    "followerCount": 128,
    "followingCount": 56,
    "createdAt": "2026-05-20 12:00:00"
  }
}
```

---

### 2.2 获取资料设置信息

#### 2.2.1 基本信息

> 请求路径：/api/users/me/profile
>
> 请求方式：GET
>
> 接口描述：该接口用于获取当前登录用户的资料设置信息（资料设置页面回显用）

#### 2.2.2 请求参数

无

#### 2.2.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| \|- avatar | string | 非必须 | 头像 URL |
| \|- nickname | string | 非必须 | 昵称（可为空） |
| \|- bio | string | 非必须 | 个人简介 |

响应数据样例：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "avatar": "http://localhost:9000/echospace/avatars/1.png",
    "nickname": "张三",
    "bio": "这个人很懒，什么都没写"
  }
}
```

---

### 2.3 获取账号设置信息

#### 2.3.1 基本信息

> 请求路径：/api/users/me/settings
>
> 请求方式：GET
>
> 接口描述：该接口用于获取当前登录用户的账号设置信息（账号设置页面回显用）

#### 2.3.2 请求参数

无

#### 2.3.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| \|- phone | string | 非必须 | 手机号 |
| \|- email | string | 非必须 | 邮箱 |

响应数据样例：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "phone": "13800138000",
    "email": "zhangsan@example.com"
  }
}
```

---

### 2.4 更新资料设置

#### 2.4.1 基本信息

> 请求路径：/api/users/me/profile
>
> 请求方式：PUT
>
> 接口描述：该接口用于更新当前登录用户的资料信息（头像、昵称、个人简介）

#### 2.4.2 请求参数

格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| avatar | string | 非必须 | 头像 URL |
| nickname | string | 非必须 | 昵称，最长 50 字 |
| bio | string | 非必须 | 个人简介，最长 500 字 |

请求参数样例：

```json
{
  "avatar": "http://localhost:9000/echospace/avatars/1.png",
  "nickname": "张三",
  "bio": "新个性签名"
}
```

#### 2.4.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据（成功时为 null） |

响应数据样例：

```json
{
  "code": 1,
  "msg": "更新成功",
  "data": null
}
```

---

### 2.5 更新账号设置

#### 2.5.1 基本信息

> 请求路径：/api/users/me/settings
>
> 请求方式：PUT
>
> 接口描述：该接口用于更新当前登录用户的账号信息（手机号、邮箱）

#### 2.5.2 请求参数

格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| phone | string | 非必须 | 手机号，需唯一 |
| email | string | 非必须 | 邮箱，需唯一 |

请求参数样例：

```json
{
  "phone": "13900139000",
  "email": "new@example.com"
}
```

#### 2.5.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据（成功时为 null） |

响应数据样例：

```json
{
  "code": 1,
  "msg": "更新成功",
  "data": null
}
```

---

### 2.6 修改密码

#### 2.6.1 基本信息

> 请求路径：/api/users/me/password
>
> 请求方式：PUT
>
> 接口描述：该接口用于修改当前登录用户的密码

#### 2.6.2 请求参数

格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| oldPassword | string | 必须 | 原密码 |
| newPassword | string | 必须 | 新密码，长度 6~20 |
| confirmPassword | string | 必须 | 确认新密码，须与 newPassword 一致 |

请求参数样例：

```json
{
  "oldPassword": "123456",
  "newPassword": "654321",
  "confirmPassword": "654321"
}
```

#### 2.6.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据（成功时为 null） |

响应数据样例：

```json
{
  "code": 1,
  "msg": "密码修改成功",
  "data": null
}
```

---

### 2.7 查询用户帖子列表

#### 2.7.1 基本信息

> 请求路径：/api/users/{id}/posts
>
> 请求方式：GET
>
> 接口描述：该接口用于游标分页查询指定用户发布的帖子列表（无限滚动加载）

#### 2.7.2 请求参数

参数格式：路径参数

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| id | number | 必须 | 用户ID |

参数格式：query 参数

参数说明：

| 参数名 | 类型 | 是否必须 | 默认值 | 备注 |
|--------|------|----------|--------|------|
| cursor | string | 非必须 | — | 游标，格式 `{timestamp}_{id}`，首次请求不传 |
| size | number | 非必须 | 10 | 每页条数 |
| sort | string | 非必须 | created_at | 排序字段：created_at(最新) / like_count(最热) |

请求参数样例：

```
/api/users/1/posts?size=10&sort=created_at
/api/users/1/posts?cursor=1704067200000_100&size=10&sort=created_at
```

#### 2.7.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| \|- cursor | string | 非必须 | 下页游标，格式 `{timestamp}_{id}`（无更多数据时为 null） |
| \|- hasMore | boolean | 必须 | 是否还有更多数据 |
| \|- count | number | 必须 | 本次返回的记录数 |
| \|- records | object[] | 必须 | 帖子列表 |
| \|- records[].id | number | 必须 | 帖子ID |
| \|- records[].title | string | 必须 | 标题 |
| \|- records[].coverImage | string | 非必须 | 封面图 URL |
| \|- records[].likeCount | number | 必须 | 点赞数 |
| \|- records[].commentCount | number | 必须 | 评论数 |
| \|- records[].isLiked | boolean | 必须 | 当前用户是否已点赞（未登录时为 false） |
| \|- records[].isCollected | boolean | 必须 | 当前用户是否已收藏（未登录时为 false） |
| \|- records[].createdAt | string | 必须 | 发布时间 |

响应数据样例：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "records": [
      {
        "id": 100,
        "title": "帖子标题",
        "coverImage": "http://localhost:9000/echospace/posts/100/cover.jpg",
        "likeCount": 32,
        "commentCount": 8,
        "isLiked": true,
        "isCollected": false,
        "createdAt": "2026-05-20 15:30:00"
      }
    ],
    "cursor": "1704067200000_100",
    "hasMore": true,
    "count": 10
  }
}
```

---

### 2.8 关注/取消关注

#### 2.8.1 基本信息

> 请求路径：/api/users/{id}/follow
>
> 请求方式：POST
>
> 接口描述：该接口用于关注或取消关注指定用户（toggle 模式：已关注则取消，未关注则关注）

#### 2.8.2 请求参数

参数格式：路径参数

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| id | number | 必须 | 被关注的用户ID |

#### 2.8.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| \|- followed | boolean | 必须 | true=已关注，false=已取消 |

响应数据样例：

```json
{
  "code": 1,
  "msg": "关注成功",
  "data": { "followed": true }
}
```

---

### 2.9 粉丝列表

#### 2.9.1 基本信息

> 请求路径：/api/users/{id}/followers
>
> 请求方式：GET
>
> 接口描述：该接口用于分页查询指定用户的粉丝列表

#### 2.9.2 请求参数

参数格式：路径参数

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| id | number | 必须 | 用户ID |

参数格式：query 参数

参数说明：

| 参数名 | 类型 | 是否必须 | 默认值 | 备注 |
|--------|------|----------|--------|------|
| current | number | 非必须 | 1 | 页码 |
| size | number | 非必须 | 10 | 每页条数 |

请求参数样例：

```
/api/users/1/followers?current=1&size=10
```

#### 2.9.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| \|- total | number | 必须 | 总记录数 |
| \|- current | number | 必须 | 当前页码 |
| \|- size | number | 必须 | 每页条数 |
| \|- records | object[] | 必须 | 粉丝列表 |
| \|- records[].id | number | 必须 | 用户ID |
| \|- records[].nickname | string | 非必须 | 昵称（可为空） |
| \|- records[].avatar | string | 非必须 | 头像 URL |
| \|- records[].followedAt | string | 必须 | 关注时间 |

响应数据样例：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "records": [
      {
        "id": 2,
        "nickname": "李四",
        "avatar": "http://...",
        "followedAt": "2026-05-19 10:00:00"
      }
    ],
    "total": 128,
    "current": 1,
    "size": 10
  }
}
```

---

### 2.10 关注列表

#### 2.10.1 基本信息

> 请求路径：/api/users/{id}/following
>
> 请求方式：GET
>
> 接口描述：该接口用于分页查询指定用户关注的人的列表

#### 2.10.2 请求参数

参数格式：路径参数

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| id | number | 必须 | 用户ID |

参数格式：query 参数

参数说明：

| 参数名 | 类型 | 是否必须 | 默认值 | 备注 |
|--------|------|----------|--------|------|
| current | number | 非必须 | 1 | 页码 |
| size | number | 非必须 | 10 | 每页条数 |

#### 2.10.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| \|- total | number | 必须 | 总记录数 |
| \|- current | number | 必须 | 当前页码 |
| \|- size | number | 必须 | 每页条数 |
| \|- records | object[] | 必须 | 关注列表 |
| \|- records[].id | number | 必须 | 用户ID |
| \|- records[].nickname | string | 非必须 | 昵称（可为空） |
| \|- records[].avatar | string | 非必须 | 头像 URL |
| \|- records[].followedAt | string | 必须 | 关注时间 |

响应数据样例：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "records": [
      {
        "id": 3,
        "nickname": "王五",
        "avatar": "http://...",
        "followedAt": "2026-05-19 10:00:00"
      }
    ],
    "total": 56,
    "current": 1,
    "size": 10
  }
}
```

---

## 3. 帖子模块

### 3.1 发布帖子

#### 3.1.1 基本信息

> 请求路径：/api/posts
>
> 请求方式：POST
>
> 接口描述：该接口用于发布新帖子（支持 Tiptap 富文本内容）

#### 3.1.2 请求参数

格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| title | string | 必须 | 标题，长度 1~200 |
| contentHtml | string | 必须 | Tiptap 生成的富文本 HTML（纯文本由后端 jsoup 自动提取，无需前端传） |

请求参数样例：

```json
{
  "title": "帖子标题",
  "contentHtml": "<p>这是正文内容，包含一张图片：</p><img src=\"http://localhost:9000/echospace/images/abc123.jpg\">"
}
```

#### 3.1.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| \|- id | number | 必须 | 新帖子ID |

响应数据样例：

```json
{
  "code": 1,
  "msg": "发布成功",
  "data": {
    "id": 100
  }
}
```

---

### 3.2 帖子详情

#### 3.2.1 基本信息

> 请求路径：/api/posts/{id}
>
> 请求方式：GET
>
> 接口描述：该接口用于根据ID查询帖子详情（含作者信息、点赞/收藏/关注状态）

#### 3.2.2 请求参数

参数格式：路径参数

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| id | number | 必须 | 帖子ID |

请求参数样例：

```
/api/posts/100
```

#### 3.2.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| \|- id | number | 必须 | 帖子ID |
| \|- title | string | 必须 | 标题 |
| \|- contentHtml | string | 必须 | 富文本 HTML |
| \|- author | object | 必须 | 作者信息 |
| \|- author.id | number | 必须 | 作者ID |
| \|- author.username | string | 必须 | 作者用户名 |
| \|- author.nickname | string | 非必须 | 作者昵称（可为空） |
| \|- author.avatar | string | 非必须 | 作者头像 URL |
| \|- likeCount | number | 必须 | 点赞数 |
| \|- commentCount | number | 必须 | 评论数 |
| \|- collectCount | number | 必须 | 收藏数 |
| \|- viewCount | number | 必须 | 浏览数 |
| \|- isLiked | boolean | 必须 | 当前用户是否已点赞 |
| \|- isCollected | boolean | 必须 | 当前用户是否已收藏 |
| \|- isFollowed | boolean | 必须 | 当前用户是否已关注作者 |
| \|- createdAt | string | 必须 | 发布时间 |
| \|- updatedAt | string | 必须 | 最后更新时间 |

响应数据样例：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 100,
    "title": "帖子标题",
    "contentHtml": "<p>富文本 HTML...</p><img src=\"...\">",
    "author": {
      "id": 1,
      "username": "zhangsan",
      "nickname": "张三",
      "avatar": "http://..."
    },
    "likeCount": 32,
    "commentCount": 8,
    "collectCount": 5,
    "viewCount": 256,
    "isLiked": true,
    "isCollected": false,
    "isFollowed": true,
    "createdAt": "2026-05-20 15:30:00",
    "updatedAt": "2026-05-20 15:30:00"
  }
}
```

---

### 3.3 编辑帖子

#### 3.3.1 基本信息

> 请求路径：/api/posts/{id}
>
> 请求方式：PUT
>
> 接口描述：该接口用于编辑帖子（只能修改自己的帖子）

#### 3.3.2 请求参数

参数格式：路径参数

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| id | number | 必须 | 帖子ID |

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| title | string | 必须 | 新标题，长度 1~200 |
| contentHtml | string | 必须 | 新的富文本 HTML（纯文本由后端 jsoup 自动提取，无需前端传） |

请求参数样例：

```json
{
  "title": "修改后的标题",
  "contentHtml": "<p>修改后的内容</p>"
}
```

#### 3.3.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据（成功时为 null） |

响应数据样例：

```json
{
  "code": 1,
  "msg": "修改成功",
  "data": null
}
```

---

### 3.4 删除帖子

#### 3.4.1 基本信息

> 请求路径：/api/posts/{id}
>
> 请求方式：DELETE
>
> 接口描述：该接口用于软删除帖子（只能删除自己的帖子）

#### 3.4.2 请求参数

参数格式：路径参数

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| id | number | 必须 | 帖子ID |

请求参数样例：

```
/api/posts/100
```

#### 3.4.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据（成功时为 null） |

响应数据样例：

```json
{
  "code": 1,
  "msg": "删除成功",
  "data": null
}
```

---

### 3.5 帖子列表

#### 3.5.1 基本信息

> 请求路径：/api/posts
>
> 请求方式：GET
>
> 接口描述：该接口用于游标分页查询首页帖子列表（无限滚动加载）

#### 3.5.2 请求参数

参数格式：query 参数

参数说明：

| 参数名 | 类型 | 是否必须 | 默认值 | 备注 |
|--------|------|----------|--------|------|
| cursor | string | 非必须 | — | 游标，格式 `{timestamp}_{id}`，首次请求不传 |
| size | number | 非必须 | 10 | 每页条数 |
| sort | string | 非必须 | created_at | 排序：created_at(最新) / hot(热门) |

请求参数样例：

```
/api/posts?size=10&sort=created_at
/api/posts?cursor=1704067200000_100&size=10&sort=created_at
```

#### 3.5.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| \|- cursor | string | 非必须 | 下页游标，格式 `{timestamp}_{id}`（无更多数据时为 null） |
| \|- hasMore | boolean | 必须 | 是否还有更多数据 |
| \|- count | number | 必须 | 本次返回的记录数 |
| \|- records | object[] | 必须 | 帖子列表 |
| \|- records[].id | number | 必须 | 帖子ID |
| \|- records[].title | string | 必须 | 标题 |
| \|- records[].coverImage | string | 非必须 | 封面图 URL |
| \|- records[].contentText | string | 非必须 | 纯文本摘要（前200字） |
| \|- records[].author | object | 必须 | 作者信息 |
| \|- records[].author.id | number | 必须 | 作者ID |
| \|- records[].author.username | string | 必须 | 作者用户名 |
| \|- records[].author.nickname | string | 非必须 | 作者昵称（可为空） |
| \|- records[].author.avatar | string | 非必须 | 作者头像 URL |
| \|- records[].likeCount | number | 必须 | 点赞数 |
| \|- records[].commentCount | number | 必须 | 评论数 |
| \|- records[].collectCount | number | 必须 | 收藏数 |
| \|- records[].isLiked | boolean | 必须 | 当前用户是否已点赞（未登录时为 false） |
| \|- records[].isCollected | boolean | 必须 | 当前用户是否已收藏（未登录时为 false） |
| \|- records[].createdAt | string | 必须 | 发布时间 |

响应数据样例：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "records": [
      {
        "id": 100,
        "title": "帖子标题",
        "coverImage": "http://...",
        "contentText": "纯文本前200字...",
        "author": {
          "id": 1,
          "username": "zhangsan",
          "nickname": "张三",
          "avatar": "http://..."
        },
        "likeCount": 32,
        "commentCount": 8,
        "collectCount": 5,
        "isLiked": true,
        "isCollected": false,
        "createdAt": "2026-05-20 15:30:00"
      }
    ],
    "cursor": "1704067200000_100",
    "hasMore": true,
    "count": 10
  }
}
```

---

### 3.6 帖子点赞/取消

#### 3.6.1 基本信息

> 请求路径：/api/posts/{id}/like
>
> 请求方式：POST
>
> 接口描述：该接口用于对帖子进行点赞或取消点赞（toggle 模式）

#### 3.6.2 请求参数

参数格式：路径参数

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| id | number | 必须 | 帖子ID |

#### 3.6.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| \|- liked | boolean | 必须 | true=已点赞，false=已取消 |
| \|- likeCount | number | 必须 | 操作后的最新点赞数 |

响应数据样例：

```json
{
  "code": 1,
  "msg": "点赞成功",
  "data": { "liked": true, "likeCount": 33 }
}
```

---

### 3.7 帖子收藏/取消

#### 3.7.1 基本信息

> 请求路径：/api/posts/{id}/favorite
>
> 请求方式：POST
>
> 接口描述：该接口用于收藏或取消收藏帖子（toggle 模式）

#### 3.7.2 请求参数

参数格式：路径参数

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| id | number | 必须 | 帖子ID |

#### 3.7.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| \|- favorited | boolean | 必须 | true=已收藏，false=已取消 |

响应数据样例：

```json
{
  "code": 1,
  "msg": "收藏成功",
  "data": { "favorited": true }
}
```

---

### 3.8 我的收藏列表

#### 3.8.1 基本信息

> 请求路径：/api/posts/favorites
>
> 请求方式：GET
>
> 接口描述：该接口用于分页查询当前登录用户的收藏帖子列表

#### 3.8.2 请求参数

参数格式：query 参数

参数说明：

| 参数名 | 类型 | 是否必须 | 默认值 | 备注 |
|--------|------|----------|--------|------|
| current | number | 非必须 | 1 | 页码 |
| size | number | 非必须 | 10 | 每页条数 |

请求参数样例：

```
/api/posts/favorites?current=1&size=10
```

#### 3.8.3 响应数据

参数格式：application/json

参数说明：结构同 3.5 帖子列表。

---

### 3.9 搜索帖子

#### 3.9.1 基本信息

> 请求路径：/api/posts/search
>
> 请求方式：GET
>
> 接口描述：该接口用于通过 Elasticsearch 对帖子进行全文搜索

#### 3.9.2 请求参数

参数格式：query 参数

参数说明：

| 参数名 | 类型 | 是否必须 | 默认值 | 备注 |
|--------|------|----------|--------|------|
| q | string | 必须 | — | 搜索关键词 |
| current | number | 非必须 | 1 | 页码 |
| size | number | 非必须 | 10 | 每页条数 |
| sort | string | 非必须 | created_at | 排序：created_at(最新) / hot(最热) |

请求参数样例：

```
/api/posts/search?q=Spring Boot&current=1&size=10
```

#### 3.9.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| \|- total | number | 必须 | 总记录数 |
| \|- current | number | 必须 | 当前页码 |
| \|- size | number | 必须 | 每页条数 |
| \|- records | object[] | 必须 | 搜索结果列表 |
| \|- records[].id | number | 必须 | 帖子ID |
| \|- records[].title | string | 必须 | 标题（含 `<em>` 高亮标签） |
| \|- records[].coverImage | string | 非必须 | 封面图 URL |
| \|- records[].contentText | string | 非必须 | 纯文本摘要（含 `<em>` 高亮标签） |
| \|- records[].author | object | 必须 | 作者信息 |
| \|- records[].author.id | number | 必须 | 作者ID |
| \|- records[].author.username | string | 必须 | 作者用户名 |
| \|- records[].author.nickname | string | 非必须 | 作者昵称（可为空） |
| \|- records[].author.avatar | string | 非必须 | 作者头像 URL |
| \|- records[].likeCount | number | 必须 | 点赞数 |
| \|- records[].commentCount | number | 必须 | 评论数 |
| \|- records[].collectCount | number | 必须 | 收藏数 |
| \|- records[].createdAt | string | 必须 | 发布时间 |

> 搜索关键词在 `title` 和 `contentText` 中由 ES highlight 功能以 `<em>` 标签包裹高亮。

响应数据样例：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "records": [
      {
        "id": 55,
        "title": "如何在<em>Spring Boot</em>中配置Redis",
        "coverImage": "http://...",
        "contentText": "...Spring Boot配置<em>Redis</em>的步骤...",
        "author": {
          "id": 3,
          "username": "wangwu",
          "nickname": "王五",
          "avatar": "http://..."
        },
        "likeCount": 15,
        "commentCount": 3,
        "collectCount": 2,
        "createdAt": "2026-05-18 09:00:00"
      }
    ],
    "total": 12,
    "current": 1,
    "size": 10
  }
}
```

---

## 4. 评论模块

### 4.1 发表评论/回复

#### 4.1.1 基本信息

> 请求路径：/api/posts/{postId}/comments
>
> 请求方式：POST
>
> 接口描述：该接口用于发表一级评论或二级回复

#### 4.1.2 请求参数

参数格式：路径参数

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| postId | number | 必须 | 帖子ID |

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| parentId | number | 必须 | 父评论ID：0=一级评论，非0=二级回复 |
| replyToUid | number | 非必须 | 被回复的用户ID（仅二级回复时填写） |
| content | string | 必须 | 评论内容，长度 1~5000 |

请求参数样例：

一级评论：
```json
{
  "parentId": 0,
  "content": "写得太好了，支持！"
}
```

二级回复：
```json
{
  "parentId": 15,
  "replyToUid": 3,
  "content": "@wangwu 你说得对，我补充一下..."
}
```

#### 4.1.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| \|- id | number | 必须 | 新评论ID |

响应数据样例：

```json
{
  "code": 1,
  "msg": "评论成功",
  "data": {
    "id": 20
  }
}
```

---

### 4.2 评论列表（一级评论 + 二级回复）

#### 4.2.1 基本信息

> 请求路径：/api/posts/{postId}/comments
>
> 请求方式：GET
>
> 接口描述：该接口用于游标分页查询帖子的评论列表（无限滚动加载），每条一级评论附带若干条二级回复

#### 4.2.2 请求参数

参数格式：路径参数

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| postId | number | 必须 | 帖子ID |

参数格式：query 参数

参数说明：

| 参数名 | 类型 | 是否必须 | 默认值 | 备注 |
|--------|------|----------|--------|------|
| cursor | string | 非必须 | — | 游标，格式 `{timestamp}_{id}`，首次请求不传 |
| size | number | 非必须 | 10 | 每页一级评论条数 |
| replySize | number | 非必须 | 3 | 每条一级评论下预加载的二级回复数 |

请求参数样例：

```
/api/posts/100/comments?size=10&replySize=3
/api/posts/100/comments?cursor=1704067200000_15&size=10&replySize=3
```

#### 4.2.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| \|- cursor | string | 非必须 | 下页游标，格式 `{timestamp}_{id}`（无更多数据时为 null） |
| \|- hasMore | boolean | 必须 | 是否还有更多数据 |
| \|- count | number | 必须 | 本次返回的记录数 |
| \|- records | object[] | 必须 | 一级评论列表 |
| \|- records[].id | number | 必须 | 评论ID |
| \|- records[].postId | number | 必须 | 所属帖子ID |
| \|- records[].user | object | 必须 | 评论者信息 |
| \|- records[].user.id | number | 必须 | 评论者ID |
| \|- records[].user.username | string | 必须 | 评论者用户名 |
| \|- records[].user.nickname | string | 非必须 | 评论者昵称（可为空） |
| \|- records[].user.avatar | string | 非必须 | 评论者头像 URL |
| \|- records[].content | string | 必须 | 评论内容 |
| \|- records[].likeCount | number | 必须 | 点赞数 |
| \|- records[].isLiked | boolean | 必须 | 当前用户是否已点赞 |
| \|- records[].createdAt | string | 必须 | 评论时间 |
| \|- records[].replies | object[] | 必须 | 预加载的二级回复列表 |
| \|- records[].replies[].id | number | 必须 | 回复ID |
| \|- records[].replies[].postId | number | 必须 | 所属帖子ID |
| \|- records[].replies[].parentId | number | 必须 | 父评论ID |
| \|- records[].replies[].user | object | 必须 | 回复者信息 |
| \|- records[].replies[].user.id | number | 必须 | 回复者ID |
| \|- records[].replies[].user.username | string | 必须 | 回复者用户名 |
| \|- records[].replies[].user.nickname | string | 非必须 | 回复者昵称（可为空） |
| \|- records[].replies[].user.avatar | string | 非必须 | 回复者头像 URL |
| \|- records[].replies[].replyToUser | object | 非必须 | 被回复的用户信息 |
| \|- records[].replies[].replyToUser.id | number | 必须 | 被回复的用户ID |
| \|- records[].replies[].replyToUser.username | string | 必须 | 被回复的用户用户名 |
| \|- records[].replies[].replyToUser.nickname | string | 非必须 | 被回复的用户昵称（可为空） |
| \|- records[].replies[].content | string | 必须 | 回复内容 |
| \|- records[].replies[].likeCount | number | 必须 | 点赞数 |
| \|- records[].replies[].isLiked | boolean | 必须 | 当前用户是否已点赞 |
| \|- records[].replies[].createdAt | string | 必须 | 回复时间 |
| \|- records[].replyCount | number | 必须 | 该评论的二级回复总数 |
| \|- records[].hasMoreReplies | boolean | 必须 | 是否还有更多二级回复 |

> 每条一级评论默认最多带 3 条二级回复（`replySize=3`）。`hasMoreReplies=true` 时前端展示"查看更多回复"按钮。

响应数据样例：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "records": [
      {
        "id": 15,
        "postId": 100,
        "user": {
          "id": 3,
          "username": "wangwu",
          "nickname": "王五",
          "avatar": "http://..."
        },
        "content": "写得太好了，支持！",
        "likeCount": 5,
        "isLiked": false,
        "createdAt": "2026-05-20 16:00:00",
        "replies": [
          {
            "id": 20,
            "postId": 100,
            "parentId": 15,
            "user": {
              "id": 1,
              "username": "zhangsan",
              "nickname": "张三",
              "avatar": "http://..."
            },
            "replyToUser": {
              "id": 3,
              "username": "wangwu",
              "nickname": "王五"
            },
            "content": "谢谢支持！",
            "likeCount": 1,
            "isLiked": false,
            "createdAt": "2026-05-20 16:10:00"
          }
        ],
        "replyCount": 8,
        "hasMoreReplies": true
      }
    ],
    "cursor": "1704067200000_15",
    "hasMore": true,
    "count": 10
  }
}
```

---

### 4.3 查询更多二级回复

#### 4.3.1 基本信息

> 请求路径：/api/comments/{commentId}/replies
>
> 请求方式：GET
>
> 接口描述：该接口用于分页查询指定一级评论下的更多二级回复

#### 4.3.2 请求参数

参数格式：路径参数

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| commentId | number | 必须 | 一级评论ID |

参数格式：query 参数

参数说明：

| 参数名 | 类型 | 是否必须 | 默认值 | 备注 |
|--------|------|----------|--------|------|
| current | number | 非必须 | 1 | 页码 |
| size | number | 非必须 | 10 | 每页条数 |

请求参数样例：

```
/api/comments/15/replies?current=1&size=10
```

#### 4.3.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| \|- total | number | 必须 | 总记录数 |
| \|- current | number | 必须 | 当前页码 |
| \|- size | number | 必须 | 每页条数 |
| \|- records | object[] | 必须 | 二级回复列表（字段结构同 4.2 中 replies[] 项） |

响应数据样例：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "records": [
      {
        "id": 21,
        "postId": 100,
        "parentId": 15,
        "user": { "id": 5, "username": "zhaoliu", "avatar": "http://..." },
        "replyToUser": { "id": 1, "username": "zhangsan" },
        "content": "我也觉得很好",
        "likeCount": 0,
        "isLiked": false,
        "createdAt": "2026-05-20 16:15:00"
      }
    ],
    "total": 8,
    "current": 1,
    "size": 10
  }
}
```

---

### 4.4 删除评论

#### 4.4.1 基本信息

> 请求路径：/api/comments/{id}
>
> 请求方式：DELETE
>
> 接口描述：该接口用于软删除评论（只能删除自己的评论）。删除一级评论时，其下所有二级回复也一并软删除

#### 4.4.2 请求参数

参数格式：路径参数

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| id | number | 必须 | 评论ID |

请求参数样例：

```
/api/comments/20
```

#### 4.4.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据（成功时为 null） |

响应数据样例：

```json
{
  "code": 1,
  "msg": "删除成功",
  "data": null
}
```

---

### 4.5 评论点赞/取消

#### 4.5.1 基本信息

> 请求路径：/api/comments/{id}/like
>
> 请求方式：POST
>
> 接口描述：该接口用于对评论进行点赞或取消点赞（toggle 模式）

#### 4.5.2 请求参数

参数格式：路径参数

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| id | number | 必须 | 评论ID |

#### 4.5.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| \|- liked | boolean | 必须 | true=已点赞，false=已取消 |

响应数据样例：

```json
{
  "code": 1,
  "msg": "点赞成功",
  "data": { "liked": true }
}
```

---

## 5. 文件上传模块

> 文件上传提供 2 个通用接口：上传图片（帖子用）和上传头像。底层存储由 `storage.type` 配置决定注入 `MinioFileServiceImpl` 还是 `OssFileServiceImpl`，前端无需感知后端存储方案。

---

### 5.1 上传图片

#### 5.1.1 基本信息

> 请求路径：/api/upload/image
>
> 请求方式：POST
>
> 接口描述：上传帖子中的图片（Tiptap 编辑器使用），前端拿到返回的 URL 后自动插入编辑器。底层存储方案由后端配置决定。

#### 5.1.2 请求参数

参数格式：multipart/form-data

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| file | file | 必须 | 图片文件，限制 jpg/png/gif/webp，最大 10MB |

#### 5.1.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| \|- url | string | 必须 | 图片访问 URL |

响应数据样例：

```json
{
  "code": 1,
  "msg": "上传成功",
  "data": {
    "url": "http://localhost:9000/echospace/images/2026/05/abc123.jpg"
  }
}
```

---

### 5.2 上传头像

#### 5.2.1 基本信息

> 请求路径：/api/upload/avatar
>
> 请求方式：POST
>
> 接口描述：上传用户头像。底层存储方案由后端配置决定。

#### 5.2.2 请求参数

参数格式：multipart/form-data

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| file | file | 必须 | 图片，限制 jpg/png，最大 2MB |

#### 5.2.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| \|- url | string | 必须 | 头像访问 URL |

响应数据样例：

```json
{
  "code": 1,
  "msg": "上传成功",
  "data": {
    "url": "http://localhost:9000/echospace/avatars/1/abc456.jpg"
  }
}
```

---

### 5.3 删除文件

#### 5.3.1 基本信息

> 请求路径：/api/upload/file
>
> 请求方式：DELETE
>
> 接口描述：根据上传时返回的 URL 删除对应文件，用于更新失败回滚等场景。仅允许删除当前存储桶内的文件。

#### 5.3.2 请求参数

参数格式：query string

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| url | string | 必须 | 上传时返回的文件访问 URL |

请求示例：`DELETE /api/upload/file?url=http://localhost:9000/echospace/avatars/uuid.jpg`

#### 5.3.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | null | 非必须 | 无返回数据 |

响应数据样例：

```json
{
  "code": 1,
  "msg": null,
  "data": null
}
```

---

## 6. 管理模块

### 6.1 同步帖子到 ES

#### 6.1.1 基本信息

> 请求路径：/api/admin/sync-es
>
> 请求方式：POST
>
> 接口描述：将数据库中所有未删除的帖子全量写入 Elasticsearch 索引。由管理员手动触发，返回成功同步的帖子数量。

#### 6.1.2 请求参数

无请求参数。需携带认证 Token。

#### 6.1.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
|--------|------|----------|------|
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | number | 非必须 | 成功同步的帖子数量 |

响应数据样例：

```json
{
  "code": 1,
  "msg": "ES 同步完成，共同步 24 条帖子",
  "data": 24
}
```

---

## 附录：接口汇总表

| 序号 | 方法 | 路径 | 说明 | 模块 |
|------|------|------|------|------|
| 1 | POST | /api/auth/register | 用户注册 | 认证 |
| 2 | POST | /api/auth/login | 用户登录 | 认证 |
| 3 | POST | /api/auth/refresh | 刷新 Token | 认证 |
| 4 | GET | /api/auth/me | 当前用户信息 | 认证 |
| 5 | GET | /api/users/{id} | 用户信息（个人主页） | 用户 |
| 6 | GET | /api/users/me/profile | 资料设置信息 | 用户 |
| 7 | GET | /api/users/me/settings | 账号设置信息 | 用户 |
| 8 | PUT | /api/users/me/profile | 更新资料设置 | 用户 |
| 9 | PUT | /api/users/me/settings | 更新账号设置 | 用户 |
| 10 | PUT | /api/users/me/password | 修改密码 | 用户 |
| 11 | GET | /api/users/{id}/posts | 用户帖子列表 | 用户 |
| 12 | POST | /api/users/{id}/follow | 关注/取消（toggle） | 用户 |
| 13 | GET | /api/users/{id}/followers | 粉丝列表 | 用户 |
| 14 | GET | /api/users/{id}/following | 关注列表 | 用户 |
| 15 | POST | /api/posts | 发布帖子 | 帖子 |
| 16 | GET | /api/posts/{id} | 帖子详情 | 帖子 |
| 17 | PUT | /api/posts/{id} | 编辑帖子 | 帖子 |
| 18 | DELETE | /api/posts/{id} | 删除帖子（软删除） | 帖子 |
| 19 | GET | /api/posts | 帖子列表（分页） | 帖子 |
| 20 | POST | /api/posts/{id}/like | 帖子点赞/取消（toggle） | 帖子 |
| 21 | POST | /api/posts/{id}/favorite | 帖子收藏/取消（toggle） | 帖子 |
| 22 | GET | /api/posts/favorites | 我的收藏列表 | 帖子 |
| 23 | GET | /api/posts/search | 搜索帖子（ES） | 帖子 |
| 24 | POST | /api/posts/{postId}/comments | 发表评论/回复 | 评论 |
| 25 | GET | /api/posts/{postId}/comments | 评论列表（游标分页，含二级回复） | 评论 |
| 26 | GET | /api/comments/{id}/replies | 加载更多二级回复 | 评论 |
| 27 | DELETE | /api/comments/{id} | 删除评论 | 评论 |
| 28 | POST | /api/comments/{id}/like | 评论点赞/取消（toggle） | 评论 |
| 29 | POST | /api/upload/image | 上传图片 | 文件 |
| 30 | POST | /api/upload/avatar | 上传头像 | 文件 |
| 31 | DELETE | /api/upload/file | 删除文件（回滚清理） | 文件 |
| 32 | POST | /api/admin/sync-es | 全量同步帖子到 ES | 管理 |
