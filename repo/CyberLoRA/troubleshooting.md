# 🔧 故障排查速查表

## Colab 训练阶段

| 症状 | 原因 | 解决方案 |
|:---|:---|:---|
| `Out of Memory (OOM)` | T4 16GB 不够 | 减小 `network_dim` 到 16，或关闭 `cache_latents` |
| 训练中途断连 | Colab 闲置超时 | 每 30 分钟点一下页面；或用 Colab Pro |
| `CUDA out of memory` | batch size 太大 | 确保 `train_batch_size=1` |
| 下载 SDXL 模型超时 | HuggingFace 被墙/慢 | 重试；或设置 `export HF_ENDPOINT=https://hf-mirror.com` |
| `ModuleNotFoundError: xformers` | 安装失败 | 跳过 xformers，用 `--sdpa` 代替 |
| 训练 Loss 不下降 | 学习率不合适 | 尝试 lr=5e-5 或 lr=2e-4 |
| `.safetensors` 文件 0KB | 训练异常终止 | 检查是否有足够的步数完成；确保至少跑完 1 个 epoch |

---

## 图片准备阶段

| 症状 | 原因 | 解决方案 |
|:---|:---|:---|
| 生成的人脸不像自己 | 照片角度单一 / 数量不足 | 增加照片到 20-30 张，确保有侧脸、不同表情 |
| 生成效果模糊 | 训练照片质量低 | 确保照片清晰、分辨率 ≥ 512×512 |
| 生成的人脸变形 | 过拟合 | 降低 LoRA Weight 到 0.5-0.65，或减少训练 epoch |
| 触发词不生效 | 触发词拼写错误或在 prompt 末尾 | 把 `cyberboy` 放在 prompt **最前面** |

---

## 推理（生成）阶段

| 症状 | 原因 | 解决方案 |
|:---|:---|:---|
| 面部特征完全不对 | 没加载 LoRA 或 weight=0 | 确认 `<lora:cyberboy_sdxl:0.75>` 语法正确 |
| 底模错误 | 用了 SD 1.5 的底模 | LoRA 必须配合 **SDXL Base 1.0** 使用 |
| 画面很糊 | Steps 太少 | 增加到 25-30 |
| LoRA 权重太高导致画面崩 | weight > 0.95 | 降低到 0.7-0.85 |
| 用别的 LoRA 一起用冲突 | 多个 LoRA 权重叠加 | 逐个降低各 LoRA 的 weight |

---

## 在线平台

| 症状 | 原因 | 解决方案 |
|:---|:---|:---|
| 上传 .safetensors 失败 | 文件损坏 | 重新下载，检查文件大小 > 10MB |
| 平台提示 "模型不兼容" | 上传了非 SDXL LoRA | 确认训练时用的是 SDXL 底模 |
| 生成队列等待太久 | 免费用户排队 | 错峰使用（凌晨），或升级付费套餐 |
