# 腕上AI · Wrist AI

> 在小米手表上跟大模型聊天 —— 小米 HyperOS（Vela）快应用

一个跑在**小米手表/手环**上的 AI 聊天快应用。支持任何 **OpenAI 兼容接口**，DeepSeek、通义千问、Kimi、自建服务都能接。

## 为什么做这个

手表上想用 AI，现有方案要么得开手机、要么只能看固定内容。这个应用把大模型直接搬到了手腕上 —— 抬腕就能问。

## 特性

- 🎤 **语音输入** —— 表上录音，直传 Whisper 兼容接口转文字（S5 是唯一支持 `@system.record` 的小米手表）
- 🔊 **长按朗读** —— 长按 AI 回复，调 TTS 接口合成语音播放
- ⚡ **快捷提问** —— 空聊天时显示预设问题，点一下直接问
- 🎭 **多套提示词** —— 极简 / 闲聊 / 翻译 / 润色 / 代码 / 学习，点顶部标签一键切换
- 📱 **纯手表端运行** —— 不依赖手机 App
- 🔌 **任意 OpenAI 兼容接口** —— DeepSeek / 通义 / Kimi / Ollama
- ⚙️ **表上直接配置** —— 不用改代码重新打包
- 🎯 **圆屏适配** —— 480×480 圆形 AMOLED 安全区布局
- 💬 **多轮上下文** —— 自动带最近 6 轮

## 支持的设备

| 机型 | 屏幕 | 状态 |
|---|---|---|
| 小米 Watch S5 46mm eSIM | 480×480 圆形 | ✅ 已适配 |
| 小米 Watch S5 42mm | 466×466 圆形 | ⚠️ 未测试 |
| 小米 Watch S4 / S4 Sport | — | ⚠️ 未测试 |
| 小米手环 9 Pro | — | ⚠️ 未测试（手环可能无网络能力） |

## 安装

### 1. 装 AstroBox

手机上安装 [AstroBox-NG](https://github.com/AstralSightStudios/AstroBox-NG/releases)，通过蓝牙配对手表。

### 2. 侧载应用

把 `dist/腕上AI-v1.1.0-S5.rpk` 传到手机 → AstroBox → 本地安装 → 选择该文件。

> 本地侧载不看 AstroBox 资源库里的设备代号（`o65`/`o65m` 那套是给在线分发用的）。

### 3. 配置 API

打开「腕上AI」→ 点右上角 ⚙ → 点预设一键填入（或手填）→ 填密钥 → 保存。

| 服务商 | API 地址 | 模型名 |
|---|---|---|
| DeepSeek | `https://api.deepseek.com/chat/completions` | `deepseek-chat` |
| 通义千问 | `https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions` | `qwen-plus` |
| Kimi | `https://api.moonshot.cn/v1/chat/completions` | `moonshot-v1-8k` |
| 本地 Ollama | `http://<你的IP>:11434/v1/chat/completions` | 你自己拉的模型 |

### 4. 语音功能（可选）

语音输入和朗读都需要额外配接口，不配也不影响文字聊天。

| 能力 | 推荐接口 | 说明 |
|---|---|---|
| 语音识别 | `https://api.openai.com/v1/audio/transcriptions` | 模型 `whisper-1`，也支持 Groq / 硅基流动等 Whisper 兼容服务 |
| 朗读 TTS | `https://api.openai.com/v1/audio/speech` | 模型 `tts-1`，音色 `alloy` / `nova` / `shimmer` |

> 注意：Vela 快应用**没有内置的语音识别和 TTS API**，必须走云端接口。
> 录音/上传/播放这三个底层能力只有 **Xiaomi Watch S5** 全部支持，其他机型请自测。

## 自己构建

```bash
# 1. 装工具链（必须是 1.1.4，见下方说明）
npm i aiot-toolkit@1.1.4

# 2. 生成签名密钥（首次）
node -e "
const forge=require('node-forge'), fs=require('fs');
const k=forge.pki.rsa.generateKeyPair(2048);
const c=forge.pki.createCertificate();
c.publicKey=k.publicKey; c.serialNumber='01'+Date.now();
c.validity.notBefore=new Date();
c.validity.notAfter=new Date(2099,0,1);
const a=[{name:'commonName',value:'wrist-ai'}];
c.setSubject(a); c.setIssuer(a); c.sign(k.privateKey,forge.md.sha256.create());
fs.mkdirSync('sign/release',{recursive:true});
fs.writeFileSync('sign/release/private.pem',forge.pki.privateKeyToPem(k.privateKey));
fs.writeFileSync('sign/release/certificate.pem',forge.pki.certificateToPem(c));
"

# 3. 打包
npx aiot release
# → dist/com.wrist.ai.chat.release.1.0.0.rpk
```

### ⚠️ 为什么必须锁 1.1.4

`aiot-toolkit` 2.x 用 **rspack**（Rust 打包器），依赖 `@rspack/binding-android-arm64` 这种原生模块 —— **在 ARM64 Linux/Android 上根本装不上**。1.x 用纯 JS 的 webpack，任何平台都能跑。

本项目就是在**小米手机（Android/arm64 + Node v24）** 上构建的：

```json
"packageInfo": {
  "toolkit": "1.1.4", "node": "v24.18.0",
  "platform": "android", "arch": "arm64"
}
```

## 项目结构

```
src/
  app.ux                    应用入口
  manifest.json             应用清单（包名/权限/路由/designWidth）
  config-watch.json         手表端配置
  common/
    api.js                  OpenAI 兼容请求封装（@system.fetch）
    config.js               配置读写（@system.storage）
  pages/
    index/index.ux          聊天页
    settings/index.ux       配置页
sign/release/
  certificate.pem           签名证书（私钥不入库）
```

## 圆屏适配说明

S5 46mm 是 480×480 圆形屏，四角会被裁掉。布局用左右内缩保证内容不出界：

```css
顶栏      padding-left/right: 70px
消息区    padding: 6px 34px      气泡 max-width 300/320px
输入条    padding-left/right: 62px   胶囊形 radius 21px
设置页    padding-left/right: 64px / 40px
```

## 排错

| 现象 | 原因 |
|---|---|
| `网络错误 xxx` | eSIM 没开数据 / 没连 WiFi / API 地址写错 / 密钥无效 |
| `响应格式异常` | 接口不是 OpenAI 兼容格式 |
| 装不上 | AstroBox 没连上手表，或手表不是 HyperOS（Vela） |
| 界面被裁 | 机型不是 480 圆屏，改 `manifest.json` 的 `designWidth` |

## 协议

MIT

## 致谢

- [aiot-toolkit](https://www.npmjs.com/package/aiot-toolkit) —— 小米官方快应用工具链
- [AstroBox](https://github.com/AstralSightStudios/AstroBox-NG) —— 第三方穿戴应用分发工具
