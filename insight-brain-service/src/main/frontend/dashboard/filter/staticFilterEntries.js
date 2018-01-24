/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export const ages = [
  {name: 'past 24 hours', maxDaysOld: 1},
  {name: 'past 7 days', maxDaysOld: 7},
  {name: 'past 30 days', maxDaysOld: 30},
  {name: 'past 90 days', maxDaysOld: 90},
  {name: 'past 12 months', maxDaysOld: 365},
  {name: 'all time', maxDaysOld: null}
];

export const policyTypes = [
  {
    id: 'SECURITY',
    name: 'Security'
  }, {
    id: 'LICENSE',
    name: 'License'
  }, {
    id: 'QUALITY',
    name: 'Quality'
  }, {
    id: 'OTHER',
    name: 'Other'
  }
];

export const policyViolationStates = [
  {
    id: 'OPEN',
    name: 'Open'
  }, {
    id: 'WAIVED',
    name: 'Waived'
  }
];

export const policySliderRangeHighlights = [
  { start: 0, end: 0.5, cls: 'threat-none' },
  { start: 0.5, end: 1.5, cls: 'threat-low' },
  { start: 1.5, end: 3.5, cls: 'threat-moderate' },
  { start: 3.5, end: 7.5, cls: 'threat-severe' },
  { start: 7.5, end: 10, cls: 'threat-critical' }
];

export const uncategorizedCategory = {
  description: 'uncategorized applications',
  id: null, // NOTE that in this case null specifically means include uncategorized apps
  name: 'No Category',
  nameLowercaseNoWhitespace: 'nocategory'
};
