/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Writes the current Guide URL to sessionStorage so the legacy IQ shell can
// bounce the user back here after they sign in via LoginModal.
//
// Counterpart reader lives in `src/main/frontend/user/guideReturnTo.js`.
// The shared sessionStorage key string ('iqGuideReturnTo') is intentionally
// duplicated in both files; do not "DRY" them by introducing a cross-bundle
// import. See docs/superpowers/specs/2026-05-19-guide-uses-iq-login-design.md
// for rationale.

const GUIDE_RETURN_TO_KEY = 'iqGuideReturnTo';

export function captureGuideReturnTo(): void {
  sessionStorage.setItem(GUIDE_RETURN_TO_KEY, window.location.href);
}
