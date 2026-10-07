<script setup lang="ts">
const auth = useAuth()
</script>

<template>
  <header class="site-header">
    <NuxtLink
      class="brand"
      to="/"
      aria-label="HOOTOOM Forum 首页"
    >
      <span
        class="brand-mark"
        aria-hidden="true"
      >H</span>
      <span>HOOTOOM Forum</span>
    </NuxtLink>
    <nav aria-label="主导航">
      <NuxtLink to="/">首页</NuxtLink>
      <NuxtLink
        v-if="auth.store.isAuthenticated"
        to="/user/profile"
      >个人中心</NuxtLink>
      <button
        v-if="auth.store.isAuthenticated"
        class="link-button"
        type="button"
        @click="auth.logout"
      >
        退出
      </button>
      <template v-else>
        <NuxtLink to="/login">登录</NuxtLink>
        <NuxtLink
          class="nav-cta"
          to="/register"
        >注册</NuxtLink>
      </template>
    </nav>
  </header>
</template>
