# 部署到 Vercel（手机浏览学习笔记）

本仓库已配置好 [VitePress](https://vitepress.dev/)，可把所有学习笔记生成一个带**侧边栏导航、全文搜索、暗色模式**、手机浏览友好的网站，并一键部署到 Vercel。

## 收录范围

网站自动收录以下目录（新增笔记会自动出现在侧边栏，无需改配置）：

`算法思想`、`leetcode-hot100`、`华为OD机试`、`数据结构`、`mysql`、`redis`、`rocketMq`、`java`、`面试准备`

已排除：`repo/`（第三方仓库）、简历、`lover/`、`公司赔偿/` 等隐私内容。

## 本地预览

```bash
npm install          # 首次需要，安装依赖
npm run docs:dev     # 本地开发，热更新，默认 http://localhost:5173
npm run docs:build   # 构建静态站点到 .vitepress/dist
npm run docs:preview # 预览构建产物
```

## 部署到 Vercel

### 方式一：网页导入（推荐，最省事）

1. 把本仓库推到 GitHub（或 GitLab / Bitbucket）。
2. 打开 [vercel.com](https://vercel.com)，用 GitHub 账号登录。
3. 点 **Add New → Project**，选择本仓库导入。
4. Vercel 会自动读取仓库根的 [vercel.json](./vercel.json)，构建配置已经写好，**无需手动填**：
   - Build Command：`npm run docs:build`
   - Output Directory：`.vitepress/dist`
5. 点 **Deploy**，等 1~2 分钟即可。完成后会得到一个 `xxx.vercel.app` 网址，手机浏览器打开即可随时看笔记。

> 之后每次 `git push`，Vercel 会自动重新构建部署，笔记更新后网站自动同步。

### 方式二：命令行部署

```bash
npm i -g vercel   # 安装 Vercel CLI（首次）
vercel login      # 登录
vercel            # 首次部署（按提示确认，全部回车用默认即可）
vercel --prod     # 部署到正式环境
```

## 手机使用小技巧

- 在手机浏览器打开网址后，用「添加到主屏幕」，就像 App 一样一点即开。
- 右上角有🔍搜索按钮，可全文搜索所有笔记。
- 右上角可切换暗色 / 亮色模式。

## 常见问题

- **构建报错 `Error parsing JavaScript expression`**：笔记正文里出现了 `{{ }}` 这类花括号被 Vue 当成模板插值。本项目已在 [.vitepress/config.mts](./.vitepress/config.mts) 中修改了插值分隔符规避，一般不会再遇到。若新笔记仍触发，可把相关内容放进代码块（用 ``` 包裹）。
- **数学公式不显示**：项目已集成 `markdown-it-mathjax3`，支持 `\( ... \)`、`$$ ... $$` 语法，正常应能渲染。
- **新增了目录但侧边栏没有**：侧边栏只收录 [.vitepress/config.mts](./.vitepress/config.mts) 里 `INCLUDE_DIRS` 列出的目录，新增一级目录时在该数组里补一行即可（目录内的文件/子目录会自动扫描）。
