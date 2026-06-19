/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// 9-token qualitative palette drawn from NX swatches. Uses WCAG-passing lightness
// variants selected to achieve >= 4.5:1 contrast ratio against a white (#FFFFFF)
// page background. Saturated -50 swatches are replaced where they fail: teal/green
// swapped to -30, orange to -40, grey to -40. Matches CLM-40967 palette decisions.
export const CHART_COLOR_TOKENS = {
  // Named tokens with explicit lightness suffix — canonical, WCAG-passing.
  // Marginal-threshold escape route: if a future RSC version nudges any
  // hue/sat enough to drop a token below 4.5:1, swap to the next darker
  // lightness step (e.g. ORANGE_40 → ORANGE_30 at 7.03:1).
  BLUE_50: 'var(--nx-swatch-blue-50)', // 5.22:1 vs white ✓
  TEAL_30: 'var(--nx-swatch-teal-30)', // 5.69:1 vs white ✓ (teal-50 failed at 2.28)
  INDIGO_50: 'var(--nx-swatch-indigo-50)', // 5.90:1 vs white ✓
  GREEN_30: 'var(--nx-swatch-green-30)', // 4.60:1 vs white ✓ (green-50 failed at 1.75)
  ORANGE_40: 'var(--nx-swatch-orange-40)', // 4.50:1 vs white ✓ (marginal — exactly at the AA threshold; documented fallback if RSC shifts the hex: orange-30 at 7.03:1) (orange-50 failed at 3.00)
  PURPLE_50: 'var(--nx-swatch-purple-50)', // 7.72:1 vs white ✓
  RED_50: 'var(--nx-swatch-red-50)', // 4.65:1 vs white ✓
  YELLOW_50: 'var(--nx-swatch-yellow-50)', // unused in categories; retained as a token but parametric WCAG tests will fail it (1.79:1) if a category map ever references it — see WARN comment in TOKEN_HEX
  GREY_40: 'var(--nx-swatch-grey-40)', // 5.45:1 vs white ✓ (grey-50 failed at 3.73)
};

// Resolved hex values for each token. Used only by jest contrast tests
// (jsdom doesn't resolve CSS variables); production code uses the var() forms.
// Values computed from RSC hsl() definitions in
// @sonatype/react-shared-components/base-styles/_nx-color-swatches.scss.
// HSL hue/sat from --nx-swatch-<color>-hs; lightness from suffix (e.g. -30 = 30%).
export const TOKEN_HEX = {
  'var(--nx-swatch-blue-50)': '#1363EC', // hsl(218, 85%, 50%)
  'var(--nx-swatch-teal-30)': '#006E99', // hsl(197, 100%, 30%)
  'var(--nx-swatch-indigo-50)': '#475EB8', // hsl(228, 44%, 50%)
  'var(--nx-swatch-green-30)': '#138654', // hsl(154, 75%, 30%)
  'var(--nx-swatch-orange-40)': '#C85204', // hsl(24, 96%, 40%)
  'var(--nx-swatch-purple-50)': '#5813EC', // hsl(259, 85%, 50%)
  'var(--nx-swatch-red-50)': '#E61919', // hsl(0, 80%, 50%)
  // WARN: yellow-50 fails 4.5:1 vs white (1.79:1). Do NOT add to category maps without swapping to yellow-30 (4.71:1). Parametric WCAG tests only iterate category maps.
  'var(--nx-swatch-yellow-50)': '#FFB200', // hsl(42, 100%, 50%) — banker's-rounded; either #FFB200 or #FFB300 is within ±0.5 LSB of the mathematical value
  'var(--nx-swatch-grey-40)': '#6A6A62', // hsl(60, 4%, 40%)
};

// Activity-key → token (matches ACTIVITY_KEYS in ConsumptionChart.jsx).
// All swatches pass WCAG 4.5:1 vs white page background.
export const ACTIVITY_COLORS = {
  'App Scan + Re-evaluate': CHART_COLOR_TOKENS.BLUE_50,
  'Continuous Monitoring': CHART_COLOR_TOKENS.TEAL_30,
  'Component Details': CHART_COLOR_TOKENS.INDIGO_50,
  'Version Recommendations': CHART_COLOR_TOKENS.GREEN_30,
  APIs: CHART_COLOR_TOKENS.ORANGE_40,
  'Reachability Analysis': CHART_COLOR_TOKENS.PURPLE_50,
};

// Source-token → token (matches SOURCE_LABELS keys in ConsumptionBySourceChart.jsx).
// All swatches pass WCAG 4.5:1 vs white page background.
export const SOURCE_COLORS = {
  CI_CD: CHART_COLOR_TOKENS.BLUE_50,
  IDE: CHART_COLOR_TOKENS.TEAL_30,
  CLI: CHART_COLOR_TOKENS.INDIGO_50,
  REPO_MANAGER: CHART_COLOR_TOKENS.GREEN_30,
  CONTINUOUS_MONITOR: CHART_COLOR_TOKENS.ORANGE_40,
  UI: CHART_COLOR_TOKENS.PURPLE_50,
  API: CHART_COLOR_TOKENS.RED_50,
  UNKNOWN: CHART_COLOR_TOKENS.GREY_40,
};

// Stage-token → token. Stage tokens are the raw values stored in
// policy_evaluation.stage_type_id (lowercase, hyphenated).
// All swatches pass WCAG 4.5:1 vs white page background.
export const STAGE_COLORS = {
  build: CHART_COLOR_TOKENS.BLUE_50,
  'stage-release': CHART_COLOR_TOKENS.TEAL_30,
  release: CHART_COLOR_TOKENS.INDIGO_50,
  operate: CHART_COLOR_TOKENS.GREEN_30,
  develop: CHART_COLOR_TOKENS.ORANGE_40,
  'continuous-monitoring': CHART_COLOR_TOKENS.PURPLE_50,
  proxy: CHART_COLOR_TOKENS.RED_50,
  Unknown: CHART_COLOR_TOKENS.GREY_40,
};

export const FALLBACK_COLOR = CHART_COLOR_TOKENS.GREY_40;
