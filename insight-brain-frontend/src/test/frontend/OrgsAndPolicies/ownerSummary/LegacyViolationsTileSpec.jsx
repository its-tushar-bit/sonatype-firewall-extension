/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, waitFor } from 'TestRoot/SpecUtil';
import LegacyViolationsTile from 'MainRoot/OrgsAndPolicies/ownerSummary/LegacyViolationsTile';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import * as legacyViolationSelectors from 'MainRoot/OrgsAndPolicies/legacyViolationSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/legacyViolationSlice';
import * as routerStateContext from 'MainRoot/react/RouterStateContext';

describe('LegacyViolationsTile', () => {
  let renderComponent, selectLoadErrorSpy, selectLoadingSpy;

  beforeEach(() => {
    spyOn(legacyViolationSelectors, 'selectLegacyViolationsStatusMessage').and.returnValue(
      'Legacy violations are enabled'
    );
    selectLoadErrorSpy = spyOn(legacyViolationSelectors, 'selectLoadError').and.returnValue(null);
    selectLoadingSpy = spyOn(legacyViolationSelectors, 'selectLoading').and.returnValue(false);
    spyOn(legacyViolationSelectors, 'selectLegacyViolationLinkParams').and.returnValue({
      to: 'management.edit.application.legacy-violations',
      params: {
        applicationPublicId: 'owl',
      },
    });

    spyOn(actions, 'loadLegacyViolation').and.returnValue({
      type: 'legacyViolation/loadLegacyViolation/fulfilled',
      payload: {},
    });

    renderComponent = () => render(<LegacyViolationsTile />);
  });

  describe('LegacyViolations Feature is Enabled', () => {
    beforeEach(() => {
      spyOn(productFeaturesSelectors, 'selectIsLegacyViolationSupported').and.returnValue(true);
    });
    it('renders loading indicator', () => {
      selectLoadingSpy.and.returnValue(true);
      renderComponent();
      expect(screen.getByText('Loading…')).toBeVisible();
    });

    it('renders error alert on load error', () => {
      selectLoadErrorSpy.and.returnValue('Load Error');
      renderComponent();

      const error = screen.getByRole('alert');

      expect(error).toBeVisible();
    });

    it('renders tile with the correct page title', () => {
      renderComponent();
      expect(screen.getByText('Legacy Violations')).toBeVisible();
    });

    it('renders legacy violations status', () => {
      renderComponent();
      expect(screen.getByText('Legacy violations are enabled')).toBeVisible();
    });

    it('renders link with href to legacy violations configuration page', () => {
      spyOn(routerStateContext, 'useRouterState').and.returnValue({
        href: jasmine.createSpy('href').and.returnValue('editPageHref'),
      });
      renderComponent();

      const linkItem = screen.getByText('Legacy violations are enabled');

      expect(linkItem.closest('a')).toHaveAttribute('href', 'editPageHref');
    });
  });

  describe('LegacyViolations Feature is Disabled', () => {
    beforeEach(() => {
      spyOn(productFeaturesSelectors, 'selectIsLegacyViolationSupported').and.returnValue(false);
    });
    it('does not render with a Firewall only license', async () => {
      await waitFor(() => expect(screen.queryByText('Loading')).toBeNull());
      const title = await screen.queryByText('Legacy Violations');
      expect(title).toBeNull();
    });
  });
});
