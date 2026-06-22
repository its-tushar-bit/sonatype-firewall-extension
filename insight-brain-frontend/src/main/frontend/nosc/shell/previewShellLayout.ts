/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
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
    top: TOP_NAV_HEIGHT_PX,
    left: collapsed ? LEFT_NAV_COLLAPSED_WIDTH_PX : LEFT_NAV_EXPANDED_WIDTH_PX,
    zIndex: PAGE_CONTENT_Z_INDEX,
    pointerEvents: 'auto',
    transition: 'left 120ms ease',
  };
}
