# RBAC 表设计与落地（Java / Spring Security 实战）

> **P7 面试核心**：「RBAC 五张表怎么画」是权限设计的标准开场题，但只会画表拿不到高分——面试官会追：**权限点怎么定义？接口/菜单/按钮怎么映射？鉴权在哪一层做？权限缓存怎么更新？** 本文从表设计 → 权限点体系 → 代码落地 → 缓存治理，一次讲完生产级 RBAC。

---

## 一、经典表设计（RBAC0 五表 + 扩展）

### 1.1 五张核心表

```
┌─────────┐  M:N  ┌──────────┐  M:N  ┌─────────┐
│  user   │──────▶│ user_role│◀──────│  role   │
└─────────┘       └──────────┘       └────┬────┘
                                          │ M:N
                                   ┌──────▼─────┐
                                   │ role_menu  │
                                   └──────┬─────┘
                                          │ M:N
                                   ┌──────▼─────┐
                                   │    menu    │ (权限点/资源)
                                   └────────────┘
```

```sql
-- 1. 用户表
CREATE TABLE sys_user (
    id        BIGINT PRIMARY KEY,
    username  VARCHAR(64) NOT NULL UNIQUE,
    password  VARCHAR(128) NOT NULL COMMENT 'BCrypt 加密存储',
    status    TINYINT NOT NULL DEFAULT 1 COMMENT '1 启用 0 禁用',
    dept_id   BIGINT COMMENT '部门（数据权限用）'
);

-- 2. 角色表
CREATE TABLE sys_role (
    id        BIGINT PRIMARY KEY,
    role_code VARCHAR(64) NOT NULL UNIQUE COMMENT '角色标识：order_admin',
    role_name VARCHAR(64) NOT NULL COMMENT '展示名：订单管理员',
    status    TINYINT NOT NULL DEFAULT 1
);

-- 3. 权限点表（menu 既当菜单又当权限点，用 type 区分）
CREATE TABLE sys_menu (
    id          BIGINT PRIMARY KEY,
    parent_id   BIGINT NOT NULL DEFAULT 0 COMMENT '0=根，树形结构',
    name        VARCHAR(64) NOT NULL,
    type        TINYINT NOT NULL COMMENT '1 目录 2 菜单 3 按钮 4 接口',
    perm_code   VARCHAR(128) NULL COMMENT '权限标识：order:delete',
    path        VARCHAR(128) NULL COMMENT '前端路由 / 接口 URI',
    method      VARCHAR(10)  NULL COMMENT 'GET/POST...，接口型才有'
);

-- 4. 用户-角色关联
CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

-- 5. 角色-权限点关联
CREATE TABLE sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
);
```

### 1.2 常见扩展（面试加分项）

| 扩展 | 加什么表 | 解决什么 |
|:---|:---|:---|
| 用户组 | `user_group` + `group_role` | 一批人批量授权（外包团队整体给角色）|
| 数据权限 | `sys_role` 加 `data_scope` 字段（1 全部/2 本部门/3 本部门及以下/4 仅本人/5 自定义）| 行级数据过滤，见 [数据权限与多租户](数据权限与多租户.md) |
| 临时授权 | `role` 加 `expire_at` | 活动运营 7 天权限到期自动回收 |
| 委托/代理 | `delegation` 表 | 请假期间把审批权委托给他人 |

---

## 二、权限点体系（perm_code 设计）

### 2.1 三层权限点

| 层级 | 例子 | 谁消费 |
|:---|:---|:---|
| **菜单** | `/order/list` | 前端路由守卫（渲染左侧菜单）|
| **按钮** | `order:export` | 前端 `v-permission` 指令（按钮显隐）|
| **接口** | `DELETE /api/order/{id}` ↔ `order:delete` | **后端拦截器/AOP（真正防线）** |

```text
菜单树示例（parent_id 组成树）：
订单管理(目录, 1)
 ├── 订单列表(菜单, 2, path=/order/list)
 │    ├── 导出(按钮, 3, perm=order:export)
 │    └── 删除(按钮, 3, perm=order:delete)
 └── 审核中心(菜单, 2, path=/order/audit)
      └── 审批(按钮, 3, perm=order:approve)
```

**命名规范**：`资源:操作` 小写冒号分隔（`order:delete`、`user:role:assign`），接口型权限点与 URL+Method 建立映射表，启动时扫描 `@PreAuthorize` 注解自动注册。

### 2.2 权限点 vs URL 鉴权（两种流派）

| 流派 | 做法 | 优点 | 缺点 |
|:---|:---|:---|:---|
| **权限点（推荐）** | 注解声明 `@PreAuthorize("hasAuthority('order:delete')")` | 语义清晰，与 URL 解耦，改路由不影响权限 | 权限点要维护 |
| URL 规则 | 网关/拦截器按 `URL + Method` 匹配 | 无侵入、集中管理 | URL 重构即翻车；通配符易配漏 |

生产常见**两者结合**：默认 URL 级黑名单（登录、管理员）+ 敏感操作权限点注解。

---

## 三、代码落地（Spring Security 为例）

### 3.1 登录后加载权限集合

```java
// UserDetailsService 实现：登录时把「权限点集合」塞进认证上下文
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Resource private SysUserMapper userMapper;
    @Resource private SysPermissionMapper permMapper;

    @Override
    public UserDetails loadUserByUsername(String username) {
        SysUser user = userMapper.selectByUsername(username);
        if (user == null || user.getStatus() == 0) {
            throw new UsernameNotFoundException("用户不存在或已禁用");
        }
        // 查权限点：user → role → menu.perm_code（去重）
        Set<String> authorities = permMapper.selectPermCodesByUserId(user.getId());
        return new LoginUser(user, authorities); // authorities = ["order:list", "order:delete", ...]
    }
}
```

### 3.2 接口鉴权（方法级注解）

```java
@RestController
@RequestMapping("/api/order")
public class OrderController {

    // 权限点校验：认证上下文里没有 order:delete 直接 403
    @PreAuthorize("hasAuthority('order:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return Result.ok();
    }

    // 组合表达式：角色 OR 权限点
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('order:export')")
    @GetMapping("/export")
    public void export(OrderQuery q, HttpServletResponse resp) { /* ... */ }
}
```

### 3.3 查询 SQL（一次 join 拿全权限）

```sql
-- 用户权限点：user_role → role_menu → menu（status=1，perm_code 非空）
SELECT DISTINCT m.perm_code
FROM sys_user_role ur
JOIN sys_role      r  ON r.id = ur.role_id     AND r.status = 1
JOIN sys_role_menu rm ON rm.role_id = r.id
JOIN sys_menu      m  ON m.id = rm.menu_id     AND m.perm_code IS NOT NULL
WHERE ur.user_id = #{userId};
```

> 性能要点：这条链路（3 次 join）在登录/鉴权时高频执行，**必须走缓存**，见第五节。

---

## 四、前端如何配合

1. **登录后拉权限**：`GET /api/user/profile` 返回 `{ roles: ["order_admin"], perms: ["order:list", "order:export"] }`，存入前端 store。
2. **动态路由**：按 `perms` 过滤路由表（`meta.perm` 匹配）生成左侧菜单；无权限路由不注册（防地址栏直敲）。
3. **按钮级控制**：自定义指令 `v-permission="'order:export'"`，无权限则移除 DOM。

```javascript
// Vue3 自定义指令示例
app.directive('permission', {
  mounted(el, binding) {
    const perms = useUserStore().perms
    if (!perms.includes(binding.value)) {
      el.parentNode?.removeChild(el)   // 直接移除，比 v-if 更彻底
    }
  }
})
```

> 再强调一次：前端控制只是体验，**后端每个接口独立鉴权才是安全边界**。

---

## 五、权限缓存与变更生效（高频追问）

### 5.1 为什么要缓存

鉴权链路 = 每个请求都要查「用户 → 角色 → 权限点」，QPS 高时 DB 扛不住 → 权限集合放 Redis（key：`auth:perms:{userId}`，TTL 30min）+ 本地 Caffeine（TTL 10s 抗热点）。

### 5.2 变更生效（改了权限多久生效？）

| 方案 | 时效 | 复杂度 | 说明 |
|:---|:---|:---:|:---|
| 等 TTL 过期 | 最长 30min | ★ | 可接受于低敏系统 |
| 删缓存（推荐） | 秒级 | ★★ | 改角色/授权时，**精准删除**该角色下所有用户的缓存 key（user_role 反查 userId 列表）|
| 广播失效 | 秒级 | ★★★ | MQ/Redis pub-sub 广播 `PERM_CHANGED`，各实例清本地 Caffeine；多实例必须做，否则各节点权限不一致 |

```java
// 角色授权变更：DB 更新 + 删受影响用户缓存（先更新库，再删缓存）
@Transactional
public void assignRoles(Long userId, List<Long> roleIds) {
    userRoleMapper.deleteByUserId(userId);
    userRoleMapper.batchInsert(userId, roleIds);
    redis.delete("auth:perms:" + userId);          // 下次请求回源重建
    mqSend("perm-changed", userId);                 // 通知各节点清本地缓存
}
```

### 5.3 超级管理员放行

```java
// 兜底：超管角色不走权限点比对，避免「误删超管权限点把自己锁死」
if (loginUser.hasRole("SUPER_ADMIN")) return true;
```

---

## 六、易错点

1. **权限点塞进 JWT 永不刷新**：Token 有效期内权限是旧快照，封禁/降权不生效；正确做法是 Token 只放 userId，权限查缓存。
2. **只删用户缓存不删角色缓存**：改角色权限影响 N 个用户，按 `user_id` 逐个删会漏；应按**角色反查用户列表**批量失效。
3. **本地缓存多实例不一致**：节点 A 改了权限，节点 B 的 Caffeine 还在放行 → 必须广播失效。
4. **接口型权限点漏扫**：新增接口忘配权限点 = 裸奔；用启动扫描 + 灰度报告（列出未保护端点）兜底。
5. **密码哈希用 MD5**：必须 BCrypt/PBKDF2/Argon2（自带盐 + 慢哈希）；MD5 彩虹表秒破。
6. **越权测试只测水平不测垂直**：水平越权（改 id 看别人数据）、垂直越权（普通用户调管理员接口）都要测，见第八节。

---

## 七、一句话总结

> **五表打底（user / role / menu / user_role / role_menu），权限点三级（菜单/按钮/接口）但只信后端注解，权限集合走 Redis + 本地双层缓存，变更时按角色反查用户批量删缓存并广播本地失效。**

## 相关笔记

- 六大权限模型与选型 → [权限模型](权限模型.md)
- 凭证与会话（JWT/Session/OAuth2）→ [认证与会话](认证与会话.md)
- 行级数据权限实现 → [数据权限与多租户](数据权限与多租户.md)
- 网关统一鉴权与服务间认证 → [微服务鉴权架构](微服务鉴权架构.md)
- MyBatis 拦截器做数据权限 → [插件机制与高级特性](../Mybatis/插件机制与高级特性.md)
