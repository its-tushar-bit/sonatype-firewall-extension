/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { checkSbomManagerIsOnlyProductEnabled } from 'MainRoot/sbomManager/sbomManagerUtil';

describe('sbomManagerUtil', () => {
  let initialState;

  beforeEach(() => {
    initialState = {
      productLicense: {
        license: {
          products: ['Sonatype Sbom Manager'],
        },
      },
      productFeatures: {
        productFeatures: {
          'sbom-manager': true,
        },
      },
    };
  });

  describe('checkSbomManagerIsOnlyProductEnabled', () => {
    it('returns true when Sbom manager is the only product in the license', function () {
      expect(checkSbomManagerIsOnlyProductEnabled(initialState)).toBeTruthy();
    });

    it('returns true when Sbom manager SaaS is the only product in the license', function () {
      initialState.productLicense.license.products[0] = 'Sonatype Sbom Manager SaaS';
      expect(checkSbomManagerIsOnlyProductEnabled(initialState)).toBeTruthy();
    });

    it('returns false when Sbom manager is NOT the only product in the license', function () {
      const products = initialState.productLicense.license.products;
      products.push('TEST_PRODUCT');
      expect(checkSbomManagerIsOnlyProductEnabled(initialState)).toBeFalsy();
    });
  });
});
