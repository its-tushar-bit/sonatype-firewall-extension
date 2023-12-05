/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, waitFor } from 'TestRoot/SpecUtil';
import * as proprietarySelectors from 'MainRoot/OrgsAndPolicies/proprietarySelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/proprietarySlice';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import ProprietaryComponentConfigurationTile from 'MainRoot/OrgsAndPolicies/ownerSummary/ProprietaryComponentConfigurationTile';
import * as routerStateContext from 'MainRoot/react/RouterStateContext';

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

  describe('ProprietaryComponent Feature is Enabled', () => {
    beforeEach(() => {
      spyOn(productFeaturesSelectors, 'selectIsProprietaryComponentsEnabled').and.returnValue(true);
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

    it('renders link with href to edit proprietary component configuration page', () => {
      spyOn(routerStateContext, 'useRouterState').and.returnValue({
        href: jasmine.createSpy('href').and.returnValue('editPageHref'),
      });

      renderComponent();

      const linkItem = screen.getByText('1 local');
      expect(linkItem.closest('a')).toHaveAttribute('href', 'editPageHref');
    });
  });

  describe('ProprietaryComponents Feature is Disabled', () => {
    beforeEach(() => {
      spyOn(productFeaturesSelectors, 'selectIsProprietaryComponentsEnabled').and.returnValue(false);
    });

    it('does not render', async () => {
      renderComponent();

      await waitFor(() => expect(screen.queryByText('Loading')).toBeNull());
      const title = await screen.queryByText('Proprietary Component Configuration');
      expect(title).toBeNull();
    });
  });
});
