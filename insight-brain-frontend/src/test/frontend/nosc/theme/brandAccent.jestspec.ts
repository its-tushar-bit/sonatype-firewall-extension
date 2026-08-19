/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { BRAND_ACCENT } from 'MainRoot/nosc/theme';

describe('BRAND_ACCENT', () => {
  it('is exported', () => {
    expect(BRAND_ACCENT).toBeDefined();
  });

  it('is a valid Radix accent color', () => {
    // Radix `AccentColor` enum as of @radix-ui/themes v3.3 (point-in-time
    // 2026-05-10). If Radix adds an accent in a future version this list
    // goes stale; the right fix is to import the type, not extend the list.
    // Kept as a literal here for an explicit-failure mode.
    const validAccents = [
      'gray', 'gold', 'bronze', 'brown', 'yellow', 'amber', 'orange',
      'tomato', 'red', 'ruby', 'crimson', 'pink', 'plum', 'purple',
      'violet', 'iris', 'indigo', 'blue', 'cyan', 'teal', 'jade',
      'green', 'grass', 'lime', 'mint', 'sky',
    ] as const;
    expect(validAccents).toContain(BRAND_ACCENT);
  });
});
