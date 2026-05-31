-- ============================================================
-- EchoSpace 测试数据
-- ============================================================
-- 用法:
--   1. 启动应用完成 init.sql 建表
--   2. 通过注册接口创建用户（密码使用 BCrypt 加密），或手动执行本文件
--   3. MySQL 命令行: source data.sql
--
-- 注意: 用户密码通过 BCrypt 加密，下方占位 hash 为示例。
--       建议先通过 POST /api/auth/register 注册以下 5 个用户，
--       然后注释掉用户 INSERT，只执行帖子及之后的部分。
-- ============================================================

USE echospace;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 用户（5 人，密码统一为 123456 的 BCrypt 哈希，cost=10）
-- 实际哈希值请通过注册接口生成，下方为占位值
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

-- ============================================================
-- 评论（38 条：20 条一级评论 + 18 条二级回复）
-- parent_id=0 为一级评论，非0 为二级回复
-- reply_to_uid 指向被回复用户（二级回复时填写）
-- ============================================================

-- 帖子 1（admin 欢迎帖）的 4 条一级评论
INSERT INTO comment (id, post_id, user_id, parent_id, reply_to_uid, content, like_count, status, created_at) VALUES
(1,  1, 2048, 0, NULL, '支持官方账号！期待更多精彩内容，一起把社区做大做强', 12, 1, DATE_SUB(NOW(), INTERVAL 25 MINUTE)),
(2,  1, 337,  0, NULL, '欢迎欢迎，大家都是热爱技术的人，一起交流学习', 8, 1, DATE_SUB(NOW(), INTERVAL 22 MINUTE)),
(3,  1, 512,  0, NULL, '社区氛围很好，从设计到实现都很用心，加油！', 15, 1, DATE_SUB(NOW(), INTERVAL 18 MINUTE)),
(4,  1, 789,  0, NULL, '新人报到！刚注册就感受到了社区的温暖，请多关照', 5, 1, DATE_SUB(NOW(), INTERVAL 15 MINUTE)),

-- 帖子 1 的 2 条二级回复
(5,  1, 1001, 1, 2048, '感谢支持！我们一起把社区建设好', 10, 1, DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
(6,  1, 2048, 3, 512,  '双手赞同，技术社区氛围最重要，拒绝撕逼', 6, 1, DATE_SUB(NOW(), INTERVAL 14 MINUTE)),

-- 帖子 2（Vue 3 架构）的 3 条一级评论
(7,  2, 1001, 0, NULL, '很详细的架构分享，模块化划分的思路非常清晰，学到了', 18, 1, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(8,  2, 337,  0, NULL, 'Pinia 确实比 Vuex 好用很多，TypeScript 支持也更友好', 22, 1, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(9,  2, 512,  0, NULL, 'TypeScript 的类型推断在大型项目中太重要了，及早引入收益很大', 14, 1, DATE_SUB(NOW(), INTERVAL 45 MINUTE)),

-- 帖子 2 的 3 条二级回复
(10, 2, 2048, 7, 1001, '谢谢官方大大认可！这套架构已经在生产环境验证过了', 9, 1, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(11, 2, 1001, 8, 337,  '是的，Pinia 的 devtools 体验也比 Vuex 好不少', 7, 1, DATE_SUB(NOW(), INTERVAL 40 MINUTE)),
(12, 2, 789,  9, 512,  '而且配合 Volar 插件开发体验极佳，类型提示很到位', 4, 1, DATE_SUB(NOW(), INTERVAL 30 MINUTE)),

-- 帖子 4（Spring Boot 升级）的 4 条一级评论
(13, 4, 1001, 0, NULL, '升级经验非常实用，收藏了慢慢消化', 20, 1, DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(14, 4, 2048, 0, NULL, '刚好最近也要从 2.x 升级，这篇文章简直是及时雨', 16, 1, DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(15, 4, 337,  0, NULL, '从 2.x 到 3.x 确实坑不少，javax → jakarta 迁移就搞了好久', 11, 1, DATE_SUB(NOW(), INTERVAL 15 HOUR)),
(16, 4, 789,  0, NULL, 'Spring Boot 3.x 的虚拟线程支持值得尝试，性能提升明显', 13, 1, DATE_SUB(NOW(), INTERVAL 12 HOUR)),

-- 帖子 4 的 3 条二级回复
(17, 4, 512,  13, 1001, '有帮助就好！有问题随时交流，升级过程中注意 security 配置变化', 8, 1, DATE_SUB(NOW(), INTERVAL 16 HOUR)),
(18, 4, 2048, 16, 789,  '虚拟线程确实是大亮点，我们已经在测试环境验证了吞吐量提升', 6, 1, DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(19, 4, 789,  14, 2048, '期待更多 Spring 实战文章，已关注', 5, 1, DATE_SUB(NOW(), INTERVAL 14 HOUR)),

-- 帖子 5（TypeScript 书籍推荐）的 2 条一级评论
(20, 5, 1001, 0, NULL, '推荐《Programming TypeScript》这本，讲得很透彻，适合进阶', 9, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(21, 5, 2048, 0, NULL, '类型体操可以从简单泛型练起，推荐 type-challenges 项目', 7, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- 帖子 5 的 2 条二级回复
(22, 5, 789,  20, 1001, '感谢推荐！马上去看看，正好缺一本系统性的书', 3, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(23, 5, 337,  21, 2048, 'type-challenges 确实好，有没有其他练习平台推荐？', 4, 1, DATE_SUB(NOW(), INTERVAL 20 HOUR)),

-- 帖子 14（数据库索引优化）的 2 条一级评论
(24, 14, 1001, 0, NULL, '索引优化是后端的基本功，这篇文章写得深入浅出，点赞', 25, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(25, 14, 2048, 0, NULL, '慢 SQL 优化的实战案例太有价值了，希望能出一个系列', 19, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- 帖子 14 的 3 条二级回复
(26, 14, 512,  24, 1001, '过奖了，大家互相学习。后续会出更多数据库相关的文章', 12, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(27, 14, 337,  25, 2048, '支持出系列！MySQL 索引、SQL 优化、分库分表都安排上', 8, 1, DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(28, 14, 789,  24, 1001, '这个确实，好的索引设计能省掉一大半的慢查询问题', 6, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- 其他帖子的一级评论（5 条）
(29, 3,  512,  0, NULL, '骑行是个好习惯！风景很美，也锻炼身体', 5, 1, DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(30, 6,  2048, 0, NULL, 'Docker Compose 管理多容器确实方便，配合 Portainer 更直观', 8, 1, DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(31, 7,  789,  0, NULL, 'gRPC 在微服务间调用的性能优势很明显，我们团队也在用', 10, 1, DATE_SUB(NOW(), INTERVAL 16 HOUR)),
(32, 8,  1001, 0, NULL, '前端性能优化永无止境，好文收藏！懒加载那部分尤其实用', 15, 1, DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(33, 12, 789,  0, NULL, '开源精神值得敬佩，维护开源项目确实不容易', 12, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- 其他帖子的二级回复（5 条）
(34, 3,  337,  29, 512,  '是的！骑行让人心情舒畅，推荐你也试试', 3, 1, DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(35, 6,  1001, 30, 2048, '建议配合 Portainer 做可视化管理，容器状态一目了然', 6, 1, DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(36, 7,  512,  31, 789,  '我们团队就在用 gRPC 做服务间调用，延迟比 REST 低很多', 7, 1, DATE_SUB(NOW(), INTERVAL 14 HOUR)),
(37, 12, 2048, 33, 789,  '感谢理解！开源社区需要大家共同维护，一起加油', 8, 1, DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(38, 12, 1001, 33, 789,  '+1，开源不易，且用且珍惜，给维护者们点个赞', 5, 1, DATE_SUB(NOW(), INTERVAL 18 HOUR));

-- ============================================================
-- 点赞（user_like）
-- target_type: 1=帖子点赞, 2=评论点赞
-- 每个用户给 10~12 个帖子点赞，部分评论也有点赞
-- UNIQUE KEY (user_id, target_type, target_id) 保证不重复
-- ============================================================

-- 帖子点赞（target_type=1）：每人给 10~12 篇帖子点赞
INSERT INTO user_like (user_id, target_type, target_id, created_at) VALUES
-- admin (1001) 点赞的帖子：2,3,4,5,7,8,9,10,12,14,19,22
(1001, 1, 2,  DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(1001, 1, 3,  DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(1001, 1, 4,  DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(1001, 1, 5,  DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1001, 1, 7,  DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(1001, 1, 8,  DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(1001, 1, 9,  DATE_SUB(NOW(), INTERVAL 22 HOUR)),
(1001, 1, 10, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1001, 1, 12, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1001, 1, 14, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1001, 1, 19, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(1001, 1, 22, DATE_SUB(NOW(), INTERVAL 4 DAY)),

-- vue_master (2048) 点赞的帖子：1,3,4,6,10,11,13,14,16,18,21,23
(2048, 1, 1,  DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
(2048, 1, 3,  DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(2048, 1, 4,  DATE_SUB(NOW(), INTERVAL 15 HOUR)),
(2048, 1, 6,  DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(2048, 1, 10, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2048, 1, 11, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2048, 1, 13, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2048, 1, 14, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2048, 1, 16, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(2048, 1, 18, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(2048, 1, 21, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(2048, 1, 23, DATE_SUB(NOW(), INTERVAL 5 DAY)),

-- cycling_fan (337) 点赞的帖子：1,2,4,5,7,9,11,14,15,17,19,20
(337, 1, 1,  DATE_SUB(NOW(), INTERVAL 18 MINUTE)),
(337, 1, 2,  DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(337, 1, 4,  DATE_SUB(NOW(), INTERVAL 16 HOUR)),
(337, 1, 5,  DATE_SUB(NOW(), INTERVAL 1 DAY)),
(337, 1, 7,  DATE_SUB(NOW(), INTERVAL 15 HOUR)),
(337, 1, 9,  DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(337, 1, 11, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(337, 1, 14, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(337, 1, 15, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(337, 1, 17, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(337, 1, 19, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(337, 1, 20, DATE_SUB(NOW(), INTERVAL 4 DAY)),

-- java_dev (512) 点赞的帖子：1,2,5,6,8,12,13,16,18,22,23
(512, 1, 1,  DATE_SUB(NOW(), INTERVAL 15 MINUTE)),
(512, 1, 2,  DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
(512, 1, 5,  DATE_SUB(NOW(), INTERVAL 2 DAY)),
(512, 1, 6,  DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(512, 1, 8,  DATE_SUB(NOW(), INTERVAL 16 HOUR)),
(512, 1, 12, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(512, 1, 13, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(512, 1, 16, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(512, 1, 18, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(512, 1, 22, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(512, 1, 23, DATE_SUB(NOW(), INTERVAL 4 DAY)),

-- ts_learner (789) 点赞的帖子：1,3,4,7,9,10,14,15,19,20,21,23
(789, 1, 1,  DATE_SUB(NOW(), INTERVAL 12 MINUTE)),
(789, 1, 3,  DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(789, 1, 4,  DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(789, 1, 7,  DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(789, 1, 9,  DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(789, 1, 10, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(789, 1, 14, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(789, 1, 15, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(789, 1, 19, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(789, 1, 20, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(789, 1, 21, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(789, 1, 23, DATE_SUB(NOW(), INTERVAL 4 DAY));

-- 评论点赞（target_type=2）：部分用户给部分评论点赞
INSERT INTO user_like (user_id, target_type, target_id, created_at) VALUES
-- admin (1001) 点赞的评论
(1001, 2, 2,  DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
(1001, 2, 4,  DATE_SUB(NOW(), INTERVAL 12 MINUTE)),
(1001, 2, 7,  DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(1001, 2, 8,  DATE_SUB(NOW(), INTERVAL 38 MINUTE)),
(1001, 2, 13, DATE_SUB(NOW(), INTERVAL 16 HOUR)),
(1001, 2, 14, DATE_SUB(NOW(), INTERVAL 14 HOUR)),
(1001, 2, 16, DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(1001, 2, 24, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1001, 2, 25, DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(1001, 2, 32, DATE_SUB(NOW(), INTERVAL 14 HOUR)),

-- vue_master (2048) 点赞的评论
(2048, 2, 1,  DATE_SUB(NOW(), INTERVAL 18 MINUTE)),
(2048, 2, 3,  DATE_SUB(NOW(), INTERVAL 14 MINUTE)),
(2048, 2, 7,  DATE_SUB(NOW(), INTERVAL 50 MINUTE)),
(2048, 2, 9,  DATE_SUB(NOW(), INTERVAL 28 MINUTE)),
(2048, 2, 15, DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(2048, 2, 20, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2048, 2, 24, DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(2048, 2, 33, DATE_SUB(NOW(), INTERVAL 16 HOUR)),

-- cycling_fan (337) 点赞的评论
(337, 2, 1,  DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
(337, 2, 2,  DATE_SUB(NOW(), INTERVAL 16 MINUTE)),
(337, 2, 7,  DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
(337, 2, 13, DATE_SUB(NOW(), INTERVAL 14 HOUR)),
(337, 2, 15, DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(337, 2, 29, DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(337, 2, 32, DATE_SUB(NOW(), INTERVAL 12 HOUR)),

-- java_dev (512) 点赞的评论
(512, 2, 3,  DATE_SUB(NOW(), INTERVAL 12 MINUTE)),
(512, 2, 4,  DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
(512, 2, 9,  DATE_SUB(NOW(), INTERVAL 25 MINUTE)),
(512, 2, 13, DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(512, 2, 16, DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(512, 2, 24, DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(512, 2, 31, DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(512, 2, 33, DATE_SUB(NOW(), INTERVAL 14 HOUR)),

-- ts_learner (789) 点赞的评论
(789, 2, 1,  DATE_SUB(NOW(), INTERVAL 18 MINUTE)),
(789, 2, 3,  DATE_SUB(NOW(), INTERVAL 8 MINUTE)),
(789, 2, 8,  DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(789, 2, 14, DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(789, 2, 16, DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(789, 2, 20, DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(789, 2, 24, DATE_SUB(NOW(), INTERVAL 16 HOUR)),
(789, 2, 29, DATE_SUB(NOW(), INTERVAL 4 HOUR));

-- ============================================================
-- 收藏（user_favorite）
-- UNIQUE KEY (user_id, post_id) 保证不重复收藏
-- ============================================================
INSERT INTO user_favorite (user_id, post_id, created_at) VALUES
-- admin (1001)
(1001, 2,  DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(1001, 4,  DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(1001, 7,  DATE_SUB(NOW(), INTERVAL 15 HOUR)),
(1001, 14, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1001, 22, DATE_SUB(NOW(), INTERVAL 3 DAY)),

-- vue_master (2048)
(2048, 1,  DATE_SUB(NOW(), INTERVAL 15 MINUTE)),
(2048, 4,  DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(2048, 8,  DATE_SUB(NOW(), INTERVAL 16 HOUR)),
(2048, 14, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2048, 16, DATE_SUB(NOW(), INTERVAL 2 DAY)),

-- cycling_fan (337)
(337, 2,  DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(337, 4,  DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(337, 5,  DATE_SUB(NOW(), INTERVAL 1 DAY)),
(337, 9,  DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(337, 14, DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- java_dev (512)
(512, 1,  DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
(512, 2,  DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(512, 10, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(512, 14, DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(512, 19, DATE_SUB(NOW(), INTERVAL 2 DAY)),

-- ts_learner (789)
(789, 4,  DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(789, 6,  DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(789, 12, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(789, 14, DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(789, 21, DATE_SUB(NOW(), INTERVAL 2 DAY));

-- ============================================================
-- 关注（user_follow）
-- UNIQUE KEY (follower_id, followed_id) 保证不重复关注
-- follower_id = 关注者（谁点了关注）
-- followed_id = 被关注者（被关注的人）
-- ============================================================
INSERT INTO user_follow (follower_id, followed_id, created_at) VALUES
-- admin (1001) 关注了所有人
(1001, 2048, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(1001, 337,  DATE_SUB(NOW(), INTERVAL 4 DAY)),
(1001, 512,  DATE_SUB(NOW(), INTERVAL 6 DAY)),
(1001, 789,  DATE_SUB(NOW(), INTERVAL 2 DAY)),

-- vue_master (2048) 关注了 admin、java_dev、ts_learner
(2048, 1001, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(2048, 512,  DATE_SUB(NOW(), INTERVAL 3 DAY)),
(2048, 789,  DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- cycling_fan (337) 关注了 admin、vue_master、java_dev
(337, 1001, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(337, 2048, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(337, 512,  DATE_SUB(NOW(), INTERVAL 2 DAY)),

-- java_dev (512) 关注了 admin、vue_master、cycling_fan、ts_learner
(512, 1001, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(512, 2048, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(512, 337,  DATE_SUB(NOW(), INTERVAL 3 DAY)),
(512, 789,  DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- ts_learner (789) 关注了 admin、vue_master、java_dev
(789, 1001, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(789, 2048, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(789, 512,  DATE_SUB(NOW(), INTERVAL 2 DAY));

SET FOREIGN_KEY_CHECKS = 1;
