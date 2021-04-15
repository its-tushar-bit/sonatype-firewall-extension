/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
// Copied from our AngularCommon library
export default function agoFilter() {
  return function (date) {
    var ago = '',
      diff,
      unit,
      val;

    if (!date) {
      return ago;
    }
    diff = new Date().getTime() - date;

    if (diff > 12 * 30 * 24 * 60 * 60 * 1000) {
      val = diff / (12 * 30 * 24 * 60 * 60 * 1000);
      unit = 'year';
    } else if (diff > 30 * 24 * 60 * 60 * 1000) {
      val = diff / (30 * 24 * 60 * 60 * 1000);
      unit = 'month';
    } else if (diff > 24 * 60 * 60 * 1000) {
      val = diff / (24 * 60 * 60 * 1000);
      unit = 'day';
    } else if (diff > 60 * 60 * 1000) {
      val = diff / (60 * 60 * 1000);
      unit = 'hour';
    } else if (diff > 60 * 1000) {
      val = diff / (60 * 1000);
      unit = 'minute';
    } else {
      return 'seconds ago';
    }
    val = Math.floor(val);
    if (val > 1) {
      unit += 's';
    }
    return val + ' ' + unit + ' ago';
  };
}
