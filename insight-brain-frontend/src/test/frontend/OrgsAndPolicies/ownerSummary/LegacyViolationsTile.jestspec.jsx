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
import router from 'MainRoot/router/routerInstance';

describe('LegacyViolationsTile', () => {
  let renderComponent, selectLoadErrorSpy, selectLoadingSpy;

  beforeEach(() => {
    jest
      .spyOn(legacyViolationSelectors, 'selectLegacyViolationsStatusMessage')
      .mockReturnValue('Legacy violations are enabled');
    selectLoadErrorSpy = jest.spyOn(legacyViolationSelectors, 'selectLoadError').mockReturnValue(null);
    selectLoadingSpy = jest.spyOn(legacyViolationSelectors, 'selectLoading').mockReturnValue(false);
    jest.spyOn(legacyViolationSelectors, 'selectLegacyViolationLinkParams').mockReturnValue({
      to: 'management.edit.application.legacy-violations',
      params: {
        applicationPublicId: 'owl',
      },
    });

    jest.spyOn(actions, 'loadLegacyViolation').mockReturnValue({
      type: 'legacyViolation/loadLegacyViolation/fulfilled',
      payload: {},
    });

    jest.spyOn(router.stateService, 'href').mockReturnValue('editPageHref');
    jest.spyOn(router.stateService, 'includes').mockReturnValue(false);
    jest.spyOn(router.stateService, 'get').mockReturnValue(null);

    renderComponent = () => render(<LegacyViolationsTile />);
  });

  describe('LegacyViolations Feature is Enabled', () => {
    beforeEach(() => {
      jest.spyOn(productFeaturesSelectors, 'selectIsLegacyViolationSupported').mockReturnValue(true);
    });
    it('renders loading indicator', () => {
      selectLoadingSpy.mockReturnValue(true);
      renderComponent();
      expect(screen.getByText('Loading…')).toBeVisible();
    });

    it('renders error alert on load error', () => {
      selectLoadErrorSpy.mockReturnValue('Load Error');
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
      renderComponent();

      const linkItem = screen.getByText('Legacy violations are enabled');

      expect(linkItem.closest('a')).toHaveAttribute('href', 'editPageHref');
    });
  });

  describe('LegacyViolations Feature is Disabled', () => {
    beforeEach(() => {
      jest.spyOn(productFeaturesSelectors, 'selectIsLegacyViolationSupported').mockReturnValue(false);
    });
    it('does not render with a Firewall only license', async () => {
      await waitFor(() => expect(screen.queryByText('Loading')).toBeNull());
      const title = await screen.queryByText('Legacy Violations');
      expect(title).toBeNull();
    });
  });
});
