/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import ComponentDetails from '../../../main/frontend/componentDetails/ComponentDetails';

describe('ComponentDetails', function () {
  let minimalProps, getShallowComponent, getMountedComponent, loadReportAndSelectComponentSpy;

  beforeEach(function () {
    loadReportAndSelectComponentSpy = jasmine.createSpy('loadResults');

    minimalProps = {
      selectedComponent: null,
      publicId: 'publicId',
      scanId: 'scanId',
      unknownjs: false,
      hash: 'hash',
      loadReportAndSelectComponentByHash: loadReportAndSelectComponentSpy,
    };

    (getShallowComponent = enzymeUtils.getShallowComponent(ComponentDetails, minimalProps)),
      (getMountedComponent = enzymeUtils.getMountedComponent(ComponentDetails, minimalProps));
  });

  it('renders a component', () => {
    expect(getShallowComponent()).toExist();
  });

  it('calls loadReportAndSelectComponentByHash if there is no selectedComponent in the state', () => {
    getMountedComponent();
    expect(loadReportAndSelectComponentSpy).toHaveBeenCalledWith('publicId', 'scanId', 'hash', false);
  });

  it('does not calls loadReportAndSelectComponentByHash if there is a selectedComponent in the state', () => {
    getMountedComponent({ selectedComponent: { derivedComponentName: 'MockName' } });
    expect(loadReportAndSelectComponentSpy).not.toHaveBeenCalled();
  });
});
