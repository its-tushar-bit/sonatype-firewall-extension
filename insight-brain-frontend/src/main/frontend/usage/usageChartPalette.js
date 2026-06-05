/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// 7-token qualitative palette drawn from NX swatches. Uses -50 swatches
// (darker, more saturated) for better visual impact on both light and dark themes.
// Matches the color approach from CLM-38071 for consistency with Lifecycle tiers.
export const CHART_COLOR_TOKENS = {
  BLUE: 'var(--nx-swatch-blue-50)',
  TEAL: 'var(--nx-swatch-teal-50)',
  INDIGO: 'var(--nx-swatch-indigo-50)',
  GREEN: 'var(--nx-swatch-green-50)',
  ORANGE: 'var(--nx-swatch-orange-50)',
  PURPLE: 'var(--nx-swatch-purple-50)',
  RED: 'var(--nx-swatch-red-50)',
  YELLOW: 'var(--nx-swatch-yellow-50)',
  GREY: 'var(--nx-swatch-grey-50)',
};

// Resolved hex values for each token. Used only by jest contrast tests
// (jsdom doesn't resolve CSS variables); production code uses the var() forms.
// Values derived from NX swatch CSS variable definitions in
// @sonatype/react-shared-components/base-styles/_nx-color-swatches.scss
export const TOKEN_HEX = {
  'var(--nx-swatch-blue-50)': '#1363EC',
  'var(--nx-swatch-teal-50)': '#00A3B5',
  'var(--nx-swatch-indigo-50)': '#475EB8',
  'var(--nx-swatch-green-50)': '#00852D',
  'var(--nx-swatch-orange-50)': '#E86F28',
  'var(--nx-swatch-purple-50)': '#5813EC',
  'var(--nx-swatch-red-50)': '#C8102E',
  'var(--nx-swatch-yellow-50)': '#F5A623',
  'var(--nx-swatch-grey-50)': '#6A6A6A',
};

// Activity-key → token (matches ACTIVITY_KEYS in ConsumptionChart.jsx).
// Using saturated -50 swatches for consistent visual impact.
export const ACTIVITY_COLORS = {
  'App Scan + Re-evaluate': CHART_COLOR_TOKENS.BLUE,
  'Continuous Monitoring': CHART_COLOR_TOKENS.TEAL,
  'Component Details': CHART_COLOR_TOKENS.INDIGO,
  'Version Recommendations': CHART_COLOR_TOKENS.GREEN,
  APIs: CHART_COLOR_TOKENS.ORANGE,
  'Reachability Analysis': CHART_COLOR_TOKENS.PURPLE,
};

// Source-token → token (matches SOURCE_LABELS keys in ConsumptionBySourceChart.jsx).
// Uses saturated -50 swatches for vibrant, distinct categories.
export const SOURCE_COLORS = {
  CI_CD: CHART_COLOR_TOKENS.BLUE,
  IDE: CHART_COLOR_TOKENS.TEAL,
  CLI: CHART_COLOR_TOKENS.INDIGO,
  REPO_MANAGER: CHART_COLOR_TOKENS.GREEN,
  CONTINUOUS_MONITOR: CHART_COLOR_TOKENS.ORANGE,
  UI: CHART_COLOR_TOKENS.PURPLE,
  API: CHART_COLOR_TOKENS.RED,
  UNKNOWN: CHART_COLOR_TOKENS.GREY,
};

// Stage-token → token. Stage tokens are the raw values stored in
// policy_evaluation.stage_type_id (lowercase, hyphenated).
// Uses saturated -50 swatches for vibrant, distinct categories.
export const STAGE_COLORS = {
  build: CHART_COLOR_TOKENS.BLUE,
  'stage-release': CHART_COLOR_TOKENS.TEAL,
  release: CHART_COLOR_TOKENS.INDIGO,
  operate: CHART_COLOR_TOKENS.GREEN,
  develop: CHART_COLOR_TOKENS.ORANGE,
  'continuous-monitoring': CHART_COLOR_TOKENS.PURPLE,
  proxy: CHART_COLOR_TOKENS.RED,
  Unknown: CHART_COLOR_TOKENS.GREY,
};

export const FALLBACK_COLOR = CHART_COLOR_TOKENS.GREY;
