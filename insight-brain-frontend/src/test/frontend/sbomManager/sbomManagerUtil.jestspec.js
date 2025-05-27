/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { isSbomManagerOnlyLicenseProduct, nameStartsWithSbomManager } from 'MainRoot/sbomManager/sbomManagerUtil';

describe('sbomManagerUtil', () => {
  describe('isSbomManagerOnlyLicenseProduct', () => {
    it('returns true when Sbom manager is the only product in the license', function () {
      const products = ['Sonatype SBOM Manager'];
      expect(isSbomManagerOnlyLicenseProduct(products)).toBeTruthy();
    });

    it('returns true when both Sbom manager and ALP are the only products in the license', function () {
      const products = ['Sonatype SBOM Manager', 'Sonatype Advanced Legal Pack'];
      expect(isSbomManagerOnlyLicenseProduct(products)).toBeTruthy();
    });

    it('returns true when Sbom manager SaaS is the only product in the license', function () {
      const products = ['Sonatype SBOM Manager SaaS'];
      expect(isSbomManagerOnlyLicenseProduct(products)).toBeTruthy();
    });

    it('returns false when Sbom manager is NOT the only product in the license', function () {
      const products = ['Sonatype SBOM Manager SaaS', 'TEST_PRODUCT'];
      expect(isSbomManagerOnlyLicenseProduct(products)).toBeFalsy();
    });

    it('returns false when products is empty', function () {
      expect(isSbomManagerOnlyLicenseProduct([])).toBeFalsy();
    });
  });

  describe('nameStartsWithSbomManager', () => {
    it('returns true when the string starts with sbomManager', function () {
      expect(nameStartsWithSbomManager('sbomManager.test')).toBeTruthy();
    });

    it('returns false when the string does not start with sbomManager', function () {
      expect(nameStartsWithSbomManager('test.sbomManager')).toBeFalsy();
    });
  });
});
