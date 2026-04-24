export default defineNuxtConfig({
  compatibilityDate: '2026-04-14',
  srcDir: '.',
  modules: [
    '@nuxt/ui',
    '@pinia/nuxt',
    'nuxt-auth-utils',
  ],
  css: ['~/assets/css/main.css', '~/assets/css/theme.css'],
  app: {
    head: {
      title: 'Krachtix Fitness Plan',
      meta: [
        { name: 'description', content: 'Adaptive fitness planning with AI-assisted logging.' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1' }
      ],
      link: [
        { rel: 'manifest', href: '/manifest.webmanifest' },
        { rel: 'icon', type: 'image/png', href: '/icon-192.png' },
        { rel: 'preconnect', href: 'https://fonts.googleapis.com' },
        { rel: 'preconnect', href: 'https://fonts.gstatic.com', crossorigin: '' },
        { rel: 'stylesheet', href: 'https://fonts.googleapis.com/css2?family=Oswald:wght@400;500;600;700&family=Source+Sans+3:wght@300;400;500;600&display=swap' }
      ]
    }
  },
  runtimeConfig: {
    sessionSecret: '',
    databaseUrl: '',
    encryptionKey: '',
    anthropicApiKey: '',
    public: {
      apiBase: '/api'
    }
  }
})
