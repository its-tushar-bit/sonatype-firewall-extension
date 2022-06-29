/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import * as proprietarySelectors from 'MainRoot/OrgsAndPolicies/proprietarySelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/proprietarySlice';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import ProprietaryComponentConfigurationTile from 'MainRoot/OrgsAndPolicies/ownerSummary/ProprietaryComponentConfigurationTile';

describe('ProprietaryComponentConfigurationTile', () => {
  let renderComponent;
  let selectIsRootOrganizationSpy;

  beforeEach(() => {
    selectIsRootOrganizationSpy = spyOn(routerSelectors, 'selectIsRootOrganization').and.returnValue(true);

    spyOn(proprietarySelectors, 'selectProprietaryConfigLocalMatchersCount').and.returnValue(1);
    spyOn(proprietarySelectors, 'selectProprietaryConfigInheritedMatchersCount').and.returnValue(1);
    spyOn(routerSelectors, 'selectRouterSlice').and.returnValue(() => ({
      currentState: { name: 'repositories' },
      currentParams: {
        organizationId: 'organizationId',
      },
    }));

    spyOn(actions, 'loadProprietaryConfig').and.returnValue({
      type: 'proprietary/loadProprietaryConfig/fulfilled',
      payload: {},
    });

    renderComponent = () => render(<ProprietaryComponentConfigurationTile />);
  });

  it('renders loading indicator', () => {
    spyOn(proprietarySelectors, 'selectIsLoading').and.returnValue(true);

    renderComponent();

    expect(screen.getByText('Loading…')).toBeVisible();
  });

  it('renders error alert on load error', () => {
    spyOn(proprietarySelectors, 'selectLoadError').and.returnValue('loadError');
    renderComponent();

    const error = screen.getByRole('alert');

    expect(error).toBeVisible();
  });

  it('renders just local proprietary count', () => {
    renderComponent();

    expect(screen.getByText('1 local')).toBeVisible();
    expect(screen.queryByText('1 inherited')).toBeNull();
  });

  it('renders both local and inherited proprietary count', () => {
    selectIsRootOrganizationSpy.and.returnValue(false);
    renderComponent();

    expect(screen.getByText('1 local, 1 inherited')).toBeVisible();
  });

  it('navigates to edit proprietary component configuration page', () => {
    renderComponent();

    const linkItem = screen.getByText('1 local');

    fireEvent.click(linkItem);

    // navigates to the edit page
    expect(screen.getByText('Proprietary Component Configuration')).toBeVisible();
  });
});
