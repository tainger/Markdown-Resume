#!/usr/bin/env python3
"""
CyberLoRA 图片预处理脚本
功能：将任意尺寸照片统一处理为 1024×1024，用于 LoRA 训练
用法：python preprocess.py --input ./raw_photos --output ./train_data/100_cyberboy
"""

import os
import argparse
from PIL import Image


def preprocess_image(input_path, output_path, target_size=1024, quality=95):
    """等比缩放 + 居中裁剪到 target_size × target_size"""
    img = Image.open(input_path).convert('RGB')
    w, h = img.size

    # 等比缩放：短边对齐 target_size
    scale = target_size / min(w, h)
    new_w, new_h = int(w * scale), int(h * scale)
    img = img.resize((new_w, new_h), Image.LANCZOS)

    # 居中裁剪
    left = (new_w - target_size) // 2
    top = (new_h - target_size) // 2
    img = img.crop((left, top, left + target_size, top + target_size))

    img.save(output_path, quality=quality)
    return True


def main():
    parser = argparse.ArgumentParser(description='CyberLoRA 图片预处理')
    parser.add_argument('--input', '-i', required=True, help='原始图片目录')
    parser.add_argument('--output', '-o', required=True, help='输出目录')
    parser.add_argument('--size', '-s', type=int, default=1024, help='目标尺寸 (默认 1024)')
    parser.add_argument('--quality', '-q', type=int, default=95, help='JPEG 质量 (默认 95)')
    args = parser.parse_args()

    os.makedirs(args.output, exist_ok=True)

    valid_ext = {'.jpg', '.jpeg', '.png', '.webp', '.bmp'}
    files = [f for f in os.listdir(args.input)
             if os.path.splitext(f)[1].lower() in valid_ext]

    if not files:
        print(f'❌ {args.input} 中没有找到图片文件')
        return

    success, skipped = 0, 0
    for i, fname in enumerate(sorted(files), 1):
        in_path = os.path.join(args.input, fname)
        out_name = f'img_{i:03d}.jpg'
        out_path = os.path.join(args.output, out_name)

        try:
            preprocess_image(in_path, out_path, args.size, args.quality)
            success += 1
            print(f'  [{i}/{len(files)}] ✅ {fname} → {out_name}')
        except Exception as e:
            skipped += 1
            print(f'  [{i}/{len(files)}] ⚠️ 跳过 {fname}: {e}')

    print(f'\n✅ 处理完成: {success} 张成功, {skipped} 张跳过')
    print(f'📁 输出目录: {os.path.abspath(args.output)}')


if __name__ == '__main__':
    main()
