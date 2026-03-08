import {createRouter, createWebHistory, RouteRecordRaw} from 'vue-router'
import {useUserStore} from '@/stores/user'

const routes: RouteRecordRaw[] = [
    {
        path: '/',
        redirect: '/login'
    },
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/views/LoginView.vue'),
        meta: {requiresAuth: false}
    },
    {
        path: '/feishu/callback',
        name: 'FeishuCallback',
        component: () => import('@/views/FeishuCallbackView.vue'),
        meta: {requiresAuth: false}
    },
    {
        path: '/dashboard',
        name: 'Dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: {requiresAuth: true}
    },
    {
        path: '/upload',
        name: 'Upload',
        component: () => import('@/views/UploadView.vue'),
        meta: {requiresAuth: true}
    },
{
  path: '/devices',
  name: 'Devices',
  component: () => import('@/views/DeviceListView.vue'),
  meta: { requiresAuth: true }
},
{
  path: '/audit',
  name: 'Audit',
  component: () => import('@/views/AuditListView.vue'),
  meta: { requiresAuth: true }
},
{
  path: '/push-image',
  name: 'PushImage',
  component: () => import('@/views/PushImageView.vue'),
  meta: { requiresAuth: true }
}
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach((to, _from, next) => {
    const userStore = useUserStore()
    userStore.init()

    if (to.meta.requiresAuth && !userStore.isAuthenticated) {
        next('/login')
    } else if (!to.meta.requiresAuth && userStore.isAuthenticated && to.path === '/login') {
        next('/dashboard')
    } else {
        next()
    }
})

export default router