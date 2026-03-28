<template>
  <div class="page-header">
    <div class="page-header__left">
      <span v-if="icon" class="page-header__icon" aria-hidden="true"></span>
      <div class="page-header__text">
        <p class="page-header__kicker">Fund Terminal</p>
        <h1 class="page-header__title">{{ title }}</h1>
      </div>
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
  align-items: flex-start;
  gap: 12px;
}

.page-header__icon {
  margin-top: 6px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--info-color);
  box-shadow: 0 0 0 6px rgba(201, 168, 76, 0.14);
}

.page-header__text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-header__kicker {
  margin: 0;
  color: var(--text-tertiary);
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-size: 11px;
  font-weight: 600;
}

.page-header__title {
  font-size: 24px;
  font-weight: 650;
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
