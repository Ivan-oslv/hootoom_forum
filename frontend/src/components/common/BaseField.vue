<script setup lang="ts">
defineProps<{
  modelValue: string
  label: string
  name: string
  type?: string
  autocomplete?: string
  required?: boolean
  disabled?: boolean
  hint?: string
}>()
defineEmits<{ 'update:modelValue': [value: string] }>()
</script>

<template>
  <label
    class="field"
    :for="name"
  >
    <span class="field-label">{{ label }}</span>
    <input
      :id="name"
      :name="name"
      :type="type || 'text'"
      :value="modelValue"
      :autocomplete="autocomplete"
      :required="required"
      :disabled="disabled"
      :aria-describedby="hint ? `${name}-hint` : undefined"
      @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
    >
    <small
      v-if="hint"
      :id="`${name}-hint`"
    >{{ hint }}</small>
  </label>
</template>
