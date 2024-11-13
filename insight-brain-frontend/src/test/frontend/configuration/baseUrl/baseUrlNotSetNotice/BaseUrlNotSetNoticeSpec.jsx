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
    loadSpy = spyOn(actions, 'load').and.callThrough();
  });

  describe('Component load', () => {
    it('should render when shouldDisplayNotice is true', () => {
      spyOn(baseUrlConfigurationSelectors, 'selectShouldDisplayNotice').and.callFake(() => true);
      renderComponent();
      expect(screen.getByText('The Base URL is not configured.')).toBeInTheDocument();
    });

    it('should not render when shouldDisplayNotice is false', () => {
      spyOn(baseUrlConfigurationSelectors, 'selectShouldDisplayNotice').and.callFake(() => false);
      renderComponent();
      expect(screen.queryByText('The Base URL is not configured.')).not.toBeInTheDocument();
    });

    it('should dispatch a load if it is an admin user and single tenant', () => {
      spyOn(userSelectors, 'selectCurrentUser').and.returnValue({ authenticated: true });
      spyOn(productFeaturesSelectors, 'selectIsBaseUrlConfigurationEnabled').and.returnValue(true);
      renderComponent();
      expect(loadSpy).toHaveBeenCalled();
    });

    it('should not dispatch a load if it is multi tenant', () => {
      spyOn(userSelectors, 'selectCurrentUser').and.returnValue({ authenticated: true });
      spyOn(productFeaturesSelectors, 'selectIsBaseUrlConfigurationEnabled').and.returnValue(false);
      renderComponent();
      expect(loadSpy).not.toHaveBeenCalled();
    });

    it('should not dispatch a load if it is not authenticated', () => {
      spyOn(userSelectors, 'selectCurrentUser').and.returnValue({ authenticated: false });
      spyOn(productFeaturesSelectors, 'selectIsBaseUrlConfigurationEnabled').and.returnValue(true);
      renderComponent();
      expect(loadSpy).not.toHaveBeenCalled();
    });

    it('should not dispatch a load if there is no current user', () => {
      spyOn(userSelectors, 'selectCurrentUser').and.returnValue(null);
      spyOn(productFeaturesSelectors, 'selectIsBaseUrlConfigurationEnabled').and.returnValue(true);
      renderComponent();
      expect(loadSpy).not.toHaveBeenCalled();
    });
  });
});
