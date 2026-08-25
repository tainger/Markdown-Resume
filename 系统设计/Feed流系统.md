# Feed 流系统

> **P7 面试核心**：Feed 流是「**数据模型 + 投递模型 + 排序**」三位一体的题——微博/朋友圈/抖音首页都属此类。核心难点是「**大 V 发帖时如何不拖垮系统**」和「**推拉结合的边界**」。

---

## 一、需求澄清

### 1.1 功能性需求

| # | 需求 | 说明 |
|:---:|:---|:---|
| 1 | 发帖 | 用户发一条内容（文字/图/视频），粉丝可见 |
| 2 | 关注/取关 | 用户 A 关注 B，B 的内容出现在 A 的 Feed 流 |
| 3 | 查看首页 Feed | 拉取「我关注的人」的最新帖子，按时间倒序 |
| 4 | 点赞/评论 | 互动行为，会更新到 Feed 的统计字段 |
| 5 | 删帖 | 作者删除，所有粉丝 Feed 流同步消失 |

### 1.2 非功能性需求

| 维度 | 目标 | 量化 |
|:---:|:---|:---|
| **读多写多** | 读 ≈ 10× 写 | 首页刷 Feed 是高频操作 |
| **实时性** | 发帖后 ≤ 5s 粉丝可见 | 朋友圈可秒级，微博容忍分钟级 |
| **高可用** | 99.99% | Feed 挂了 = 用户看不到内容 = 产品死亡 |
| **个性化** | 可重排 | 微博做个性化推荐混排，朋友圈严格时间序 |

### 1.3 范围澄清

- 微博式（关注关系）vs 推荐式（抖音首页）？→ P0 先做关注关系式
- 是否支持视频？→ 是，但视频走 CDN，Feed 只存元信息
- 朋友圈（双向关注）vs 微博（单向关注）？→ P0 做微博式单向关注（更复杂）

---

## 二、容量估算

### 2.1 假设条件

- 总用户 1 亿，日活 3000 万
- 平均每用户关注 100 人，每天发 1 条
- 大 V 粉丝数：百万级（明星 100w+，头部大 V 500w+）
- 平均帖子大小 1KB（元数据，不含图视频）

### 2.2 写入量

- 发帖 QPS = 3000w / 86400 ≈ **350 QPS**（峰值估 5000）
- 大 V 发帖 = 1 次 DB 写 + N 次「推送到粉丝收件箱」（N=粉丝数）

### 2.3 读取量

- 读 Feed QPS = 3000w × 10 次/天 / 86400 ≈ **3500 QPS**（峰值估 5 万）
- 单次读 Feed 拉 20~50 条帖子

### 2.4 关键结论

> 发帖写入压力低（350 QPS），**难点是大 V 发帖时的「扇出」问题**：100w 粉丝 = 要写 100w 次收件箱，DB 撑不住。这是 Feed 流架构的核心矛盾。

---

## 三、API 设计

```
# 1. 发帖
POST /feed
Body: { content, media_urls, type }
→ { post_id, created_at }

# 2. 关注/取关
POST /user/{userId}/follow
DELETE /user/{userId}/follow

# 3. 拉取首页 Feed（核心）
GET /feed?cursor={last_id}&size=20
→ { posts: [...], has_more, next_cursor }

# 4. 点赞
POST /post/{postId}/like

# 5. 删帖
DELETE /post/{postId}
```

---

## 四、数据模型

### 4.1 关系表

```sql
-- 关注关系表（关注者 → 被关注者）
CREATE TABLE follow_relation (
    follower_id BIGINT NOT NULL COMMENT '关注者',
    followee_id BIGINT NOT NULL COMMENT '被关注者',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (follower_id, followee_id),  -- 防重复关注
    INDEX idx_followee (followee_id)          -- 反查「我的粉丝」
) ENGINE=InnoDB;

-- 粉丝列表冗余表（被关注者视角，方便大 V 推送时拉粉）
CREATE TABLE follower_list (
    followee_id BIGINT NOT NULL,
    follower_id BIGINT NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (followee_id, follower_id),
    INDEX idx_created (followee_id, created_at)
);
```

### 4.2 帖子表（Outbox 发件箱）

```sql
CREATE TABLE post (
    id          BIGINT PRIMARY KEY COMMENT 'Snowflake ID，趋势递增',
    author_id   BIGINT NOT NULL,
    content     TEXT NOT NULL,
    media_urls  JSON NULL,
    like_count  INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    status      TINYINT NOT NULL DEFAULT 1 COMMENT '0删除 1正常',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_author_time (author_id, created_at),
    INDEX idx_created (created_at)
);
```

### 4.3 收件箱表（Inbox）

```sql
-- 写扩散模式下，每个用户都有一个收件箱，存「推给我的帖子ID」
CREATE TABLE feed_inbox (
    user_id     BIGINT NOT NULL COMMENT '收件人',
    post_id     BIGINT NOT NULL COMMENT '帖子ID',
    author_id   BIGINT NOT NULL COMMENT '作者（冗余便于过滤）',
    created_at  DATETIME NOT NULL COMMENT '帖子时间（用于排序）',
    PRIMARY KEY (user_id, post_id),
    INDEX idx_user_time (user_id, created_at)
);
```

---

## 五、三种投递模型对比 ⭐⭐⭐（核心考点）

### 5.1 写扩散（Fanout-on-Write / 推模式）

**作者发帖时，主动把帖子推到所有粉丝的收件箱**：

```
大 V 发帖（粉丝 100w）
   │
   ▼ Fanout
遍历粉丝列表 → 逐个 INSERT feed_inbox（粉丝 A, 帖子ID）
   │
   ▼ 写了 100w 次
粉丝 A 打开首页
   │
   ▼ SELECT * FROM feed_inbox WHERE user_id = A ORDER BY created_at DESC LIMIT 20
   │
   ▼ 一次查询拿到 Feed（极快）
```

| ✅ 优点 | ❌ 缺点 |
|:---|:---|
| **读极快**（粉丝查询直接从收件箱拉）| **写放大严重**：100w 粉丝 = 100w 次写 |
| 用户体验好（拉取延迟低）| **大 V 发帖拖垮系统**：写扩散的致命伤 |
| 排序简单（按收件箱 created_at 倒序）| 存储膨胀：每个粉丝都存一份帖子 ID |

**适合**：粉丝数 < 10w 的普通用户（90% 的用户）

### 5.2 读扩散（Fanout-on-Read / 拉模式）

**作者发帖只写一次到发件箱（post 表）。粉丝读时，去拉所有关注人的最新帖子合并**：

```
大 V 发帖（粉丝 100w）
   │
   ▼ 只写一次
INSERT INTO post (author_id, ...) VALUES (大V, ...)
   │
   ▼ 完成
粉丝 A 打开首页
   │
   ▼ 拉取 A 关注的所有人 + 各自最新 20 条
   SELECT author_id FROM follow_relation WHERE follower_id = A
   → [大V, B, C, ..., 共 100 人]
   SELECT * FROM post WHERE author_id IN (...) ORDER BY created_at DESC LIMIT 20
   │
   ▼ 合并 100 个列表（堆/归并排序）
```

| ✅ 优点 | ❌ 缺点 |
|:---|:---|
| **写极轻**（发帖只写 1 次）| **读放大**：关注 100 人 = 查 100 个发件箱合并 |
| 大 V 发帖无压力 | 关注的人多时，读延迟高 |
| 存储省（不冗余）| 排序复杂（多个列表归并）|

**适合**：百万粉丝大 V（避免写扩散的 100w 次写）

### 5.3 推拉结合（Hybrid）⭐ 生产首选

**根据用户类型分流**：普通用户发帖走「推」，大 V 发帖走「拉」，结合两者优势。

```
发帖判断：粉丝数 < 10w？
   │
   ├─是（普通用户）─► 写扩散（推到所有粉丝收件箱）
   │
   └─否（大 V）────► 只写发件箱，不推
                       │
                       ▼
              粉丝打开首页时：
                  │
                  ├─ 普通关注的人：从我的收件箱拉（已推过来）
                  │
                  └─ 大 V 关注的人：从大 V 发件箱实时拉（按时间合并）
```

| 用户类型 | 发帖 | 读 Feed |
|:---|:---|:---|
| **普通用户**（粉丝 <10w）| **写扩散**：推到所有粉丝收件箱 | 直接读收件箱（已收推送）|
| **大 V**（粉丝 ≥10w）| **读扩散**：只写发件箱 | 读时实时拉大 V 发件箱，与收件箱合并 |
| **活跃用户** | 都推（即使关注大 V，也把大 V 帖子推过来）| 直接读收件箱，省合并成本 |
| **非活跃用户** | 都不推（避免给僵尸粉推）| 读时全部走读扩散（拉所有关注）|

---

## 六、核心深挖

### 6.1 活跃用户判定 ⭐（推拉结合的优化核心）

为什么要判定活跃用户？「**给僵尸粉推帖子 = 浪费 80% 写入**」。

| 判定方法 | 实现 | 阈值 |
|:---|:---|:---|
| **最近登录** | 7 天内有登录 → 活跃 | 简单可靠 |
| **最近刷 Feed** | 1 天内有读 Feed 行为 → 活跃 | 更精确，但需埋点 |
| **Redis HyperLogLog** | 用 HLL 统计去重 UV | 内存省 |

```java
public boolean isActiveUser(Long userId) {
    // Redis BitMap 标记：每天用 1bit 标记是否活跃
    // 7 天内至少 1 天标记为 1 即活跃
    String key = "active:" + LocalDate.now();
    return redisTemplate.opsForValue().getBit(key, userId);
}

// 发帖时只推活跃粉丝
public void fanoutPost(Post post) {
    List<Long> followers = followService.getFollowers(post.getAuthorId());
    List<Long> activeFollowers = followers.stream()
        .filter(this::isActiveUser)  // 只推活跃粉丝
        .collect(Collectors.toList());
    // 批量写收件箱
    batchInsertInbox(activeFollowers, post);
}
```

### 6.2 Timeline 排序：Redis ZSet ⭐

收件箱不用 MySQL，用 **Redis ZSet**（score = 帖子时间戳，value = 帖子 ID）：

```
key: inbox:{userId}
ZADD inbox:123  1730000000  post_001
ZADD inbox:123  1730000010  post_002
ZADD inbox:123  1730000020  post_003
                ↑ score = created_at 毫秒
                ↓
ZREVRANGEBYSCORE inbox:123 +inf -inf LIMIT 0 20  → 按时间倒序拉 20 条
```

| 优势 | 说明 |
|:---|:---|
| **天然有序** | ZSet 按 score 排序，无需额外 ORDER BY |
| **高性能** | 内存操作，10w+ QPS 单分片 |
| **TTL 自动清理** | 给 inbox 设 7 天 TTL，超 7 天自动清，省内存 |
| **分页天然支持** | `ZREVRANGEBYSCORE` + LIMIT |

#### 朋友圈 vs 微博排序差异

| 维度 | 朋友圈 | 微博 |
|:---|:---|:---|
| **排序** | 严格时间倒序 | 时间序 + 个性化重排（推荐混入）|
| **可见范围** | 双向好友（强关系）| 单向关注（弱关系）|
| **互动权重** | 无 | 点赞/评论多的帖子上浮 |
| **实现** | ZSet 时间序即可 | 需推荐系统打分重排 |

### 6.3 大 V 难点 ⭐⭐

百万粉丝大 V 的处理是 Feed 流的「考题核心」：

| 难点 | 解法 |
|:---|:---|:---|
| **发帖扇出** | 不推，走读扩散；粉丝拉时实时合并 |
| **粉丝实时合并慢** | 给活跃粉丝也推一份（推拉结合）|
| **大 V 帖子被频繁拉** | 大 V 发件箱做 Redis 缓存（最近 100 条 + 7 天）|
| **取关大 V 后 Feed 不变** | 收件箱历史推送不删（已是历史快照）|
| **大 V 删帖** | 发「删帖事件」MQ，订阅者异步删收件箱对应记录 |

### 6.4 冷热分离

```
热数据（最近 7 天）→ Redis ZSet（inbox）
   ↓ 7 天后过期
冷数据（>7 天）→ MySQL feed_inbox 表（按 user_id 分库分表）
   ↓ 用户翻很久以前
查询时：先查 Redis（热），不夠再查 MySQL（冷），合并返回
```

```java
public List<Post> getFeed(Long userId, long cursor, int size) {
    // 1. 先查 Redis 热数据
    Set<Object> hotPostIds = redisTemplate.opsForZSet()
        .reverseRangeByScore("inbox:" + userId, 0, cursor, 0, size);

    // 2. 不够再查冷数据
    if (hotPostIds.size() < size) {
        List<Long> coldPostIds = feedInboxMapper.selectColdPostIds(userId, cursor, size - hotPostIds.size());
        hotPostIds.addAll(coldPostIds);
    }

    // 3. 批量拉帖子详情（也走缓存）
    return batchGetPosts(hotPostIds);
}
```

### 6.5 发帖异步化（写扩散削峰）

普通用户发帖虽只推粉丝收件箱（不写 post 大表多次），但仍是 N 次写：

```
用户发帖
   │
   ▼ 1. 同步：写 post 表（1 次）→ 立即返回成功
   │
   ▼ 2. 异步：发 MQ「新帖事件」
       │
       ▼ Consumer：拉粉丝列表 + 批量写收件箱（Redis ZSet 批量 ZADD）
       │
       ▼ 3. 大 V 不推（写扩散跳过），仅刷新大 V 发件箱缓存
```

```java
public PublishResult publishPost(Long authorId, Post post) {
    // 1. 同步落库（用户等的就是这条）
    postMapper.insert(post);

    // 2. 异步推送给粉丝
    mqTemplate.send("post-published", new PostEvent(post.getId(), authorId));

    return PublishResult.ok(post.getId());
}

@RocketMQMessageListener(topic = "post-published")
public class PostFanoutListener {
    public void onMessage(PostEvent event) {
        Long authorId = event.getAuthorId();
        if (isBigV(authorId)) {
            // 大 V：只刷发件箱缓存，不推
            redisTemplate.opsForList().leftPush("outbox:" + authorId, event.getPostId());
            return;
        }
        // 普通用户：写扩散到活跃粉丝收件箱
        List<Long> activeFollowers = followService.getActiveFollowers(authorId);
        // 批量 ZADD 到 Redis
        batchZAddInbox(activeFollowers, event.getPostId(), event.getCreatedAt());
    }
}
```

---

## 七、扩展性与容错

### 7.1 大 V 突然爆火（如明星官宣）

某用户粉丝数突然从 1w 涨到 100w，触发「大 V 阈值」切换：

| 策略 | 实现 |
|:---|:---|
| **降级到读扩散** | 不再写扩散，新粉读时实时拉 |
| **存量收件箱不删** | 已推的快照保留，不影响历史 Feed |
| **告警 + 人工确认** | 避免误判（如刷粉）导致错误切换 |

### 7.2 Redis 集群容量

1 亿用户 × 7 天 × 20 条/天 = 14 亿 ZSet 元素。单 Redis 元素 50 字节 → 70GB，分 16 个分片 = 单分片 4.4GB，合理。

### 7.3 删帖同步

```
作者删帖 → 写 post.status = 0
   │
   ▼ 发 MQ「删帖事件」
   │
   ▼ Consumer：
       ① 清作者发件箱缓存（outbox:authorId）
       ② 批量删所有收件箱 inbox:*  中的 post_id（ZREM）
       ③ （大 V）粉丝下次拉时自然过滤掉已删帖
```

### 7.4 监控告警

| SLI | 阈值 | 处置 |
|:---|:---|:---|
| 发帖到可见延迟 | < 5s | 超过排查 MQ 积压 |
| Feed 拉取 P99 | < 200ms | 超过查 Redis 命中率 |
| 写扩散失败率 | < 0.1% | 超过告警（可能收件箱丢帖）|
| 大 V 判定准确率 | > 95% | 错判会导致发帖扇出炸 |

---

## 八、易错点 ⚠️

| # | 易错点 | 正确理解 |
|:---:|:---|:---|
| 1 | 「写扩散最简单，所有用户都推」 | ❌ 大 V 100w 粉丝 = 100w 次写，发个帖要 10 秒。**必须推拉结合**，大 V 走读扩散 |
| 2 | 「推拉结合的边界是 10w 粉丝」 | ⚠️ 阈值要看业务。微博 10w，朋友圈没大 V 概念全推，B 站可能 50w。**结合写入压力实测调** |
| 3 | 「收件箱用 MySQL 就行」 | ❌ 1 亿用户 × 7 天 = 14 亿行，MySQL 撑不住。**用 Redis ZSet**（热）+ MySQL（冷）|
| 4 | 「活跃用户判定不重要」 | ❌ 给僵尸粉推 = 浪费 80% 写入。**只推活跃粉丝**是写扩散性能优化的核心 |
| 5 | 「大 V 删帖粉丝收件箱不变」 | ❌ 删帖不传播 = 粉丝还能看到已删内容（业务事故）。**必须发删帖事件 MQ 异步清收件箱** |
| 6 | 「朋友圈和微博用同一套架构」 | ❌ 朋友圈双向关注 + 无大 V → 全写扩散；微博单向 + 大 V → 必须推拉结合 |
| 7 | 「ZSet TTL 7 天会丢数据」 | ❌ 7 天后转冷存（MySQL），不丢。**冷热分离**是关键设计 |
| 8 | 「发帖同步推送所有粉丝」 | ❌ 同步推 = 阻塞用户。**写 post + 发 MQ 同步，推送异步**，用户秒级拿到成功响应 |
| 9 | 「取关后 Feed 立刻清掉旧帖」 | ⚠️ 历史推送是「快照」，**不删**。否则用户翻旧 Feed 会突然消失，体验差。新帖不再推即可 |
| 10 | 「排序按帖子 created_at」 | ⚠️ 微博要做「**个性化重排**」（互动权重、推荐系统混入）。朋友圈严格时间序，**场景不同实现不同** |

---

## 九、一句话总结

> Feed 流的本质是「**写扩散 vs 读扩散 vs 推拉结合**」三选一：小 V 写扩散（推到收件箱，读极快）、大 V 读扩散（只写发件箱，读时合并）、**生产推拉结合 + 活跃用户判定 + Redis ZSet Timeline + 冷热分离**。面试核心讲清大 V 扇出问题、推拉边界、活跃用户优化、删帖传播四件事。
