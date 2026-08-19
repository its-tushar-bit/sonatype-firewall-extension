/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { KeyboardEvent } from 'react';

/**
 * Returns an onKeyDown handler that invokes `action` on Enter or Space.
 *
 * Use on non-button clickable elements (a `div`, Radix `Card`, `Flex`, etc.)
 * that also carry `role="button"` and `tabIndex={0}` so they are operable by
 * keyboard, not just pointer (WCAG 2.1.1). Space is preventDefault-ed to stop
 * the page from scrolling.
 */
export function activateOnKey(action: () => void) {
  return (event: KeyboardEvent): void => {
    if (event.key === 'Enter' || event.key === ' ' || event.key === 'Spacebar') {
      event.preventDefault();
      action();
    }
  };
}
