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
- 🩺 **内置自检页** —— 一键检测录音/上传/播放/网络/接口，红了截图就能定位问题

## 设置页拆分（v3.1.0）

v2.1.0 把所有设置塞在一页，加大字号后直接挤爆。v3.1.0 拆成**菜单 + 分页**：

```
设置
 ├─ 对话模型    → 地址 / 密钥 / 模型（3 个字段）
 ├─ 语音        → 识别地址·密钥 / 朗读模板（3 个字段 + 试听）
 ├─ 快捷提问    → 4 个问题
 └─ 自检        → 9 项能力检测
```

每页只放 2-4 个字段，大字也放得下。

同时修正了**圆屏安全区**（之前的串位就是这个）：

| y | 圆屏安全半宽 | 可用 x |
|---|---|---|
| 40 | 133 | 107-373 |
| 72 | 171 | 69-411 |
| 120 | 208 | 32-448 |
| 240 | 240 | 0-480 |

旧代码用 `padding: 40px` → 内容 x=40~440，**每边超出 29px 被圆边裁掉**。
现改为 `margin-top: 40px + padding-left/right: 76px`，内容落在 x=76~404 安全区内。

## 界面尺寸（v2.1.0 修正）

初版字号 17-22px 在 480px 圆屏上只有约 1.5mm 高，实际看不清。v2.1.0 全面放大：

| 元素 | v2.0 | v2.1 |
|---|---|---|
| 正文 | 22px | **27px** |
| 顶栏按钮 | 24px 文字 | **60×60 圆钮** |
| 输入框 | 42px 高 | **68px 高** |
| 发送按钮 | 64×42 | **68×68** |
| 设置标签 | 17px | **23px** |
| 设置输入框 | 40px | **64px** |
| 自检条目 | 19px | **27px** |

图标也换了：192×192 圆形渐变底 + 白色对话气泡 + 黄色四角星（旧的是模板默认图标，比例和圆屏遮罩都不对）。

## 支持的设备

| 机型 | 屏幕 | 状态 |
|---|---|---|
| 小米 Watch S5 46mm eSIM | 480×480 圆形 | ✅ 已适配 |
| 小米 Watch S5 42mm | 466×466 圆形 | ⚠️ 未测试 |
| 小米 Watch S4 / S4 Sport | — | ⚠️ 未测试 |
| 小米手环 9 Pro | — | ⚠️ 未测试（手环可能无网络能力） |

## 📱 手机设置器（v3.0.0 新增）

手表上打字填 API 密钥基本没法用。配套的安卓 App 让你在手机上填，一键推到手表。

**下载**：`release/android/wrist-ai-configurator.apk`

工作方式：

```
手机 App  ──interconnect──>  手表快应用
  填表单         官方通道        自动写入 storage
  （同包名 com.wrist.ai.chat + 同签名）
```

装好后：手机打开 App → 自动列出已连接手表 → 填配置 → 点「发送到手表」→ 手表弹出「已从手机同步配置」。

> 小米的 interconnect 要求 App 与快应用的**包名和签名都一致**，本仓库两者都用同一张证书（`CN=wrist-ai-quickapp`）。

## 安装

### 1. 装 AstroBox

手机上安装 [AstroBox-NG](https://github.com/AstralSightStudios/AstroBox-NG/releases)，通过蓝牙配对手表。

### 2. 侧载应用

从 [Releases](https://github.com/6ewjso6-gif/wrist-ai/releases) 下载 `wrist-ai-v3.1.0-S5.rpk` → 传到手机 → AstroBox → 本地安装 → 选择该文件。

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

语音输入和朗读需要额外配接口，**不配也不影响文字聊天**。

#### 语音识别（录音转文字）

| 项 | 值 |
|---|---|
| 接口 | `https://api.openai.com/v1/audio/transcriptions` |
| 模型 | `whisper-1` |
| 密钥 | OpenAI key（Groq / 硅基流动的 Whisper 兼容接口也行） |

#### 朗读（TTS）

> ⚠️ `@system.request` 的 download **只支持 GET**，不能 POST。
> 所以朗读地址必须写成 **URL 模板**，`{text}` 会替换成正文，`{key}` 替换成密钥。

**默认（有道，免费无需 key）—— 已实测返回有效 MP3：**

```
https://dict.youdao.com/dictvoice?type=2&audio={text}
```

**百度（需要 access_token）：**

```
https://tsn.baidu.com/text2audio?tex={text}&tok={key}&cuid=wristai&ctp=1&lan=zh
```

设置页里有「试听一句话」按钮，直接验证配好没有。

### 5. 自检

首页右上角 🩺 打开自检页，会逐项检测：

```
● 录音 record        ● 上传 uploadtask    ● 音频播放 audio
● 下载 request       ● 振动 vibrator       ● 数据存储 storage
● 网络状态           ● 对话接口            ● 朗读接口
```

绿色=可用，红色=不可用并显示原因。**装完先跑这个，出问题截图即可。**

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
