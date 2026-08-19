/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useEffect, useState } from 'react';
import { useLeftNavCollapsed } from 'MainRoot/nosc/shell/useLeftNavCollapsed';

/**
 * Shared layout constants for the Nexus One Preview shell.
 *
 * Every Preview page is rendered as a fixed-position overlay sitting
 * below the 56px TopNav and to the right of the LeftNav. The LeftNav
 * width is dynamic (256px expanded / 64px collapsed) so the page's
 * `left` offset must follow it — otherwise collapsing the rail leaves
 * a 192px gap of empty background between the rail and the page.
 *
 * `useLeftNavCollapsed` already broadcasts via a CustomEvent so all
 * consumers (LeftNav, TopNav hamburger, every Preview page) stay in
 * sync without any extra wiring.
 *
 * Per UX-F2-007 (TopNav height) + UX-F3-001..007 (LeftNav widths).
 */
export const TOP_NAV_HEIGHT_PX = 56;
export const LEFT_NAV_EXPANDED_WIDTH_PX = 256;
export const LEFT_NAV_COLLAPSED_WIDTH_PX = 64;
/** Page surfaces sit below shell chrome (LeftNav=100, TopNav wrapper=1000). */
export const PAGE_CONTENT_Z_INDEX = 1;

/**
 * Single source of truth for the shell's inline-style z-index literals
 * (`NexusOneShellLayout`'s `<Theme>` wrapper and TopNav's fixed wrapper, and
 * `NoticeStrip`) — React inline styles are set via the CSSOM and can't
 * reliably consume a CSS custom property, so these can't share the
 * `--nexus-one-z-popper` var `nexus-one.css` uses for everything above them.
 * Full shell z-index ladder (low to high): classic mount base 1, shell Theme
 * 100, TopNav wrapper 1000, NoticeStrip 1010, poppers/dialogs
 * (`--nexus-one-z-popper`) 1100, toasts 1120 — see `nexus-one.css` for the
 * CSS-side half of the ladder.
 */
export const SHELL_THEME_Z_INDEX = 100;
export const TOP_NAV_WRAPPER_Z_INDEX = 1000;
export const NOTICE_STRIP_Z_INDEX = 1010;

/**
 * CSS custom property carrying the notice strip's live height in pixels
 * (unitless number as text, e.g. `"48"`), set on `document.documentElement`
 * by {@link NoticeStrip}'s `ResizeObserver`. Read directly by CSS consumers
 * (`.nosc-toast-host`) via `calc(var(--nosc-notice-strip-height) * 1px)`.
 */
export const NOTICE_STRIP_HEIGHT_CSS_VAR = '--nosc-notice-strip-height';

const NOTICE_STRIP_HEIGHT_CHANGE_EVENT = 'nosc.noticeStripHeight.change';

/** In-memory mirror of the CSS var so late-mounting hook consumers read the current value, not 0. */
let currentNoticeStripHeightPx = 0;

/**
 * Publishes the notice strip's measured height to both CSS (custom property)
 * and JS (React-state) consumers from a single call site — the one source of
 * truth `previewShellLayout.ts`'s doc comment calls for instead of a second
 * magic number. Called by {@link NoticeStrip}'s `ResizeObserver` callback.
 */
export function publishNoticeStripHeight(heightPx: number): void {
  currentNoticeStripHeightPx = heightPx;
  if (typeof document !== 'undefined') {
    document.documentElement.style.setProperty(NOTICE_STRIP_HEIGHT_CSS_VAR, String(heightPx));
  }
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent(NOTICE_STRIP_HEIGHT_CHANGE_EVENT, { detail: { heightPx } }));
  }
}

/**
 * Resets the module-level notice strip height to 0 for test isolation.
 *
 * Jest module registries can be shared across test files run in the same worker,
 * so a test file that renders a consumer of `useNoticeStripHeight`, `useShellTopOffsetPx`,
 * `usePreviewShellOffsets`, or any component using them without resetting this value
 * in `beforeEach` could inherit a stale non-zero height from a previous test file.
 * Call this helper in `beforeEach` to avoid cross-test-file pollution.
 */
export function resetNoticeStripHeightForTesting(): void {
  publishNoticeStripHeight(0);
}

/** Live notice strip height in pixels; 0 when no notice is visible. */
export function useNoticeStripHeight(): number {
  const [heightPx, setHeightPx] = useState<number>(currentNoticeStripHeightPx);

  useEffect(() => {
    if (typeof window === 'undefined') return;
    const onChange = (e: Event): void => {
      const detail = (e as CustomEvent<{ heightPx: number }>).detail;
      if (typeof detail?.heightPx === 'number') setHeightPx(detail.heightPx);
    };
    window.addEventListener(NOTICE_STRIP_HEIGHT_CHANGE_EVENT, onChange);
    return () => window.removeEventListener(NOTICE_STRIP_HEIGHT_CHANGE_EVENT, onChange);
  }, []);

  return heightPx;
}

/**
 * Live page-content offset that tracks the LeftNav's collapsed state.
 * Spread the returned object into the wrapping <Theme>'s style:
 *
 *   const offsets = usePreviewShellOffsets();
 *   <Theme style={{ position: 'fixed', right: 0, bottom: 0, ...offsets }}>
 *     ...
 *   </Theme>
 *
 * Includes a CSS transition on `left` matching the LeftNav's own 120ms
 * width transition so the rail and the page edge slide in unison.
 */
export interface PreviewShellOffsets {
  readonly top: number;
  readonly left: number;
  readonly zIndex: number;
  /** Re-enable clicks on the page surface; the UIView wrapper is pointer-events:none. */
  readonly pointerEvents: 'auto';
  readonly transition: string;
}

export function usePreviewShellOffsets(): PreviewShellOffsets {
  const [collapsed] = useLeftNavCollapsed();
  return {
    top: useShellTopOffsetPx(),
    left: collapsed ? LEFT_NAV_COLLAPSED_WIDTH_PX : LEFT_NAV_EXPANDED_WIDTH_PX,
    zIndex: PAGE_CONTENT_Z_INDEX,
    pointerEvents: 'auto',
    transition: 'left 120ms ease',
  };
}

/**
 * Live offset (in px) TopNav's fixed wrapper and LeftNav must sit below the
 * notice strip. Single source of truth for the `TOP_NAV_HEIGHT_PX +
 * noticeStripHeightPx` formula — {@link usePreviewShellOffsets} defers to
 * this instead of recomputing it, so the sum is expressed once.
 */
export function useShellTopOffsetPx(): number {
  return TOP_NAV_HEIGHT_PX + useNoticeStripHeight();
}
