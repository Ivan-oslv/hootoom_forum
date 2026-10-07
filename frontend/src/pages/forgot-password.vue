<script setup lang="ts">
import { useAuthApi } from '~/api/auth'

definePageMeta({ layout: 'auth' })
usePageSeo('找回密码｜HOOTOOM Forum', '申请 HOOTOOM Forum 密码重置邮件。', '/forgot-password', false)
const email = ref('')
const pending = ref(false)
const message = ref('')
const errorMessage = ref('')
const api = useAuthApi()
async function submit() {
  pending.value = true
  errorMessage.value = ''
  try {
    message.value = (await api.requestPasswordReset(email.value)).message
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
    title="找回密码"
    description="输入注册邮箱。为保护账号隐私，无论邮箱是否存在，我们都会显示相同结果。"
  >
    <h2>重置密码</h2><p class="subtitle">
      重置链接将在 15 分钟后失效
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
        v-model="email"
        label="注册邮箱"
        name="email"
        type="email"
        autocomplete="email"
        required
      />
      <button
        class="primary-button"
        type="submit"
        :disabled="pending"
      >
        {{ pending ? '正在提交…' : '发送重置邮件' }}
      </button>
    </form>
    <div class="form-links">
      <NuxtLink to="/login">返回登录</NuxtLink>
    </div>
  </AuthShell>
</template>
