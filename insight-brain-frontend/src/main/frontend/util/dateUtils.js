/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import moment from 'moment-timezone';

export const STANDARD_DATE_TIME_FORMAT = 'YYYY-MM-DD HH:mm:ss [UTC]Z';
export const STANDARD_DATE_FORMAT = 'MM/DD/YYYY';
export const formatDate = (date, format = STANDARD_DATE_TIME_FORMAT) => {
  if (typeof date === 'undefined') {
    return '';
  }
  return moment(date).format(format);
};

// Copied from our AngularCommon library
export const formatTimeAgo = (date, isTruncated = false) => {
  if (!date) {
    return '';
  }

  let diff, unit, val;

  diff = new Date().getTime() - date;

  if (diff > 12 * 30 * 24 * 60 * 60 * 1000) {
    val = diff / (12 * 30 * 24 * 60 * 60 * 1000);
    unit = isTruncated ? 'y' : 'year';
  } else if (diff > 30 * 24 * 60 * 60 * 1000) {
    val = diff / (30 * 24 * 60 * 60 * 1000);
    unit = isTruncated ? 'mo' : 'month';
  } else if (diff > 24 * 60 * 60 * 1000) {
    val = diff / (24 * 60 * 60 * 1000);
    unit = isTruncated ? 'd' : 'day';
  } else if (diff > 60 * 60 * 1000) {
    val = diff / (60 * 60 * 1000);
    unit = isTruncated ? 'h' : 'hour';
  } else if (diff > 60 * 1000) {
    val = diff / (60 * 1000);
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
