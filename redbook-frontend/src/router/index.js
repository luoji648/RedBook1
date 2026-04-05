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
        redirect: { name: 'discover', query: { tab: 'follow' } },
      },
      {
        path: 'market',
        name: 'market',
        component: () => import('../views/MarketView.vue'),
        meta: { title: '集市' },
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
        path: 'group/:groupId/edit',
        name: 'group-edit',
        component: () => import('../views/GroupEditView.vue'),
        meta: { title: '编辑群资料', requiresAuth: true },
      },
      {
        path: 'group/:groupId',
        name: 'group-chat',
        component: () => import('../views/GroupChatView.vue'),
        meta: { title: '群聊', requiresAuth: true },
      },
      {
        path: 'chat/notices/:slug',
        name: 'notice-category',
        component: () => import('../views/NoticeCategoryView.vue'),
        meta: { title: '通知', requiresAuth: true },
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
        path: 'me/sign',
        name: 'daily-sign',
        component: () => import('../views/DailySignView.vue'),
        meta: { title: '每日签到', requiresAuth: true },
      },
      {
        path: 'user/:userId/following',
        name: 'user-following',
        component: () => import('../views/FollowListView.vue'),
        meta: { title: '关注' },
      },
      {
        path: 'user/:userId/followers',
        name: 'user-followers',
        component: () => import('../views/FollowListView.vue'),
        meta: { title: '粉丝' },
      },
      {
        path: 'user/:userId',
        name: 'user-profile',
        component: () => import('../views/UserProfileView.vue'),
        meta: { title: '用户主页' },
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
      {
        path: 'orders/detail/:orderId',
        name: 'order-detail',
        component: () => import('../views/OrderDetailView.vue'),
        meta: { title: '订单详情', requiresAuth: true },
      },
      {
        path: 'market/coupons',
        name: 'market-coupons',
        component: () => import('../views/CouponsView.vue'),
        meta: { title: '优惠券', requiresAuth: true },
      },
      {
        path: 'market/wallet',
        name: 'market-wallet',
        component: () => import('../views/WalletView.vue'),
        meta: { title: '我的钱包', requiresAuth: true },
      },
      {
        path: 'market/footprint',
        name: 'product-footprint',
        component: () => import('../views/ProductFootprintView.vue'),
        meta: { title: '商品足迹' },
      },
      {
        path: 'market/product/save',
        name: 'product-save',
        component: () => import('../views/ProductSaveView.vue'),
        meta: { title: '发布商品', requiresAuth: true },
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

const noticeSlugTitles = {
  'like-collect': '赞和收藏',
  follow: '新增关注',
  comment: '评论和@',
}

router.beforeEach((to, _from, next) => {
  if (to.name === 'notice-category') {
    const sub = noticeSlugTitles[String(to.params.slug)] || '通知'
    document.title = `${sub} · RedBook`
  } else {
    document.title = to.meta.title ? `${to.meta.title} · RedBook` : 'RedBook'
  }
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
