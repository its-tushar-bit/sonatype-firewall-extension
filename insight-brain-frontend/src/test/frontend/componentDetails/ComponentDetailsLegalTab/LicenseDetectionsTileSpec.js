/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from 'TestRoot/enzymeUtils';
import { NxLoadWrapper } from '@sonatype/react-shared-components';

import LicenseDetectionsTile from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/LicenseDetectionsTile';
import LicenseDetections from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/LicenseDetections';

describe('LicenseDetectionsTile', function () {
  let getShallow, minimalProps, licenseDetectionsProps;

  beforeEach(function () {
    licenseDetectionsProps = {
      licenseOverride: null,
      declaredLicenses: null,
      effectiveLicenses: null,
      observedLicenses: null,
      loadLicenses: () => {},
      loading: false,
      loadError: null,
      toggleShowEditLicensesPopover: () => {},
      reviewObligationsClickHandler: () => {},
    };
    minimalProps = {
      ...licenseDetectionsProps,
      isLoadingComponentDetails: false,
      componentDetailsLoadError: null,
      loadComponentDetails: () => {},
    };
    getShallow = enzymeUtils.getShallowComponent(LicenseDetectionsTile, minimalProps);
  });

  it('renders a LoadWrapper with proper props', () => {
    const component = getShallow({ isLoadingComponentDetails: true });
    const loadWrapper = component.find(NxLoadWrapper);

    expect(loadWrapper).toHaveProp('loading', true);
    expect(loadWrapper).toHaveProp('error', minimalProps.componentDetailsLoadError);
    expect(loadWrapper).toHaveProp('retryHandler', minimalProps.loadComponentDetails);
  });

  it('renders LicenseDetections when component details are not loading', () => {
    const component = getShallow();
    const loadWrapper = component.find(NxLoadWrapper).dive();
    const licenseDetectionsComponent = loadWrapper.find(LicenseDetections);

    expect(licenseDetectionsComponent).toHaveProp(licenseDetectionsProps);
  });
});
