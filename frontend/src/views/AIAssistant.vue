<template>
  <div class="ai-assistant-page page-container">
    <div class="chat-layout">
      <!-- 侧边栏 -->
      <div class="sidebar">
        <div class="sidebar-header">
          <n-button type="primary" block @click="createNewSession">
            <template #icon>
              <n-icon><IconPlus /></n-icon>
            </template>
            新建对话
          </n-button>
        </div>
        <div class="session-list">
          <div
            v-for="session in sessions"
            :key="session.sessionId"
            class="session-item"
            :class="{ active: currentSessionId === session.sessionId }"
            @click="switchSession(session.sessionId)"
          >
            <n-icon><IconMessages /></n-icon>
            <span class="session-title">{{ session.title || '新对话' }}</span>
            <n-button text size="small" @click.stop="deleteSession(session.sessionId)">
              <n-icon><IconTrash /></n-icon>
            </n-button>
          </div>
        </div>
      </div>

      <!-- 主聊天区域 -->
      <div class="chat-main">
        <div class="chat-header">
          <h2>AI 投资助手</h2>
          <div class="model-selector">
            <n-select
              v-model:value="currentModel"
              :options="modelOptions"
              style="width: 150px"
            />
          </div>
        </div>

        <div class="chat-messages" ref="messagesContainer">
          <div v-if="messages.length === 0" class="welcome-section">
            <div class="welcome-icon">
              <n-icon size="64"><IconSparkles /></n-icon>
            </div>
            <h2>您好！我是您的 AI 投资助手</h2>
            <p>我可以帮助您分析基金、解读市场、提供投资建议。</p>
            <div class="quick-actions">
              <n-button
                v-for="action in quickActions"
                :key="action.text"
                @click="handleQuickAction(action.text)"
                class="quick-action-btn"
              >
                <template #icon>
                  <n-icon><component :is="action.icon" /></n-icon>
                </template>
                {{ action.text }}
              </n-button>
            </div>
          </div>

          <div
            v-for="(msg, index) in messages"
            :key="index"
            class="message"
            :class="msg.role"
          >
            <div class="message-avatar">
              <n-icon v-if="msg.role === 'user'" size="20"><IconUser /></n-icon>
              <n-icon v-else size="20"><IconSparkles /></n-icon>
            </div>
            <div class="message-content">
              <div class="message-text" v-html="formatMessage(msg.content)"></div>
              <div v-if="msg.role === 'assistant'" class="message-actions">
                <n-button text size="small" @click="copyMessage(msg.content)">
                  <template #icon>
                    <n-icon><IconCopy /></n-icon>
                  </template>
                  复制
                </n-button>
              </div>
            </div>
          </div>

          <div v-if="loading" class="message assistant">
            <div class="message-avatar">
              <n-icon size="20"><IconSparkles /></n-icon>
            </div>
            <div class="message-content">
              <div class="typing-indicator">
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
          </div>
        </div>

        <div class="chat-input-area">
          <n-input
            v-model:value="inputText"
            type="textarea"
            placeholder="输入您的投资问题..."
            :autosize="{ minRows: 1, maxRows: 4 }"
            @keydown.enter.exact.prevent="sendMessage"
          />
          <n-button type="primary" :loading="loading" :disabled="!inputText.trim()" @click="sendMessage">
            <template #icon>
              <n-icon><IconSend /></n-icon>
            </template>
            发送
          </n-button>
        </div>
        <div class="input-tips">
          按 Enter 发送，Shift + Enter 换行
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  NButton, NIcon, NInput, NSelect, useMessage
} from 'naive-ui'
import {
  IconPlus, IconMessages, IconTrash, IconSparkles,
  IconUser, IconSend, IconCopy, IconTrendingUp,
  IconHelpCircle, IconBulb
} from '@tabler/icons-vue'
import { fundApi } from '@/api/fund'

const route = useRoute()
const message = useMessage()

// 状态
const sessions = ref<any[]>([])
const currentSessionId = ref<string>('')
const messages = ref<any[]>([])
const inputText = ref('')
const loading = ref(false)
const currentModel = ref('claude-sonnet-4-6')
const messagesContainer = ref<HTMLElement | null>(null)

// 模型选项
const modelOptions = [
  { label: 'Claude Sonnet 4.6', value: 'claude-sonnet-4-6' },
  { label: 'Claude Opus 4', value: 'claude-opus-4-20250514' },
  { label: 'Claude Haiku 3.5', value: 'claude-3-5-haiku-20241022' }
]

// 快捷操作
const quickActions = [
  { text: '今日市场行情分析', icon: IconTrendingUp },
  { text: '如何选择合适的基金？', icon: IconHelpCircle },
  { text: '定投策略建议', icon: IconBulb }
]

// 加载会话列表
const loadSessions = async () => {
  try {
    const res = await fundApi.getAISessions()
    sessions.value = res || []
  } catch (error) {
    console.error('加载会话失败', error)
  }
}

// 创建新会话
const createNewSession = async () => {
  try {
    const res = await fundApi.createAISession()
    sessions.value.unshift(res)
    currentSessionId.value = res.sessionId
    messages.value = []
  } catch (error) {
    message.error('创建会话失败')
  }
}

// 切换会话
const switchSession = async (sessionId: string) => {
  currentSessionId.value = sessionId
  try {
    const res = await fundApi.getAIChatHistory(sessionId)
    messages.value = res || []
    scrollToBottom()
  } catch (error) {
    console.error('加载历史失败', error)
  }
}

// 删除会话
const deleteSession = async (sessionId: string) => {
  try {
    await fundApi.deleteAISession(sessionId)
    sessions.value = sessions.value.filter(s => s.sessionId !== sessionId)
    if (currentSessionId.value === sessionId) {
      if (sessions.value.length > 0) {
        switchSession(sessions.value[0].sessionId)
      } else {
        currentSessionId.value = ''
        messages.value = []
      }
    }
    message.success('删除成功')
  } catch (error) {
    message.error('删除失败')
  }
}

// 发送消息
const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  // 如果没有会话，先创建
  if (!currentSessionId.value) {
    await createNewSession()
  }

  // 添加用户消息
  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  scrollToBottom()

  loading.value = true
  try {
    const res = await fundApi.sendAIMessage(currentSessionId.value, text)
    messages.value.push({ role: 'assistant', content: res.content })
    scrollToBottom()
  } catch (error: any) {
    message.error(error.message || '发送失败')
    messages.value.pop() // 移除用户消息
  } finally {
    loading.value = false
  }
}

// 快捷操作
const handleQuickAction = (text: string) => {
  inputText.value = text
  sendMessage()
}

// 格式化消息
const formatMessage = (content: string) => {
  // 简单的 Markdown 转换
  return content
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br>')
}

// 复制消息
const copyMessage = (content: string) => {
  navigator.clipboard.writeText(content)
  message.success('已复制')
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

onMounted(() => {
  loadSessions()

  // 检查 URL 参数
  const query = route.query.q as string
  if (query) {
    inputText.value = query
  }
})
</script>

<style scoped>
.ai-assistant-page {
  height: calc(100vh - 64px - 48px);
  padding: 24px;
}

.chat-layout {
  display: flex;
  height: 100%;
  background: var(--card-bg);
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: var(--shadow);
}

.sidebar {
  width: 260px;
  background: var(--bg-secondary);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid var(--border-color);
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 4px;
}

.session-item:hover {
  background: var(--card-bg-hover);
}

.session-item.active {
  background: rgba(79, 70, 229, 0.12);
  color: var(--primary-color);
}

.session-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid var(--border-color);
}

.chat-header h2 {
  font-size: 18px;
  font-weight: 600;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.welcome-section {
  text-align: center;
  padding: 60px 20px;
}

.welcome-icon {
  width: 100px;
  height: 100px;
  margin: 0 auto 24px;
  background: var(--gradient-primary);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.welcome-section h2 {
  font-size: 24px;
  margin-bottom: 12px;
}

.welcome-section p {
  color: var(--text-secondary);
  margin-bottom: 32px;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 400px;
  margin: 0 auto;
}

.quick-action-btn {
  justify-content: flex-start;
  height: auto;
  padding: 16px 20px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  font-size: 14px;
}

.quick-action-btn:hover {
  border-color: var(--primary-color);
  background: rgba(79, 70, 229, 0.08);
}

.message {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.message.user .message-avatar {
  background: var(--bg-secondary);
  color: var(--text-color);
}

.message.assistant .message-avatar {
  background: var(--gradient-primary);
  color: white;
}

.message-content {
  max-width: 70%;
  min-width: 100px;
}

.message-text {
  padding: 12px 16px;
  border-radius: var(--radius-xl);
  line-height: 1.6;
  font-size: 14px;
}

.message.user .message-text {
  background: var(--primary-color);
  color: white;
  border-bottom-right-radius: 4px;
}

.message.assistant .message-text {
  background: var(--bg-secondary);
  border-bottom-left-radius: 4px;
}

.message-actions {
  margin-top: 8px;
  opacity: 0;
  transition: opacity 0.2s;
}

.message:hover .message-actions {
  opacity: 1;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 16px;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: var(--text-tertiary);
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out both;
}

.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }

@keyframes typing {
  0%, 80%, 100% { transform: scale(0.6); }
  40% { transform: scale(1); }
}

.chat-input-area {
  display: flex;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid var(--border-color);
}

.chat-input-area .n-input {
  flex: 1;
}

.input-tips {
  padding: 0 24px 12px;
  font-size: 12px;
  color: var(--text-tertiary);
  text-align: right;
}

@media (max-width: 768px) {
  .sidebar {
    display: none;
  }

  .message-content {
    max-width: 85%;
  }
}
</style>
