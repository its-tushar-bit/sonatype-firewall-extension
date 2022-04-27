/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as enzymeUtils from 'TestRoot/enzymeUtils';
import OverriddenField from 'MainRoot/componentDetails/ComponentDetailsLegalTab/EditLicensesPopover/OverriddenField';
import { NxTransferList } from '@sonatype/react-shared-components';
import { render, fireEvent, screen } from 'TestRoot/SpecUtil';
import { actions } from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/licenseDetectionsTileSlice';

describe('ComponentDetailsLegalTab EditLicensesForm OverriddenField', () => {
  let minimalProps, mountedComponent, getShallowComponent, getMountedComponent, setSelectedLicensesSpy, onUnmountSpy;

  beforeEach(() => {
    onUnmountSpy = jasmine.createSpy('onUnmount');
    setSelectedLicensesSpy = jasmine.createSpy('setSelectedLicenses');
    minimalProps = {
      setSelectedLicenses: setSelectedLicensesSpy,
      licenseIds: [],
      onUnmount: onUnmountSpy,
      allLicenses: [],
    };
    getShallowComponent = enzymeUtils.getShallowComponent(OverriddenField, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(OverriddenField, minimalProps);
  });

  afterEach(() => {
    if (mountedComponent?.exists()) {
      mountedComponent.unmount();
    }
  });

  it('calls onUnmountSpy when component unmounted', () => {
    mountedComponent = getMountedComponent();

    mountedComponent.unmount();

    expect(onUnmountSpy).toHaveBeenCalledTimes(1);
  });

  it('converts licenseIds into set for NxTransferList props', () => {
    const component = getShallowComponent();
    const transferList = component.find(NxTransferList);

    expect(transferList.props().selectedItems).toEqual(new Set([]));
  });

  it('user should not able to select Disabled license', () => {
    const store = SpecUtil.mockReduxStore({});

    minimalProps.setSelectedLicenses = (payload) => store.dispatch(actions.setLicenseFormLicenseIds(payload));

    minimalProps.allLicenses = [
      { id: 'Disabled', displayName: 'Disabled' },
      { id: 'TestLicenseId', displayName: 'TestLicenseId' },
    ];

    render(<OverriddenField {...minimalProps} />);

    expect(screen.getByText(/TestLicenseId/i)).toBeInTheDocument();
    expect(screen.getByText(/Disabled/i)).toBeInTheDocument();

    fireEvent.click(screen.getByText(/TestLicenseId/i));
    fireEvent.click(screen.getByText(/Disabled/i));

    const expectedAction = {
      type: 'componentDetailsLicenseDetectionsTile/setLicenseFormLicenseIds',
      payload: ['TestLicenseId'],
    };

    expect(store.getActions()).toHaveSize(1);
    expect(store.getActions()[0]).toEqual(expectedAction);
  });
});
