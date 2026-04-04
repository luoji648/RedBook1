import { apiBase } from '../api/http'

/**
 * 使用 fetch 读取 SSE（可携带 Authorization），逐帧解析 data: 行。
 */
export async function streamNoteAi({ noteId, mode, question, token, onAnswer, signal }) {
  const base = apiBase || ''
  const q = new URLSearchParams({ mode })
  if (question && question.trim()) {
    q.set('question', question.trim())
  }
  const url = `${base}/ai/note/${noteId}/stream?${q.toString()}`
  const res = await fetch(url, {
    headers: {
      authorization: `Bearer ${token}`,
    },
    signal,
  })
  if (!res.ok) {
    const t = await res.text().catch(() => '')
    throw new Error(t || `AI 请求失败 (${res.status})`)
  }
  const reader = res.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    let idx
    while ((idx = buffer.indexOf('\n')) >= 0) {
      const line = buffer.slice(0, idx).trimEnd()
      buffer = buffer.slice(idx + 1)
      if (line.startsWith('data:')) {
        const raw = line.slice(5).trim()
        if (!raw || raw === '[DONE]') continue
        try {
          const obj = JSON.parse(raw)
          const piece = obj.answer || obj.text
          if (piece) onAnswer(piece)
        } catch {
          /* 非 JSON 行忽略 */
        }
      }
    }
  }
}
