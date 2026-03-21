<template>
  <div class="skeleton-wrapper">
    <div v-if="type === 'card'" class="skeleton-card">
      <div class="skeleton skeleton--header">
        <div class="skeleton skeleton--avatar"></div>
        <div class="skeleton skeleton--title-group">
          <div class="skeleton skeleton--title"></div>
          <div class="skeleton skeleton--subtitle"></div>
        </div>
      </div>
      <div class="skeleton skeleton--content">
        <div class="skeleton skeleton--line"></div>
        <div class="skeleton skeleton--line skeleton--line--short"></div>
      </div>
    </div>
    
    <div v-else-if="type === 'stat'" class="skeleton-stat">
      <div class="skeleton skeleton--label"></div>
      <div class="skeleton skeleton--value"></div>
      <div class="skeleton skeleton--change"></div>
    </div>
    
    <div v-else-if="type === 'list'" class="skeleton-list">
      <div v-for="i in rows" :key="i" class="skeleton-list__item">
        <div class="skeleton skeleton--avatar skeleton--avatar--sm"></div>
        <div class="skeleton-list__content">
          <div class="skeleton skeleton--line skeleton--line--title"></div>
          <div class="skeleton skeleton--line skeleton--line--subtitle"></div>
        </div>
        <div class="skeleton skeleton--action"></div>
      </div>
    </div>
    
    <div v-else-if="type === 'table'" class="skeleton-table">
      <div class="skeleton-table__header">
        <div v-for="i in columns" :key="i" class="skeleton skeleton--th"></div>
      </div>
      <div v-for="row in rows" :key="row" class="skeleton-table__row">
        <div v-for="col in columns" :key="col" class="skeleton skeleton--td"></div>
      </div>
    </div>
    
    <div v-else-if="type === 'chart'" class="skeleton-chart">
      <div class="skeleton-chart__header">
        <div class="skeleton skeleton--title"></div>
        <div class="skeleton skeleton--controls"></div>
      </div>
      <div class="skeleton skeleton--chart-area"></div>
    </div>
    
    <div v-else-if="type === 'text'" class="skeleton-text">
      <div v-for="i in rows" :key="i" class="skeleton skeleton--line" :style="getLineStyle(i)"></div>
    </div>
    
    <slot v-else></slot>
  </div>
</template>

<script setup lang="ts">
interface Props {
  type?: 'card' | 'stat' | 'list' | 'table' | 'chart' | 'text'
  rows?: number
  columns?: number
}

withDefaults(defineProps<Props>(), {
  type: 'card',
  rows: 3,
  columns: 4
})

const getLineStyle = (index: number) => {
  if (index === 3) return { width: '60%' }
  if (index === 4) return { width: '40%' }
  return {}
}
</script>

<style scoped>
.skeleton-wrapper {
  width: 100%;
}

.skeleton {
  background: linear-gradient(
    90deg,
    var(--bg-tertiary) 25%,
    var(--bg-secondary) 50%,
    var(--bg-tertiary) 75%
  );
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s ease-in-out infinite;
  border-radius: var(--radius-md);
}

@keyframes skeleton-shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

.skeleton--header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  background: transparent;
  animation: none;
}

.skeleton--avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  flex-shrink: 0;
}

.skeleton--avatar--sm {
  width: 36px;
  height: 36px;
}

.skeleton--title-group {
  flex: 1;
  background: transparent;
  animation: none;
}

.skeleton--title {
  height: 18px;
  width: 60%;
  margin-bottom: 8px;
}

.skeleton--subtitle {
  height: 14px;
  width: 40%;
}

.skeleton--content {
  background: transparent;
  animation: none;
}

.skeleton--line {
  height: 14px;
  margin-bottom: 8px;
}

.skeleton--line:last-child {
  margin-bottom: 0;
}

.skeleton--line--short {
  width: 60%;
}

.skeleton--line--title {
  height: 16px;
  margin-bottom: 6px;
}

.skeleton--line--subtitle {
  height: 12px;
  width: 70%;
}

.skeleton-card {
  background: var(--card-bg);
  border-radius: var(--radius-xl);
  padding: 20px;
  border: 1px solid var(--border-color);
}

.skeleton-stat {
  background: var(--card-bg);
  border-radius: var(--radius-xl);
  padding: 24px;
  border: 1px solid var(--border-color);
}

.skeleton--label {
  height: 14px;
  width: 80px;
  margin-bottom: 12px;
}

.skeleton--value {
  height: 32px;
  width: 120px;
  margin-bottom: 12px;
}

.skeleton--change {
  height: 24px;
  width: 80px;
  border-radius: var(--radius-full);
}

.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton-list__item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
}

.skeleton-list__content {
  flex: 1;
  min-width: 0;
}

.skeleton--action {
  width: 60px;
  height: 28px;
  border-radius: var(--radius-md);
  flex-shrink: 0;
}

.skeleton-table {
  background: var(--card-bg);
  border-radius: var(--radius-xl);
  overflow: hidden;
  border: 1px solid var(--border-color);
}

.skeleton-table__header {
  display: flex;
  gap: 16px;
  padding: 12px 16px;
  background: var(--bg-secondary);
}

.skeleton--th {
  flex: 1;
  height: 14px;
}

.skeleton-table__row {
  display: flex;
  gap: 16px;
  padding: 14px 16px;
  border-top: 1px solid var(--border-color);
}

.skeleton--td {
  flex: 1;
  height: 14px;
}

.skeleton-chart {
  background: var(--card-bg);
  border-radius: var(--radius-xl);
  padding: 20px;
  border: 1px solid var(--border-color);
}

.skeleton-chart__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.skeleton--controls {
  width: 200px;
  height: 32px;
}

.skeleton--chart-area {
  height: 280px;
  border-radius: var(--radius-lg);
}

.skeleton-text {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
