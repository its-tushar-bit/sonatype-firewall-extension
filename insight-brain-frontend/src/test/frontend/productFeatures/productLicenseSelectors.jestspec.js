/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  selectProductLicense,
  selectProductLicenseSlice,
  selectProducts,
} from 'MainRoot/productFeatures/productLicenseSelectors';

describe('productLicenseSelectors', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      productLicense: {
        license: {
          products: [],
        },
      },
    };
  });

  describe('selectProductLicenseSlice', () => {
    it('returns productLicense', () => {
      expect(selectProductLicenseSlice(mockState)).toEqual(mockState.productLicense);
    });
  });

  describe('selectProductLicense', () => {
    it('returns license object', () => {
      expect(selectProductLicense(mockState)).toEqual(mockState.productLicense.license);
    });
  });

  describe('selectProducts', () => {
    it('returns products', () => {
      expect(selectProducts(mockState)).toEqual(mockState.productLicense.license.products);
    });
  });
});
