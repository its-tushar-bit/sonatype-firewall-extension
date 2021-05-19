/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import moment from 'moment';

const STANDARD_DATE_FORMAT = 'YYYY-MM-DD HH:mm:ss [UTC]Z';
export const formatDate = (date, format = STANDARD_DATE_FORMAT) => {
  if (typeof date === 'undefined') {
    return '';
  }
  return moment(date).format(format);
};
