import { get, post, put, del, postForm, apiBase } from './http'

export { apiBase }

export function sendCode(phone) {
  return postForm('/user/code', { phone })
}

export function login(body) {
  return post('/user/login', body)
}

export function logout() {
  return post('/user/logout')
}

export function fetchMe() {
  return get('/user/me')
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

export function commentTree(noteId) {
  return get(`/note/comment/tree/${noteId}`)
}

export function commentAdd(noteId, body) {
  return post(`/note/comment/${noteId}`, body)
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

export function collectNote(noteId) {
  return post(`/collect/${noteId}`)
}

export function uncollectNote(noteId) {
  return del(`/collect/${noteId}`)
}

export function collectMy(params) {
  return get('/collect/my', params)
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

export function shareNote(noteId) {
  return post(`/share/note/${noteId}`)
}

export function ossPresign(ext, contentType) {
  const params = {}
  if (ext) params.ext = ext
  if (contentType) params.contentType = contentType
  return postForm('/oss/presign', params)
}

export function productList(params) {
  return get('/product/list', params)
}

export function productGet(id) {
  return get(`/product/${id}`)
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

export function orderCreate() {
  return post('/order/create')
}

export function orderMy(params) {
  return get('/order/my', params)
}

export function chatThreads(params) {
  return get('/chat/threads', params)
}

export function chatMessages(threadId, params) {
  return get(`/chat/${threadId}/messages`, params)
}

export function chatSend(toUserId, content) {
  return postForm('/chat/send', { toUserId, content })
}
