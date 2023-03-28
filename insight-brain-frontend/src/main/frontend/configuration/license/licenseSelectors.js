/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';

export const selectProductLicense = prop('productLicense');

export const selectLicense = createSelector(selectProductLicense, prop('license'));

export const selectProducts = createSelector(selectLicense, prop('products'));

export const selectIsFirewallOnlyLicense = createSelector(selectProducts, (products) => {
  let isFirewallOnlyLicense = true;
  if (!products) {
    return null;
  }
  for (let i = 0; i < products.length; i++) {
    let product = products[i];
    if (!nameIncludesFirewall(product)) {
      return false;
    }
  }
  return isFirewallOnlyLicense;
});

const includesNamePart = (part) => (stringToSearch = '') => stringToSearch.includes(part);
const nameIncludesFirewall = includesNamePart('Firewall');
