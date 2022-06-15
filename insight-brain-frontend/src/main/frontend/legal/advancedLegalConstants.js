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

export const OBLIGATION_STATUS_FULFILLED = 'FULFILLED';
export const OBLIGATION_STATUS_FLAGGED = 'FLAGGED';
export const OBLIGATION_STATUS_IGNORED = 'IGNORED';
export const OBLIGATION_STATUS_OPEN = 'OPEN';

export const OBLIGATION_STATUSES = [
  OBLIGATION_STATUS_FULFILLED,
  OBLIGATION_STATUS_FLAGGED,
  OBLIGATION_STATUS_IGNORED,
  OBLIGATION_STATUS_OPEN,
];

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
    itemsPerPage: 10,
    pagesToFill: 5,
  },
};

export const STAGE_NAME_TO_DISPLAY = {
  Source: 'Source',
  Build: 'Build',
  'Stage Release': 'Stage',
  Release: 'Release',
  Operate: 'Operate',
};

export const STAGE_NAME_TO_ID = {
  Source: 'source',
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

export const NO_LICENSE_THREAT_GROUP_ASSIGNED = 'No LTG Assigned';

export const ATTRIBUTION_DISPLAY_MAP = {
  'Must Give Credit': {
    headerTitle: 'Give Credit',
    modalTitleSuffix: 'Attribution for Giving Credit',
    emptyMessage: 'No attribution for giving credit added',
  },
  'Must State Changes': {
    headerTitle: 'Stated Changes',
    modalTitleSuffix: 'Attribution for Stating Changes',
    emptyMessage: 'No attribution for stated changes added',
  },
  'Inclusion of Install Instructions': {
    headerTitle: 'Install Instructions',
    modalTitleSuffix: 'Attribution for Install Instructions',
    emptyMessage: 'No attribution for install instructions added',
  },
};

export const EDIT_LICENSE_MODAL_STATUS_OPTIONS = ['Open', 'Acknowledged', 'Overridden', 'Selected', 'Confirmed'];

export const statusTagPropsMap = {
  Overridden: 'purple',
  Selected: 'indigo',
};

export const SUPPORTED_COMPONENTS_ECOSYSTEM = ['gem', 'maven', 'npm', 'nuget', 'pypi', 'rpm', 'a-name', 'composer'];
