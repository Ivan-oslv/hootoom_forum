<script setup lang="ts">
definePageMeta({ layout: 'auth' })
usePageSeo('登录｜HOOTOOM Forum', '登录 HOOTOOM Forum。', '/login', false)
const auth = useAuth()
const form = reactive({ identifier: '', password: '' })
const pending = ref(false)
const errorMessage = ref('')

async function submit() {
  pending.value = true
  errorMessage.value = ''
  try {
    await auth.login({ ...form, deviceName: navigator.userAgent.slice(0, 100) })
    await navigateTo('/user/profile')
  }
  catch (error) {
    errorMessage.value = getErrorMessage(error)
  }
  finally {
    pending.value = false
  }
}
</script>

<template>
  <AuthShell
    title="欢迎回来"
    description="继续参与社区讨论，追踪官方动态，并管理你的内容与收藏。"
  >
    <h2>登录账号</h2>
    <p class="subtitle">
      支持用户名或邮箱登录
    </p>
    <form
      class="form-stack"
      @submit.prevent="submit"
    >
      <FormAlert
        v-if="errorMessage"
        :message="errorMessage"
      />
      <BaseField
        v-model="form.identifier"
        label="用户名或邮箱"
        name="identifier"
        autocomplete="username"
        required
      />
      <BaseField
        v-model="form.password"
        label="密码"
        name="password"
        type="password"
        autocomplete="current-password"
        required
      />
      <button
        class="primary-button"
        type="submit"
        :disabled="pending"
      >
        {{ pending ? '正在登录…' : '登录' }}
      </button>
    </form>
    <div class="form-links">
      <NuxtLink to="/forgot-password">忘记密码？</NuxtLink><NuxtLink to="/register">创建账号</NuxtLink>
    </div>
  </AuthShell>
</template>
