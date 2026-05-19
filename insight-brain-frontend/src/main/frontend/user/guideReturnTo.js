/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Reads (and clears) a sessionStorage entry written by Guide before its
// redirect to `/`. After a successful session fetch, the legacy IQ SPA
// uses this to bounce the user back to the Guide URL they originally
// requested.
//
// Counterpart writer lives in `src/main/frontend/guide/auth/guideReturnTo.ts`.
// The shared sessionStorage key string ('iqGuideReturnTo') is intentionally
// duplicated in both files; do not "DRY" them by introducing a cross-bundle
// import. See docs/superpowers/specs/2026-05-19-guide-uses-iq-login-design.md
// for rationale.

const GUIDE_RETURN_TO_KEY = 'iqGuideReturnTo';

export function consumeGuideReturnTo() {
  const value = sessionStorage.getItem(GUIDE_RETURN_TO_KEY);
  if (value === null) return null;
  sessionStorage.removeItem(GUIDE_RETURN_TO_KEY);
  try {
    const url = new URL(value, window.location.origin);
    if (url.origin !== window.location.origin) return null;
    // This prefix must stay in sync with the Guide SPA's esbuild output path
    // and the servlet mapping that serves it. If the deployment path changes,
    // update this constant — otherwise the redirect-back feature silently
    // stops working (consumeGuideReturnTo returns null with no error).
    if (!url.pathname.startsWith('/assets/guide/')) return null;
    return url.href;
  } catch {
    return null;
  }
}
