// 与 AI 服务交互的三件事：对话 / 语音识别 / 语音合成
import fetch from '@system.fetch'
import { getActivePrompt } from './config.js'

/* ---------------- 对话 ---------------- */
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

/* ---------------- 语音识别 ---------------- */
// 用 @system.record 录音，@system.uploadtask 以 multipart 上传到 Whisper 兼容接口
export function recordVoice(ms) {
  return new Promise((resolve, reject) => {
    let record
    try { record = require('@system.record') } catch (e) { return reject('这台手表不支持录音') }

    record.start({
      duration: ms || 8000,
      sampleRate: 16000,
      numberOfChannels: 1,
      format: 'wav',
      success: (data) => resolve(data.uri),
      fail: (err, code) => reject('录音失败 ' + code + '：' + err)
    })
  })
}

export function stopVoice() {
  try { require('@system.record').stop() } catch (e) {}
}

export function transcribe(cfg, fileUri) {
  return new Promise((resolve, reject) => {
    if (!cfg.asrKey) return reject('还没配语音识别的密钥')
    let uploadtask
    try { uploadtask = require('@system.uploadtask') } catch (e) { return reject('这台手表不支持上传') }

    uploadtask.uploadFile({
      url: cfg.asrUrl,
      filePath: fileUri,
      name: 'file',
      header: { 'Authorization': 'Bearer ' + cfg.asrKey },
      formData: { model: cfg.asrModel, language: 'zh', response_format: 'json' },
      timeout: 30000,
      success: (res) => {
        let body = res.data
        if (typeof body === 'string') {
          try { body = JSON.parse(body) } catch (e) { return resolve(String(body).trim()) }
        }
        if (body && body.text) return resolve(String(body.text).trim())
        if (body && body.error) return reject(body.error.message || '识别服务报错')
        reject('识别结果异常：' + JSON.stringify(body).slice(0, 100))
      },
      fail: (err, code) => reject('上传失败 ' + code + '：' + err)
    })
  })
}

/* ---------------- 语音合成 ---------------- */
// 下载 TTS 音频到本地缓存，再交给 @system.audio 播放
export function speakText(cfg, text) {
  return new Promise((resolve, reject) => {
    if (!cfg.ttsUrl) return reject('还没配朗读接口')
    if (!text) return reject('没有可读的内容')

    let request, audio
    try { request = require('@system.request') } catch (e) { return reject('不支持下载') }
    try { audio = require('@system.audio') } catch (e) { return reject('不支持播放') }

    const url = cfg.ttsUrl
    const header = { 'Content-Type': 'application/json' }
    if (cfg.ttsKey) header['Authorization'] = 'Bearer ' + cfg.ttsKey

    request.download({
      url: url,
      method: 'POST',
      header: header,
      // 部分实现要求把 body 放 data
      data: JSON.stringify({ model: cfg.ttsModel, voice: cfg.ttsVoice, input: text, response_format: 'mp3' }),
      success: (res) => {
        request.onDownloadComplete({
          token: res.token,
          success: (r) => {
            try {
              audio.src = r.uri
              audio.play()
              resolve(r.uri)
            } catch (e) { reject('播放失败：' + e.message) }
          },
          fail: (e, c) => reject('下载音频失败 ' + c)
        })
      },
      fail: (err, code) => reject('发起下载失败 ' + code + '：' + err)
    })
  })
}

export function stopSpeak() {
  try { require('@system.audio').stop() } catch (e) {}
}

/* ---------------- 振动反馈 ---------------- */
export function buzz(short) {
  try { require('@system.vibrator').start ? require('@system.vibrator').start(short ? 30 : 60) : null } catch (e) {}
}
