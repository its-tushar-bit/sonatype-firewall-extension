/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { captureGuideReturnTo } from 'GuideRoot/auth/guideReturnTo';

describe('captureGuideReturnTo', () => {
  beforeEach(() => {
    sessionStorage.clear();
    Object.defineProperty(window, 'location', {
      value: { ...window.location, href: 'http://localhost/assets/guide/index.html#/components' },
      writable: true,
      configurable: true,
    });
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('writes the current href to sessionStorage under iqGuideReturnTo', () => {
    captureGuideReturnTo();
    expect(sessionStorage.getItem('iqGuideReturnTo')).toBe(
      'http://localhost/assets/guide/index.html#/components'
    );
  });

  it('overwrites any prior value', () => {
    sessionStorage.setItem('iqGuideReturnTo', 'http://localhost/assets/guide/index.html#/old');
    captureGuideReturnTo();
    expect(sessionStorage.getItem('iqGuideReturnTo')).toBe(
      'http://localhost/assets/guide/index.html#/components'
    );
  });
});
