/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { getCsrfToken } from 'GuideRoot/auth/csrfToken';

describe('getCsrfToken', () => {
  afterEach(() => {
    Object.defineProperty(document, 'cookie', { value: '', writable: true });
  });

  it('returns the value of the CLM-CSRF-TOKEN cookie', () => {
    Object.defineProperty(document, 'cookie', {
      value: 'CLM-CSRF-TOKEN=abc123',
      writable: true,
    });

    expect(getCsrfToken()).toBe('abc123');
  });

  it('returns undefined when the cookie is not present', () => {
    Object.defineProperty(document, 'cookie', {
      value: 'OTHER_COOKIE=value',
      writable: true,
    });

    expect(getCsrfToken()).toBeUndefined();
  });

  it('returns the correct value when multiple cookies are present', () => {
    Object.defineProperty(document, 'cookie', {
      value: 'CLMSESSIONID=sess1; CLM-CSRF-TOKEN=xyz789; OTHER=val',
      writable: true,
    });

    expect(getCsrfToken()).toBe('xyz789');
  });

  it('handles URI-encoded cookie values', () => {
    Object.defineProperty(document, 'cookie', {
      value: 'CLM-CSRF-TOKEN=token%3Dwith%3Dequals',
      writable: true,
    });

    expect(getCsrfToken()).toBe('token=with=equals');
  });

  it('returns undefined when document.cookie is empty', () => {
    Object.defineProperty(document, 'cookie', { value: '', writable: true });

    expect(getCsrfToken()).toBeUndefined();
  });
});
