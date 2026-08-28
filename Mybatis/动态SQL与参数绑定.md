# 动态 SQL 与参数绑定

MyBatis 面试里的安全与工程题集中在这一篇：**`#{}` vs `${}`（防 SQL 注入）**、动态标签、批量写法取舍、resultMap 映射与 N+1 问题。

---

## 一、`#{}` vs `${}`（必考）

| 对比项 | `#{}` | `${}` |
|:---|:---|:---|
| 底层 | `PreparedStatement` **占位符 `?`**，值由 ParameterHandler 安全绑定 | **字符串拼接**进 SQL 后再编译 |
| 编译时机 | 预编译后传参（一次解析，N 次执行） | 每次拼接都重新编译 |
| SQL 注入 | **天然免疫**（参数不参与语法解析） | 有风险，需白名单校验 |
| 使用限制 | 只能用于**值**的位置 | 可用于表名/列名/排序字段等**结构**位置 |

```xml
<!-- 安全：预编译占位符 -->
<select id="getById"> SELECT * FROM user WHERE id = #{id} </select>

<!-- 危险：直接拼接，id = "1 OR 1=1 --" 即注入 -->
<select id="getByIdBad"> SELECT * FROM user WHERE id = ${id} </select>
```

`${}` 唯一合理场景——**结构化部分**不能参数化的地方，且必须白名单：

```xml
<!-- 动态排序：列名/方向无法用 ? 占位，只能 ${} + 白名单校验 -->
<if test="orderBy == 'created_at' or orderBy == 'id'">
  ORDER BY ${orderBy} ${orderDir}   <!-- orderDir 限 ASC/DESC，服务端枚举校验 -->
</if>
```

**like 模糊查询的正确写法**（高频小题）：

```xml
<!-- ✅ 推荐：concat 传参走预编译 -->
WHERE name LIKE CONCAT('%', #{keyword}, '%')

<!-- ✅ 或 bind 标签 -->
<bind name="kw" value="'%' + keyword + '%'"/>
WHERE name LIKE #{kw}

<!-- ❌ WHERE name LIKE '%${keyword}%' → 注入 -->
```

> 索引提示：前缀 `%` 的 like 无法走普通索引，需要覆盖索引或搜索引擎（ES）。见 [../mysql/索引.md](../mysql/索引.md)。

---

## 二、动态 SQL 标签速查

| 标签 | 作用 | 典型示例 |
|:---|:---|:---|
| `<if test>` | 条件拼接 | `AND name = #{name}` |
| `<where>` | 自动加 WHERE 并**去掉开头多余的 AND/OR** | 多条件搜索 |
| `<set>` | 自动加 SET 并**去掉末尾多余逗号** | 动态更新 |
| `<trim>` | 通用版 where/set：自定义前缀 + 去除前后缀 | 特殊 SQL 组装 |
| `<choose>/<when>/<otherwise>` | if-else if-else | 只命中一个分支 |
| `<foreach>` | 遍历集合：`collection/item/index/open/close/separator` | `IN (?,?,?)`、批量 VALUES |
| `<bind>` | 定义 OGNL 表达式变量 | like 关键字包装 |
| `<sql>/<include>` | SQL 片段复用 | 公共列名抽取 |

```xml
<!-- 动态更新：set 标签自动处理逗号 -->
<update id="updateUser">
  UPDATE user
  <set>
    <if test="name != null">name = #{name},</if>
    <if test="age != null">age = #{age},</if>
  </set>
  WHERE id = #{id}
</update>

<!-- IN 查询：foreach 三件套 -->
<select id="getByIds">
  SELECT * FROM user WHERE id IN
  <foreach collection="ids" item="id" open="(" separator="," close=")">
    #{id}
  </foreach>
</select>
```

> `<where>/<set>` 本质都是 `<trim>` 的特例。`if` 里 OGNL 判空注意：字符串 `'A'.equals(name)`，数字 0/空串会被当作 falsy 的坑见「易错点」。

---

## 三、批量操作：foreach vs BATCH 执行器

| 方案 | 原理 | 优点 | 缺点 |
|:---|:---|:---|:---|
| `foreach` 拼 VALUES | 一条多值 SQL：`INSERT INTO t VALUES (...),(...),...` | 一次网络往返，快 | SQL 超长（`max_allowed_packet`）、占位符数量上限、解析成本高 |
| `ExecutorType.BATCH` | JDBC `addBatch/executeBatch` | 稳定可控、内存友好、可分批 | 多次往返、默认不返回自增主键 |

经验值：**几百行以内 foreach 分批（500~1000 一批）；万级用 BATCH**。

```java
// foreach 插入模板（分批控制长度）
Lists.partition(list, 500).forEach(batch -> userMapper.insertBatch(batch));
```

---

## 四、参数绑定与结果映射

### 4.1 参数传递

| 形式 | 取参方式 | 说明 |
|:---|:---|:---|
| 单个基本类型 | `#{任意名}` | 按位置绑定 |
| 单个对象 | `#{属性名}` | getter 反射取值 |
| **多个参数** | `#{arg0}/#{param1}` 或 **`@Param`** | 多参务必 `@Param("name")`，可读且防漂移 |
| Map | `#{key}` | 少用，类型不安全 |

```java
// 多参数必须 @Param（除非编译带 -parameters）
List<User> search(@Param("name") String name, @Param("minAge") Integer minAge);
```

### 4.2 resultType vs resultMap

| 对比项 | resultType | resultMap |
|:---|:---|:---|
| 映射规则 | 列名 → 属性名（开启驼峰 `mapUnderscoreToCamelCase`） | 显式定义 `<id>/<result>` |
| 关联映射 | ❌ | ✅ `<association>`（一对一）/ `<collection>`（一对多） |
| 适用 | 简单表 | 嵌套对象、自定义映射 |

```xml
<resultMap id="orderMap" type="Order">
  <id property="id" column="id"/>
  <result property="userName" column="user_name"/>
  <!-- 一对多：order → items，嵌套查询实现懒加载 -->
  <collection property="items" column="order_id" ofType="OrderItem"
              select="listItemsByOrderId" fetchType="lazy"/>
</resultMap>
```

### 4.3 N+1 问题（必考）

- **现象**：查 1 次 List，再对每行发 1 次关联查询 → 1 + N 次 SQL。
- **根因**：`association/collection` 用了**嵌套查询**（`select=`）且未懒加载/未走 join。
- **解法**：
  1. 改**嵌套结果**映射（join 一条 SQL + resultMap 按列映射，`columnPrefix` 防列名冲突）；
  2. 需要时才用的场景开**延迟加载**（`lazyLoadingEnabled=true`），把 N+1 摊到真正访问时；
  3. 数据量大直接**分步查询 + 内存组装**（IN 批量取关联数据）。

---

## 五、易错点

| 易错点 | 澄清 |
|:---|:---|
| **`${}` 能用 #{} 替代的地方仍用 `${}`** | 只有表名/列名/排序等结构位置才允许 `${}`，且白名单校验 |
| **`if` 判断数字 0 被跳过** | OGNL 中 `age != null and age != ''` 会把 0 当空串处理；数字只判 `!= null` |
| **`foreach` 的 collection 名写错** | 用 `@Param` 显式命名最稳；List 默认 `list`、数组 `array` |
| **模糊查询用 `'%${kw}%'`** | SQL 注入；用 `CONCAT('%',#{kw},'%')` |
| **批量插入一条 SQL 拼几万行** | `max_allowed_packet` 溢出、SQL 解析慢；分批或换 BATCH |
| **动态 SQL 后多余 AND** | 交给 `<where>/<set>/<trim>` 处理，不要手拼 |
| **`orderBy` 用 `#{}`** | 会变成常量字符串 `ORDER BY 'created_at'` 不生效；排序字段只能 `${}` + 白名单 |
| **一对一映射列名冲突** | join 双表同名列用**别名 + `columnPrefix`** 区分 |

---

## 六、一句话总结

参数绑定铁律：**值一律 `#{}`（预编译防注入），结构（表名/列名/排序）才用 `${}` 且必须白名单**。动态 SQL = if/where/set/foreach 等标签拼装，批量插入几百行 foreach 分批、万级换 BATCH 执行器；关联映射能 join 用嵌套结果，嵌套查询要警惕 N+1——懒加载只是延迟不是消除。

---

## 七、相关笔记

| 主题 | 笔记 |
|:---|:---|
| 执行流程（占位符谁在什么时候绑定） | [核心架构与执行流程.md](核心架构与执行流程.md) |
| like '%x%' 走不走索引 | [../mysql/索引.md](../mysql/索引.md) |
| 批量写与锁（update where 无索引→锁全表） | [../mysql/事务与锁.md](../mysql/事务与锁.md) |
| 分页插件与拦截器改写 SQL | [插件机制与高级特性.md](插件机制与高级特性.md) |
