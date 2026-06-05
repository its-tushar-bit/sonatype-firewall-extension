/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Formats a number with thousands separators for full-precision display.
 * @param {number|null|undefined} num
 * @returns {string}
 */
export function formatNumber(num) {
  if (num === null || num === undefined) {
    return '0';
  }
  return num.toLocaleString('en-US');
}

/**
 * Compact-suffix formatter for narrow UI contexts (sidebar tile, chart axis).
 * - <1000: as-is (e.g. 638)
 * - 1000–9999: 1 decimal place + "k", rounded to nearest hundred (e.g. 1.6k, 9.9k)
 * - 10000–999999: rounded integer + "k" (e.g. 25k, 650k)
 * - >=1000000: 1 decimal place + "M", no decimal when integral (e.g. 1.2M, 25M)
 *
 * @param {number} value
 * @returns {string}
 */
export function formatCount(value) {
  if (value >= 1000000) {
    const m = value / 1000000;
    return Number.isInteger(m) ? `${m}M` : `${m.toFixed(1)}M`;
  }
  if (value >= 10000) {
    return `${Math.round(value / 1000)}k`;
  }
  if (value >= 1000) {
    // 1k–10k band: 1 decimal place rounded down to nearest hundred so that
    // 9999 stays in the band as "9.9k" rather than spilling to "10k".
    const k = Math.floor(value / 100) / 10;
    return Number.isInteger(k) ? `${k}k` : `${k.toFixed(1)}k`;
  }
  return String(value);
}

/**
 * Rounds a percentage to the nearest integer and appends "%".
 * @param {number} pct
 * @returns {string}
 */
export function formatPercent(pct) {
  return `${Math.round(pct)}%`;
}
