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
import router from 'MainRoot/router/routerInstance';

describe('ProprietaryComponentConfigurationTile', () => {
  let renderComponent;
  let selectIsRootOrganizationSpy;

  beforeEach(() => {
    selectIsRootOrganizationSpy = jest.spyOn(routerSelectors, 'selectIsRootOrganization').mockReturnValue(true);

    jest.spyOn(proprietarySelectors, 'selectProprietaryConfigLocalMatchersCount').mockReturnValue(1);
    jest.spyOn(proprietarySelectors, 'selectProprietaryConfigInheritedMatchersCount').mockReturnValue(1);
    jest.spyOn(routerSelectors, 'selectRouterSlice').mockReturnValue(() => ({
      currentState: { name: 'repositories' },
      currentParams: {
        organizationId: 'organizationId',
      },
    }));

    jest.spyOn(actions, 'loadProprietaryConfig').mockReturnValue({
      type: 'proprietary/loadProprietaryConfig/fulfilled',
      payload: {},
    });

    jest.spyOn(router.stateService, 'href').mockReturnValue('editPageHref');
    jest.spyOn(router.stateService, 'includes').mockReturnValue(false);
    jest.spyOn(router.stateService, 'get').mockReturnValue(null);

    renderComponent = () => render(<ProprietaryComponentConfigurationTile />);
  });

  describe('ProprietaryComponent Feature is Enabled', () => {
    beforeEach(() => {
      jest.spyOn(productFeaturesSelectors, 'selectIsProprietaryComponentsEnabled').mockReturnValue(true);
    });

    it('renders loading indicator', () => {
      jest.spyOn(proprietarySelectors, 'selectIsLoading').mockReturnValue(true);

      renderComponent();

      expect(screen.getByText('Loading…')).toBeVisible();
    });

    it('renders error alert on load error', () => {
      jest.spyOn(proprietarySelectors, 'selectLoadError').mockReturnValue('loadError');
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
      selectIsRootOrganizationSpy.mockReturnValue(false);
      renderComponent();

      expect(screen.getByText('1 local, 1 inherited')).toBeVisible();
    });

    it('renders link with href to edit proprietary component configuration page', () => {
      renderComponent();

      const linkItem = screen.getByText('1 local');
      expect(linkItem.closest('a')).toHaveAttribute('href', 'editPageHref');
    });
  });

  describe('ProprietaryComponents Feature is Disabled', () => {
    beforeEach(() => {
      jest.spyOn(productFeaturesSelectors, 'selectIsProprietaryComponentsEnabled').mockReturnValue(false);
    });

    it('does not render', async () => {
      renderComponent();

      await waitFor(() => expect(screen.queryByText('Loading')).toBeNull());
      const title = await screen.queryByText('Proprietary Component Configuration');
      expect(title).toBeNull();
    });
  });
});
