/** 发现页「推荐」流滚动位置（切换底部 Tab 或其它路由时记忆） */
let savedRecommendScrollY = 0
/** 与 DiscoverView 同步：仅在推荐子 Tab 时离开发现页需要记录滚动 */
let recommendTabActive = true

export function setDiscoverRecommendTabActive(active) {
  recommendTabActive = !!active
}

export function isDiscoverRecommendTabActive() {
  return recommendTabActive
}

function readScrollTop() {
  const root = document.scrollingElement || document.documentElement
  return Math.max(0, window.scrollY ?? root.scrollTop ?? 0)
}

function maxDocumentScrollY() {
  const root = document.scrollingElement || document.documentElement
  return Math.max(0, root.scrollHeight - root.clientHeight)
}

export function captureDiscoverRecommendScroll() {
  savedRecommendScrollY = readScrollTop()
}

export function getDiscoverRecommendScroll() {
  return savedRecommendScrollY
}

export function applyDiscoverRecommendScroll() {
  const y = savedRecommendScrollY
  if (y <= 0) return
  const root = document.scrollingElement || document.documentElement
  const top = Math.min(y, maxDocumentScrollY())
  root.scrollTop = top
  window.scrollTo({ top, left: 0, behavior: 'auto' })
}

/**
 * 路由切回发现页时，DOM/keep-alive 可能尚未撑开高度，单次 scrollTo 会被钳到 0。
 * 在后续帧重试直到可滚高度足够或达到次数上限。
 */
export function applyDiscoverRecommendScrollDeferred(maxAttempts = 32) {
  const y = savedRecommendScrollY
  if (y <= 0) return
  let attempts = 0
  const tick = () => {
    attempts += 1
    const maxY = maxDocumentScrollY()
    const top = Math.min(y, maxY)
    const root = document.scrollingElement || document.documentElement
    root.scrollTop = top
    window.scrollTo({ top, left: 0, behavior: 'auto' })
    if (maxY >= y - 2 || attempts >= maxAttempts) return
    requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)
}

export function isDiscoverRecommendRoute(route) {
  return (
    route.name === 'discover' && String(route.query.tab || '') !== 'follow'
  )
}
