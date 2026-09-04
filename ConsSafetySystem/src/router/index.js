// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import store from '@/store'

import Login from '../views/Login.vue'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { requiresAuth: false }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login'
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

router.beforeEach((to, from, next) => {
  const token = store.state.token

  console.log('路由守卫检查:', { to: to.path, token: !!token })

  if (to.meta.requiresAuth && !token) {
    console.log('未登录，跳转到登录页')
    next('/login')
  } else if (to.path === '/login' && token) {
    console.log('已登录，跳转到首页')
    next('/home')
  } else if (to.path === '/' && !token) {
    console.log('访问根路径未登录，跳转到登录页')
    next('/login')
  } else if (to.path === '/' && token) {
    console.log('访问根路径已登录，跳转到首页')
    next('/home')
  } else {
    next()
  }
})

export default router