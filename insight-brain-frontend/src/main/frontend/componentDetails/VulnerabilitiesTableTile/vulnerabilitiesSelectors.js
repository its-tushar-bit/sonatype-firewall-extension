/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

export const selectVulnerabilitiesSlice = prop('componentDetailsVulnerabilities');

export const selectVulnerabilitiesSortedSlice = createSelector(selectVulnerabilitiesSlice, (vulnerabilitiesSlice) => {
  if (vulnerabilitiesSlice.vulnerabilities.data) {
    const sortedVulnerabilities = [...vulnerabilitiesSlice.vulnerabilities.data];
    sortedVulnerabilities.sort((a, b) => b.severity - a.severity);
    return { ...vulnerabilitiesSlice.vulnerabilities, data: sortedVulnerabilities };
  }
  return vulnerabilitiesSlice.vulnerabilities;
});

export const selectShowVulnerabilityDetailPopover = createSelector(
  selectVulnerabilitiesSlice,
  prop('showVulnerabilityDetailPopover')
);

export const selectVulnerabilityDetailsSlice = createSelector(selectVulnerabilitiesSlice, prop('vulnerabilityDetails'));

export const selectVulnerabityRefId = createSelector(selectVulnerabilitiesSlice, prop('selectedRefId'));
