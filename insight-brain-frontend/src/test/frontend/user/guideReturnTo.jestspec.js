/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { consumeGuideReturnTo } from 'MainRoot/user/guideReturnTo';

describe('consumeGuideReturnTo', () => {
  beforeEach(() => {
    sessionStorage.clear();
    Object.defineProperty(window, 'location', {
      value: { ...window.location, origin: 'http://localhost' },
      writable: true,
      configurable: true,
    });
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('returns null and does nothing when nothing is stored', () => {
    expect(consumeGuideReturnTo()).toBeNull();
    expect(sessionStorage.getItem('iqGuideReturnTo')).toBeNull();
  });

  it('returns and clears a valid same-origin /assets/guide/ URL', () => {
    sessionStorage.setItem('iqGuideReturnTo', 'http://localhost/assets/guide/index.html#/components');

    const result = consumeGuideReturnTo();

    expect(result).toBe('http://localhost/assets/guide/index.html#/components');
    expect(sessionStorage.getItem('iqGuideReturnTo')).toBeNull();
  });

  it('returns null and clears entry for cross-origin URL', () => {
    sessionStorage.setItem('iqGuideReturnTo', 'https://evil.example.com/assets/guide/index.html');

    expect(consumeGuideReturnTo()).toBeNull();
    expect(sessionStorage.getItem('iqGuideReturnTo')).toBeNull();
  });

  it('returns null and clears entry for non-/assets/guide/ path', () => {
    sessionStorage.setItem('iqGuideReturnTo', 'http://localhost/assets/index.html');

    expect(consumeGuideReturnTo()).toBeNull();
    expect(sessionStorage.getItem('iqGuideReturnTo')).toBeNull();
  });

  it('returns null and clears entry for unparseable garbage', () => {
    sessionStorage.setItem('iqGuideReturnTo', 'not a url at all :://');

    expect(consumeGuideReturnTo()).toBeNull();
    expect(sessionStorage.getItem('iqGuideReturnTo')).toBeNull();
  });
});
