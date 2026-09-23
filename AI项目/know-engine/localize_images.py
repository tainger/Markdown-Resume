#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
从新导出的 Markdown 文档中提取图片 URL，下载到本地，并替换 know-engine 目录下
旧文件中已过期的远程图片引用。

匹配策略：
  1. 文件匹配：按文件名精确匹配 → 按数字前缀匹配 → 按标题相似度匹配
  2. 图片匹配：在配对的文件内，按图片出现顺序一一对应（同一文档重新导出后
     图片顺序不变，只是 URL 签名刷新）

用法：
  python3 localize_images.py --new-dir /path/to/newly_exported_docs
  python3 localize_images.py --new-dir /path/to/new --dry-run   # 只预览不修改
"""

import argparse
import hashlib
import os
import re
import sys
import time
from pathlib import Path
from urllib.parse import urlparse

import urllib.request
import urllib.error

# ---------------- 配置 ----------------

DEFAULT_OLD_DIR = Path(__file__).resolve().parent
DEFAULT_ASSETS_DIR = DEFAULT_OLD_DIR / "assets"

# 跳过这些域名（已本地化或不需要处理）
SKIP_DOMAINS = ("localhost", "127.0.0.1")

# 图片 URL 正则：同时兼容 Markdown 图片和 HTML img 标签
MD_IMG_RE = re.compile(r'!\[([^\]]*)\]\((https?://[^)\s]+)\)')
HTML_IMG_RE = re.compile(r'<img[^>]+src=["\'](https?://[^"\']+)["\'][^>]*>', re.IGNORECASE)


# ---------------- 工具函数 ----------------

def extract_image_urls(md_text):
    """从 Markdown 文本中按出现顺序提取所有远程图片 URL。

    返回 [(url, original_markdown_snippet), ...]
    """
    results = []
    # 合并两种语法的匹配，按在文本中出现的位置排序
    matches = []
    for m in MD_IMG_RE.finditer(md_text):
        matches.append((m.start(), m.group(2), m.group(0), "md"))
    for m in HTML_IMG_RE.finditer(md_text):
        matches.append((m.start(), m.group(1), m.group(0), "html"))
    matches.sort(key=lambda x: x[0])
    for _, url, snippet, _ in matches:
        results.append((url, snippet))
    return results


def is_remote_url(url):
    """判断是否为需要处理的远程 URL。"""
    if not url.startswith(("http://", "https://")):
        return False
    host = urlparse(url).netloc.lower()
    return host not in SKIP_DOMAINS


def get_file_number(name):
    """提取文件名开头的数字编号，如 '7.xxx.md' -> '7'。"""
    m = re.match(r'^(\d+)', name)
    return m.group(1) if m else None


def get_title(md_text):
    """提取 Markdown 第一个一级标题作为文档标题。"""
    for line in md_text.splitlines():
        line = line.strip()
        if line.startswith("# "):
            return line[2:].strip()
    return ""


def levenshtein_ratio(s1, s2):
    """简单的字符串相似度（0~1）。"""
    if not s1 and not s2:
        return 1.0
    if not s1 or not s2:
        return 0.0
    s1, s2 = s1.lower(), s2.lower()
    if s1 == s2:
        return 1.0
    m, n = len(s1), len(s2)
    dp = list(range(n + 1))
    for i in range(1, m + 1):
        prev = dp[0]
        dp[0] = i
        for j in range(1, n + 1):
            tmp = dp[j]
            dp[j] = min(dp[j] + 1, dp[j - 1] + 1, prev + (0 if s1[i - 1] == s2[j - 1] else 1))
            prev = tmp
    return 1.0 - dp[n] / max(m, n)


def match_old_file(new_path, old_files):
    """为新导出的文件找到对应的旧文件。

    old_files: {Path: (text, number, title)}
    返回 (old_path, score) 或 (None, 0)
    """
    new_name = new_path.name
    new_number = get_file_number(new_name)
    new_text = new_path.read_text(encoding="utf-8", errors="ignore")
    new_title = get_title(new_text)

    best_path, best_score = None, 0.0

    for old_path, (_, old_number, old_title) in old_files.items():
        score = 0.0
        # 1. 精确文件名匹配
        if old_path.name == new_name:
            score = 1.0
        # 2. 数字编号匹配
        elif new_number and old_number and new_number == old_number:
            score = 0.8
        # 3. 标题相似度
        if new_title and old_title:
            title_sim = levenshtein_ratio(new_title, old_title)
            if title_sim > 0.6:
                score = max(score, 0.5 + title_sim * 0.4)
        if score > best_score:
            best_score, best_path = score, old_path

    return best_path, best_score


def guess_extension(url, content_type):
    """根据 URL 和 Content-Type 猜测文件扩展名。"""
    # 优先从 URL 路径判断
    path = urlparse(url).path.lower()
    for ext in (".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg", ".bmp"):
        if path.endswith(ext):
            return ext
    # 再从 Content-Type 判断
    ct = (content_type or "").lower()
    if "png" in ct:
        return ".png"
    if "jpeg" in ct or "jpg" in ct:
        return ".jpg"
    if "gif" in ct:
        return ".gif"
    if "webp" in ct:
        return ".webp"
    if "svg" in ct:
        return ".svg"
    return ".png"  # 默认


def download_image(url, dest_dir, timeout=30, retries=3):
    """下载图片到 dest_dir，返回本地文件名（不含目录），失败返回 None。"""
    # 用 URL 的稳定部分作为文件名基础
    path = urlparse(url).path
    # aliyuncs: /storage/<id>  → 用 <id>
    # nlark: /yuque/.../<hash>.png → 用 basename
    base = path.rstrip("/").rsplit("/", 1)[-1]
    if not base or "." not in base:
        # 没有扩展名，用 URL hash
        base = hashlib.md5(url.encode()).hexdigest()[:16]

    dest_dir = Path(dest_dir)
    dest_dir.mkdir(parents=True, exist_ok=True)

    for attempt in range(1, retries + 1):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
            with urllib.request.urlopen(req, timeout=timeout) as resp:
                data = resp.read()
                content_type = resp.headers.get("Content-Type", "")
            ext = guess_extension(url, content_type)
            # 确保文件名带正确扩展名
            name = base if base.lower().endswith(ext) else base + ext
            out = dest_dir / name
            out.write_bytes(data)
            return name
        except urllib.error.HTTPError as e:
            if e.code == 403:
                print(f"  [403] 可能已过期: {url[:80]}...")
                return None
            if attempt < retries:
                time.sleep(1 * attempt)
                continue
            print(f"  [HTTP {e.code}] 下载失败: {url[:80]}...")
            return None
        except Exception as e:
            if attempt < retries:
                time.sleep(1 * attempt)
                continue
            print(f"  [ERR] {e}: {url[:80]}...")
            return None
    return None


# ---------------- 主流程 ----------------

def build_old_index(old_dir):
    """扫描旧目录，建立 {old_path: (text, number, title)} 索引。"""
    index = {}
    for p in sorted(old_dir.glob("*.md")):
        text = p.read_text(encoding="utf-8", errors="ignore")
        index[p] = (text, get_file_number(p.name), get_title(text))
    return index


def run(new_dir, old_dir, assets_dir, dry_run=False):
    new_dir = Path(new_dir)
    old_dir = Path(old_dir)
    assets_dir = Path(assets_dir)

    if not new_dir.is_dir():
        print(f"错误: 新文档目录不存在: {new_dir}")
        sys.exit(1)

    old_index = build_old_index(old_dir)
    print(f"扫描旧目录: {len(old_index)} 个 .md 文件")
    print(f"扫描新目录: {len(list(new_dir.glob('*.md')))} 个 .md 文件")
    print(f"图片输出目录: {assets_dir}")
    print(f"模式: {'预览(dry-run)' if dry_run else '执行'}\n")

    new_files = sorted(new_dir.glob("*.md"))
    total_old_urls = 0
    total_replaced = 0
    total_downloaded = 0
    total_failed = 0

    for new_path in new_files:
        old_path, score = match_old_file(new_path, old_index)
        if not old_path or score < 0.5:
            print(f"[跳过] 未匹配到旧文件: {new_path.name} (最佳得分 {score:.2f})")
            continue

        print(f"[匹配] {new_path.name}  <->  {old_path.name}  (相似度 {score:.2f})")

        new_text = new_path.read_text(encoding="utf-8", errors="ignore")
        old_text = old_index[old_path][0]

        new_imgs = extract_image_urls(new_text)
        old_imgs = extract_image_urls(old_text)

        # 只保留远程 URL
        new_remote = [(u, s) for u, s in new_imgs if is_remote_url(u)]
        old_remote = [(u, s) for u, s in old_imgs if is_remote_url(u)]

        if not old_remote:
            print(f"  旧文件无远程图片，跳过")
            continue

        total_old_urls += len(old_remote)

        # 按顺序配对
        pair_count = min(len(new_remote), len(old_remote))
        if len(new_remote) != len(old_remote):
            print(f"  ⚠ 图片数量不一致: 新={len(new_remote)} 旧={len(old_remote)}，"
                  f"按前 {pair_count} 张配对")

        updated_text = old_text
        file_replaced = 0
        file_downloaded = 0
        file_failed = 0

        for i in range(pair_count):
            old_url, old_snippet = old_remote[i]
            new_url, _ = new_remote[i]

            # 下载新图片
            local_name = None
            if not dry_run:
                local_name = download_image(new_url, assets_dir)
            else:
                # 预览模式：生成一个预期的文件名
                path = urlparse(new_url).path
                base = path.rstrip("/").rsplit("/", 1)[-1]
                if not base or "." not in base:
                    base = hashlib.md5(new_url.encode()).hexdigest()[:16] + ".png"
                local_name = base

            if local_name:
                local_ref = f"./assets/{local_name}"
                new_snippet = old_snippet.replace(old_url, local_ref)
                updated_text = updated_text.replace(old_snippet, new_snippet, 1)
                file_replaced += 1
                if not dry_run:
                    file_downloaded += 1
                print(f"  ✓ [{i+1}] {old_url[:60]}... -> {local_ref}")
            else:
                file_failed += 1
                print(f"  ✗ [{i+1}] 下载失败: {new_url[:60]}...")

        if not dry_run and file_replaced > 0:
            old_path.write_text(updated_text, encoding="utf-8")

        total_replaced += file_replaced
        total_downloaded += file_downloaded
        total_failed += file_failed
        print(f"  结果: 替换 {file_replaced} / {len(old_remote)}，失败 {file_failed}\n")

    print("=" * 60)
    print(f"完成: 旧文件远程图片 {total_old_urls} 张")
    print(f"      成功替换 {total_replaced} 张，下载失败 {total_failed} 张")
    if dry_run:
        print("(预览模式，未实际修改文件)")


def main():
    parser = argparse.ArgumentParser(
        description="从新导出的文档中提取图片 URL，下载并替换旧文件中过期的图片引用"
    )
    parser.add_argument("--new-dir", required=True, help="新导出的 Markdown 文档所在目录")
    parser.add_argument("--old-dir", default=str(DEFAULT_OLD_DIR), help="旧文档目录（默认 know-engine）")
    parser.add_argument("--assets-dir", default=str(DEFAULT_ASSETS_DIR), help="图片保存目录")
    parser.add_argument("--dry-run", action="store_true", help="预览模式，不修改文件也不下载")
    args = parser.parse_args()

    run(args.new_dir, args.old_dir, args.assets_dir, dry_run=args.dry_run)


if __name__ == "__main__":
    main()
