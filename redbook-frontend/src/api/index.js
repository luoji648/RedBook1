import { get, post, put, del, postForm, postMultipart, apiBase } from './http'

export { apiBase }

export function sendCode(phone) {
  return postForm('/user/code', { phone })
}

export function login(body) {
  return post('/user/login', body)
}

export function loginByPassword(body) {
  return post('/user/login/password', body)
}

export function logout() {
  return post('/user/logout')
}

export function fetchMe() {
  return get('/user/me')
}

export function userPublic(userId) {
  return get(`/user/public/${userId}`)
}

export function fetchUserInfo() {
  return get('/user/info')
}

export function updateProfile(body) {
  return put('/user/profile', body)
}

export function updateUserInfo(body) {
  return put('/user/info', body)
}

export function changePassword(body) {
  return put('/user/password', body)
}

export function signIn() {
  return post('/user/sign')
}

export function signCount() {
  return get('/user/sign/count')
}

export function saveNote(body) {
  return post('/note/save', body)
}

export function publishNote(id) {
  return postForm(`/note/publish/${id}`)
}

export function deleteNote(id) {
  return del(`/note/${id}`)
}

export function noteDetail(id) {
  return get(`/note/detail/${id}`)
}

export function noteMy(params) {
  return get('/note/my', params)
}

export function noteFeed(params) {
  return get('/note/feed', params)
}

export function noteRecommend(params) {
  return get('/note/recommend', params)
}

export function noteRelated(id) {
  return get(`/note/related/${id}`)
}

export function noteByUser(userId, params) {
  return get(`/note/user/${userId}`, params)
}

export function commentTree(noteId) {
  return get(`/note/comment/tree/${noteId}`, { _t: Date.now() })
}

export function commentAdd(noteId, body) {
  return post(`/note/comment/${noteId}`, body)
}

export function commentDelete(commentId) {
  return del(`/note/comment/${commentId}`)
}

export function likeNote(noteId) {
  return post(`/like/${noteId}`)
}

export function unlikeNote(noteId) {
  return del(`/like/${noteId}`)
}

export function likeMy(params) {
  return get('/like/my', params)
}

export function likeByUser(userId, params) {
  return get(`/like/user/${userId}`, params)
}

export function collectNote(noteId) {
  return post(`/collect/${noteId}`)
}

export function uncollectNote(noteId) {
  return del(`/collect/${noteId}`)
}

export function collectMy(params) {
  return get('/collect/my', params)
}

export function collectByUser(userId, params) {
  return get(`/collect/user/${userId}`, params)
}

export function followUser(userId) {
  return post(`/follow/${userId}`)
}

export function unfollowUser(userId) {
  return del(`/follow/${userId}`)
}

export function followFollowing(params) {
  return get('/follow/following', params)
}

export function followFollowers(params) {
  return get('/follow/followers', params)
}

/** 指定用户的关注列表（公开接口） */
export function followFollowingOfUser(userId, params) {
  return get(`/follow/public/${userId}/following`, params)
}

/** 指定用户的粉丝列表（公开接口） */
export function followFollowersOfUser(userId, params) {
  return get(`/follow/public/${userId}/followers`, params)
}

export function shareNote(noteId) {
  return post(`/share/note/${noteId}`)
}

export function ossPresign(ext, contentType) {
  const params = {}
  if (ext) params.ext = ext
  if (contentType) params.contentType = contentType
  return postForm('/oss/presign', params)
}

/** 经后端写入 OSS，避免外网环境下 OSS 未配置 CORS 时浏览器直连 PUT 失败；返回结构与预签名成功后的 data 一致（无 uploadUrl） */
export function ossUploadViaApi(file) {
  const fd = new FormData()
  fd.append('file', file)
  return postMultipart('/oss/upload', fd)
}

export function productList(params) {
  return get('/product/list', params)
}

/** 当前卖家自己的商品分页（需登录） */
export function productMy(params) {
  return get('/product/my', params)
}

export function productGet(id) {
  return get(`/product/${id}`)
}

/** 新建或更新商品（需登录），body 见 ProductSaveDTO */
export function productSave(body) {
  return post('/product/save', body)
}

/** 调整已上架商品库存（需登录），body: { stock } */
export function productUpdateStock(productId, body) {
  return put(`/product/${productId}/stock`, body)
}

export function cartAdd(productId, quantity) {
  return postForm('/cart/add', { productId, quantity: quantity ?? 1 })
}

export function cartMy() {
  return get('/cart/my')
}

export function cartUpdate(cartId, quantity) {
  return put(`/cart/${cartId}`, {}, { params: { quantity } })
}

export function cartRemove(cartId) {
  return del(`/cart/${cartId}`)
}

/** body 可选 { userCouponId } */
export function orderCreate(body) {
  return post('/order/create', body ?? {})
}

/** 立即支付页预览：关注提示、可领券、实付试算；fallbackSellerUserId=笔记作者（商品无 seller_id 时） */
export function orderDirectPreview(productId, quantity = 1, fallbackSellerUserId) {
  const params = { productId, quantity }
  if (fallbackSellerUserId != null && fallbackSellerUserId !== '') {
    params.fallbackSellerUserId = fallbackSellerUserId
  }
  return get('/order/direct-preview', params)
}

/** body: { productId, quantity, userCouponId? } */
export function orderCreateDirect(body) {
  return post('/order/create-direct', body)
}

export function orderMy(params) {
  return get('/order/my', params)
}

export function orderDetail(orderId) {
  return get(`/order/detail/${orderId}`)
}

/** body: { payPassword }，模拟密码固定为 123456 */
export function orderPay(orderId, body) {
  return post(`/order/pay/${orderId}`, body ?? { payPassword: '123456' })
}

export function orderRefund(orderId) {
  return post(`/order/refund/${orderId}`)
}

export function orderClose(orderId) {
  return post(`/order/${orderId}/close`, {})
}

export function orderDeleteRecord(orderId) {
  return del(`/order/${orderId}`)
}

export function walletBalance() {
  return get('/wallet/balance')
}

export function walletRechargeAlipay(body) {
  return post('/wallet/recharge/alipay', body)
}

export function couponMy(params) {
  return get('/coupon/my', params)
}

/** body: { productId } — 需已关注卖家 */
export function couponClaimFollow(body) {
  return post('/coupon/claim-follow', body)
}

export function chatUnreadTotal() {
  return get('/chat/unread-total')
}

export function chatThreads(params) {
  return get('/chat/threads', params)
}

export function chatMarkThreadRead(threadId) {
  return post(`/chat/${threadId}/read`, {})
}

/** 获取或创建与对方的会话，返回 threadId */
export function chatEnsureThread(peerUserId) {
  return get('/chat/peer-thread', { peerUserId })
}

export function chatMessages(threadId, params) {
  return get(`/chat/${threadId}/messages`, params)
}

export function chatSend(toUserId, content) {
  return postForm('/chat/send', { toUserId, content })
}

/** 创建群聊 body: { name, joinMode, avatar? } joinMode: 0 无需验证 1 需群主验证；avatar 为可选 OSS URL */
export function groupCreate(body) {
  return post('/group/create', body)
}

/** 群主主页群列表；登录时带 inGroup */
export function groupByOwner(ownerUserId) {
  return get(`/group/by-owner/${ownerUserId}`)
}

export function groupMy() {
  return get('/group/my')
}

export function groupJoin(groupId) {
  return post(`/group/${groupId}/join`, {})
}

export function groupJoinRequestHandle(requestId, approve) {
  return postForm(`/group/join-request/${requestId}/handle`, { approve })
}

export function groupMeta(groupId) {
  return get(`/group/${groupId}/meta`)
}

export function groupMessages(groupId, params) {
  return get(`/group/${groupId}/messages`, params)
}

export function groupSend(groupId, content) {
  return postForm(`/group/${groupId}/send`, { content })
}

export function groupMarkRead(groupId) {
  return post(`/group/${groupId}/read`, {})
}

/** 群详情与成员列表（在群成员） */
export function groupDetail(groupId) {
  return get(`/group/${groupId}/detail`)
}

/** 群主移出成员 */
export function groupKick(groupId, targetUserId) {
  return postForm(`/group/${groupId}/kick`, { targetUserId })
}

/** 群主更新群名称、头像；avatar 传空字符串表示使用默认（群主头像） */
export function groupUpdateProfile(groupId, body) {
  return put(`/group/${groupId}/profile`, body)
}

export function noticeInteractions(params) {
  return get('/notice/interactions', params)
}

/** category: like_collect | follow | comment */
export function noticeMarkRead(category) {
  return postForm('/notice/read', { category })
}
