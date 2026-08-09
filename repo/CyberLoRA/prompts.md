# 🧪 CyberLoRA 测试 Prompt 模板

训练完成后，在 Stable Diffusion 中用以下 Prompt 测试效果。

**通用参数**：Sampler: DPM++ 2M Karras, Steps: 25-30, CFG Scale: 7, LoRA Weight: 0.75

---

## 1. 棚拍肖像

```
cyberboy, 1boy, portrait, studio lighting, bokeh, detailed face, looking at camera
Negative: worst quality, low quality, blurry, deformed face, extra fingers
```

## 2. 赛博朋克街景

```
cyberboy, 1boy, standing on Tokyo street, neon lights, rain, night, cyberpunk, 8k, cinematic lighting
Negative: worst quality, low quality, blurry, deformed, bad anatomy
```

## 3. 科幻宇航员

```
cyberboy, 1boy, astronaut in spacesuit, walking on Mars surface, red desert, sci-fi, cinematic, hdr, 8k
Negative: worst quality, low quality, blurry, deformed
```

## 4. 商务正装

```
cyberboy, 1boy, wearing black suit, white shirt, necktie, office background, professional, natural light, corporate headshot
Negative: worst quality, low quality, blurry, deformed face, bad anatomy
```

## 5. 户外运动

```
cyberboy, 1boy, surfing on ocean waves, sunset sky, dynamic action pose, splashing water, golden hour
Negative: worst quality, low quality, blurry, deformed, extra limbs
```

## 6. 古风侠客

```
cyberboy, 1boy, ancient Chinese warrior, traditional armor, holding sword, temple in background, ink painting style, epic
Negative: worst quality, low quality, blurry, deformed, modern elements
```

## 7. 咖啡厅日常

```
cyberboy, 1boy, sitting in cozy cafe, holding coffee cup, reading book, warm lighting, window with street view, depth of field, 35mm photography
Negative: worst quality, low quality, blurry, deformed
```

## 8. 未来战士

```
cyberboy, 1boy, futuristic soldier, mechanical armor, glowing visor, destroyed city background, sci-fi, hdr, 8k, unreal engine 5 render
Negative: worst quality, low quality, blurry, deformed, bad anatomy
```

---

## 🔧 LoRA 权重调节指南

| LoRA Weight | 效果 | 适用场景 |
|:---|:---|:---|
| 0.5 - 0.65 | 轻度影响，面部特征不明显 | 远景、背影、侧面 |
| 0.7 - 0.85 | 最佳平衡（**推荐**） | 大多数场景 |
| 0.85 - 1.0 | 强影响，可能过拟合 | 特写肖像 |

---

## 📝 Prompt 格式说明

```
cyberboy, 1boy, <场景描述>, <风格描述>, <画质词>

触发词       主体     构图/场景        艺术风格    提升画质
```

> ⚠️ 触发词 `cyberboy` **必须在最前面**，否则 LoRA 可能不生效
