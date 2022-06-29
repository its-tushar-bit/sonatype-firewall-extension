/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import PolicyGrandfatheringTile from 'MainRoot/OrgsAndPolicies/ownerSummary/PolicyGrandfatheringTile';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import * as policyGrandfatheringSelectors from 'MainRoot/OrgsAndPolicies/policyViolationGrandfatheringSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/policyViolationGrandfatheringSlice';

describe('PolicyGrandfatheringTile', () => {
  let renderComponent, selectIsGrandfatheringSupportedSpy, selectLoadErrorSpy, selectLoadingSpy;

  beforeEach(() => {
    selectIsGrandfatheringSupportedSpy = spyOn(
      productFeaturesSelectors,
      'selectIsGrandfatheringSupported'
    ).and.returnValue(true);
    spyOn(policyGrandfatheringSelectors, 'selectGrandfatheringStatusMessage').and.returnValue(
      'Grandfathering is enabled'
    );
    selectLoadErrorSpy = spyOn(policyGrandfatheringSelectors, 'selectLoadError').and.returnValue(null);
    selectLoadingSpy = spyOn(policyGrandfatheringSelectors, 'selectLoading').and.returnValue(false);
    spyOn(policyGrandfatheringSelectors, 'selectGrandfatheringLinkParams').and.returnValue({
      to: 'management.edit.application.violation-grandfathering-policy',
      params: {
        applicationPublicId: 'owl',
      },
    });

    spyOn(actions, 'loadPolicyViolationGrandfathering').and.returnValue({
      type: 'policyViolationGrandfathering/loadPolicyViolationGrandfathering/fulfilled',
      payload: {},
    });

    renderComponent = () => render(<PolicyGrandfatheringTile />);
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
    expect(screen.getByText('Policy Violation Grandfathering')).toBeVisible();
  });

  it('renders policy grandfathering status', () => {
    renderComponent();
    expect(screen.getByText('Grandfathering is enabled')).toBeVisible();
  });

  it('renders not supported message if grandfathering is not supported', () => {
    selectIsGrandfatheringSupportedSpy.and.returnValue(false);
    renderComponent();

    expect(screen.getByText('Policy Violation Grandfathering is not supported by your license')).toBeVisible();
  });

  it('navigates to policy grandfathering configuration page', () => {
    renderComponent();

    const linkItem = screen.getByText('Grandfathering is enabled');
    fireEvent.click(linkItem);

    expect(screen.getByText('Policy Violation Grandfathering')).toBeVisible();
  });
});
