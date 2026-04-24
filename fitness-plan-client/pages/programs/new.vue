<template>
  <div class="wizard-page">
    <div class="wizard-header">
      <NuxtLink to="/programs" class="back-arrow">
        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6" /></svg>
      </NuxtLink>
      <h2 class="wizard-title">New Program</h2>
    </div>

    <div class="chat-area" ref="chatArea" @scroll="onChatScroll">
      <div v-if="!hasUserSentMessage" class="message assistant">
        <div class="bubble assistant-bubble">
          <p class="msg-text">Hi! I'm your AI fitness coach. Tell me about your goals and I'll design a personalized program for you.</p>
          <p class="hint-text">Try: "I want to build muscle, 4 days a week, I have a full gym"</p>
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
            <span class="loading-label">Building your program... This may take a minute.</span>
          </div>
        </div>
      </div>

      <div v-if="currentProposal" class="message assistant">
        <div class="bubble assistant-bubble">
          <p class="msg-text">Here's your program:</p>
          <ProgramPreview :program="currentProposal" />
        </div>
      </div>
    </div>

    <div class="input-area">
      <div v-if="currentProposal && !saved" class="confirm-bar">
        <button class="confirm-btn" :disabled="saving" @click="saveProgram">
          {{ saving ? 'Saving...' : 'Confirm & Save Program' }}
        </button>
      </div>

      <form v-if="!saved" class="chat-form" @submit.prevent="handleSubmit">
        <input
          v-model="input"
          type="text"
          class="chat-input"
          :placeholder="currentProposal ? 'Ask for changes or save above...' : 'Describe your fitness goals...'"
          :disabled="isLoading"
          autocomplete="off"
        />
        <button type="submit" class="send-btn" :disabled="!input.trim() || isLoading">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="22" y1="2" x2="11" y2="13" /><polygon points="22 2 15 22 11 13 2 9 22 2" /></svg>
        </button>
      </form>

      <div v-if="saved" class="success-bar">
        <p class="success-text">Program saved! Your calendar has been updated.</p>
        <NuxtLink to="/programs" class="go-btn">View Programs</NuxtLink>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { marked } from 'marked'

marked.setOptions({ breaks: true, gfm: true })

definePageMeta({ middleware: 'auth' })

const chatArea = ref<HTMLElement>()
const messages = ref<any[]>([])
const input = ref('')
const isLoading = ref(false)
const currentProposal = ref<any>(null)
const saving = ref(false)
const saved = ref(false)
const hasUserSentMessage = ref(false)
const toast = useToast()

let chatInstance: any = null

onMounted(async () => {
  const { Chat } = await import('@ai-sdk/vue')
  const { DefaultChatTransport } = await import('ai')

  chatInstance = new Chat({
    transport: new DefaultChatTransport({ api: '/api/ai/chat' }),
  })

  const state = (chatInstance as any).state
  setInterval(() => {
    const msgs = state.messagesRef.value
    const status = state.statusRef.value
    if (msgs.length !== messages.value.length || status !== (isLoading.value ? 'streaming' : 'ready')) {
      console.log('UPDATE - messages:', msgs.length, 'status:', status)
      msgs.forEach((m: any, i: number) => {
        const text = m.content || m.parts?.map((p: any) => p.type === 'text' ? p.text : `[${p.type}]`).join('') || ''
        console.log(`  msg[${i}] role=${m.role} content=${JSON.stringify(text).substring(0, 100)} parts=${m.parts?.length || 0}`)
      })
    }
    messages.value = [...msgs]
    isLoading.value = status === 'streaming' || status === 'submitted'
    if (msgs.length > 0) checkForReadySignal(messages.value)
  }, 300)
})

const building = ref(false)
const buildAttempted = ref(false)

async function checkForReadySignal(msgs: any[]) {
  if (building.value || currentProposal.value || buildAttempted.value) return

  for (let i = msgs.length - 1; i >= 0; i--) {
    const msg = msgs[i]
    if (msg.role !== 'assistant') continue
    const content = getMessageText(msg)
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
  hasUserSentMessage.value = true
  try {
    await chatInstance.sendMessage({ text })
  } catch (err: any) {
    console.error('sendMessage error:', err)
    toast.add({ title: 'Failed to send', description: err.message, color: 'error' })
  }
}

async function saveProgram() {
  if (!currentProposal.value) return
  saving.value = true
  try {
    await $fetch('/api/fitness/programs/save', {
      method: 'POST',
      body: { program: currentProposal.value },
    })
    saved.value = true
    toast.add({ title: 'Program saved!', description: 'Calendar updated with 4 weeks of workouts.', color: 'success' })
    navigateTo('/programs')
  } catch (e: any) {
    toast.add({ title: 'Save failed', description: e.data?.statusMessage || 'Please try again.', color: 'error' })
  } finally {
    saving.value = false
  }
}

function renderMarkdown(text: string): string {
  if (!text) return ''
  try {
    return marked.parse(text, { async: false }) as string
  } catch {
    return text
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.*?)\*/g, '<em>$1</em>')
      .replace(/\n/g, '<br>')
  }
}

function getMessageText(msg: any): string {
  if (msg.content && typeof msg.content === 'string' && msg.content.trim()) {
    return msg.content.replace(/---READY---[\s\S]*?---END---/g, '').trim()
  }
  if (msg.parts && Array.isArray(msg.parts)) {
    const text = msg.parts
      .filter((p: any) => p.type === 'text')
      .map((p: any) => p.text)
      .join('')
    return text.replace(/---READY---[\s\S]*?---END---/g, '').trim()
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
      if (chatArea.value) {
        chatArea.value.scrollTop = chatArea.value.scrollHeight
      }
    })
  }
}, { deep: true })
</script>

<style scoped>
.wizard-page {
  max-width: 650px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}
.wizard-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
  flex-shrink: 0;
}
.back-arrow {
  color: var(--text-muted);
  display: flex;
  transition: color 0.2s;
}
.back-arrow:hover { color: var(--accent-orange); }
.wizard-title {
  font-family: 'Oswald', sans-serif;
  font-size: 1.5rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--text-primary);
  margin: 0;
}

.chat-area {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding-bottom: 1rem;
}
.message { display: flex; }
.message.user { justify-content: flex-end; }
.message.assistant { justify-content: flex-start; }
.bubble {
  max-width: 90%;
  padding: 0.75rem 1rem;
  border-radius: 12px;
  font-size: 0.95rem;
  line-height: 1.5;
}
.user-bubble {
  background: var(--accent-orange);
  color: #fff;
  border-bottom-right-radius: 4px;
}
.assistant-bubble {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  color: var(--text-primary);
  border-bottom-left-radius: 4px;
}
.msg-text { margin: 0; white-space: pre-wrap; }
.msg-md { line-height: 1.6; }
.msg-md :deep(p) { margin: 0 0 0.5rem; }
.msg-md :deep(p:last-child) { margin-bottom: 0; }
.msg-md :deep(strong) { font-weight: 600; }
.msg-md :deep(ul), .msg-md :deep(ol) { margin: 0.25rem 0 0.5rem; padding-left: 1.25rem; }
.msg-md :deep(li) { margin-bottom: 0.15rem; }
.msg-md :deep(h1), .msg-md :deep(h2), .msg-md :deep(h3) {
  font-family: 'Oswald', sans-serif;
  margin: 0.75rem 0 0.25rem;
  font-weight: 600;
}
.msg-md :deep(code) {
  background: var(--section-bg);
  padding: 0.1rem 0.3rem;
  border-radius: 3px;
  font-size: 0.85em;
}
.hint-text {
  margin: 0.75rem 0 0;
  font-size: 0.8rem;
  color: var(--text-muted);
  font-style: italic;
}

.tool-loading {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 0;
}
.loading-label {
  font-size: 0.85rem;
  color: var(--accent-orange);
  font-style: italic;
}
.loading-dots {
  display: flex;
  gap: 4px;
}
.loading-dots span {
  width: 8px;
  height: 8px;
  background: var(--accent-orange);
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}
.loading-dots span:nth-child(1) { animation-delay: 0s; }
.loading-dots span:nth-child(2) { animation-delay: 0.2s; }
.loading-dots span:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.input-area {
  flex-shrink: 0;
  border-top: 1px solid var(--border-subtle);
  padding-top: 0.75rem;
}
.confirm-bar { margin-bottom: 0.75rem; }
.confirm-btn {
  width: 100%;
  background: linear-gradient(135deg, var(--accent-orange), #ff8c5a);
  color: #fff;
  font-family: 'Oswald', sans-serif;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-size: 1rem;
  border: none;
  border-radius: 10px;
  padding: 0.85rem;
  cursor: pointer;
  transition: all 0.2s;
}
.confirm-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(232, 93, 37, 0.4);
}
.confirm-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.chat-form { display: flex; gap: 0.5rem; }
.chat-input {
  flex: 1;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: 10px;
  padding: 0.75rem 1rem;
  font-size: 0.95rem;
  color: var(--text-primary);
  font-family: 'Source Sans 3', sans-serif;
  outline: none;
  transition: border-color 0.2s;
}
.chat-input:focus { border-color: var(--accent-orange); }
.chat-input::placeholder { color: var(--text-muted); }
.send-btn {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: var(--accent-orange);
  color: #fff;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  padding: 0;
  transition: all 0.2s;
}
.send-btn:hover:not(:disabled) { filter: brightness(1.1); }
.send-btn:disabled { opacity: 0.3; cursor: not-allowed; }

.success-bar { text-align: center; padding: 1rem; }
.success-text { color: var(--accent-green); font-weight: 600; margin: 0 0 1rem; }
.go-btn {
  display: inline-block;
  background: var(--accent-orange);
  color: #fff;
  font-family: 'Oswald', sans-serif;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 0.75rem 2rem;
  border-radius: 8px;
  text-decoration: none;
  transition: all 0.2s;
}
.go-btn:hover { filter: brightness(1.1); transform: translateY(-1px); }
</style>
