/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { equals, length } from 'ramda';

export const nameStartsWithSbomManager = (stringToSearch = '') => stringToSearch.startsWith('sbomManager');

export const nameContainsComponentDetails = (stringToSearch = '') => stringToSearch.includes('.component');

export const isSbomManagerComponentDetails = (stringToSearch = '') =>
  nameStartsWithSbomManager(stringToSearch) && nameContainsComponentDetails(stringToSearch);

export const isSbomManagerOnlyLicenseProduct = (products) =>
  length(products) === 1 &&
  (equals(products[0], 'Sonatype SBOM Manager SaaS') || equals(products[0], 'Sonatype SBOM Manager'));
