/** 后端商品 status：1 上架，0 下架 */
export function isProductOnShelf(product) {
  if (product == null) return false
  return Number(product.status) === 1
}

export const PRODUCT_OFF_SHELF_MSG = '商品已下架'

/** 上架且库存为 0（或缺失视为 0）时视为售罄 */
export function isProductSoldOut(product) {
  if (product == null || !isProductOnShelf(product)) return false
  return Number(product.stock ?? 0) <= 0
}

export const PRODUCT_SOLD_OUT_MSG = '已售罄'
