export type MediaType = 'youtube' | 'video' | 'image' | 'link'

export function detectMediaType(url: string): MediaType {
  const u = url.toLowerCase().trim()
  if (u.match(/youtube\.com|youtu\.be/)) return 'youtube'
  if (u.match(/\.(mp4|mov|webm|m4v)(\?|$|#)/)) return 'video'
  if (u.match(/\.(jpg|jpeg|png|gif|webp|svg)(\?|$|#)/)) return 'image'
  return 'link'
}

export function extractYouTubeId(url: string): string | null {
  const m = url.match(/(?:youtube\.com\/(?:[^/]+\/.+\/|(?:v|e(?:mbed)?)\/|.*[?&]v=)|youtu\.be\/)([^"&?/\s]{11})/)
  return m ? m[1] : null
}
