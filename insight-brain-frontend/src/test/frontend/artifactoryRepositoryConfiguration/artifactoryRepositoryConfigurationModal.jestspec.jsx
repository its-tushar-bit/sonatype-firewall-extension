/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ArtifactoryRepositoryConfigurationModal from 'MainRoot/artifactoryRepositoryConfiguration/ArtifactoryRepositoryConfigurationModal';
import { getInitialState } from 'TestRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryConfigurationModalTestData';
import * as artifactoryRepositoryConfigurationModalSelectors from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryConfigurationModalSelectors';
import { render, screen } from 'TestRoot/SpecUtil';
import React from 'react';

import 'TestRoot/SpecUtil';

describe('ArtifactoryRepositoryConfigurationModal', function () {
  let renderComponent,
    spySelectArtifactoryRepositoryConfigurationModalSlice,
    spySelectIsUpdate,
    spySelectHasAllRequiredData;

  beforeEach(() => {
    spySelectArtifactoryRepositoryConfigurationModalSlice = jest.spyOn(
      artifactoryRepositoryConfigurationModalSelectors,
      'selectArtifactoryRepositoryConfigurationModalSlice'
    );
    spySelectIsUpdate = jest.spyOn(artifactoryRepositoryConfigurationModalSelectors, 'selectIsUpdate');
    spySelectHasAllRequiredData = jest.spyOn(
      artifactoryRepositoryConfigurationModalSelectors,
      'selectHasAllRequiredData'
    );
    spySelectArtifactoryRepositoryConfigurationModalSlice.mockReturnValue({
      ...getInitialState(),
      showModal: true,
    });
    renderComponent = () => render(<ArtifactoryRepositoryConfigurationModal />);
  });

  describe('adding', function () {
    it('sets the correct title', function () {
      renderComponent();
      expect(screen.getByText('Add Artifactory Repository Configuration')).toBeInTheDocument();
    });

    it('sets the correct submit button text', function () {
      renderComponent();
      expect(screen.getByText('Create')).toBeInTheDocument();
    });

    it('renders the save configuration error', function () {
      spySelectArtifactoryRepositoryConfigurationModalSlice.mockReturnValue({
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
      expect(screen.getByText('Edit Artifactory Repository Configuration')).toBeInTheDocument();
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
      spySelectArtifactoryRepositoryConfigurationModalSlice.mockReturnValue({
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
      spySelectArtifactoryRepositoryConfigurationModalSlice.mockReturnValue({
        ...getInitialState(),
        showModal: true,
        testConfigurationError: 'someError',
      });
      renderComponent();
      expect(screen.getByText('Unable to connect to the configured repository. someError')).toBeInTheDocument();
    });
  });
});
