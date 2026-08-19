/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { getCookieValue } from 'GuideRoot/auth/cookieUtils';

describe('getCookieValue', () => {
  afterEach(() => {
    Object.defineProperty(document, 'cookie', { value: '', writable: true });
  });

  it('returns the value for an existing cookie', () => {
    Object.defineProperty(document, 'cookie', {
      value: 'MY-COOKIE=hello',
      writable: true,
    });

    expect(getCookieValue('MY-COOKIE')).toBe('hello');
  });

  it('returns undefined for a missing cookie', () => {
    Object.defineProperty(document, 'cookie', {
      value: 'OTHER=val',
      writable: true,
    });

    expect(getCookieValue('MY-COOKIE')).toBeUndefined();
  });

  it('handles URI-encoded values', () => {
    Object.defineProperty(document, 'cookie', {
      value: 'TOKEN=a%3Db%3Dc',
      writable: true,
    });

    expect(getCookieValue('TOKEN')).toBe('a=b=c');
  });

  it('finds the correct cookie among multiple', () => {
    Object.defineProperty(document, 'cookie', {
      value: 'A=1; TARGET=found; B=2',
      writable: true,
    });

    expect(getCookieValue('TARGET')).toBe('found');
  });

  it('returns undefined when document.cookie is empty', () => {
    Object.defineProperty(document, 'cookie', { value: '', writable: true });

    expect(getCookieValue('ANY')).toBeUndefined();
  });
});
