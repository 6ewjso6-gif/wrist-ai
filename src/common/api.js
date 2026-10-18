// 三件事：对话 / 语音识别 / 语音合成
import fetch from '@system.fetch'
import { getActivePrompt } from './config.js'

/* ================= 对话 ================= */
export function askAI(cfg, messages) {
  return new Promise((resolve, reject) => {
    const body = JSON.stringify({
      model: cfg.model,
      messages: [{ role: 'system', content: getActivePrompt(cfg) }].concat(messages),
      temperature: 0.7,
      max_tokens: 400,
      stream: false
    })

    fetch.fetch({
      url: cfg.apiUrl,
      method: 'POST',
      header: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + cfg.apiKey
      },
      data: body,
      responseType: 'json',
      success: (res) => {
        let data = res.data
        if (typeof data === 'string') {
          try { data = JSON.parse(data) } catch (e) {
            return reject('返回不是 JSON：' + String(data).slice(0, 80))
          }
        }
        if (data && data.error) return reject(data.error.message || JSON.stringify(data.error))
        const c = data && data.choices && data.choices[0]
        const txt = c && c.message && (c.message.content || c.message.reasoning_content)
        if (!txt) return reject('响应格式异常：' + JSON.stringify(data).slice(0, 100))
        resolve(String(txt).trim())
      },
      fail: (err, code) => reject('网络错误 ' + code + '：' + (err && err.data ? err.data : ''))
    })
  })
}

/* ================= 语音识别 ================= */
// @system.record 录音 → @system.uploadtask multipart 上传到 Whisper 兼容接口
export function recordVoice(ms) {
  return new Promise((resolve, reject) => {
    let record
    try { record = require('@system.record') } catch (e) {
      return reject('这台设备没有录音能力')
    }

    // 16000Hz 单声道按官方表用 256000 码率
    record.start({
      duration: ms || 8000,
      sampleRate: 16000,
      numberOfChannels: 1,
      encodeBitRate: 256000,
      format: 'wav',
      success: (data) => {
        if (!data || !data.uri) return reject('录音没有返回文件路径')
        resolve(data.uri)
      },
      fail: (err, code) => reject('录音失败(' + code + ')：' + err)
    })
  })
}

export function stopVoice() {
  try { require('@system.record').stop() } catch (e) {}
}

export function transcribe(cfg, fileUri) {
  return new Promise((resolve, reject) => {
    if (!cfg.asrKey) return reject('还没配置语音识别密钥')
    let uploadtask
    try { uploadtask = require('@system.uploadtask') } catch (e) {
      return reject('这台设备不支持文件上传')
    }

    uploadtask.uploadFile({
      url: cfg.asrUrl,
      filePath: fileUri,
      name: 'file',
      header: { 'Authorization': 'Bearer ' + cfg.asrKey },
      formData: {
        model: cfg.asrModel || 'whisper-1',
        language: 'zh',
        response_format: 'json'
      },
      timeout: 40000,
      success: (res) => {
        let body = res.data
        if (typeof body === 'string') {
          try { body = JSON.parse(body) } catch (e) {
            return resolve(String(body).trim())   // 有的服务直接回纯文本
          }
        }
        if (body && body.text) return resolve(String(body.text).trim())
        if (body && body.error) return reject(body.error.message || '识别服务报错')
        reject('识别结果异常：' + JSON.stringify(body).slice(0, 100))
      },
      fail: (err, code) => reject('上传失败(' + code + ')：' + err)
    })
  })
}

/* ================= 语音合成 ================= */
// request.download 只支持 GET，所以 TTS 地址必须写成 URL 模板：
//   {text} 会被替换成 URL 编码后的正文，{key} 替换成密钥
// 例（有道，免费无需 key）:
//   https://dict.youdao.com/dictvoice?type=2&audio={text}
// 例（百度）:
//   https://tsn.baidu.com/text2audio?tex={text}&tok={key}&cuid=wristai&ctp=1&lan=zh
function buildTtsUrl(tpl, text, key) {
  return tpl
    .replace(/\{text\}/g, encodeURIComponent(text))
    .replace(/\{key\}/g, encodeURIComponent(key || ''))
}

export function speakText(cfg, text) {
  return new Promise((resolve, reject) => {
    if (!cfg.ttsUrl) return reject('还没配朗读接口')
    if (!text) return reject('没有可读的内容')

    let request, audio
    try { request = require('@system.request') } catch (e) { return reject('不支持下载') }
    try { audio = require('@system.audio') } catch (e) { return reject('不支持音频播放') }

    const url = buildTtsUrl(cfg.ttsUrl, text.slice(0, 300), cfg.ttsKey)

    try { audio.stop() } catch (e) {}

    request.download({
      url: url,
      header: cfg.ttsKey && cfg.ttsUrl.indexOf('{key}') < 0
        ? { 'Authorization': 'Bearer ' + cfg.ttsKey } : {},
      success: (res) => {
        if (!res || !res.token) return reject('下载没有返回 token')
        request.onDownloadComplete({
          token: res.token,
          success: (r) => {
            if (!r || !r.uri) return reject('下载完成但没有文件路径')
            try {
              audio.onerror = (e) => reject('播放出错：' + JSON.stringify(e).slice(0, 80))
              audio.onended = () => {}
              audio.src = r.uri
              audio.autoplay = true
              audio.play()
              resolve(r.uri)
            } catch (e) {
              reject('播放失败：' + (e && e.message ? e.message : e))
            }
          },
          fail: (e, c) => reject('下载音频失败(' + c + ')：' + e)
        })
      },
      fail: (err, code) => reject('发起下载失败(' + code + ')：' + err)
    })
  })
}

export function stopSpeak() {
  try { require('@system.audio').stop() } catch (e) {}
}

/* ================= 振动 ================= */
export function buzz(long) {
  try {
    const v = require('@system.vibrator')
    if (v && v.start) v.start(long ? 60 : 25)
  } catch (e) {}
}
