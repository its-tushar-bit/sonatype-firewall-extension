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
export const formatDate = (date, format = STANDARD_DATE_TIME_FORMAT) => {
  if (typeof date === 'undefined' || date === null) {
    return '';
  }
  return moment(date).format(format);
};

// Copied from our AngularCommon library
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

// Copied from our AngularCommon library
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
