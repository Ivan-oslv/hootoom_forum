export function readCookie(name: string, cookieSource?: string): string | null {
  const source = cookieSource ?? (import.meta.client ? document.cookie : '')
  const prefix = `${encodeURIComponent(name)}=`
  const item = source.split(';').map(value => value.trim()).find(value => value.startsWith(prefix))
  return item ? decodeURIComponent(item.slice(prefix.length)) : null
}
