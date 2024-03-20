/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectProducts } from 'MainRoot/productFeatures/productLicenseSelectors';
import { selectIsSbomManagerEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';

export function checkSbomManagerIsOnlyProductEnabled(state) {
  const products = selectProducts(state);
  const isSbomManagerEnabled = selectIsSbomManagerEnabled(state);

  return (
    products &&
    Array.isArray(products) &&
    products.length === 1 &&
    (products[0] === 'Sonatype Sbom Manager' || products[0] === 'Sonatype Sbom Manager SaaS') &&
    isSbomManagerEnabled
  );
}
