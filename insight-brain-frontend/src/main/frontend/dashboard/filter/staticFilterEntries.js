/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export const ages = [
  { name: 'past 24 hours', id: 1 },
  { name: 'past 7 days', id: 7 },
  { name: 'past 30 days', id: 30 },
  { name: 'past 90 days', id: 90 },
  { name: 'past 12 months', id: 365 },
  { name: 'all time', id: null },
];

export const defaultMaxDaysOld = ages[2].id;

export const policyTypes = [
  {
    id: 'SECURITY',
    name: 'Security',
  },
  {
    id: 'LICENSE',
    name: 'License',
  },
  {
    id: 'QUALITY',
    name: 'Quality',
  },
  {
    id: 'OTHER',
    name: 'Other',
  },
];

export const policyViolationStates = [
  {
    id: 'OPEN',
    name: 'Open',
  },
  {
    id: 'WAIVED',
    name: 'Waived',
  },
  {
    id: 'LEGACY_VIOLATION',
    name: 'Legacy',
  },
];

export const EXPIRATION_FILTER_ALL = 'ALL';
export const EXPIRATION_FILTER_AUTO = 'AUTO';
export const EXPIRATION_FILTER_IN_24_HOURS = 'IN_24_HOURS';
export const EXPIRATION_FILTER_IN_7_DAYS = 'IN_7_DAYS';
export const EXPIRATION_FILTER_IN_30_DAYS = 'IN_30_DAYS';
export const EXPIRATION_FILTER_IN_90_DAYS = 'IN_90_DAYS';
export const EXPIRATION_FILTER_IN_OVER_90_DAYS = 'IN_OVER_90_DAYS';
export const EXPIRATION_FILTER_NEVER = 'NEVER';
export const EXPIRATION_FILTER_EXPIRED = 'EXPIRED';

export const expirationDates = [
  {
    id: EXPIRATION_FILTER_ALL,
    name: 'all',
  },
  {
    id: EXPIRATION_FILTER_AUTO,
    name: 'auto',
  },
  {
    id: EXPIRATION_FILTER_IN_24_HOURS,
    name: 'in 24 hours',
  },
  {
    id: EXPIRATION_FILTER_IN_7_DAYS,
    name: 'in 7 days',
  },
  {
    id: EXPIRATION_FILTER_IN_30_DAYS,
    name: 'in 30 days',
  },
  {
    id: EXPIRATION_FILTER_IN_90_DAYS,
    name: 'in 90 days',
  },
  {
    id: EXPIRATION_FILTER_IN_OVER_90_DAYS,
    name: 'in over 90 days',
  },
  {
    id: EXPIRATION_FILTER_NEVER,
    name: 'never',
  },
  {
    id: EXPIRATION_FILTER_EXPIRED,
    name: 'expired',
  },
];

export const defaultMinExpiration = expirationDates[0].id;

export const uncategorizedCategory = {
  id: null, // NOTE that in this case null specifically means include uncategorized apps
  name: 'uncategorized applications',
  nameLowercaseNoWhitespace: 'uncategorizedapplications',
};

export const dashboardFilterOptionsTab = {
  'dashboard.overview.violations': {
    showAgeFilter: true,
    showStagesFilter: true,
    showViolationStateFilter: true,
    showExpirationDateFilter: false,
    showRepositoriesFilter: false,
    showPolicyWaiverReasonFilter: false,
  },
  'dashboard.overview.components': {
    showAgeFilter: false,
    showStagesFilter: true,
    showViolationStateFilter: true,
    showExpirationDateFilter: false,
    showRepositoriesFilter: false,
    showPolicyWaiverReasonFilter: false,
  },
  'dashboard.overview.applications': {
    showAgeFilter: false,
    showStagesFilter: true,
    showViolationStateFilter: true,
    showExpirationDateFilter: false,
    showRepositoriesFilter: false,
    showPolicyWaiverReasonFilter: false,
  },
  'dashboard.overview.waivers': {
    showAgeFilter: false,
    showStagesFilter: false,
    showViolationStateFilter: false,
    showExpirationDateFilter: true,
    showRepositoriesFilter: true,
    showPolicyWaiverReasonFilter: true,
  },
  'dashboard.overview.waiverRequests': {
    showAgeFilter: false,
    showStagesFilter: false,
    showViolationStateFilter: false,
    showExpirationDateFilter: true,
    showRepositoriesFilter: true,
    showPolicyWaiverReasonFilter: true,
  },
  default: {
    showAgeFilter: true,
    showStagesFilter: true,
    showViolationStateFilter: true,
    showExpirationDateFilter: true,
    showRepositoriesFilter: true,
    showPolicyWaiverReasonFilter: false,
  },
};
