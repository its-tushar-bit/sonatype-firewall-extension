/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * jsdom does not implement several browser APIs that Radix's DropdownMenu /
 * Tooltip (and the embedded ScrollArea) rely on during open / close. Without
 * these shims `userEvent.click` on a trigger throws an opaque AggregateError.
 * The gaps are well-documented: `ResizeObserver` (jsdom #3368), Pointer
 * Capture (jsdom #3209), and `scrollIntoView` (jsdom #1695).
 *
 * Call once from `beforeAll` in any spec that opens a Radix popper.
 */
export function installRadixJsdomShims(): void {
  if (typeof (globalThis as any).ResizeObserver === 'undefined') {
    (globalThis as any).ResizeObserver = class {
      observe(): void {}
      unobserve(): void {}
      disconnect(): void {}
    };
  }
  if (!Element.prototype.hasPointerCapture) {
    Element.prototype.hasPointerCapture = (): boolean => false;
  }
  if (!Element.prototype.setPointerCapture) {
    Element.prototype.setPointerCapture = (): void => undefined;
  }
  if (!Element.prototype.releasePointerCapture) {
    Element.prototype.releasePointerCapture = (): void => undefined;
  }
  if (!Element.prototype.scrollIntoView) {
    Element.prototype.scrollIntoView = (): void => undefined;
  }
}
