/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { Provider } from 'react-redux';
import configureStore from 'redux-mock-store';
import * as baseUrlConfigurationSelectors from 'MainRoot/configuration/baseUrl/baseUrlConfigurationSelectors';
import { render, screen } from 'TestRoot/SpecUtil';
import BaseUrlNotSetNotice from 'MainRoot/configuration/baseUrl/baseUrlNotSetNotice/BaseUrlNotSetNotice';

describe('BaseUrlNotSetNotice component', () => {
  const mockStore = configureStore([]);
  const store = mockStore({});
  const renderComponent = () =>
    render(
      <Provider store={store}>
        <BaseUrlNotSetNotice />
      </Provider>
    );

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
  });
});
