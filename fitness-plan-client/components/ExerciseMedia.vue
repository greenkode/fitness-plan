<template>
  <div class="media-section">
    <div v-if="media.length > 0" class="media-grid">
      <div v-for="m in media" :key="m.id" class="media-item">
        <a :href="m.url" target="_blank" rel="noopener" class="media-thumb" :class="m.mediaType">
          <img v-if="m.mediaType === 'image'" :src="m.url" :alt="m.label || 'Exercise reference'" loading="lazy" />
          <img v-else-if="m.mediaType === 'youtube' && getYoutubeThumb(m.url)" :src="getYoutubeThumb(m.url)!" alt="" loading="lazy" />
          <div v-else class="media-icon">
            <svg v-if="m.mediaType === 'youtube' || m.mediaType === 'video'" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z" /></svg>
            <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" /><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" /></svg>
          </div>
          <span class="play-overlay" v-if="m.mediaType === 'youtube' || m.mediaType === 'video'">
            <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="white"><path d="M8 5v14l11-7z" /></svg>
          </span>
        </a>
        <div class="media-meta">
          <span class="media-label">{{ m.label || labelFromUrl(m.url) }}</span>
          <button v-if="canEdit" class="remove-btn" @click="removeMedia(m.id)" aria-label="Remove">
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
          </button>
        </div>
      </div>
    </div>

    <div v-if="canEdit" class="add-media">
      <button v-if="!showInput" class="add-btn" @click="showInput = true">
        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" /></svg>
        Add video or image link
      </button>
      <form v-else class="add-form" @submit.prevent="addMedia">
        <input
          v-model="newUrl"
          type="url"
          class="url-input"
          placeholder="Paste YouTube URL, video link, or image URL"
          autocomplete="off"
          required
        />
        <input
          v-model="newLabel"
          type="text"
          class="label-input"
          placeholder="Label (optional)"
          autocomplete="off"
        />
        <div class="form-actions">
          <button type="submit" class="save-btn" :disabled="adding || !newUrl.trim()">{{ adding ? 'Adding...' : 'Save' }}</button>
          <button type="button" class="cancel-btn" @click="cancel">Cancel</button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
interface MediaItem {
  id: string
  url: string
  mediaType: string
  label: string | null
  sortOrder: number
}

const props = defineProps<{
  exerciseId: string
  initialMedia?: MediaItem[]
  canEdit?: boolean
}>()

const emit = defineEmits<{ updated: [media: MediaItem[]] }>()

const media = ref<MediaItem[]>(props.initialMedia || [])
const showInput = ref(false)
const newUrl = ref('')
const newLabel = ref('')
const adding = ref(false)
const toast = useToast()

watch(() => props.initialMedia, (v) => { if (v) media.value = v }, { deep: true })

function getYoutubeThumb(url: string): string | null {
  const m = url.match(/(?:youtube\.com\/(?:[^/]+\/.+\/|(?:v|e(?:mbed)?)\/|.*[?&]v=)|youtu\.be\/)([^"&?/\s]{11})/)
  return m ? `https://i.ytimg.com/vi/${m[1]}/mqdefault.jpg` : null
}

function labelFromUrl(url: string): string {
  try {
    const u = new URL(url)
    return u.hostname.replace(/^www\./, '')
  } catch {
    return 'Link'
  }
}

async function addMedia() {
  if (!newUrl.value.trim()) return
  adding.value = true
  try {
    const created = await $fetch<MediaItem>(`/api/fitness/exercises/${props.exerciseId}/media`, {
      method: 'POST',
      body: { url: newUrl.value, label: newLabel.value || undefined },
    })
    media.value = [...media.value, created]
    newUrl.value = ''
    newLabel.value = ''
    showInput.value = false
    emit('updated', media.value)
  } catch (e: any) {
    toast.add({ title: 'Failed to add link', description: e.data?.statusMessage || 'Try again', color: 'error' })
  } finally {
    adding.value = false
  }
}

async function removeMedia(id: string) {
  try {
    await $fetch(`/api/fitness/exercises/${props.exerciseId}/media/${id}`, { method: 'DELETE' })
    media.value = media.value.filter(m => m.id !== id)
    emit('updated', media.value)
  } catch {
    toast.add({ title: 'Failed to remove', color: 'error' })
  }
}

function cancel() {
  newUrl.value = ''
  newLabel.value = ''
  showInput.value = false
}
</script>

<style scoped>
.media-section {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.media-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 0.5rem;
}
.media-item {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}
.media-thumb {
  position: relative;
  display: block;
  aspect-ratio: 16 / 9;
  background: var(--section-bg);
  border: 1px solid var(--border-subtle);
  border-radius: 8px;
  overflow: hidden;
  text-decoration: none;
  color: var(--text-secondary);
  transition: border-color 0.2s, transform 0.2s;
}
.media-thumb:hover {
  border-color: var(--accent-orange);
  transform: translateY(-1px);
}
.media-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.media-icon {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
}
.play-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.25);
  opacity: 0;
  transition: opacity 0.2s;
}
.media-thumb:hover .play-overlay {
  opacity: 1;
}
.media-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.4rem;
}
.media-label {
  font-size: 0.7rem;
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.remove-btn {
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  padding: 2px;
  border-radius: 4px;
  display: flex;
  flex-shrink: 0;
}
.remove-btn:hover { color: #ef4444; }

.add-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  background: none;
  border: 1px dashed var(--border-subtle);
  color: var(--text-muted);
  font-size: 0.75rem;
  padding: 0.4rem 0.75rem;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  align-self: flex-start;
}
.add-btn:hover {
  border-color: var(--accent-orange);
  color: var(--accent-orange);
  border-style: solid;
}
.add-form {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  padding: 0.6rem;
  background: var(--section-bg);
  border-radius: 8px;
}
.url-input, .label-input {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: 6px;
  padding: 0.5rem 0.7rem;
  font-size: 0.85rem;
  color: var(--text-primary);
  outline: none;
}
.url-input:focus, .label-input:focus { border-color: var(--accent-orange); }
.form-actions {
  display: flex;
  gap: 0.4rem;
}
.save-btn {
  background: var(--accent-orange);
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 0.4rem 0.85rem;
  font-family: 'Oswald', sans-serif;
  font-weight: 600;
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  cursor: pointer;
  transition: filter 0.2s;
}
.save-btn:hover:not(:disabled) { filter: brightness(1.1); }
.save-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.cancel-btn {
  background: none;
  border: 1px solid var(--border-subtle);
  color: var(--text-muted);
  border-radius: 6px;
  padding: 0.4rem 0.85rem;
  font-size: 0.75rem;
  cursor: pointer;
}
.cancel-btn:hover { border-color: var(--text-secondary); }
</style>
