/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import InnerSourceRepositoryConfiguration from 'MainRoot/innerSourceRepositoryConfiguration/InnerSourceRepositoryConfiguration';
import { getInitialState } from 'TestRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryConfigurationTestData';
import * as innerSourceRepositoryConfigurationSelectors from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryConfigurationSelectors';
import { render, screen } from 'TestRoot/SpecUtil';
import React from 'react';

describe('InnerSourceRepositoryConfiguration', function () {
  let renderComponent,
    spySelectInnerSourceRepositoryConfigurationSlice,
    spySelectIsUpdate,
    spySelectIsDirty,
    spySelectHasAllRequiredData;

  beforeEach(() => {
    spySelectInnerSourceRepositoryConfigurationSlice = spyOn(
      innerSourceRepositoryConfigurationSelectors,
      'selectInnerSourceRepositoryConfigurationSlice'
    ).and.callThrough();
    spySelectIsUpdate = spyOn(innerSourceRepositoryConfigurationSelectors, 'selectIsUpdate').and.callThrough();
    spySelectIsDirty = spyOn(innerSourceRepositoryConfigurationSelectors, 'selectIsDirty').and.callThrough();
    spySelectHasAllRequiredData = spyOn(
      innerSourceRepositoryConfigurationSelectors,
      'selectHasAllRequiredData'
    ).and.callThrough();
    renderComponent = () => render(<InnerSourceRepositoryConfiguration />);
  });

  describe('configuration load', function () {
    it('disables the cancel button if not dirty', function () {
      renderComponent();
      const cancelButton = screen.getByText('Cancel');
      expect(cancelButton).toBeInTheDocument();
      expect(cancelButton).toBeDisabled();
    });

    it('enables the cancel button if dirty', function () {
      spySelectIsDirty.and.returnValue(true);
      renderComponent();
      const cancelButton = screen.getByText('Cancel');
      expect(cancelButton).toBeInTheDocument();
      expect(cancelButton).toBeEnabled();
    });
  });

  describe('adding', function () {
    it('sets the correct title', function () {
      renderComponent();
      expect(screen.getByText('Add Repository Configuration')).toBeInTheDocument();
    });

    it('sets the correct submit button text', function () {
      renderComponent();
      expect(screen.getByText('Create')).toBeInTheDocument();
    });

    it('does not render the delete configuration button', function () {
      renderComponent();
      expect(screen.queryByText('Delete Configuration')).toBeNull();
    });

    it('renders the save configuration error', function () {
      spySelectInnerSourceRepositoryConfigurationSlice.and.returnValue({
        ...getInitialState(),
        saveConfigurationError: 'someError',
      });
      renderComponent();
      expect(screen.getByText('An error occurred saving data. someError')).toBeInTheDocument();
    });
  });

  describe('editing', function () {
    beforeEach(() => {
      spySelectIsUpdate.and.returnValue(true);
    });

    it('sets the correct title', function () {
      renderComponent();
      expect(screen.getByText('Edit Repository Configuration')).toBeInTheDocument();
    });

    it('sets the correct submit button text', function () {
      renderComponent();
      expect(screen.getByText('Update')).toBeInTheDocument();
    });

    it('renders the delete configuration button', function () {
      renderComponent();
      expect(screen.getByText('Delete Configuration')).toBeInTheDocument();
    });

    it('does not render the delete configuration modal', function () {
      renderComponent();
      expect(screen.queryByText('Delete Repository Configuration?')).toBeNull();
    });

    it('renders the delete configuration modal', function () {
      spySelectInnerSourceRepositoryConfigurationSlice.and.returnValue({ ...getInitialState(), showDeleteModal: true });
      renderComponent();
      expect(screen.getByText('Delete Repository Configuration?')).toBeInTheDocument();
    });

    it('renders the delete configuration error', function () {
      spySelectInnerSourceRepositoryConfigurationSlice.and.returnValue({
        ...getInitialState(),
        showDeleteModal: true,
        deleteConfigurationError: 'someError',
      });
      renderComponent();
      expect(screen.getByText('Unable to delete the configured repository. someError')).toBeInTheDocument();
    });
  });

  describe('testing', function () {
    it('disables the test button if there is missing data', function () {
      renderComponent();
      const testButton = screen.getByText('Test Configuration');
      expect(testButton).toBeInTheDocument();
      expect(testButton).toHaveClassName('disabled');
    });

    it('enables the test button if there is no missing data', function () {
      spySelectHasAllRequiredData.and.returnValue(true);
      renderComponent();
      const testButton = screen.getByText('Test Configuration');
      expect(testButton).toBeInTheDocument();
      expect(testButton).not.toHaveClassName('disabled');
    });

    it('shows an info alert if the test was successful', function () {
      spySelectInnerSourceRepositoryConfigurationSlice.and.returnValue({
        ...getInitialState(),
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
      spySelectInnerSourceRepositoryConfigurationSlice.and.returnValue({
        ...getInitialState(),
        testConfigurationError: 'someError',
      });
      renderComponent();
      expect(screen.getByText('Unable to connect to the configured repository. someError')).toBeInTheDocument();
    });
  });
});
