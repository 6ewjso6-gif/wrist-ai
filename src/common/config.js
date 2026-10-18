// 全局配置：默认值，可在设置页里改（存到 storage）
const DEFAULTS = {
  apiUrl: 'https://api.deepseek.com/chat/completions',
  apiKey: '',
  model: 'deepseek-chat',
  systemPrompt: '你是运行在手表上的 AI 助手。回答要极简，最多 60 个字，不要用 markdown。'
}

export function loadConfig() {
  return new Promise((resolve) => {
    try {
      const storage = require('@system.storage')
      storage.get({
        key: 'aiConfig',
        success: (res) => {
          if (res && res.data) {
            try { resolve(Object.assign({}, DEFAULTS, JSON.parse(res.data))) }
            catch (e) { resolve(Object.assign({}, DEFAULTS)) }
          } else resolve(Object.assign({}, DEFAULTS))
        },
        fail: () => resolve(Object.assign({}, DEFAULTS))
      })
    } catch (e) {
      resolve(Object.assign({}, DEFAULTS))
    }
  })
}

export function saveConfig(cfg) {
  return new Promise((resolve) => {
    try {
      const storage = require('@system.storage')
      storage.set({ key: 'aiConfig', value: JSON.stringify(cfg), success: () => resolve(true), fail: () => resolve(false) })
    } catch (e) { resolve(false) }
  })
}

export { DEFAULTS }
