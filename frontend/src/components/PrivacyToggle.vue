<template>
  <n-button quaternary circle size="small" @click="toggle" :title="isHidden ? '显示数据' : '隐藏数据'">
    <template #icon>
      <n-icon size="18">
        <IconEyeOff v-if="isHidden" />
        <IconEye v-else />
      </n-icon>
    </template>
  </n-button>
</template>

<script setup lang="ts">
import { NButton, NIcon } from 'naive-ui'
import { IconEye, IconEyeOff } from '@tabler/icons-vue'
import { usePrivacy } from '../composables/usePrivacy'

const { isHidden, toggle } = usePrivacy()
</script>
