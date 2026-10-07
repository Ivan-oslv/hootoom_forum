<script setup lang="ts">
import { useAuthApi } from '~/api/auth'

definePageMeta({ layout: 'auth' })
usePageSeo('重置密码｜HOOTOOM Forum', '设置新的 HOOTOOM Forum 登录密码。', '/reset-password', false)
const route = useRoute()
const api = useAuthApi()
const token = computed(() => typeof route.query.token === 'string' ? route.query.token : '')
const form = reactive({ password: '', confirmPassword: '' })
const pending = ref(false)
const message = ref('')
const errorMessage = ref('')
async function submit() {
  errorMessage.value = ''
  if (!token.value) {
    errorMessage.value = '重置链接缺少令牌，请重新申请'
    return
  }
  if (form.password !== form.confirmPassword) {
    errorMessage.value = '两次输入的密码不一致'
    return
  }
  pending.value = true
  try {
    message.value = (await api.confirmPasswordReset(token.value, form.password)).message
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
    title="设置新密码"
    description="新密码生效后，所有旧设备会话都会退出，需要使用新密码重新登录。"
  >
    <h2>重置密码</h2><p class="subtitle">
      密码需要至少 10 位，并同时包含字母和数字
    </p>
    <form
      class="form-stack"
      @submit.prevent="submit"
    >
      <FormAlert
        v-if="errorMessage"
        :message="errorMessage"
      /><FormAlert
        v-if="message"
        type="success"
        :message="message"
      />
      <BaseField
        v-model="form.password"
        label="新密码"
        name="password"
        type="password"
        autocomplete="new-password"
        required
      />
      <BaseField
        v-model="form.confirmPassword"
        label="确认新密码"
        name="confirmPassword"
        type="password"
        autocomplete="new-password"
        required
      />
      <button
        class="primary-button"
        type="submit"
        :disabled="pending || Boolean(message)"
      >
        {{ pending ? '正在重置…' : '确认重置' }}
      </button>
    </form>
    <div class="form-links">
      <NuxtLink to="/forgot-password">重新申请链接</NuxtLink><NuxtLink to="/login">返回登录</NuxtLink>
    </div>
  </AuthShell>
</template>
