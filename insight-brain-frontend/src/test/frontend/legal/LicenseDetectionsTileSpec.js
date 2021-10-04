/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import LicenseDetectionsTile from 'MainRoot/componentDetails/LicenseDetectionsTile/LicenseDetectionsTile';

describe('LicenseDetectionsTile', function () {
  let getShallow;

  beforeEach(function () {
    getShallow = enzymeUtils.getShallowComponent(LicenseDetectionsTile);
  });

  describe('header', function () {
    it('renders a header with label `License Detections`', function () {
      const header = getShallow().find('#license-detections-title');
      expect(header).toHaveText('License Detections');
    });
  });
});
