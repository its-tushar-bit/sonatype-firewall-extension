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

const PAGE_BG_LIGHT = '#FFFFFF';

describe('usageChartPalette', () => {
  it('exposes only canonical, suffixed token names (legacy aliases removed)', () => {
    expect(Object.keys(CHART_COLOR_TOKENS).sort()).toEqual([
      'BLUE_50',
      'GREEN_30',
      'GREY_40',
      'INDIGO_50',
      'ORANGE_40',
      'PURPLE_50',
      'RED_50',
      'TEAL_30',
      'YELLOW_50',
    ]);
  });

  it('every token resolves to an NX swatch CSS variable', () => {
    Object.values(CHART_COLOR_TOKENS).forEach((value) => {
      expect(value).toMatch(/^var\(--nx-swatch-/);
    });
  });

  it('FALLBACK_COLOR is the GREY_40 token', () => {
    expect(FALLBACK_COLOR).toBe(CHART_COLOR_TOKENS.GREY_40);
  });

  it('TOKEN_HEX maps every unique CSS variable to a correct hex string', () => {
    const uniqueVars = new Set(Object.values(CHART_COLOR_TOKENS));
    uniqueVars.forEach((cssVar) => {
      expect(TOKEN_HEX[cssVar]).toMatch(/^#[0-9A-Fa-f]{6}$/);
    });
  });

  // Per-row map identity. it.each so a single regression doesn't hide siblings
  // and the failure message names the offending key.
  it.each(Object.entries(ACTIVITY_COLORS))('ACTIVITY_COLORS[%s] = %s', (key, expected) => {
    expect(ACTIVITY_COLORS[key]).toBe(expected);
  });

  it('ACTIVITY_COLORS shape matches the canonical mapping', () => {
    expect(ACTIVITY_COLORS).toEqual({
      'App Scan + Re-evaluate': CHART_COLOR_TOKENS.BLUE_50,
      'Continuous Monitoring': CHART_COLOR_TOKENS.TEAL_30,
      'Component Details': CHART_COLOR_TOKENS.INDIGO_50,
      'Version Recommendations': CHART_COLOR_TOKENS.GREEN_30,
      APIs: CHART_COLOR_TOKENS.ORANGE_40,
      'Reachability Analysis': CHART_COLOR_TOKENS.PURPLE_50,
    });
  });

  it.each(Object.entries(SOURCE_COLORS))('SOURCE_COLORS[%s] = %s', (key, expected) => {
    expect(SOURCE_COLORS[key]).toBe(expected);
  });

  it('SOURCE_COLORS shape matches the canonical mapping', () => {
    expect(SOURCE_COLORS).toEqual({
      CI_CD: CHART_COLOR_TOKENS.BLUE_50,
      IDE: CHART_COLOR_TOKENS.TEAL_30,
      CLI: CHART_COLOR_TOKENS.INDIGO_50,
      REPO_MANAGER: CHART_COLOR_TOKENS.GREEN_30,
      CONTINUOUS_MONITOR: CHART_COLOR_TOKENS.ORANGE_40,
      UI: CHART_COLOR_TOKENS.PURPLE_50,
      API: CHART_COLOR_TOKENS.RED_50,
      UNKNOWN: CHART_COLOR_TOKENS.GREY_40,
    });
  });

  it.each(Object.entries(STAGE_COLORS))('STAGE_COLORS[%s] = %s', (key, expected) => {
    expect(STAGE_COLORS[key]).toBe(expected);
  });

  it('STAGE_COLORS shape matches the canonical mapping', () => {
    expect(STAGE_COLORS).toEqual({
      build: CHART_COLOR_TOKENS.BLUE_50,
      'stage-release': CHART_COLOR_TOKENS.TEAL_30,
      release: CHART_COLOR_TOKENS.INDIGO_50,
      operate: CHART_COLOR_TOKENS.GREEN_30,
      develop: CHART_COLOR_TOKENS.ORANGE_40,
      'continuous-monitoring': CHART_COLOR_TOKENS.PURPLE_50,
      proxy: CHART_COLOR_TOKENS.RED_50,
      Unknown: CHART_COLOR_TOKENS.GREY_40,
    });
  });

  // WCAG 4.5:1 contrast ratio tests — light page background (#FFFFFF)
  // All chart colors must be legible on white background (legend swatches, chart fills).

  it.each([
    ['App Scan + Re-evaluate', ACTIVITY_COLORS['App Scan + Re-evaluate']],
    ['Continuous Monitoring', ACTIVITY_COLORS['Continuous Monitoring']],
    ['Component Details', ACTIVITY_COLORS['Component Details']],
    ['Version Recommendations', ACTIVITY_COLORS['Version Recommendations']],
    ['APIs', ACTIVITY_COLORS['APIs']],
    ['Reachability Analysis', ACTIVITY_COLORS['Reachability Analysis']],
  ])('%s ACTIVITY color passes WCAG 4.5:1 vs light page background', (_name, token) => {
    const hex = TOKEN_HEX[token];
    expect(hex).toBeDefined();
    expect(contrastRatio(hex, PAGE_BG_LIGHT)).toBeGreaterThanOrEqual(4.5);
  });

  it.each([
    ['UI', SOURCE_COLORS.UI],
    ['CLI', SOURCE_COLORS.CLI],
    ['REPO_MANAGER', SOURCE_COLORS.REPO_MANAGER],
    ['CI_CD', SOURCE_COLORS.CI_CD],
    ['IDE', SOURCE_COLORS.IDE],
    ['CONTINUOUS_MONITOR', SOURCE_COLORS.CONTINUOUS_MONITOR],
    ['API', SOURCE_COLORS.API],
    ['UNKNOWN', SOURCE_COLORS.UNKNOWN],
  ])('%s SOURCE color passes WCAG 4.5:1 vs light page background', (_name, token) => {
    const hex = TOKEN_HEX[token];
    expect(hex).toBeDefined();
    expect(contrastRatio(hex, PAGE_BG_LIGHT)).toBeGreaterThanOrEqual(4.5);
  });

  it.each([
    ['build', STAGE_COLORS.build],
    ['stage-release', STAGE_COLORS['stage-release']],
    ['release', STAGE_COLORS.release],
    ['operate', STAGE_COLORS.operate],
    ['develop', STAGE_COLORS.develop],
    ['continuous-monitoring', STAGE_COLORS['continuous-monitoring']],
    ['proxy', STAGE_COLORS.proxy],
    ['Unknown', STAGE_COLORS.Unknown],
  ])('%s STAGE color passes WCAG 4.5:1 vs light page background', (_name, token) => {
    const hex = TOKEN_HEX[token];
    expect(hex).toBeDefined();
    expect(contrastRatio(hex, PAGE_BG_LIGHT)).toBeGreaterThanOrEqual(4.5);
  });
});
