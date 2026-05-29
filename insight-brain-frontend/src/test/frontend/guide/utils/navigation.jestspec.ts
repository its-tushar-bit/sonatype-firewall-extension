/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { reloadPage, clearErrorRetries, getErrorRetryCount } from 'GuideRoot/utils/navigation';

const reloadMock = jest.fn();

beforeAll(() => {
  // Replace the entire window.location so reload is a controllable jest.fn().
  // We do this at the object level (not via spyOn) to avoid the jsdom
  // non-configurable property restriction that breaks jest.restoreAllMocks().
  Object.defineProperty(window, 'location', {
    configurable: true,
    writable: true,
    value: { reload: reloadMock },
  });
});

afterEach(() => {
  reloadMock.mockClear();
  sessionStorage.clear();
});

describe('getErrorRetryCount', () => {
  it('returns 0 when nothing has been stored', () => {
    expect(getErrorRetryCount()).toBe(0);
  });

  it('returns the count that is currently in sessionStorage', () => {
    sessionStorage.setItem('guide_error_retry_count', '5');
    expect(getErrorRetryCount()).toBe(5);
  });
});

describe('reloadPage', () => {
  it('increments the retry counter in sessionStorage on each call', () => {
    reloadPage();
    expect(getErrorRetryCount()).toBe(1);
    reloadPage();
    expect(getErrorRetryCount()).toBe(2);
  });

  it('calls window.location.reload', () => {
    reloadPage();
    expect(reloadMock).toHaveBeenCalledTimes(1);
  });

  it('increments from an existing non-zero count', () => {
    sessionStorage.setItem('guide_error_retry_count', '2');
    reloadPage();
    expect(getErrorRetryCount()).toBe(3);
  });
});

describe('clearErrorRetries', () => {
  it('resets the counter to zero', () => {
    sessionStorage.setItem('guide_error_retry_count', '3');
    clearErrorRetries();
    expect(getErrorRetryCount()).toBe(0);
  });

  it('is safe to call when no count is stored', () => {
    expect(() => clearErrorRetries()).not.toThrow();
    expect(getErrorRetryCount()).toBe(0);
  });
});
