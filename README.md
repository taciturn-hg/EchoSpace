# EchoSpace

基于 **Vue 3 + Spring Boot** 的现代社区论坛系统，支持帖子发布浏览、富文本编辑、嵌套评论、全文搜索、用户关注等社交功能。

[![Java 17](https://img.shields.io/badge/Java-17-brightgreen.svg)](https://adoptium.net/)
[![Spring Boot 3.5](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue 3.5](https://img.shields.io/badge/Vue-3.5-4fc08d.svg)](https://vuejs.org/)
[![TypeScript 6](https://img.shields.io/badge/TypeScript-6-3178c6.svg)](https://www.typescriptlang.org/)
[![MySQL 8](https://img.shields.io/badge/MySQL-8-4479a1.svg)](https://www.mysql.com/)
[![Elasticsearch 8](https://img.shields.io/badge/Elasticsearch-8-005571.svg)](https://www.elastic.co/elasticsearch/)

---

## ✨ 功能特性

- **用户系统** — 注册 / 登录 / JWT 双 Token 鉴权、个人信息编辑、密码修改
- **帖子管理** — 富文本发布编辑（Tiptap）、游标分页、点赞、收藏
- **评论互动** — 一级评论 + 二级回复、嵌套展示、点赞切换
- **全文搜索** — Elasticsearch + ik 中文分词、关键词高亮
- **社交关注** — 关注 / 取消关注、粉丝列表、关注列表
- **文件上传** — 头像 / 文章图片上传，MinIO 存储（可切换 OSS）
- **XSS 防护** — DOMPurify 前端消毒 + Jsoup 服务端清洗，双重防护

## 🏗️ 技术栈

| 层 | 后端 (echo-server) | 前端 (echo-web) |
|---|---|---|
| 核心 | Spring Boot 3.5 + Java 17 | Vue 3.5 + TypeScript 6 |
| 构建 | Maven | Vite 8 |
| ORM | MyBatis-Plus 3.5.7 | — |
| 数据库 | MySQL 8 | — |
| 搜索 | Elasticsearch 8 (ik 分词) | — |
| 存储 | MinIO | — |
| 安全 | Spring Security + JWT (jjwt 0.12.6) | Axios 拦截器 |
| UI | — | Element Plus 2 + @lucide/vue |
| 富文本 | — | TipTap 3 + DOMPurify 3 |
| 状态 | — | Pinia 3 + Vue Router 5 |

## 📁 项目结构

```
EchoSpace/
├── echo-server/                         # Spring Boot 后端
│   ├── controller/                      # REST 控制器
│   │   ├── AuthController.java          # 认证接口（注册/登录/刷新）
│   │   ├── UserController.java          # 用户接口（/me）
│   │   ├── UserPublicController.java    # 用户公开接口
│   │   ├── PostController.java          # 帖子 CRUD + 分页
│   │   ├── CommentController.java       # 评论 CRUD
│   │   ├── FileController.java          # 文件上传
│   │   └── AdminController.java         # 管理接口
│   ├── service/impl/                    # 服务层实现
│   ├── security/                        # JWT 过滤器 + 安全配置
│   ├── mapper/                          # MyBatis Mapper + XML
│   └── common/                          # 统一响应、异常处理
├── echo-web/                            # Vue 3 前端
│   ├── views/                           # 页面组件
│   │   ├── HomePage.vue                 # 首页（帖子列表）
│   │   ├── PostDetail.vue               # 帖子详情 + 评论区
│   │   ├── PostCreate.vue               # 写帖子（Tiptap 富文本）
│   │   ├── SearchPage.vue               # 全文搜索
│   │   ├── UserProfile.vue              # 用户主页
│   │   └── *Settings*.vue               # 设置页（资料/密码）
│   ├── components/                      # 通用组件（PostCard、CommentThread 等）
│   ├── composables/                     # useInfiniteList 等组合式函数
│   ├── api/                             # Axios API 封装 + 类型定义
│   ├── stores/                          # Pinia 状态管理
│   └── router/                          # 路由守卫
└── docs/                                # 项目文档
    ├── 01-requirements-and-plan.md      # 需求分析与技术方案
    ├── 02-api-documentation.md          # API 接口文档
    ├── 03-database-design.md            # 数据库设计
    ├── 04-development-plan.md           # 开发计划与 Sprint 规划
    └── 05-feature-flows.md              # 功能流程图
```

## 🚀 快速开始

### 环境要求

- **JDK 17+**
- **Node.js 24+** + pnpm
- **MySQL 8.0+**
- **Elasticsearch 8.x**（需安装 ik 分词器插件）
- **MinIO**（本地开发直接下载二进制运行即可）

### 后端启动

1. 创建 MySQL 数据库并执行建表脚本：

```bash
mysql -u root -p < docs/schema.sql
```

2. 修改 `echo-server/src/main/resources/application.yml` 中的数据库、ES、MinIO 连接信息。

3. 启动 Elasticsearch 和 MinIO。

4. 启动 Spring Boot 应用：

```bash
cd echo-server
mvn spring-boot:run
```

### 前端启动

```bash
cd echo-web
pnpm install
pnpm dev
```

访问 `http://localhost:5173`。

## 📖 文档

| 文档 | 说明 |
|------|------|
| `.claude/CLAUDE.md` | 开发规范与约定速查 |
| `.claude/PROJECT_ARCHITECTURE.md` | 完整架构设计、Sprint 历史 |
| `docs/01-requirements-and-plan.md` | 需求分析与技术选型 |
| `docs/02-api-documentation.md` | REST API 接口文档 |
| `docs/03-database-design.md` | 数据库表结构设计 |
| `docs/04-development-plan.md` | Sprint 开发计划 |
| `docs/05-feature-flows.md` | 功能流程与数据流 |
| `docs/Question.md` | 开发问题记录与解决方案 |

## 🧭 开发路线

- [x] Sprint 1 — 认证基础（注册/登录/JWT）
- [x] Sprint 2 — 用户管理（资料/设置/头像）
- [x] Sprint 3 — 帖子系统（CRUD/富文本/分页）
- [x] Sprint 4 — 评论系统（嵌套评论/点赞）
- [x] Sprint 5 — 搜索系统（ES/ik 分词/高亮）
- [x] Sprint 6 — 关注系统（关注/粉丝/个人主页）
- [ ] 忘记密码 / 重置密码（Redis 接入）
- [ ] 关注时间线
- [ ] 消息通知（RabbitMQ）
- [ ] 阿里云 OSS 切换
- [ ] Docker 部署

## 🤝 贡献

个人练手项目，欢迎提 Issue 交流讨论。

## 📄 License

MIT
