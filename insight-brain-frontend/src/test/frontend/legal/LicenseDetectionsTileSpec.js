/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import LicenseDetectionsTile from 'MainRoot/componentDetails/LicenseDetectionsTile/LicenseDetectionsTile';

describe('LicenseDetectionsTile', function () {
  let getShallow, minimalProps, toggleShowEditLicensesPopoverSpy;

  beforeEach(function () {
    toggleShowEditLicensesPopoverSpy = jasmine.createSpy('toggleShowEditLicensesPopover');
    minimalProps = {
      toggleShowEditLicensesPopover: toggleShowEditLicensesPopoverSpy,
    };
    getShallow = enzymeUtils.getShallowComponent(LicenseDetectionsTile, minimalProps);
  });

  describe('header', function () {
    it('renders a header with label `License Detections`', function () {
      const header = getShallow().find('#license-detections-title');
      expect(header).toHaveText('License Detections');
    });
  });

  it('renders an NxButton with label `Edit`', () => {
    const button = getShallow().find('#component-details-edit-licenses');

    expect(button.text()).toContain('Edit');
  });

  it('calls `toggleShowEditLicensesPopoverSpy` when `Edit` button clicked', () => {
    const button = getShallow().find('#component-details-edit-licenses');

    button.simulate('click');

    expect(toggleShowEditLicensesPopoverSpy).toHaveBeenCalledTimes(1);
  });
});
