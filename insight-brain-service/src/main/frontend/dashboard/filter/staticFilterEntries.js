/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export const ages = [
  {name: 'past 24 hours', id: 1},
  {name: 'past 7 days', id: 7},
  {name: 'past 30 days', id: 30},
  {name: 'past 90 days', id: 90},
  {name: 'past 12 months', id: 365},
  {name: 'all time', id: null}
];

export const defaultMaxDaysOld = ages[2].id;

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
  id: null, // NOTE that in this case null specifically means include uncategorized apps
  name: 'uncategorized applications',
  nameLowercaseNoWhitespace: 'uncategorizedapplications'
};
