# 评论功能（Giscus）接入说明

站点已集成 [Giscus](https://giscus.app) 评论——基于 **GitHub Discussions**，评论数据存在你自己的仓库里，方便后续用 `gh` / GitHub API 拉取喂给 AI 做闭环迭代。

代码已经写好，**你只需完成下面 4 步拿到参数并填入即可上线**。参数没填之前，评论框不会显示（不会报错、不会出现空框）。

## 一、开启并配置（一次性，约 3 分钟）

1. **开启 Discussions**
   打开仓库 `https://github.com/tainger/Markdown-Resume` → **Settings** → 往下找到 **Features** → 勾选 **Discussions**。

2. **安装 giscus app**
   打开 <https://github.com/apps/giscus> → **Install** → 选择只授权给 `Markdown-Resume` 仓库。

3. **生成参数**
   打开 <https://giscus.app>，页面往下：
   - 「仓库」填 `tainger/Markdown-Resume`（页面会校验是否满足条件，全绿即可）
   - 「页面 ↔️ discussion 映射关系」选 **pathname**（与代码里一致）
   - 「Discussion 分类」选 **Announcements**（或你自建的分类）
   - 页面底部「启用 giscus」代码框里，会出现类似这样的属性：
     ```
     data-repo-id="R_kgDOxxxxxx"
     data-category-id="DIC_kwDOxxxxxxxx"
     ```

4. **把参数填进项目**
   打开 [.vitepress/theme/giscus.config.ts](./.vitepress/theme/giscus.config.ts)，替换两个 TODO：
   ```ts
   repoId: 'R_kgDOxxxxxx',          // 填 data-repo-id
   categoryId: 'DIC_kwDOxxxxxxxx',  // 填 data-category-id
   ```
   （`repo`、`category` 一般已经对了，若你用了别的分类名记得同步改 `category`。）

5. **验证并上线**
   ```bash
   npm run docs:dev   # 打开任意笔记页，底部应出现「💬 评论」框
   git add -A && git commit -m "feat: 启用 giscus 评论" && git push
   ```
   push 后 Vercel 自动部署，线上即可评论。

## 二、行为说明

- 每个页面按 **路径（pathname）** 对应一条 Discussion，第一次有人评论时自动创建。
- 评论框自动跟随站点的 **暗色 / 亮色模式**。
- 首页不显示评论（[index.md](./index.md) 里 `comment: false`）。任何页面想关掉评论，在其 frontmatter 加 `comment: false` 即可。

## 三、拉取评论喂给 AI（闭环迭代）

评论都在仓库的 Discussions 里，可用 GitHub CLI / GraphQL 导出。

### 快速查看

```bash
# 列出所有 discussion（每条对应一个页面）
gh api graphql -f query='
{
  repository(owner: "tainger", name: "Markdown-Resume") {
    discussions(first: 50, orderBy: {field: UPDATED_AT, direction: DESC}) {
      nodes { title url comments { totalCount } }
    }
  }
}'
```

### 导出全部评论正文（可直接投喂 AI）

```bash
gh api graphql -f query='
{
  repository(owner: "tainger", name: "Markdown-Resume") {
    discussions(first: 50) {
      nodes {
        title
        url
        comments(first: 100) {
          nodes { author { login } body createdAt }
        }
      }
    }
  }
}' > comments.json
```

拿到 `comments.json` 后，就能让 AI 按「页面标题 → 读者评论」逐条分析：哪些笔记有疑问、哪些解法被指正、该补充什么，形成「评论 → 修订笔记」的闭环。

> 提示：GraphQL 查询需要 `gh auth login` 已登录，且 token 有 `read:discussion` 权限（默认登录一般已包含）。
