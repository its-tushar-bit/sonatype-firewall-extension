/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default {
  QUARANTINE_TIME: {
    ONE_DAY: {
      VALUE: 1,
      LABEL: 'Past 24 hours',
    },
    SEVEN_DAYS: {
      VALUE: 7,
      LABEL: 'Past 7 days',
    },
    THIRTY_DAYS: {
      VALUE: 30,
      LABEL: 'Past 30 days',
    },
    NINETY_DAYS: {
      VALUE: 90,
      LABEL: 'Past 90 days',
    },
    ONE_YEAR: {
      VALUE: 365,
      LABEL: 'Past 12 months',
    },
    ALL_TIME: {
      VALUE: -1,
      LABEL: 'All',
    },
  },
  FILTER: 'Filter',
};
