/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { length } from 'ramda';

export const nameStartsWithSbomManager = (stringToSearch = '') => stringToSearch.startsWith('sbomManager');

export const nameContainsComponentDetails = (stringToSearch = '') => stringToSearch.includes('.component');

export const isSbomManagerComponentDetails = (stringToSearch = '') =>
  nameStartsWithSbomManager(stringToSearch) && nameContainsComponentDetails(stringToSearch);

export const isSbomManagerOnlyLicenseProduct = (products) => {
  const allowedProducts = new Set(['Sonatype SBOM Manager SaaS', 'Sonatype SBOM Manager']);
  const filtered = products?.filter((p) => p !== 'Sonatype Advanced Legal Pack');
  return length(filtered) === 1 && allowedProducts.has(filtered[0]);
};
