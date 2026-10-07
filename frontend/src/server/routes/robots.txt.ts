export default defineEventHandler((event) => {
  const config = useRuntimeConfig(event)
  const siteUrl = config.public.siteUrl.replace(/\/$/, '')
  setHeader(event, 'content-type', 'text/plain; charset=utf-8')
  return `User-agent: *
Allow: /
Disallow: /login
Disallow: /register
Disallow: /verify-email
Disallow: /forgot-password
Disallow: /reset-password
Disallow: /user/
Disallow: /admin/
Disallow: /search

Sitemap: ${siteUrl}/sitemap.xml
`
})
