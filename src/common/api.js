// 调用 OpenAI 兼容接口
import fetch from '@system.fetch'

export function askAI(cfg, messages) {
  return new Promise((resolve, reject) => {
    const body = JSON.stringify({
      model: cfg.model,
      messages: [{ role: 'system', content: cfg.systemPrompt }].concat(messages),
      temperature: 0.7,
      max_tokens: 300,
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
          try { data = JSON.parse(data) } catch (e) { return reject('返回不是 JSON：' + String(data).slice(0, 80)) }
        }
        if (data && data.error) return reject(data.error.message || JSON.stringify(data.error))
        const txt = data && data.choices && data.choices[0] && data.choices[0].message && data.choices[0].message.content
        if (!txt) return reject('响应格式异常：' + JSON.stringify(data).slice(0, 100))
        resolve(txt.trim())
      },
      fail: (err, code) => reject('网络错误 ' + code + '：' + (err && err.data ? err.data : ''))
    })
  })
}
