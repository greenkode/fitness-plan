<template>
  <div class="wizard-page">
    <div class="wizard-header">
      <NuxtLink to="/programs" class="back-arrow">
        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6" /></svg>
      </NuxtLink>
      <h2 class="wizard-title">{{ programName || 'Program' }}</h2>
      <span v-if="programStatus" class="status-badge" :class="'status-' + programStatus">{{ programStatus }}</span>
    </div>

    <div v-if="loadingHistory" class="loading-state">Loading conversation...</div>

    <div v-else class="chat-area" ref="chatArea" @scroll="onChatScroll">
      <div v-if="!messages.length" class="message assistant">
        <div class="bubble assistant-bubble">
          <p class="msg-text">This is your program conversation. You can refine your program here — try things like "switch cardio to Tuesday" or "add more core exercises".</p>
        </div>
      </div>

      <div
        v-for="msg in messages"
        :key="msg.id"
        class="message"
        :class="msg.role"
      >
        <div class="bubble" :class="msg.role + '-bubble'">
          <div class="msg-md" v-html="renderMarkdown(getMessageText(msg))" />
        </div>
      </div>

      <div v-if="isLoading" class="message assistant">
        <div class="bubble assistant-bubble">
          <span class="loading-dots"><span></span><span></span><span></span></span>
        </div>
      </div>

      <div v-if="building" class="message assistant">
        <div class="bubble assistant-bubble">
          <div class="tool-loading">
            <span class="loading-dots"><span></span><span></span><span></span></span>
            <span class="loading-label">Updating your program...</span>
          </div>
        </div>
      </div>

      <div v-if="currentProposal" class="message assistant">
        <div class="bubble assistant-bubble">
          <p class="msg-text">Here's the updated program:</p>
          <ProgramPreview :program="currentProposal" />
        </div>
      </div>
    </div>

    <div class="input-area">
      <div v-if="currentProposal" class="confirm-bar">
        <button class="confirm-btn" :disabled="saving" @click="updateProgram">
          {{ saving ? 'Updating...' : 'Update Program' }}
        </button>
      </div>

      <form class="chat-form" @submit.prevent="handleSubmit">
        <input
          v-model="input"
          type="text"
          class="chat-input"
          placeholder="Ask to modify your program..."
          :disabled="isLoading"
          autocomplete="off"
        />
        <button type="submit" class="send-btn" :disabled="!input.trim() || isLoading">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="22" y1="2" x2="11" y2="13" /><polygon points="22 2 15 22 11 13 2 9 22 2" /></svg>
        </button>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { marked } from 'marked'

marked.setOptions({ breaks: true, gfm: true })

definePageMeta({ middleware: 'auth' })

const route = useRoute()
const programId = route.params.id as string
const chatArea = ref<HTMLElement>()
const messages = ref<any[]>([])
const input = ref('')
const isLoading = ref(false)
const loadingHistory = ref(true)
const currentProposal = ref<any>(null)
const saving = ref(false)
const building = ref(false)
const buildAttempted = ref(false)
const programName = ref('')
const programStatus = ref('')
const toast = useToast()

let chatInstance: any = null

onMounted(async () => {
  await loadProgramInfo()
  await loadConversationHistory()

  const { Chat } = await import('@ai-sdk/vue')
  const { DefaultChatTransport } = await import('ai')

  const initialMessages = messages.value.map((m: any) => ({
    id: m.id,
    role: m.role,
    parts: [{ type: 'text', text: m.content }],
  }))

  chatInstance = new Chat({
    messages: initialMessages,
    transport: new DefaultChatTransport({
      api: '/api/ai/chat',
      body: { userProgramId: programId },
    }),
  })

  const state = (chatInstance as any).state

  watch(state.messagesRef, (msgs: any[]) => {
    messages.value = [...msgs]
    if (msgs.length > 0) checkForReadySignal(messages.value)
  }, { deep: true, immediate: true })

  watch(state.statusRef, (status: string) => {
    isLoading.value = status === 'streaming' || status === 'submitted'
  }, { immediate: true })
})

async function loadProgramInfo() {
  try {
    const programs = await $fetch<any[]>('/api/fitness/programs')
    const program = programs.find((p: any) => p.id === programId)
    if (program) {
      programName.value = program.templateName
      programStatus.value = program.status
    }
  } catch {}
}

async function loadConversationHistory() {
  try {
    const history = await $fetch<any[]>(`/api/ai/conversations/${programId}`)
    messages.value = history.map((m: any) => ({
      id: m.id,
      role: m.role,
      content: m.content,
    }))
  } catch {}
  loadingHistory.value = false
}

async function checkForReadySignal(msgs: any[]) {
  if (building.value || currentProposal.value || buildAttempted.value) return

  for (let i = msgs.length - 1; i >= 0; i--) {
    const msg = msgs[i]
    if (msg.role !== 'assistant') continue
    const fullContent = typeof msg.content === 'string' ? msg.content : (msg.parts?.filter((p: any) => p.type === 'text').map((p: any) => p.text).join('') || '')
    const readyMatch = fullContent.match(/---READY---([\s\S]*?)---END---/)
    if (readyMatch) {
      const lines = readyMatch[1].trim().split('\n')
      const params: Record<string, string> = {}
      for (const line of lines) {
        const [key, ...rest] = line.split(':')
        if (key && rest.length) params[key.trim()] = rest.join(':').trim()
      }
      if (params.goals && params.daysPerWeek) {
        building.value = true
        buildAttempted.value = true
        try {
          const result = await $fetch<{ program: any }>('/api/ai/build-program', {
            method: 'POST',
            body: params,
          })
          currentProposal.value = result.program
        } catch (e: any) {
          toast.add({ title: 'Failed to build program', description: e.data?.statusMessage || 'Please try again.', color: 'error' })
        } finally {
          building.value = false
        }
      }
      return
    }
  }
}

async function handleSubmit(e?: Event) {
  e?.preventDefault()
  if (!chatInstance || !input.value.trim()) return
  const text = input.value
  input.value = ''
  try {
    await chatInstance.sendMessage({ text })
  } catch (err: any) {
    console.error('sendMessage error:', err)
    toast.add({ title: 'Failed to send', description: err.message, color: 'error' })
  }
}

async function updateProgram() {
  if (!currentProposal.value) return
  saving.value = true
  try {
    await $fetch(`/api/fitness/programs/${programId}/update`, {
      method: 'POST',
      body: { program: currentProposal.value },
    })
    toast.add({ title: 'Program updated!', description: 'Your workout calendar has been refreshed.', color: 'success' })
    currentProposal.value = null
    buildAttempted.value = false
  } catch (e: any) {
    toast.add({ title: 'Update failed', description: e.data?.statusMessage || 'Please try again.', color: 'error' })
  } finally {
    saving.value = false
  }
}

function renderMarkdown(text: string): string {
  if (!text) return ''
  try {
    return marked.parse(text, { async: false }) as string
  } catch {
    return text.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>').replace(/\*(.*?)\*/g, '<em>$1</em>').replace(/\n/g, '<br>')
  }
}

function getMessageText(msg: any): string {
  if (msg.content && typeof msg.content === 'string' && msg.content.trim()) {
    return msg.content.replace(/---READY---[\s\S]*?---END---/g, '').trim()
  }
  if (msg.parts && Array.isArray(msg.parts)) {
    return msg.parts.filter((p: any) => p.type === 'text').map((p: any) => p.text).join('').replace(/---READY---[\s\S]*?---END---/g, '').trim()
  }
  return ''
}

const shouldAutoScroll = ref(true)

function onChatScroll() {
  if (!chatArea.value) return
  const { scrollTop, scrollHeight, clientHeight } = chatArea.value
  shouldAutoScroll.value = scrollHeight - scrollTop - clientHeight < 100
}

watch(messages, () => {
  if (shouldAutoScroll.value) {
    nextTick(() => {
      if (chatArea.value) chatArea.value.scrollTop = chatArea.value.scrollHeight
    })
  }
}, { deep: true })
</script>

<style scoped>
.wizard-page { width: 100%; max-width: 650px; margin: 0 auto; display: flex; flex-direction: column; flex: 1; min-height: 0; }
.wizard-header { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 1rem; flex-shrink: 0; }
.back-arrow { color: var(--text-muted); display: flex; transition: color 0.2s; }
.back-arrow:hover { color: var(--accent-orange); }
.wizard-title { font-family: 'Oswald', sans-serif; font-size: 1.5rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.05em; color: var(--text-primary); margin: 0; }
.status-badge { font-size: 0.6rem; text-transform: uppercase; letter-spacing: 0.1em; padding: 0.2rem 0.5rem; border-radius: 4px; font-weight: 600; }
.status-active { background: rgba(232, 93, 37, 0.15); color: var(--accent-orange); }
.status-paused { background: rgba(212, 160, 18, 0.15); color: var(--accent-yellow); }
.status-completed { background: rgba(13, 148, 136, 0.15); color: var(--accent-green); }
.loading-state { text-align: center; padding: 3rem; color: var(--text-muted); }
.chat-area { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 0.75rem; padding-bottom: 1rem; }
.message { display: flex; }
.message.user { justify-content: flex-end; }
.message.assistant { justify-content: flex-start; }
.bubble { max-width: 90%; padding: 0.75rem 1rem; border-radius: 12px; font-size: 0.95rem; line-height: 1.5; }
.user-bubble { background: var(--accent-orange); color: #fff; border-bottom-right-radius: 4px; }
.assistant-bubble { background: var(--bg-card); border: 1px solid var(--border-subtle); color: var(--text-primary); border-bottom-left-radius: 4px; }
.msg-text { margin: 0; white-space: pre-wrap; }
.msg-md { line-height: 1.6; }
.msg-md :deep(p) { margin: 0 0 0.5rem; }
.msg-md :deep(p:last-child) { margin-bottom: 0; }
.msg-md :deep(strong) { font-weight: 600; }
.msg-md :deep(ul), .msg-md :deep(ol) { margin: 0.25rem 0 0.5rem; padding-left: 1.25rem; }
.msg-md :deep(li) { margin-bottom: 0.15rem; }
.msg-md :deep(h1), .msg-md :deep(h2), .msg-md :deep(h3) { font-family: 'Oswald', sans-serif; margin: 0.75rem 0 0.25rem; font-weight: 600; }
.tool-loading { display: flex; align-items: center; gap: 0.75rem; padding: 0.5rem 0; }
.loading-label { font-size: 0.85rem; color: var(--accent-orange); font-style: italic; }
.loading-dots { display: flex; gap: 4px; }
.loading-dots span { width: 8px; height: 8px; background: var(--accent-orange); border-radius: 50%; animation: bounce 1.4s infinite ease-in-out both; }
.loading-dots span:nth-child(1) { animation-delay: 0s; }
.loading-dots span:nth-child(2) { animation-delay: 0.2s; }
.loading-dots span:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce { 0%, 80%, 100% { transform: scale(0); } 40% { transform: scale(1); } }
.input-area { flex-shrink: 0; border-top: 1px solid var(--border-subtle); padding-top: 0.75rem; }
.confirm-bar { margin-bottom: 0.75rem; }
.confirm-btn { width: 100%; background: linear-gradient(135deg, var(--accent-orange), #ff8c5a); color: #fff; font-family: 'Oswald', sans-serif; font-weight: 700; text-transform: uppercase; letter-spacing: 0.05em; font-size: 1rem; border: none; border-radius: 10px; padding: 0.85rem; cursor: pointer; transition: all 0.2s; }
.confirm-btn:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 4px 15px rgba(232, 93, 37, 0.4); }
.confirm-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.chat-form { display: flex; gap: 0.5rem; }
.chat-input { flex: 1; background: var(--bg-card); border: 1px solid var(--border-subtle); border-radius: 10px; padding: 0.75rem 1rem; font-size: 0.95rem; color: var(--text-primary); font-family: 'Source Sans 3', sans-serif; outline: none; transition: border-color 0.2s; }
.chat-input:focus { border-color: var(--accent-orange); }
.chat-input::placeholder { color: var(--text-muted); }
.send-btn { width: 44px; height: 44px; border-radius: 10px; background: var(--accent-orange); color: #fff; border: none; cursor: pointer; display: flex; align-items: center; justify-content: center; flex-shrink: 0; padding: 0; transition: all 0.2s; }
.send-btn:hover:not(:disabled) { filter: brightness(1.1); }
.send-btn:disabled { opacity: 0.3; cursor: not-allowed; }
</style>
