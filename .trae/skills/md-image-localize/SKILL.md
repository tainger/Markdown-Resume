---
name: md-image-localize
description: Localize remote images in Markdown notes by downloading them into a sibling assets folder and rewriting links. Use when the user says 图片本地化, 下载图片, 替换图片/原始图片 for one or more md files. Do not use for code formatting or non-image links.
---

# Markdown 图片本地化

把指定 md 文件里的远程图片（cdn.nlark.com、tcs-devops.aliyuncs.com 签名图、g.alicdn.com 占位图等）下载到文件同级 `assets/` 目录，并重写为 `./assets/...` 本地引用。

## 执行

1. 用 Grep 快速确认每个文件的远程图片数量（模式 `!\[.*?\]\(https?://`），无图直接回复，不跑脚本。
2. 调用脚本，可一次传多个文件：

```bash
python3 .trae/skills/md-image-localize/scripts/localize_images.py \
  "路径/11.xxx.md" "路径/12.xxx.md"
```

- 默认命名 `{父目录名}-{文件名前导数字}-{序号}.png`，如 `gogo-agent/11.x.md` → `./assets/gogo-agent-11-1.png`；同一 URL 多次出现复用同一编号。
- 需要自定义前缀时加 `--prefix NAME`；只预览加 `--dry-run`。
- 脚本自动按 magic bytes 校验下载内容，扩展名按实际内容纠正。

3. 汇报每个文件的下载数量、大小、残留数。

## 边界与纪律

- **签名图过期**（403/401 或返回 JSON `Forbidden`）：脚本自动保留原 URL 并在输出中标注。必须告知用户该图需重新从来源平台获取有效链接，不要重试、不要伪造本地文件。
- 只处理图片语法（`![]()` 与 `<img src>`）；普通超链接 `[]()` 不动。
- 已有的本地 `./assets/` 引用跳过，不重复下载。
- 不改写图片以外的正文内容；格式化代码是另一个独立任务。
- 批量处理时以用户明确列出的文件为准，不自行扩大到整个目录。

## 适用场景区别

- 本 skill：md 文件**自身**含有有效远程 URL，直接下载替换。
- 若远程 URL 已全部过期、需要从"重新导出的新文档"里取新签名 URL 按序配对刷新旧文件，那是各目录下历史脚本 `localize_images.py --new-dir ...` 的场景，不要混用。
