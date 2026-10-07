import { describe, expect, it } from 'vitest'
import { readCookie } from '../src/utils/cookies'

describe('readCookie', () => {
  it('reads and decodes the requested cookie without matching prefixes', () => {
    expect(readCookie('HOOTOOM_XSRF', 'OTHER=1; HOOTOOM_XSRF=abc%20123; HOOTOOM_XSRF_OLD=no'))
      .toBe('abc 123')
  })

  it('returns null when the cookie is missing', () => {
    expect(readCookie('HOOTOOM_XSRF', 'OTHER=1')).toBeNull()
  })
})
