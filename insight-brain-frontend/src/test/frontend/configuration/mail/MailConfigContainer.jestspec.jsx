/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, waitFor } from 'TestRoot/SpecUtil';
import MailConfigContainer from 'MainRoot/configuration/mail/MailConfigContainer';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';

describe('MailConfigContainer', () => {
  let selectIsEmailConfigurationEnabledSpy, selectLoadingFeaturesSpy;

  beforeEach(() => {
    selectIsEmailConfigurationEnabledSpy = jest
      .spyOn(productFeaturesSelectors, 'selectIsEmailConfigurationEnabled')
      .mockReturnValue(true);
    selectLoadingFeaturesSpy = jest.spyOn(productFeaturesSelectors, 'selectLoadingFeatures').mockReturnValue(false);
    jest.spyOn(productFeaturesSelectors, 'selectIsShowEmailStoppedEnabled').mockReturnValue(false);
  });

  it('should render mail config form when email-configuration feature is enabled', () => {
    render(<MailConfigContainer isAuthorized={true} />);

    expect(screen.getByText('Loading…')).toBeInTheDocument();
  });

  it('should render nothing when features are still loading', () => {
    selectLoadingFeaturesSpy.mockReturnValue(true);
    selectIsEmailConfigurationEnabledSpy.mockReturnValue(false);

    const { container } = render(<MailConfigContainer isAuthorized={true} />);

    expect(container.innerHTML).toBe('');
  });

  it('should dispatch setError when email-configuration feature is disabled', async () => {
    selectIsEmailConfigurationEnabledSpy.mockReturnValue(false);

    const { store } = render(<MailConfigContainer isAuthorized={true} />);

    await waitFor(() => {
      expect(store.getState().appError.error).toBe('Unknown Address');
    });
  });

  it('should render nothing in the component when feature is disabled', () => {
    selectIsEmailConfigurationEnabledSpy.mockReturnValue(false);

    const { container } = render(<MailConfigContainer isAuthorized={true} />);

    expect(container.innerHTML).toBe('');
  });

  it('should not dispatch setError when feature is enabled', () => {
    const { store } = render(<MailConfigContainer isAuthorized={true} />);

    expect(store.getState().appError.error).toBeNull();
  });
});
