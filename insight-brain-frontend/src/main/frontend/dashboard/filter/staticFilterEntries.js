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
    id: 'GRANDFATHERED',
    name: 'Grandfathered',
  },
];

export const uncategorizedCategory = {
  id: null, // NOTE that in this case null specifically means include uncategorized apps
  name: 'uncategorized applications',
  nameLowercaseNoWhitespace: 'uncategorizedapplications',
};
