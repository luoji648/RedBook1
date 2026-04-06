import { nextTick } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import MainLayout from '../layouts/MainLayout.vue'
import NoteDetailView from '../views/NoteDetailView.vue'
import ProductDetailView from '../views/ProductDetailView.vue'
import {
  captureDiscoverRecommendScroll,
  applyDiscoverRecommendScrollDeferred,
  getDiscoverRecommendScroll,
  isDiscoverRecommendRoute,
  isDiscoverRecommendTabActive,
} from '../utils/discoverRecommendScroll'
import {
  DiscoverView,
  MarketView,
  PublishView,
  ChatListView,
  GroupEditView,
  GroupChatView,
  NoticeCategoryView,
  ChatThreadView,
  ProfileView,
  DailySignView,
  FollowListView,
  UserProfileView,
  SettingsView,
  CartView,
  OrdersView,
  OrderDetailView,
  CouponsView,
  WalletView,
  ProductFootprintView,
  ProductSaveView,
  ProductManageView,
} from './mainLayoutComponents'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/',
    component: MainLayout,
    children: [
      {
        path: '',
        name: 'discover',
        component: DiscoverView,
        meta: { title: '发现' },
      },
      {
        path: 'follow',
        redirect: { name: 'discover', query: { tab: 'follow' } },
      },
      {
        path: 'market',
        name: 'market',
        component: MarketView,
        meta: { title: '集市' },
      },
      {
        path: 'publish',
        name: 'publish',
        component: PublishView,
        meta: { title: '发布', requiresAuth: true },
      },
      {
        path: 'chat',
        name: 'chat',
        component: ChatListView,
        meta: { title: '消息', requiresAuth: true },
      },
      {
        path: 'group/:groupId/edit',
        name: 'group-edit',
        component: GroupEditView,
        meta: { title: '编辑群资料', requiresAuth: true },
      },
      {
        path: 'group/:groupId',
        name: 'group-chat',
        component: GroupChatView,
        meta: { title: '群聊', requiresAuth: true },
      },
      {
        path: 'chat/notices/:slug',
        name: 'notice-category',
        component: NoticeCategoryView,
        meta: { title: '通知', requiresAuth: true },
      },
      {
        path: 'chat/:threadId',
        name: 'chat-thread',
        component: ChatThreadView,
        meta: { title: '私信', requiresAuth: true },
      },
      {
        path: 'me',
        name: 'me',
        component: ProfileView,
        meta: { title: '我', requiresAuth: true },
      },
      {
        path: 'me/sign',
        name: 'daily-sign',
        component: DailySignView,
        meta: { title: '每日签到', requiresAuth: true },
      },
      {
        path: 'user/:userId/following',
        name: 'user-following',
        component: FollowListView,
        meta: { title: '关注' },
      },
      {
        path: 'user/:userId/followers',
        name: 'user-followers',
        component: FollowListView,
        meta: { title: '粉丝' },
      },
      {
        path: 'user/:userId',
        name: 'user-profile',
        component: UserProfileView,
        meta: { title: '用户主页' },
      },
      {
        path: 'settings',
        name: 'settings',
        component: SettingsView,
        meta: { title: '账号设置', requiresAuth: true },
      },
      {
        path: 'cart',
        name: 'cart',
        component: CartView,
        meta: { title: '购物车', requiresAuth: true },
      },
      {
        path: 'orders',
        name: 'orders',
        component: OrdersView,
        meta: { title: '我的订单', requiresAuth: true },
      },
      {
        path: 'orders/detail/:orderId',
        name: 'order-detail',
        component: OrderDetailView,
        meta: { title: '订单详情', requiresAuth: true },
      },
      {
        path: 'market/coupons',
        name: 'market-coupons',
        component: CouponsView,
        meta: { title: '优惠券', requiresAuth: true },
      },
      {
        path: 'market/wallet',
        name: 'market-wallet',
        component: WalletView,
        meta: { title: '我的钱包', requiresAuth: true },
      },
      {
        path: 'market/footprint',
        name: 'product-footprint',
        component: ProductFootprintView,
        meta: { title: '商品足迹' },
      },
      {
        path: 'market/product/save',
        name: 'product-save',
        component: ProductSaveView,
        meta: { title: '发布商品', requiresAuth: true },
      },
      {
        path: 'market/product/manage',
        name: 'product-manage',
        component: ProductManageView,
        meta: { title: '我的商品', requiresAuth: true },
      },
    ],
  },
  {
    path: '/note/:id',
    name: 'note-detail',
    component: NoteDetailView,
    meta: { title: '笔记' },
  },
  {
    path: '/product/:id',
    name: 'product-detail',
    component: ProductDetailView,
    meta: { title: '商品' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to) {
    // 有记忆位置时不在此滚动：此时 keep-alive 子页往往尚未撑开高度，scrollTo 会被钳到顶部
    if (isDiscoverRecommendRoute(to) && getDiscoverRecommendScroll() > 0) {
      return false
    }
    return { top: 0, left: 0 }
  },
})

const noticeSlugTitles = {
  'like-collect': '赞和收藏',
  follow: '新增关注',
  comment: '评论转发',
}

router.beforeEach((to, from, next) => {
  if (from.name === 'discover' && isDiscoverRecommendTabActive()) {
    captureDiscoverRecommendScroll()
  }
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

router.afterEach((to) => {
  if (!isDiscoverRecommendRoute(to) || getDiscoverRecommendScroll() <= 0) return
  nextTick(() => {
    applyDiscoverRecommendScrollDeferred()
  })
})

export default router
