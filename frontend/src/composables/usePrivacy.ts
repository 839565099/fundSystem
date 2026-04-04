import { ref, watch } from 'vue'

const hidden = ref(localStorage.getItem('privacy-mode') === 'true')

watch(hidden, (val) => {
  localStorage.setItem('privacy-mode', val ? 'true' : 'false')
})

export function usePrivacy() {
  const toggle = () => {
    hidden.value = !hidden.value
  }

  const mask = (value: string | number | undefined | null): string => {
    if (value === undefined || value === null) return '--'
    if (!hidden.value) return String(value)
    return '****'
  }

  const maskMoney = (value: number | undefined | null): string => {
    if (value === undefined || value === null) return hidden.value ? '****' : '¥0.00'
    if (hidden.value) return '****'
    return '¥' + value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  }

  const maskMoneyShort = (value: number | undefined | null): string => {
    if (value === undefined || value === null) return hidden.value ? '****' : '-'
    if (hidden.value) return '****'
    if (value >= 10000) return `¥${(value / 10000).toFixed(2)}万`
    return `¥${value.toFixed(2)}`
  }

  const maskPercent = (value: number | undefined | null, sign = true): string => {
    if (value === undefined || value === null) return hidden.value ? '****' : '--'
    if (hidden.value) return '****'
    const prefix = sign && value >= 0 ? '+' : ''
    return `${prefix}${value.toFixed(2)}%`
  }

  return {
    isHidden: hidden,
    toggle,
    mask,
    maskMoney,
    maskMoneyShort,
    maskPercent,
  }
}
