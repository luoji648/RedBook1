const KEY = 'redbook_product_footprints'
const MAX = 50

export function recordProductFootprint(product) {
  if (!product?.id) return
  let list = []
  try {
    list = JSON.parse(localStorage.getItem(KEY) || '[]')
  } catch {
    list = []
  }
  const item = {
    id: product.id,
    title: product.title,
    cover: product.cover,
    at: Date.now(),
  }
  list = [item, ...list.filter((x) => x.id !== item.id)].slice(0, MAX)
  localStorage.setItem(KEY, JSON.stringify(list))
}

export function getProductFootprints() {
  try {
    return JSON.parse(localStorage.getItem(KEY) || '[]')
  } catch {
    return []
  }
}
