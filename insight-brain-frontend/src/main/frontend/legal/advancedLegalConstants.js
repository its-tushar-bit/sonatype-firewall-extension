/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export const TEXT_BASED_OBLIGATIONS = ['Inclusion of Install Instructions', 'Must Give Credit', 'Must State Changes'];

export const ACTIONABLE_OBLIGATIONS = [
  'Inclusion of Copyright',
  'Inclusion of Notice',
  'Inclusion of License',
  ...TEXT_BASED_OBLIGATIONS,
];

export const OBLIGATION_STATUSES = ['FULFILLED', 'FLAGGED', 'IGNORED', 'OPEN'];

export const OBLIGATION_STATUS_TO_DISPLAY = {
  FULFILLED: 'Fulfilled',
  FLAGGED: 'Flagged',
  IGNORED: 'Not Applicable',
  OPEN: 'Unreviewed',
};

export const DASHBOARD = {
  applications: {
    itemsPerPage: 15,
    pagesToFill: 5,
  },
  components: {
    itemsPerPage: 30,
    pagesToFill: 1,
  },
};

export const STAGE_NAME_TO_DISPLAY = {
  Build: 'Build',
  'Stage Release': 'Stage',
  Release: 'Release',
  Operate: 'Operate',
};

export const STAGE_NAME_TO_ID = {
  Build: 'build',
  'Stage Release': 'stage-release',
  Release: 'release',
  Operate: 'operate',
};

export const EFFECTIVELY_UNSPECIFIED_LICENSES = [
  'Not-Declared',
  'No-Sources',
  'No-Source-License',
  'UNSPECIFIED',
  'Not-Supported',
];
