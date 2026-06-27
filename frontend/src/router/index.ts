import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '../layouts/AppLayout.vue'
import LoginView from '../views/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'
import ProductListView from '../views/ProductListView.vue'
import ProductEditView from '../views/ProductEditView.vue'
import ProfileView from '../views/ProfileView.vue'
import AdminDashboardView from '../views/AdminDashboardView.vue'
import AdminUserView from '../views/AdminUserView.vue'
import AdminProductView from '../views/AdminProductView.vue'
import AdminStatsView from '../views/AdminStatsView.vue'
import AdminRecycleBinView from '../views/AdminRecycleBinView.vue'
import RecycleBinView from '../views/RecycleBinView.vue'
import LibraryView from '../views/LibraryView.vue'
import GuideView from '../views/GuideView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView, meta: { public: true } },
    {
      path: '/',
      component: AppLayout,
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', component: DashboardView },
        { path: 'products', component: ProductListView },
        { path: 'products/:id/edit', component: ProductEditView },
        { path: 'library', component: LibraryView },
        { path: 'guide', component: GuideView },
        { path: 'recycle-bin', component: RecycleBinView },
        { path: 'profile', component: ProfileView },
        { path: 'admin/dashboard', component: AdminDashboardView, meta: { admin: true } },
        { path: 'admin/users', component: AdminUserView, meta: { admin: true } },
        { path: 'admin/products', component: AdminProductView, meta: { admin: true } },
        { path: 'admin/stats', component: AdminStatsView, meta: { admin: true } },
        { path: 'admin/recycle-bin', component: AdminRecycleBinView, meta: { admin: true } },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (!to.meta.public && !token) return '/login'
  if (to.meta.admin && localStorage.getItem('role') !== 'ADMIN') {
    const role = localStorage.getItem('role')
    if (role !== 'ADMIN') return '/dashboard'
    return '/login'
  }
  // 管理员登录后自动跳管理后台
  if (to.path === '/dashboard' && localStorage.getItem('role') === 'ADMIN') {
    return '/admin/dashboard'
  }
})

export default router
