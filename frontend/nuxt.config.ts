export default defineNuxtConfig({
  modules: ['@pinia/nuxt', '@nuxt/eslint'],
  // 组件已经按 common、auth 等目录分层，模板中使用文件名即可，避免目录名重复进入组件名。
  components: [{ path: '~/components', pathPrefix: false }],
  devtools: { enabled: true },
  app: {
    head: {
      htmlAttrs: { lang: 'zh-CN' },
      meta: [
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        { name: 'theme-color', content: '#0c1222' },
      ],
    },
  },
  css: ['~/styles/tokens.css', '~/styles/global.css'],
  runtimeConfig: {
    public: {
      apiBase: process.env.NUXT_PUBLIC_API_BASE || 'http://localhost:8080/api/v1',
      siteUrl: process.env.NUXT_PUBLIC_SITE_URL || 'http://localhost:3000',
      policyVersion: process.env.NUXT_PUBLIC_POLICY_VERSION || '2026-10-07',
    },
  },
  srcDir: 'src/',
  routeRules: {
    '/login': { ssr: false },
    '/register': { ssr: false },
    '/verify-email': { ssr: false },
    '/forgot-password': { ssr: false },
    '/reset-password': { ssr: false },
    '/user/**': { ssr: false },
  },
  compatibilityDate: '2026-10-07',
  // 类型检查由独立的 pnpm typecheck 执行，避免生产构建重复启动检查器。
  typescript: { strict: true, typeCheck: false },
  eslint: { config: { stylistic: true } },
})
