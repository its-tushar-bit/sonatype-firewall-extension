/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import InnerSourceRepositoryConfigurationModal from 'MainRoot/innerSourceRepositoryConfiguration/InnerSourceRepositoryConfigurationModal';
import { getInitialState } from 'TestRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryConfigurationModalTestData';
import * as innerSourceRepositoryConfigurationModalSelectors from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryConfigurationModalSelectors';
import { render, screen } from 'TestRoot/SpecUtil';
import React from 'react';

import 'TestRoot/SpecUtil';

describe('InnerSourceRepositoryConfigurationModal', function () {
  let renderComponent,
    spySelectInnerSourceRepositoryConfigurationModalSlice,
    spySelectIsUpdate,
    spySelectHasAllRequiredData;

  beforeEach(() => {
    spySelectInnerSourceRepositoryConfigurationModalSlice = jest.spyOn(
      innerSourceRepositoryConfigurationModalSelectors,
      'selectInnerSourceRepositoryConfigurationModalSlice'
    );
    spySelectIsUpdate = jest.spyOn(innerSourceRepositoryConfigurationModalSelectors, 'selectIsUpdate');
    spySelectHasAllRequiredData = jest.spyOn(
      innerSourceRepositoryConfigurationModalSelectors,
      'selectHasAllRequiredData'
    );
    spySelectInnerSourceRepositoryConfigurationModalSlice.mockReturnValue({
      ...getInitialState(),
      showModal: true,
    });
    renderComponent = () => render(<InnerSourceRepositoryConfigurationModal />);
  });

  describe('adding', function () {
    it('sets the correct title', function () {
      renderComponent();
      expect(screen.getByText('Add InnerSource Repository Configuration')).toBeInTheDocument();
    });

    it('sets the correct submit button text', function () {
      renderComponent();
      expect(screen.getByText('Create')).toBeInTheDocument();
    });

    it('renders the save configuration error', function () {
      spySelectInnerSourceRepositoryConfigurationModalSlice.mockReturnValue({
        ...getInitialState(),
        showModal: true,
        saveConfigurationError: 'someError',
      });
      renderComponent();
      expect(screen.getByText('An error occurred saving data. someError')).toBeInTheDocument();
    });
  });

  describe('editing', function () {
    beforeEach(() => {
      spySelectIsUpdate.mockReturnValue(true);
    });

    it('sets the correct title', function () {
      renderComponent();
      expect(screen.getByText('Edit InnerSource Repository Configuration')).toBeInTheDocument();
    });

    it('sets the correct submit button text', function () {
      renderComponent();
      expect(screen.getByText('Update')).toBeInTheDocument();
    });

    it('does not render the delete configuration modal', function () {
      renderComponent();
      expect(screen.queryByText('Delete Repository Configuration?')).toBeNull();
    });
  });

  describe('testing', function () {
    it('disables the test button if there is missing data', function () {
      renderComponent();
      const testButton = screen.getByText('Test Configuration');
      expect(testButton).toBeInTheDocument();
      expect(testButton).toHaveClass('disabled');
    });

    it('enables the test button if there is no missing data', function () {
      spySelectHasAllRequiredData.mockReturnValue(true);
      renderComponent();
      const testButton = screen.getByText('Test Configuration');
      expect(testButton).toBeInTheDocument();
      expect(testButton).not.toHaveClass('disabled');
    });

    it('shows an info alert if the test was successful', function () {
      spySelectInnerSourceRepositoryConfigurationModalSlice.mockReturnValue({
        ...getInitialState(),
        showModal: true,
        testConfigurationSuccessful: true,
      });
      renderComponent();
      expect(screen.getByText('Repository configuration test successful.')).toBeInTheDocument();
    });

    it('does not show an info alert if the test was unsuccessful', function () {
      renderComponent();
      expect(screen.queryByText('Repository configuration test successful.')).toBeNull();
    });

    it('renders the test configuration error', function () {
      spySelectInnerSourceRepositoryConfigurationModalSlice.mockReturnValue({
        ...getInitialState(),
        showModal: true,
        testConfigurationError: 'someError',
      });
      renderComponent();
      expect(screen.getByText('Unable to connect to the configured repository. someError')).toBeInTheDocument();
    });
  });
});
