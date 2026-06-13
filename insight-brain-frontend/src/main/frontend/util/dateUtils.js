/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import moment from 'moment-timezone';
import momentJs from 'moment';

export const STANDARD_DATE_TIME_FORMAT = 'YYYY-MM-DD HH:mm:ss [UTC]Z';
export const SIMPLE_TIME_FORMAT = 'h:mm A';

export const STANDARD_DATE_FORMAT = 'YYYY-MM-DD';
export const STANDARD_DATE_TIME_FORMAT_NO_TZ = 'YYYY-MM-DD HH:mm:ss';

export const FIREWALL_TIME_DATE_FORMAT = 'h:mm:ss A YYYY-MM-DD';
export const FIREWALL_DATE_TIME_FORMAT = STANDARD_DATE_TIME_FORMAT_NO_TZ;

export const USER_ACTIVITY_DATE_FORMAT = 'M/D/YYYY, h:mm:ss A';
export const formatDate = (date, format = STANDARD_DATE_TIME_FORMAT) => {
  if (typeof date === 'undefined' || date === null) {
    return '';
  }
  return moment(date).format(format);
};

/**
 * Format a date as a UTC calendar day (YYYY-MM-DD), returning an em dash for
 * null/empty/invalid input. UTC (not local) so the rendered day is stable
 * regardless of viewer/test timezone — used where only the calendar day matters
 * (e.g. waiver created/expiry dates).
 */
export const formatDateUtcYYYYMMDD = (value) => {
  if (value === null || value === undefined || value === '') {
    return '—';
  }
  const m = moment.utc(value);
  return m.isValid() ? m.format('YYYY-MM-DD') : '—';
};

// Copied from our time utilities library
export const formatTimeAgo = (date, isTruncated = false, now = new Date()) => {
  if (!date) {
    return '';
  }

  let providedDate, unit, val;
  providedDate = new Date(date);

  if (momentJs(now).diff(providedDate, 'years', true) >= 1) {
    val = momentJs(now).diff(providedDate, 'years', true);
    unit = isTruncated ? 'y' : 'year';
  } else if (momentJs(now).diff(providedDate, 'months', true) >= 1) {
    val = momentJs(now).diff(providedDate, 'months', true);
    unit = isTruncated ? 'mo' : 'month';
  } else if (momentJs(now).diff(providedDate, 'days', true) >= 1) {
    val = momentJs(now).diff(providedDate, 'days', true);
    unit = isTruncated ? 'd' : 'day';
  } else if (momentJs(now).diff(providedDate, 'hours', true) >= 1) {
    val = momentJs(now).diff(providedDate, 'hours', true);
    unit = isTruncated ? 'h' : 'hour';
  } else if (momentJs(now).diff(providedDate, 'minutes', true) >= 1) {
    val = momentJs(now).diff(providedDate, 'minutes', true);
    unit = isTruncated ? 'min' : 'minute';
  } else {
    return isTruncated ? 's' : 'seconds ago';
  }
  val = Math.floor(val);
  if (isTruncated) {
    return `${val}${unit}`;
  }
  if (val > 1) {
    unit += 's';
  }
  return val + ' ' + unit + ' ago';
};

export const formatTimeAgoUpToDay = (date) => {
  const timeAgoDateInString = formatTimeAgo(date);
  return reduceStringDateToDay(timeAgoDateInString);
};

// Copied from our time utilities library
export const reduceStringDateToDay = (timeAgoDateInString) => {
  if (
    timeAgoDateInString.indexOf('seconds ago') > -1 ||
    timeAgoDateInString.indexOf('minute') > -1 ||
    timeAgoDateInString.indexOf('hour') > -1
  ) {
    return 'Less than a day ago';
  }
  return timeAgoDateInString;
};

/**
 * Formats a Date object as YYYY-MM-DD string.
 *
 * @param {Date} date - The date to format
 * @returns {string} Date formatted as YYYY-MM-DD
 */
export const formatDateAsYYYYMMDD = (date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};
