<template>
  <div class="chat-container">
    <div class="messages" ref="messagesEl">
      <div
        v-for="(msg, i) in messages"
        :key="i"
        class="message"
        :class="msg.role"
      >
        <div class="message-bubble">
          <div v-if="msg.role === 'assistant' && msg.parsed" class="parsed-result">
            <div v-for="(ex, ei) in msg.parsed" :key="ei" class="parsed-exercise">
              <span class="parsed-name">{{ ex.name }}</span>
              <div class="parsed-sets">
                <span v-for="(set, si) in ex.sets" :key="si" class="parsed-set">
                  {{ set.weightKg ? set.weightKg + 'kg' : '' }}
                  {{ set.reps ? '× ' + set.reps : '' }}
                  {{ set.rpe ? '@ RPE ' + set.rpe : '' }}
                </span>
              </div>
            </div>
            <div v-if="!msg.confirmed" class="confirm-actions">
              <button class="confirm-btn" @click="confirmParsed(i)">Save</button>
              <button class="reject-btn" @click="rejectParsed(i)">Edit</button>
            </div>
            <span v-else class="confirmed-badge">Saved</span>
          </div>
          <p v-else class="message-text">{{ msg.text }}</p>
          <span v-if="msg.source === 'ai'" class="ai-badge">AI</span>
        </div>
      </div>
      <div v-if="loading" class="message assistant">
        <div class="message-bubble">
          <span class="typing">Parsing...</span>
        </div>
      </div>
    </div>

    <form class="chat-input" @submit.prevent="send">
      <input
        v-model="input"
        type="text"
        class="chat-field"
        placeholder='e.g. "Squat 100kg x 5, 5, 5 @ RPE 7"'
        :disabled="loading"
      />
      <button type="submit" class="send-btn" :disabled="!input.trim() || loading">
        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="22" y1="2" x2="11" y2="13" /><polygon points="22 2 15 22 11 13 2 9 22 2" />
        </svg>
      </button>
    </form>
  </div>
</template>

<script setup lang="ts">
interface ParsedSet {
  weightKg: number | null
  reps: number | null
  rpe: number | null
}

interface ParsedExercise {
  name: string
  sets: ParsedSet[]
}

interface ChatMessage {
  role: 'user' | 'assistant'
  text: string
  parsed?: ParsedExercise[]
  confirmed?: boolean
  source?: 'ai' | 'parser'
}

const emit = defineEmits<{
  'exercises-confirmed': [exercises: ParsedExercise[]]
}>()

const messages = ref<ChatMessage[]>([
  {
    role: 'assistant',
    text: 'Log your exercises here. Type something like "Bench 80kg x 8, 8, 6 @ RPE 8" and I\'ll parse it for you.',
  },
])

const input = ref('')
const loading = ref(false)
const messagesEl = ref<HTMLElement>()
const toast = useToast()

function scrollToBottom() {
  nextTick(() => {
    if (messagesEl.value) {
      messagesEl.value.scrollTop = messagesEl.value.scrollHeight
    }
  })
}

async function send() {
  const text = input.value.trim()
  if (!text) return

  messages.value.push({ role: 'user', text })
  input.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const result = await $fetch<{
      source: 'ai' | 'parser'
      exercises: ParsedExercise[]
      originalMessage: string
    }>('/api/fitness/chat/parse', {
      method: 'POST',
      body: { message: text },
    })

    if (result.exercises.length > 0) {
      messages.value.push({
        role: 'assistant',
        text: 'I parsed the following:',
        parsed: result.exercises,
        confirmed: false,
        source: result.source,
      })
    } else {
      messages.value.push({
        role: 'assistant',
        text: "I couldn't parse any exercises from that. Try something like \"Squat 100kg x 5, 5, 5 @ RPE 7\"",
      })
    }
  } catch {
    toast.add({ title: 'Parse failed', description: 'Could not parse your input.', color: 'error' })
    messages.value.push({
      role: 'assistant',
      text: 'Sorry, something went wrong. Please try again.',
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function confirmParsed(index: number) {
  const msg = messages.value[index]
  if (!msg.parsed) return
  msg.confirmed = true
  emit('exercises-confirmed', msg.parsed)
  messages.value.push({
    role: 'assistant',
    text: 'Logged! Keep going or finish your workout.',
  })
  scrollToBottom()
}

function rejectParsed(index: number) {
  messages.value[index].parsed = undefined
  messages.value[index].text = 'No worries — type it again and I\'ll re-parse.'
  scrollToBottom()
}
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 300px;
}
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 1rem 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.message {
  display: flex;
}
.message.user {
  justify-content: flex-end;
}
.message.assistant {
  justify-content: flex-start;
}
.message-bubble {
  max-width: 85%;
  padding: 0.75rem 1rem;
  border-radius: 12px;
  font-size: 0.95rem;
  line-height: 1.5;
  position: relative;
}
.user .message-bubble {
  background: var(--accent-orange);
  color: #fff;
  border-bottom-right-radius: 4px;
}
.assistant .message-bubble {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  color: var(--text-primary);
  border-bottom-left-radius: 4px;
}
.message-text {
  margin: 0;
}
.ai-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  background: linear-gradient(135deg, var(--accent-purple), var(--accent-blue));
  color: #fff;
  font-size: 0.55rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  padding: 2px 6px;
  border-radius: 4px;
}
.parsed-result {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.parsed-exercise {
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--border-subtle);
}
.parsed-exercise:last-of-type {
  border-bottom: none;
}
.parsed-name {
  font-family: 'Oswald', sans-serif;
  font-weight: 600;
  font-size: 0.95rem;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  display: block;
  margin-bottom: 0.25rem;
}
.parsed-sets {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}
.parsed-set {
  background: var(--section-bg);
  border: 1px solid var(--border-subtle);
  border-radius: 6px;
  padding: 0.2rem 0.5rem;
  font-size: 0.8rem;
  color: var(--text-secondary);
  font-family: 'Source Sans 3', sans-serif;
}
.confirm-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 0.75rem;
}
.confirm-btn {
  background: var(--accent-orange);
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 0.4rem 1rem;
  font-family: 'Oswald', sans-serif;
  font-weight: 600;
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  cursor: pointer;
  transition: all 0.2s;
}
.confirm-btn:hover {
  filter: brightness(1.1);
}
.reject-btn {
  background: none;
  border: 1px solid var(--border-subtle);
  border-radius: 6px;
  padding: 0.4rem 1rem;
  font-size: 0.8rem;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.2s;
}
.reject-btn:hover {
  border-color: var(--text-secondary);
  color: var(--text-secondary);
}
.confirmed-badge {
  display: inline-block;
  margin-top: 0.5rem;
  color: var(--accent-green);
  font-size: 0.8rem;
  font-weight: 600;
}
.typing {
  color: var(--text-muted);
  font-style: italic;
}

.chat-input {
  display: flex;
  gap: 0.5rem;
  padding-top: 1rem;
  border-top: 1px solid var(--border-subtle);
}
.chat-field {
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
.chat-field:focus {
  border-color: var(--accent-orange);
}
.chat-field::placeholder {
  color: var(--text-muted);
}
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
  transition: all 0.2s;
  flex-shrink: 0;
  padding: 0;
}
.send-btn:hover:not(:disabled) {
  filter: brightness(1.1);
  transform: scale(1.03);
}
.send-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}
</style>
