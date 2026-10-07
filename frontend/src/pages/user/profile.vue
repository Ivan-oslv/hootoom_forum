<script setup lang="ts">
import { useUserApi } from '~/api/user'

definePageMeta({ middleware: 'auth' })
usePageSeo('个人资料｜HOOTOOM Forum', '管理你的 HOOTOOM Forum 个人资料。', '/user/profile', false)
const api = useUserApi()
const profile = ref(await api.getMe())
const form = reactive({ nickname: profile.value.nickname, bio: profile.value.bio || '', avatarKey: profile.value.avatarKey })
const avatars = ['default-1', 'default-2', 'default-3', 'default-4', 'default-5', 'default-6', 'default-7', 'default-8']
const pending = ref(false)
const message = ref('')
const errorMessage = ref('')
async function submit() {
  pending.value = true
  message.value = ''
  errorMessage.value = ''
  try {
    profile.value = await api.updateMe({ ...form, bio: form.bio || null, version: profile.value.version })
    message.value = '个人资料已更新'
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
  <main
    class="home"
    style="padding-top: 48px"
  >
    <p class="eyebrow">
      USER CENTER
    </p><h1 style="font-size: clamp(2.4rem, 6vw, 4.5rem)">
      个人资料
    </h1>
    <section
      class="auth-card"
      style="max-width: 700px"
    >
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
          :model-value="profile.username"
          label="用户名"
          name="username"
          disabled
        />
        <BaseField
          :model-value="profile.email"
          label="邮箱"
          name="email"
          type="email"
          disabled
        />
        <BaseField
          v-model="form.nickname"
          label="昵称"
          name="nickname"
          required
        />
        <label
          class="field"
          for="bio"
        ><span class="field-label">个人简介</span><textarea
          id="bio"
          v-model="form.bio"
          rows="5"
          maxlength="500"
        /></label>
        <fieldset class="avatar-grid">
          <legend>系统头像</legend><label
            v-for="avatar in avatars"
            :key="avatar"
          ><input
            v-model="form.avatarKey"
            type="radio"
            name="avatarKey"
            :value="avatar"
          ><span>{{ avatar }}</span></label>
        </fieldset>
        <button
          class="primary-button"
          type="submit"
          :disabled="pending"
        >
          {{ pending ? '正在保存…' : '保存修改' }}
        </button>
      </form>
    </section>
  </main>
</template>
