import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '../layouts/AppLayout.vue'
import LoginView from '../views/LoginView.vue'
import ProductListView from '../views/ProductListView.vue'
import ProductEditView from '../views/ProductEditView.vue'
import AdminProductView from '../views/AdminProductView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView, meta: { public: true } },
    {
      path: '/',
      component: AppLayout,
      children: [
        { path: '', redirect: '/products' },
        { path: 'products', component: ProductListView },
        { path: 'products/:id/edit', component: ProductEditView },
        { path: 'admin/products', component: AdminProductView, meta: { admin: true } },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (!to.meta.public && !token) return '/login'
  if (to.meta.admin && localStorage.getItem('role') !== 'ADMIN') return '/products'
})

export default router
