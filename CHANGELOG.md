# 更新日志 (Changelog)

本项目的所有重要变更都将记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [v1.0.0] - 2026-09-09

首个完整版本：高并发演出票务与社交平台全量功能。

### 新增 (Added)

#### 高并发抢票（核心链路）
- Redis Lua 原子预扣库存 + 用户资格去重（每票档每人一次）
- RabbitMQ 异步落库：Direct Exchange + Publisher Confirm/Return + Manual Ack
- 订单创建消费者：Redisson 分布式锁 + `lt_mq_consume_log` 幂等 + MySQL 事务
- 消费失败最多重试 3 次，最终失败进入 DLX/DLQ
- DLQ 消费者自动补偿 Redis 库存与用户抢票资格
- 启动预热：在售票档库存自动写入 Redis（SETNX 不覆盖存量）

#### 业务功能
- 用户：注册 / 登录（JWT）/ 个人主页
- 演出：列表（分页/城市/分类/关键词）、详情多级缓存（Caffeine + Redis 逻辑过期）、热门榜
- 票务：票档展示、普通购买、限量抢票（PROCESSING → SUCCESS 轮询）、订单支付/取消
- 社区：关注/取关、粉丝与关注列表、发布动态、点赞
- Feed 流：Redis ZSet + Fan-out on Write + maxTime/offset 滚动分页（无重复）
- 附近演出：Redis GEO 按城市分片 + 管理端缓存重建
- 观演打卡：MySQL + Redis Bitmap 双写、打卡日历、连续打卡天数
- 演出 UV 统计：HyperLogLog 按日 PV 去重
- 管理演示接口：秒杀库存初始化、GEO 重建（X-Admin-Token）

#### 前端
- React 19 + TypeScript + Vite 7 + Ant Design 6 + Zustand + React Router 7
- 11 个页面：登录/注册/首页/演出列表/详情/抢票/订单/订单详情/动态/附近/个人主页
- 响应式布局，官方演出海报素材

#### 测试与部署
- 49 个后端单元测试（Auth/EventCache/Seckill/Order/Consumer/Feed/Checkin/Post）
- JMeter 压测方案：500 用户 / 库存 100 / Ramp-Up 10s，实测无超卖、无负库存、无重复有效订单
- Docker Compose 一键部署（MySQL/Redis/RabbitMQ/后端/前端 Nginx）
- 启停与健康检查脚本

[Unreleased]: https://github.com/xiashupei4-sketch/LiveTicket/compare/v1.0.0...HEAD
[v1.0.0]: https://github.com/xiashupei4-sketch/LiveTicket/releases/tag/v1.0.0
