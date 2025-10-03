/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import * as baseUrlConfigurationSelectors from 'MainRoot/configuration/baseUrl/baseUrlConfigurationSelectors';
import * as userSelectors from 'MainRoot/user/userSelectors';
import { render, screen } from 'TestRoot/SpecUtil';
import BaseUrlNotSetNotice from 'MainRoot/configuration/baseUrl/baseUrlNotSetNotice/BaseUrlNotSetNotice';
import { actions } from 'MainRoot/configuration/baseUrl/baseUrlConfigurationSlice';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';

describe('BaseUrlNotSetNotice component', () => {
  const renderComponent = () => render(<BaseUrlNotSetNotice />);
  let loadSpy;
  beforeEach(() => {
    loadSpy = jest.spyOn(actions, 'load');
  });

  describe('Component load', () => {
    it('should render when shouldDisplayNotice is true', () => {
      jest.spyOn(baseUrlConfigurationSelectors, 'selectShouldDisplayNotice').mockImplementation(() => true);
      renderComponent();
      expect(screen.getByText('The Base URL is not configured.')).toBeInTheDocument();
    });

    it('should not render when shouldDisplayNotice is false', () => {
      jest.spyOn(baseUrlConfigurationSelectors, 'selectShouldDisplayNotice').mockImplementation(() => false);
      renderComponent();
      expect(screen.queryByText('The Base URL is not configured.')).not.toBeInTheDocument();
    });

    it('should dispatch a load if it is an admin user and single tenant', () => {
      jest.spyOn(userSelectors, 'selectCurrentUser').mockReturnValue({ authenticated: true });
      jest.spyOn(productFeaturesSelectors, 'selectIsBaseUrlConfigurationEnabled').mockReturnValue(true);
      renderComponent();
      expect(loadSpy).toHaveBeenCalled();
    });

    it('should not dispatch a load if it is multi tenant', () => {
      jest.spyOn(userSelectors, 'selectCurrentUser').mockReturnValue({ authenticated: true });
      jest.spyOn(productFeaturesSelectors, 'selectIsBaseUrlConfigurationEnabled').mockReturnValue(false);
      renderComponent();
      expect(loadSpy).not.toHaveBeenCalled();
    });

    it('should not dispatch a load if it is not authenticated', () => {
      jest.spyOn(userSelectors, 'selectCurrentUser').mockReturnValue({ authenticated: false });
      jest.spyOn(productFeaturesSelectors, 'selectIsBaseUrlConfigurationEnabled').mockReturnValue(true);
      renderComponent();
      expect(loadSpy).not.toHaveBeenCalled();
    });

    it('should not dispatch a load if there is no current user', () => {
      jest.spyOn(userSelectors, 'selectCurrentUser').mockReturnValue(null);
      jest.spyOn(productFeaturesSelectors, 'selectIsBaseUrlConfigurationEnabled').mockReturnValue(true);
      renderComponent();
      expect(loadSpy).not.toHaveBeenCalled();
    });
  });
});
