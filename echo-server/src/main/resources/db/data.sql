-- ============================================================
-- EchoSpace 测试数据
-- ============================================================
-- 用法:
--   1. 先启动应用完成 init.sql 建表
--   2. 通过注册接口创建用户（密码使用 BCrypt 加密），或手动执行下方 INSERT
--   3. 执行本文件中的帖子 INSERT
--
-- 注意: 用户密码通过 BCrypt 加密，下方占位 hash 可能不匹配。
--       建议先通过 POST /api/auth/register 注册以下 5 个用户，
--       然后只执行帖子部分（搜索 "-- 帖子" 定位）。
-- ============================================================

USE echospace;

-- ============================================================
-- 用户（密码统一为 123456 的 BCrypt 哈希，cost=10）
-- 如果你自行生成哈希，替换下面的 password 值即可
-- ============================================================
INSERT INTO user (id, username, nickname, email, phone, password, avatar, bio, status, created_at) VALUES
(1001, 'admin',        'Echo 官方',   'admin@echospace.local',    '13800001001', '$2a$10$xVqYLGEMC1JmKqiQKVfLBeK6q8qM5h7qP1vqGcMmFqZ9xVqYLGEM', 'https://picsum.photos/seed/admin-avatar/100/100',    'EchoSpace 官方账号',                              1, '2025-01-15 10:00:00'),
(2048, 'vue_master',   '前端小王子',  'vue@echospace.local',      '13800002048', '$2a$10$xVqYLGEMC1JmKqiQKVfLBeK6q8qM5h7qP1vqGcMmFqZ9xVqYLGEM', NULL,                                                  'Vue 技术爱好者',                                  1, '2025-02-20 14:30:00'),
(337,  'cycling_fan',  NULL,          'cycling@echospace.local',  '13800003370', '$2a$10$xVqYLGEMC1JmKqiQKVfLBeK6q8qM5h7qP1vqGcMmFqZ9xVqYLGEM', 'https://picsum.photos/seed/cycling-avatar/100/100', '热爱骑行，热爱生活',                              1, '2025-03-10 08:15:00'),
(512,  'java_dev',     'Java 架构师', 'java@echospace.local',     '13800005120', '$2a$10$xVqYLGEMC1JmKqiQKVfLBeK6q8qM5h7qP1vqGcMmFqZ9xVqYLGEM', 'https://picsum.photos/seed/java-avatar/100/100',    'Spring Boot / Microservices 实践者',              1, '2025-01-28 16:45:00'),
(789,  'ts_learner',   '小菜鸟',      'ts@echospace.local',       '13800007890', '$2a$10$xVqYLGEMC1JmKqiQKVfLBeK6q8qM5h7qP1vqGcMmFqZ9xVqYLGEM', NULL,                                                  'TypeScript 进阶中',                               1, '2025-04-01 12:00:00');

-- ============================================================
-- 帖子（23 条，覆盖 3 页，每页 10 条）
-- 发布时间从 30 分钟前到 7 天前，模拟真实时间线
-- ============================================================
INSERT INTO post (id, user_id, title, content_html, content_text, cover_image, like_count, comment_count, collect_count, view_count, status, created_at) VALUES
(1,  1001, '欢迎来到 EchoSpace！开启你的社区之旅',
       '<p>EchoSpace 是一个现代化的社区平台，在这里你可以自由地分享想法、交流技术、记录生活。</p>',
       'EchoSpace 是一个现代化的社区平台，在这里你可以自由地分享想法、交流技术、记录生活。',
       'https://picsum.photos/seed/post1/800/500', 128, 36, 52, 1520, 1, DATE_SUB(NOW(), INTERVAL 30 MINUTE)),

(2,  2048, '分享一下我的 Vue 3 项目架构实践',
       '<p>最近在重构一个中大型项目，采用了 Vue 3 + TypeScript + Pinia 的技术栈，整体架构按照功能模块划分。</p>',
       '最近在重构一个中大型项目，采用了 Vue 3 + TypeScript + Pinia 的技术栈，整体架构按照功能模块划分。',
       NULL, 256, 89, 143, 3200, 1, DATE_SUB(NOW(), INTERVAL 3 HOUR)),

(3,  337,  '周末骑行随拍',
       '<p>趁着天气好沿着江边骑了一圈，拍了几张照片记录一下沿途风景。</p>',
       '趁着天气好沿着江边骑了一圈，拍了几张照片记录一下沿途风景。',
       'https://picsum.photos/seed/post3/800/500', 67, 12, 8, 890, 1, DATE_SUB(NOW(), INTERVAL 12 HOUR)),

(4,  512,  'Spring Boot 3.5 升级踩坑记录：从 2.x 迁移到 3.x 的完整指南',
       '<p>上周把项目从 Spring Boot 2.7 升级到了 3.5，遇到了不少兼容性问题，这里记录完整的迁移步骤。</p>',
       '上周把项目从 Spring Boot 2.7 升级到了 3.5，遇到了不少兼容性问题，这里记录完整的迁移步骤。',
       'https://picsum.photos/seed/post4/800/500', 342, 156, 289, 5600, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),

(5,  789,  '有没有推荐的 TypeScript 进阶书？',
       '<p>断断续续用了一段时间 TypeScript，但感觉还停留在比较基础的阶段，想深入了解一下类型体操。</p>',
       '断断续续用了一段时间 TypeScript，但感觉还停留在比较基础的阶段，想深入了解一下类型体操。',
       NULL, 45, 73, 16, 1100, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),

(6,  1001, 'Docker 容器化部署最佳实践总结',
       '<p>总结了最近在 Docker 容器化部署过程中遇到的一些问题和解决方案，分享给大家参考。</p>',
       '总结了最近在 Docker 容器化部署过程中遇到的一些问题和解决方案，分享给大家参考。',
       'https://picsum.photos/seed/post6/800/500', 98, 42, 67, 1800, 1, DATE_SUB(NOW(), INTERVAL 15 HOUR)),

(7,  2048, '聊聊微服务架构中的服务间通信',
       '<p>微服务架构中服务间通信方式众多，REST、gRPC、消息队列各有优劣，聊聊我的选型经验。</p>',
       '微服务架构中服务间通信方式众多，REST、gRPC、消息队列各有优劣，聊聊我的选型经验。',
       NULL, 178, 95, 120, 2900, 1, DATE_SUB(NOW(), INTERVAL 20 HOUR)),

(8,  337,  '前端性能优化的 10 个实用技巧',
       '<p>从资源加载、渲染优化、缓存策略等角度总结了前端性能优化的实用技巧。</p>',
       '从资源加载、渲染优化、缓存策略等角度总结了前端性能优化的实用技巧。',
       NULL, 210, 68, 155, 3400, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),

(9,  512,  'Go 语言并发编程入门笔记',
       '<p>记录学习 Go 语言并发编程的过程，goroutine 和 channel 的使用场景与注意事项。</p>',
       '记录学习 Go 语言并发编程的过程，goroutine 和 channel 的使用场景与注意事项。',
       'https://picsum.photos/seed/post9/800/500', 156, 44, 89, 2100, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),

(10, 789,  'RESTful API 设计规范与经验分享',
       '<p>好的 API 设计能大幅提升开发体验，分享一下我在项目中的一些实践和规范。</p>',
       '好的 API 设计能大幅提升开发体验，分享一下我在项目中的一些实践和规范。',
       NULL, 132, 57, 98, 2700, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),

(11, 1001, '我的 2025 年技术成长回顾',
       '<p>回顾过去一年的技术成长，从后端到前端，从单体到微服务，每一步都值得记录。</p>',
       '回顾过去一年的技术成长，从后端到前端，从单体到微服务，每一步都值得记录。',
       NULL, 89, 31, 42, 1300, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),

(12, 2048, '开源项目维护的苦与乐',
       '<p>维护开源项目一年多，遇到了形形色色的人和事，有感动也有无奈，分享一下心得。</p>',
       '维护开源项目一年多，遇到了形形色色的人和事，有感动也有无奈，分享一下心得。',
       'https://picsum.photos/seed/post12/800/500', 267, 112, 178, 4200, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),

(13, 337,  '如何高效进行 Code Review',
       '<p>Code Review 是保障代码质量的重要环节，总结一些让 Review 更高效的实践经验。</p>',
       'Code Review 是保障代码质量的重要环节，总结一些让 Review 更高效的实践经验。',
       NULL, 145, 83, 102, 2300, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),

(14, 512,  '数据库索引优化实战案例',
       '<p>分享一个生产环境慢 SQL 优化的完整过程，从分析到落地，QPS 提升了 20 倍。</p>',
       '分享一个生产环境慢 SQL 优化的完整过程，从分析到落地，QPS 提升了 20 倍。',
       'https://picsum.photos/seed/post14/800/500', 389, 178, 256, 6100, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),

(15, 789,  '从零搭建 CI/CD 流水线',
       '<p>记录了如何从零开始在 GitHub Actions 上搭建一套完整的 CI/CD 流水线的过程。</p>',
       '记录了如何从零开始在 GitHub Actions 上搭建一套完整的 CI/CD 流水线的过程。',
       NULL, 112, 39, 74, 1600, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),

(16, 1001, 'React vs Vue：一个双栈开发者的视角',
       '<p>作为同时使用 React 和 Vue 的开发者，从实际开发体验出发对比两者的优劣。</p>',
       '作为同时使用 React 和 Vue 的开发者，从实际开发体验出发对比两者的优劣。',
       'https://picsum.photos/seed/post16/800/500', 298, 201, 167, 5200, 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),

(17, 2048, 'Python 异步编程 asyncio 入门',
       '<p>从同步到异步，一步步理解 Python asyncio 的核心概念和使用方式。</p>',
       '从同步到异步，一步步理解 Python asyncio 的核心概念和使用方式。',
       NULL, 78, 28, 45, 980, 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),

(18, 337,  'Kubernetes 集群管理经验小结',
       '<p>管理 K8s 集群半年来积累的一些运维经验和踩过的坑，希望对新手有帮助。</p>',
       '管理 K8s 集群半年来积累的一些运维经验和踩过的坑，希望对新手有帮助。',
       NULL, 189, 94, 133, 3100, 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),

(19, 512,  '程序员如何保持持续学习的状态',
       '<p>技术更新太快，如何保持高效学习而不焦虑？分享我的一些方法和心得。</p>',
       '技术更新太快，如何保持高效学习而不焦虑？分享我的一些方法和心得。',
       'https://picsum.photos/seed/post19/800/500', 234, 145, 198, 4500, 1, DATE_SUB(NOW(), INTERVAL 5 DAY)),

(20, 789,  '技术写作：如何写一篇好的技术博客',
       '<p>写了两年技术博客，总结出的一些写作技巧和排版心得，让你的文章更具可读性。</p>',
       '写了两年技术博客，总结出的一些写作技巧和排版心得，让你的文章更具可读性。',
       NULL, 156, 67, 112, 2400, 1, DATE_SUB(NOW(), INTERVAL 5 DAY)),

(21, 1001, '微前端方案选型：qiankun vs Module Federation',
       '<p>对目前主流的微前端方案做了详细对比，包括 qiankun、Module Federation、Micro-app 等。</p>',
       '对目前主流的微前端方案做了详细对比，包括 qiankun、Module Federation、Micro-app 等。',
       NULL, 201, 118, 145, 3800, 1, DATE_SUB(NOW(), INTERVAL 6 DAY)),

(22, 2048, 'Node.js 流式处理大文件实战',
       '<p>使用 Node.js Stream API 处理 GB 级别的大文件，内存占用控制在 50MB 以内。</p>',
       '使用 Node.js Stream API 处理 GB 级别的大文件，内存占用控制在 50MB 以内。',
       'https://picsum.photos/seed/post22/800/500', 312, 167, 203, 5400, 1, DATE_SUB(NOW(), INTERVAL 6 DAY)),

(23, 337,  '我的极简桌面搭建记录',
       '<p>最近把工位重新整理了一遍，分享一下我的极简桌面布置和生产力工具配置。</p>',
       '最近把工位重新整理了一遍，分享一下我的极简桌面布置和生产力工具配置。',
       'https://picsum.photos/seed/post23/800/500', 176, 88, 95, 2800, 1, DATE_SUB(NOW(), INTERVAL 7 DAY));
