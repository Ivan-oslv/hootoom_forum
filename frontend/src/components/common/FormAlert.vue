<script setup lang="ts">
defineProps<{ type?: 'error' | 'success', message: string }>()
</script>

<template>
  <p
    class="form-alert"
    :class="`form-alert--${type || 'error'}`"
    role="alert"
  >
    {{ message }}
  </p>
</template>
