/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as enzymeUtils from '../enzymeUtils';
import ComponentDetails from '../../../main/frontend/componentDetails/ComponentDetails';
import BackButton from '../../../main/frontend/react/BackButton';

describe('ComponentDetails', function () {
  let minimalProps, getShallowComponent, getMountedComponent, loadReportAndSelectComponentSpy, stateMock, stateGetSpy;

  beforeEach(function () {
    loadReportAndSelectComponentSpy = jasmine.createSpy('loadResults');
    stateGetSpy = jasmine.createSpy('$state.get').and.returnValue({ data: { title: 'some title' } });
    stateMock = {
      get: stateGetSpy,
      href: () => {},
    };
    spyOn(React, 'useContext').and.returnValue(stateMock);

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

  it('renders a back button', () => {
    const el = getShallowComponent(),
      backBtn = el.find(BackButton);

    expect(backBtn).toHaveProp('stateName', 'applicationReport.policy');
    expect(backBtn).toHaveProp('$state', stateMock);
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
