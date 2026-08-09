# 🖥️ LoRA 推理加载使用说明

训练完成后你会得到一个 `.safetensors` 文件（约 50-100MB），以下是在不同工具中加载的方法。

---

## 一、Stable Diffusion WebUI (Automatic1111)

### 1. 放置模型文件

```bash
# 把 .safetensors 文件复制到这个目录
stable-diffusion-webui/models/Lora/cyberboy_sdxl.safetensors
```

### 2. 加载使用

1. 启动 WebUI，选择 **SDXL Base 1.0** 作为底模
2. 点击 Generate 按钮下方的 **📌 Lora** 标签
3. 找到 `cyberboy_sdxl`，点击即可自动插入到 Prompt 中
4. Prompt 格式：`<lora:cyberboy_sdxl:0.75> cyberboy, 1boy, ...`

### 3. 推荐参数

| 参数 | 值 |
|:---|:---|
| Sampler | DPM++ 2M Karras |
| Sampling Steps | 25-30 |
| CFG Scale | 7 |
| Size | 1024×1024 |
| LoRA Weight | 0.7-0.85 |

---

## 二、ComfyUI

### 1. 放置模型文件

```bash
# 放到 ComfyUI 的 models/loras 目录
ComfyUI/models/loras/cyberboy_sdxl.safetensors
```

### 2. 工作流节点

```
Load Checkpoint (SDXL Base 1.0)
    → CLIP Text Encode (Prompt)
        → Load LoRA (cyberboy_sdxl, strength: 0.75)
            → KSampler → VAE Decode → Save Image
```

### 3. 关键节点配置

- **Load LoRA**: model_weight=0.75, clip_weight=0.75
- **KSampler**: steps=25, cfg=7, sampler=dpmpp_2m, scheduler=karras

---

## 三、在线平台（无需本地 GPU）

| 平台 | 使用方式 |
|:---|:---|
| [Tensor.Art](https://tensor.art) | 上传 LoRA → 在线生成 |
| [Civitai](https://civitai.com) | 上传 LoRA → 在线生成 |
| [LiblibAI](https://www.liblib.art) | 上传 LoRA → 在线生成（中文友好） |

---

## 四、推理参数速查

| 参数 | 推荐值 | 说明 |
|:---|:---|:---|
| LoRA Weight | 0.7-0.85 | 低于 0.5 面部不像，高于 0.9 可能过拟合 |
| Sampler | DPM++ 2M Karras | 质量和速度平衡最佳 |
| Steps | 25-30 | 超过 30 变化不大 |
| CFG Scale | 7 | 太高画面过饱和，太低细节模糊 |
| Resolution | 1024×1024 | SDXL 原生分辨率 |
| Clip Skip | 2 | SDXL 推荐值 |
