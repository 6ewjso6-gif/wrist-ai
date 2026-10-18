# 腕上AI 手机设置器（Android）

在手机上舒服地填好配置，一键通过 **小米穿戴 interconnect** 通道推到手表。

## 为什么要单独做个 App

手表屏幕小，输入 API 密钥基本没法用。这个 App 让你在手机上打字，手表什么都不用输。

## 硬性约束（否则同步不通）

| 项 | 值 |
|---|---|
| 包名 | **必须**是 `com.wrist.ai.chat` |
| 签名 | **必须**与手表快应用同一套证书 |
| 通道 | `@system.interconnect` + `xms-wearable-lib_1.4_release.aar` |

小米的机制：App 与快应用的包名和签名都一致时，`interconnect` 才允许通信。

## 构建（无 Gradle，手工链）

本仓库的 APK 是在一台 Android 手机上用纯命令行构建的：

```bash
# 1. 编译
javac -source 8 -target 8 -bootclasspath android.jar \
      -classpath libs/xms-wearable.jar -d out $(find src -name '*.java')

# 2. 转 dex（d8）
java -cp d8.jar com.android.tools.r8.D8 --min-api 21 --lib android.jar \
     --output . $(find out -name '*.class') libs/xms-wearable.jar

# 3. 打包资源
aapt package -f -M AndroidManifest.xml -S res -I android.jar -F unsigned.apk
aapt add unsigned.apk classes.dex

# 4. 对齐 + 签名（用与快应用相同的证书）
zipalign -f 4 unsigned.apk aligned.apk
apksigner sign --ks wristai.p12 --ks-key-alias wristai \
  --v1-signing-enabled true --v2-signing-enabled true \
  --out app.apk aligned.apk
```

### 三个坑（已踩过）

**0. 必须手动合并 aar 的 `<queries>`**
手工 aapt 打包没有 Gradle 的 manifest 合并。SDK 的 aar 里带了：

```xml
<queries>
    <package android:name="com.xiaomi.wearable" />
    <package android:name="com.mi.health" />
</queries>
```

Android 11+ 有包可见性限制，不声明就**绑定不到小米运动健康的服务**，
表现为「连不上」。必须手工抄进自己的 AndroidManifest.xml。

另外 `BLUETOOTH_CONNECT` / `BLUETOOTH_SCAN` 是 API 31 才有的常量，
用 API 30 的 android.jar 编译时要写字符串字面量。

1. **d8 8.2.2 会在匿名内部类和泛型接口实现上 NPE**
   - `EnclosingMethod: #x.#0`（匿名类）→ d8 崩
   - `Signature` 属性含泛型 → d8 崩
   - **解法**：不用匿名内部类，接口实现用原始类型（raw type）

2. **aapt 在 Android 上需要运行时环境变量**
   ```bash
   export ANDROID_DATA=/data ANDROID_ROOT=/system
   ```

## 依赖

- `libs/xms-wearable.jar` —— 从小米官方 demo 的 `xms-wearable-lib_1.4_release.aar` 提取
- 官方 demo：`https://cdn.cnbj3-fusion.fds.api.mi-img.com/quickapp-vela/interconnect_dev_test_demo.zip`
