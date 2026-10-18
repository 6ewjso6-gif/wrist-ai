// 全局配置：默认值可在设置页修改（存到 @system.storage）

const PROMPT_SETS = [
  { id: 'short',  name: '极简', prompt: '你是运行在手表上的 AI 助手。回答要极简，最多 60 个字，不要用 markdown、不要分点、不要表情。' },
  { id: 'chat',   name: '闲聊', prompt: '你是个幽默的聊天伙伴。口语化回答，最多 80 字，别啰嗦。' },
  { id: 'trans',  name: '翻译', prompt: '你是翻译引擎。用户发中文就译成英文，发英文就译成中文。只输出译文，不要解释。' },
  { id: 'write',  name: '润色', prompt: '你是文字润色助手。把用户的话改得更通顺简洁，只输出改写后的结果。' },
  { id: 'code',   name: '代码', prompt: '你是编程助手。给最精简可行的代码，不要长篇解释。' },
  { id: 'study',  name: '学习', prompt: '你是耐心的老师。用一句话讲清楚概念，必要时给个生活化的比喻。' }
]

const QUICK_ASKS = [
  '今天有什么值得关注的新闻？',
  '帮我写一句朋友圈文案',
  '用一句话解释量子纠缠',
  '推荐一部电影并说明理由'
]

const DEFAULTS = {
  // 对话
  apiUrl: 'https://api.deepseek.com/chat/completions',
  apiKey: '',
  model: 'deepseek-chat',
  promptSetId: 'short',
  customPrompt: '',
  // 语音识别（Whisper 兼容）
  asrUrl: 'https://api.openai.com/v1/audio/transcriptions',
  asrKey: '',
  asrModel: 'whisper-1',
  asrEnabled: true,
  // 语音朗读（TTS）—— 必须是 GET 式 URL 模板，{text} 会被替换
  ttsUrl: 'https://dict.youdao.com/dictvoice?type=2&audio={text}',
  ttsKey: '',
  ttsModel: '',
  ttsVoice: '',
  // 快捷提问
  quickAsks: QUICK_ASKS
}

function defaults() {
  return JSON.parse(JSON.stringify(DEFAULTS))
}

export function getPromptSets() { return PROMPT_SETS }

export function getActivePrompt(cfg) {
  if (cfg.customPrompt) return cfg.customPrompt
  const s = PROMPT_SETS.filter((x) => x.id === cfg.promptSetId)[0]
  return s ? s.prompt : PROMPT_SETS[0].prompt
}

export function loadConfig() {
  return new Promise((resolve) => {
    try {
      const storage = require('@system.storage')
      storage.get({
        key: 'aiConfig',
        success: (res) => {
          let saved = {}
          if (res && res.data) {
            try { saved = JSON.parse(res.data) } catch (e) { saved = {} }
          }
          resolve(Object.assign(defaults(), saved))
        },
        fail: () => resolve(defaults())
      })
    } catch (e) {
      resolve(defaults())
    }
  })
}

export function saveConfig(cfg) {
  return new Promise((resolve) => {
    try {
      const storage = require('@system.storage')
      storage.set({
        key: 'aiConfig',
        value: JSON.stringify(cfg),
        success: () => resolve(true),
        fail: () => resolve(false)
      })
    } catch (e) { resolve(false) }
  })
}

export { DEFAULTS, PROMPT_SETS, QUICK_ASKS }
