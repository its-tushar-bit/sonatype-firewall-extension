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

export const expirationDates = [
  {
    id: 'ALL',
    name: 'all',
  },
  {
    id: 'AUTO',
    name: 'auto',
  },
  {
    id: 'IN_24_HOURS',
    name: 'in 24 hours',
  },
  {
    id: 'IN_7_DAYS',
    name: 'in 7 days',
  },
  {
    id: 'IN_30_DAYS',
    name: 'in 30 days',
  },
  {
    id: 'IN_90_DAYS',
    name: 'in 90 days',
  },
  {
    id: 'IN_OVER_90_DAYS',
    name: 'in over 90 days',
  },
  {
    id: 'NEVER',
    name: 'never',
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
  default: {
    showAgeFilter: true,
    showStagesFilter: true,
    showViolationStateFilter: true,
    showExpirationDateFilter: true,
    showRepositoriesFilter: true,
    showPolicyWaiverReasonFilter: false,
  },
};
