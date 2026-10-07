export function usePageSeo(title: string, description: string, path: string, indexable = true) {
  const config = useRuntimeConfig()
  const canonical = new URL(path, config.public.siteUrl).toString()
  useSeoMeta({
    title,
    description,
    robots: indexable ? 'index,follow' : 'noindex,nofollow',
    ogTitle: title,
    ogDescription: description,
    ogUrl: canonical,
    ogType: 'website',
  })
  useHead({ link: [{ rel: 'canonical', href: canonical }] })
}
