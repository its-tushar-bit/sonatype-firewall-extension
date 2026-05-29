/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

const RETRY_COUNT_KEY = 'guide_error_retry_count';

// sessionStorage can throw SecurityError in some browsers (Safari ITP,
// enterprise policies). These helpers fall back silently so that a blocked
// storage context never crashes the error page itself.
function safeGetCount(): number {
  try {
    return parseInt(sessionStorage.getItem(RETRY_COUNT_KEY) || '0', 10);
  } catch {
    return 0;
  }
}

function safeSetCount(count: number): void {
  try {
    sessionStorage.setItem(RETRY_COUNT_KEY, String(count));
  } catch {
    // storage blocked — counter won't persist across reloads, but won't crash
  }
}

export function reloadPage() {
  safeSetCount(safeGetCount() + 1);
  window.location.reload();
}

export function clearErrorRetries() {
  try {
    sessionStorage.removeItem(RETRY_COUNT_KEY);
  } catch {
    // storage blocked — ignore
  }
}

export function getErrorRetryCount(): number {
  return safeGetCount();
}
