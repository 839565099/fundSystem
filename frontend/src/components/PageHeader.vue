<template>
  <div class="page-header">
    <div class="page-header__left">
      <span v-if="icon" class="page-header__icon">{{ icon }}</span>
      <h1 class="page-header__title">{{ title }}</h1>
    </div>
    <div v-if="$slots.actions" class="page-header__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  icon?: string
}>()
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-header__left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-header__icon {
  font-size: 24px;
}

.page-header__title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-color);
  margin: 0;
  line-height: 1.3;
}

.page-header__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
