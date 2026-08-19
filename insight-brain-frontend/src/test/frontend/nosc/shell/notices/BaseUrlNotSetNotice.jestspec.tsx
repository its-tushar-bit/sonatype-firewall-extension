/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as baseUrlConfigurationSelectors from 'MainRoot/configuration/baseUrl/baseUrlConfigurationSelectors';
import * as userSessionSelectors from 'MainRoot/user/userSessionSelectors';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import { render, screen } from 'TestRoot/SpecUtil';
import { BaseUrlNotSetNotice } from 'MainRoot/nosc/shell/notices/BaseUrlNotSetNotice';
// @ts-expect-error — legacy slice file is intentionally .js
import { actions } from 'MainRoot/configuration/baseUrl/baseUrlConfigurationSlice';
import { _setBaseUrlForTesting } from 'MainRoot/util/urlUtil';

describe('BaseUrlNotSetNotice', () => {
  const renderComponent = () => render(<BaseUrlNotSetNotice />);
  let loadSpy: jest.SpyInstance;

  beforeEach(() => {
    loadSpy = jest.spyOn(actions, 'load');
    _setBaseUrlForTesting('http://localhost');
  });

  it('renders the notice when shouldDisplayNotice is true', () => {
    jest.spyOn(baseUrlConfigurationSelectors, 'selectShouldDisplayNotice').mockReturnValue(true);
    renderComponent();
    expect(screen.getByText('The Base URL is not configured.')).toBeInTheDocument();
  });

  it('renders nothing when shouldDisplayNotice is false', () => {
    jest.spyOn(baseUrlConfigurationSelectors, 'selectShouldDisplayNotice').mockReturnValue(false);
    renderComponent();
    expect(screen.queryByText('The Base URL is not configured.')).not.toBeInTheDocument();
  });

  it('dispatches actions.load() when authenticated and the feature is enabled — reuses the selector, no re-derivation', () => {
    jest.spyOn(userSessionSelectors, 'selectCurrentUser').mockReturnValue({ authenticated: true });
    jest.spyOn(productFeaturesSelectors, 'selectIsBaseUrlConfigurationEnabled').mockReturnValue(true);
    renderComponent();
    expect(loadSpy).toHaveBeenCalled();
  });

  it('does not dispatch load when the feature flag is off', () => {
    jest.spyOn(userSessionSelectors, 'selectCurrentUser').mockReturnValue({ authenticated: true });
    jest.spyOn(productFeaturesSelectors, 'selectIsBaseUrlConfigurationEnabled').mockReturnValue(false);
    renderComponent();
    expect(loadSpy).not.toHaveBeenCalled();
  });

  it('does not dispatch load when there is no authenticated user', () => {
    jest.spyOn(userSessionSelectors, 'selectCurrentUser').mockReturnValue({ authenticated: false });
    jest.spyOn(productFeaturesSelectors, 'selectIsBaseUrlConfigurationEnabled').mockReturnValue(true);
    renderComponent();
    expect(loadSpy).not.toHaveBeenCalled();
  });

  it('renders as a warning-severity notice with role="status"', () => {
    jest.spyOn(baseUrlConfigurationSelectors, 'selectShouldDisplayNotice').mockReturnValue(true);
    renderComponent();
    expect(screen.getByTestId('nosc-base-url-not-set-notice')).toHaveAttribute('role', 'status');
  });

  it('links to the Settings hub Base URL configuration page', () => {
    jest.spyOn(baseUrlConfigurationSelectors, 'selectShouldDisplayNotice').mockReturnValue(true);
    renderComponent();
    expect(screen.getByRole('link', { name: 'Configure Base URL' })).toHaveAttribute('href', expect.stringContaining('/baseUrl'));
  });
});
