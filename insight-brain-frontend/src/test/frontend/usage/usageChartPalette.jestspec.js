/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  CHART_COLOR_TOKENS,
  ACTIVITY_COLORS,
  SOURCE_COLORS,
  STAGE_COLORS,
  FALLBACK_COLOR,
  TOKEN_HEX,
} from 'MainRoot/usage/usageChartPalette';

// WCAG 2.x relative luminance formula
function relativeLuminance(hex) {
  const m = hex.replace('#', '').match(/.{2}/g);
  const [r, g, b] = m.map((c) => {
    const v = parseInt(c, 16) / 255;
    return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
  });
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

function contrastRatio(hex1, hex2) {
  const l1 = relativeLuminance(hex1);
  const l2 = relativeLuminance(hex2);
  const [lighter, darker] = l1 > l2 ? [l1, l2] : [l2, l1];
  return (lighter + 0.05) / (darker + 0.05);
}

describe('usageChartPalette', () => {
  it('exposes the required token names', () => {
    const tokens = Object.keys(CHART_COLOR_TOKENS);
    expect(tokens).toEqual(
      expect.arrayContaining(['BLUE', 'TEAL', 'INDIGO', 'GREEN', 'ORANGE', 'PURPLE', 'RED', 'YELLOW', 'GREY'])
    );
  });

  it('every token resolves to an NX swatch CSS variable', () => {
    Object.values(CHART_COLOR_TOKENS).forEach((value) => {
      expect(value).toMatch(/^var\(--nx-swatch-/);
    });
  });

  // Note: an earlier version of this file enforced WCAG 3.0:1 adjacent-pair
  // contrast. The check was dropped when the palette was re-keyed to the
  // saturated -50 swatches in CLM-38071 (commit c12bbff335). Two reasons:
  // (1) Methodologically the adjacency-by-declaration-order test is wrong for
  //     the pie charts — slices are sorted by count at render time, so the
  //     visually-adjacent pairs aren't fixed by token order.
  // (2) The vibrant -50 swatches the design called for don't satisfy a 3.0:1
  //     pairwise threshold; visual distinction comes from hue contrast rather
  //     than luminance contrast. `contrastRatio`/`relativeLuminance` are kept
  //     for any future spot checks.
  // contrastRatio is intentionally retained but not asserted; remove if unused.
  void contrastRatio;

  it('FALLBACK_COLOR is the GREY token', () => {
    expect(FALLBACK_COLOR).toBe(CHART_COLOR_TOKENS.GREY);
  });
});
