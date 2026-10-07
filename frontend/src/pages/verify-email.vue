<script setup lang="ts">
import { useAuthApi } from '~/api/auth'

definePageMeta({ layout: 'auth' })
usePageSeo('验证邮箱｜HOOTOOM Forum', '验证 HOOTOOM Forum 注册邮箱。', '/verify-email', false)
const route = useRoute()
const api = useAuthApi()
const email = ref('')
const pending = ref(false)
const message = ref('')
const errorMessage = ref('')
const token = computed(() => typeof route.query.token === 'string' ? route.query.token : '')

async function confirm() {
  if (!token.value) return
  pending.value = true
  try {
    message.value = (await api.confirmEmail(token.value)).message
  }
  catch (error) {
    errorMessage.value = getErrorMessage(error)
  }
  finally {
    pending.value = false
  }
}
async function resend() {
  pending.value = true
  errorMessage.value = ''
  try {
    message.value = (await api.resendVerification(email.value)).message
  }
  catch (error) {
    errorMessage.value = getErrorMessage(error)
  }
  finally {
    pending.value = false
  }
}
onMounted(confirm)
</script>

<template>
  <AuthShell
    title="验证你的邮箱"
    description="邮箱验证能保护账号安全，也是参与社区互动前的必要步骤。"
  >
    <h2>{{ token ? '正在验证' : '重新发送邮件' }}</h2>
    <p class="subtitle">
      验证链接有效期为 30 分钟
    </p>
    <div class="form-stack">
      <FormAlert
        v-if="errorMessage"
        :message="errorMessage"
      />
      <FormAlert
        v-if="message"
        type="success"
        :message="message"
      />
      <p
        v-if="token && pending"
        aria-live="polite"
      >
        正在验证，请稍候…
      </p>
      <template v-if="!token">
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
          type="button"
          :disabled="pending"
          @click="resend"
        >
          发送验证邮件
        </button>
      </template>
    </div>
    <div class="form-links">
      <NuxtLink to="/login">验证完成后登录</NuxtLink>
    </div>
  </AuthShell>
</template>
