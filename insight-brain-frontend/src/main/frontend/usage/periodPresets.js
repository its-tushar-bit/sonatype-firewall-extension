/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import moment from 'moment';

// The "last calendar month" preset is intentionally calendar-month, not
// subscription-day-anchored billing window. Computing the actual last billing
// window requires `subscriptionDayOfMonth` which only the backend knows; doing
// it client-side would silently approximate. If a true "last billing period"
// preset is wanted later, mirror the `currentBillingPeriod` pattern and have
// the backend resolve { null, null } against the previous window via a new
// `?previousBillingPeriod=true` query param or similar.
export const PERIOD_PRESETS = [
  { key: 'currentBillingPeriod', label: 'Current billing period' },
  { key: 'lastCalendarMonth', label: 'Last calendar month' },
  { key: 'last30Days', label: 'Last 30 days' },
  { key: 'last90Days', label: 'Last 90 days' },
  { key: 'custom', label: 'Custom range…' },
];

export const defaultPresetKey = 'currentBillingPeriod';

/**
 * Returns { startDate, endDate } as 'YYYY-MM-DD' strings, or { null, null } for the
 * 'currentBillingPeriod' preset (which defers to the backend's billing-window default).
 *
 * @param {string} presetKey
 * @param {Date} [today] - Injectable for tests; defaults to current date.
 * @returns {{ startDate: string|null, endDate: string|null }}
 */
export function presetToRange(presetKey, today = new Date()) {
  // Accept a Date, a moment object, or an ISO string. When a Date object is
  // passed, wrap it without startOf('day') conversion so timezone-shifted tests
  // can pass a moment directly. When it's a plain string, parse in local timezone.
  const todayMoment = moment.isMoment(today)
    ? today.clone().startOf('day')
    : typeof today === 'string'
    ? moment(today, 'YYYY-MM-DD').startOf('day')
    : moment(today).startOf('day');

  switch (presetKey) {
    case 'currentBillingPeriod':
      return { startDate: null, endDate: null };

    case 'lastCalendarMonth': {
      const startOfLastMonth = todayMoment.clone().subtract(1, 'month').startOf('month');
      const endOfLastMonth = todayMoment.clone().subtract(1, 'month').endOf('month');
      return {
        startDate: startOfLastMonth.format('YYYY-MM-DD'),
        endDate: endOfLastMonth.format('YYYY-MM-DD'),
      };
    }

    case 'last30Days': {
      const start = todayMoment.clone().subtract(29, 'days');
      return {
        startDate: start.format('YYYY-MM-DD'),
        endDate: todayMoment.format('YYYY-MM-DD'),
      };
    }

    case 'last90Days': {
      const start = todayMoment.clone().subtract(89, 'days');
      return {
        startDate: start.format('YYYY-MM-DD'),
        endDate: todayMoment.format('YYYY-MM-DD'),
      };
    }

    case 'custom':
      throw new Error('Cannot compute a range for "custom" preset — use setPeriodRange directly');

    default:
      throw new Error(`Unknown preset key: "${presetKey}"`);
  }
}

/**
 * Formats a period range as a human-readable label.
 * - null/null   → "Current billing period"
 * - same year   → "Jun 1 - Jun 30, 2026"
 * - cross year  → "Dec 15, 2025 - Jan 15, 2026"
 *
 * @param {{ startDate: string|null, endDate: string|null }} range
 * @returns {string}
 */
export function formatRangeLabel(range) {
  const { startDate, endDate } = range;

  if (!startDate || !endDate) {
    return 'Current billing period';
  }

  const start = moment(startDate, 'YYYY-MM-DD');
  const end = moment(endDate, 'YYYY-MM-DD');

  if (start.year() === end.year()) {
    // Same year: "Jun 1 - Jun 30, 2026"
    return `${start.format('MMM D')} - ${end.format('MMM D, YYYY')}`;
  }

  // Cross-year: "Dec 15, 2025 - Jan 15, 2026"
  return `${start.format('MMM D, YYYY')} - ${end.format('MMM D, YYYY')}`;
}
