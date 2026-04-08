<template>
  <div class="page-header">
    <div class="page-header__left">
      <div class="page-header__indicator"></div>
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
defineProps<{ title: string; icon?: string }>()
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
  gap: 14px;
}

.page-header__indicator {
  margin-top: 6px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--primary-color);
  box-shadow: 0 0 0 4px rgba(201, 169, 110, 0.12), var(--shadow-gold-sm);
  flex-shrink: 0;
}

.page-header__text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-header__kicker {
  margin: 0;
  color: var(--text-tertiary);
  letter-spacing: 0.14em;
  text-transform: uppercase;
  font-size: 10px;
  font-weight: 600;
}

.page-header__title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-color);
  margin: 0;
  line-height: 1.3;
  letter-spacing: -0.02em;
}

.page-header__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
