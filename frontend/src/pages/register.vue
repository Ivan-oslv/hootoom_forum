<script setup lang="ts">
definePageMeta({ layout: 'auth' })
usePageSeo('注册｜HOOTOOM Forum', '注册 HOOTOOM Forum 账号。', '/register', false)
const config = useRuntimeConfig()
const auth = useAuth()
const form = reactive({ username: '', email: '', password: '', confirmPassword: '', policyAccepted: false })
const pending = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

async function submit() {
  errorMessage.value = ''
  if (form.password !== form.confirmPassword) {
    errorMessage.value = '两次输入的密码不一致'
    return
  }
  pending.value = true
  try {
    const result = await auth.register({
      username: form.username,
      email: form.email,
      password: form.password,
      policyVersion: config.public.policyVersion,
      policyAccepted: form.policyAccepted,
    })
    successMessage.value = result.message
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
    title="加入增长社区"
    description="创建账号后完成邮箱验证，即可参与讨论、收藏内容并分享实践经验。"
  >
    <h2>创建账号</h2>
    <p class="subtitle">
      用户名注册后不可直接修改
    </p>
    <form
      class="form-stack"
      @submit.prevent="submit"
    >
      <FormAlert
        v-if="errorMessage"
        :message="errorMessage"
      />
      <FormAlert
        v-if="successMessage"
        type="success"
        :message="successMessage"
      />
      <BaseField
        v-model="form.username"
        label="用户名"
        name="username"
        autocomplete="username"
        hint="3–32 位字母、数字或下划线"
        required
      />
      <BaseField
        v-model="form.email"
        label="邮箱"
        name="email"
        type="email"
        autocomplete="email"
        required
      />
      <BaseField
        v-model="form.password"
        label="密码"
        name="password"
        type="password"
        autocomplete="new-password"
        hint="10–72 位，至少包含字母和数字"
        required
      />
      <BaseField
        v-model="form.confirmPassword"
        label="确认密码"
        name="confirmPassword"
        type="password"
        autocomplete="new-password"
        required
      />
      <label class="checkbox-row"><input
        v-model="form.policyAccepted"
        type="checkbox"
        required
      ><span>我已阅读并同意用户协议与隐私政策</span></label>
      <button
        class="primary-button"
        type="submit"
        :disabled="pending || Boolean(successMessage)"
      >
        {{ pending ? '正在创建…' : '注册' }}
      </button>
    </form>
    <div class="form-links">
      <NuxtLink to="/login">已有账号，去登录</NuxtLink><NuxtLink to="/verify-email">重新发送验证邮件</NuxtLink>
    </div>
  </AuthShell>
</template>
