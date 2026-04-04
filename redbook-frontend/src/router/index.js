import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    children: [
      {
        path: '',
        name: 'discover',
        component: () => import('../views/DiscoverView.vue'),
        meta: { title: '发现' },
      },
      {
        path: 'follow',
        name: 'follow',
        component: () => import('../views/FollowFeedView.vue'),
        meta: { title: '关注', requiresAuth: true },
      },
      {
        path: 'publish',
        name: 'publish',
        component: () => import('../views/PublishView.vue'),
        meta: { title: '发布', requiresAuth: true },
      },
      {
        path: 'chat',
        name: 'chat',
        component: () => import('../views/ChatListView.vue'),
        meta: { title: '消息', requiresAuth: true },
      },
      {
        path: 'chat/:threadId',
        name: 'chat-thread',
        component: () => import('../views/ChatThreadView.vue'),
        meta: { title: '私信', requiresAuth: true },
      },
      {
        path: 'me',
        name: 'me',
        component: () => import('../views/ProfileView.vue'),
        meta: { title: '我', requiresAuth: true },
      },
      {
        path: 'settings',
        name: 'settings',
        component: () => import('../views/SettingsView.vue'),
        meta: { title: '账号设置', requiresAuth: true },
      },
      {
        path: 'cart',
        name: 'cart',
        component: () => import('../views/CartView.vue'),
        meta: { title: '购物车', requiresAuth: true },
      },
      {
        path: 'orders',
        name: 'orders',
        component: () => import('../views/OrdersView.vue'),
        meta: { title: '我的订单', requiresAuth: true },
      },
    ],
  },
  {
    path: '/note/:id',
    name: 'note-detail',
    component: () => import('../views/NoteDetailView.vue'),
    meta: { title: '笔记' },
  },
  {
    path: '/product/:id',
    name: 'product-detail',
    component: () => import('../views/ProductDetailView.vue'),
    meta: { title: '商品' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach((to, _from, next) => {
  document.title = to.meta.title ? `${to.meta.title} · RedBook` : 'RedBook'
  if (to.meta.requiresAuth) {
    const auth = useAuthStore()
    if (!auth.token) {
      next({ name: 'login', query: { redirect: to.fullPath } })
      return
    }
  }
  next()
})

export default router
