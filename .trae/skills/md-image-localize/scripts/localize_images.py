#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Markdown 远程图片本地化（单文件直处理模式）。

流程：扫描指定 md 文件中的远程图片 -> 按 URL 首次出现顺序编号 ->
下载到同级 assets/ -> 重写为本地相对路径 -> 校验残留。

命名规则（默认）：{父目录名}-{文件名前导数字}-{序号}.{ext}
  gogo-agent/11.xxx.md 的第 1 张图 -> ./assets/gogo-agent-11-1.png
同一 URL 在文件内多次出现只下载一次、复用同一编号。

用法：
  python3 localize_images.py file1.md [file2.md ...]
  python3 localize_images.py --prefix my-note *.md
  python3 localize_images.py --dry-run 11.xxx.md
"""

from __future__ import annotations

import argparse
import re
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path
from urllib.parse import urlparse

MD_IMG_RE = re.compile(r'!\[([^\]]*)\]\((https?://[^)\s]+)\)')
HTML_IMG_RE = re.compile(r'<img[^>]+src=["\'](https?://[^"\']+)["\'][^>]*>', re.IGNORECASE)

# 图片 magic bytes 校验
MAGIC = [
    (b'\x89PNG\r\n\x1a\n', '.png'),
    (b'\xff\xd8\xff', '.jpg'),
    (b'GIF87a', '.gif'),
    (b'GIF89a', '.gif'),
    (b'RIFF', '.webp'),  # 进一步检查 WEBP
]


def default_prefix(md_path: Path) -> str:
    """父目录名 + 文件名前导数字，如 gogo-agent/11.xxx.md -> gogo-agent-11。"""
    num = re.match(r'^(\d+(?:\.\d+)?)', md_path.name)
    parts = [md_path.parent.name]
    if num:
        parts.append(num.group(1))
    return '-'.join(parts)


def guess_ext(url: str) -> str:
    path = urlparse(url).path.lower()
    for ext in ('.png', '.jpg', '.jpeg', '.gif', '.webp', '.svg', '.bmp'):
        if path.endswith(ext):
            return '.jpg' if ext == '.jpeg' else ext
    return '.png'


def sniff_ext(data: bytes, fallback: str) -> str | None:
    """用 magic bytes 校验下载内容；返回规范扩展名，非图片返回 None。"""
    for magic, ext in MAGIC:
        if data.startswith(magic):
            if magic == b'RIFF' and data[8:12] != b'WEBP':
                continue
            return ext
    if data[:5] == b'<?xml' or data[:4] == b'<svg':
        return '.svg'
    return None


def download(url: str, timeout: int = 30, retries: int = 2) -> bytes | None:
    for attempt in range(1, retries + 1):
        try:
            req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
            with urllib.request.urlopen(req, timeout=timeout) as resp:
                return resp.read()
        except urllib.error.HTTPError as e:
            if e.code in (401, 403, 404):
                print(f'    [HTTP {e.code}] 签名过期或无权限，保留原 URL')
                return None
            if attempt < retries:
                time.sleep(attempt)
        except Exception as e:
            if attempt < retries:
                time.sleep(attempt)
            else:
                print(f'    [ERR] {e}')
    return None


def process_file(md_path: Path, prefix: str | None, dry_run: bool) -> tuple[int, int]:
    """返回 (远程 URL 总处数, 成功替换处数)。"""
    text = md_path.read_text(encoding='utf-8')
    prefix = prefix or default_prefix(md_path)
    assets_dir = md_path.parent / 'assets'

    # 收集所有远程图片引用（md + html），按出现位置排序
    refs = []  # (start, end, url, kind, alt_or_full)
    for m in MD_IMG_RE.finditer(text):
        refs.append((m.start(), m.end(), m.group(2), 'md', m.group(1)))
    for m in HTML_IMG_RE.finditer(text):
        refs.append((m.start(), m.end(), m.group(1), 'html', m.group(0)))
    refs.sort(key=lambda r: r[0])

    if not refs:
        print(f'[跳过] {md_path.name}: 无远程图片')
        return 0, 0

    # 同 URL 去重编号（首次出现顺序）
    url_to_name: dict[str, str] = {}
    print(f'[处理] {md_path.name}: {len(refs)} 处远程引用，前缀 {prefix}-')
    if not dry_run:
        assets_dir.mkdir(parents=True, exist_ok=True)

    replaced = 0
    # 从后往前替换，避免位置失效
    for start, end, url, kind, alt in reversed(refs):
        if url in url_to_name:
            local_name = url_to_name[url]
        else:
            seq = len(url_to_name) + 1
            ext = guess_ext(url)
            local_name = f'{prefix}-{seq}{ext}'
            url_to_name[url] = local_name

            if not dry_run:
                data = download(url)
                if data is None:
                    del url_to_name[url]
                    continue
                real_ext = sniff_ext(data, ext)
                if real_ext is None:
                    head = data[:120].decode('utf-8', errors='ignore').replace('\n', ' ')
                    print(f'    [非图片] 响应内容: {head[:100]}，保留原 URL')
                    del url_to_name[url]
                    continue
                if real_ext != ext:
                    local_name = f'{prefix}-{seq}{real_ext}'
                    url_to_name[url] = local_name
                (assets_dir / local_name).write_bytes(data)
                size = len(data)
                print(f'    ✓ {local_name} ({size/1024:.0f}KB)')
            else:
                print(f'    [dry-run] 将下载 {local_name}')

        local_ref = f'./assets/{local_name}'
        if kind == 'md':
            new_ref = f'![{alt}]({local_ref})'
        else:
            new_ref = re.sub(r'(src=["\'])https?://[^"\']+(["\'])',
                             lambda m: m.group(1) + local_ref + m.group(2), alt, count=1)
        text = text[:start] + new_ref + text[end:]
        replaced += 1

    if not dry_run and replaced:
        md_path.write_text(text, encoding='utf-8')

    remain = len(re.findall(r'!\[[^\]]*\]\(https?://', text)) + \
             len(re.findall(r'<img[^>]+src=["\']https?://', text, re.IGNORECASE))
    print(f'    结果: 替换 {replaced}/{len(refs)} 处，残留远程图片 {remain}')
    return len(refs), replaced


def main():
    ap = argparse.ArgumentParser(description='Markdown 远程图片本地化')
    ap.add_argument('files', nargs='+', help='一个或多个 md 文件')
    ap.add_argument('--prefix', help='文件名前缀（默认 父目录名-文件号）')
    ap.add_argument('--dry-run', action='store_true', help='只预览不下载不修改')
    args = ap.parse_args()

    total_refs = total_replaced = 0
    for f in args.files:
        p = Path(f).resolve()
        if not p.is_file():
            print(f'[错误] 文件不存在: {f}')
            sys.exit(1)
        refs, rep = process_file(p, args.prefix, args.dry_run)
        total_refs += refs
        total_replaced += rep

    print('=' * 50)
    print(f'总计: {total_replaced}/{total_refs} 处已本地化'
          + ('（dry-run，未修改文件）' if args.dry_run else ''))


if __name__ == '__main__':
    main()
