<script setup lang="ts">
defineProps<{ title: string, description: string }>()
</script>

<template>
  <main class="auth-page">
    <section
      class="auth-copy"
      aria-labelledby="auth-title"
    >
      <p class="eyebrow">
        HOOTOOM COMMUNITY
      </p>
      <h1 id="auth-title">
        {{ title }}
      </h1>
      <p>{{ description }}</p>
      <ul aria-label="社区特点">
        <li>连接跨境增长实践者</li>
        <li>沉淀广告投放与品牌出海经验</li>
        <li>获取官方公告与平台政策更新</li>
      </ul>
    </section>
    <section class="auth-card">
      <slot />
    </section>
  </main>
</template>
