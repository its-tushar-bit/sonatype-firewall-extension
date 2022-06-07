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

describe('ArtifactoryRepositoryConfigurationModal', function () {
  let renderComponent,
    spySelectArtifactoryRepositoryConfigurationModalSlice,
    spySelectIsUpdate,
    spySelectHasAllRequiredData;

  beforeEach(() => {
    spySelectArtifactoryRepositoryConfigurationModalSlice = spyOn(
      artifactoryRepositoryConfigurationModalSelectors,
      'selectArtifactoryRepositoryConfigurationModalSlice'
    ).and.callThrough();
    spySelectIsUpdate = spyOn(artifactoryRepositoryConfigurationModalSelectors, 'selectIsUpdate').and.callThrough();
    spySelectHasAllRequiredData = spyOn(
      artifactoryRepositoryConfigurationModalSelectors,
      'selectHasAllRequiredData'
    ).and.callThrough();
    spySelectArtifactoryRepositoryConfigurationModalSlice.and.returnValue({
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
      spySelectArtifactoryRepositoryConfigurationModalSlice.and.returnValue({
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
      spySelectIsUpdate.and.returnValue(true);
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
      spySelectArtifactoryRepositoryConfigurationModalSlice.and.returnValue({
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
      spySelectArtifactoryRepositoryConfigurationModalSlice.and.returnValue({
        ...getInitialState(),
        showModal: true,
        testConfigurationError: 'someError',
      });
      renderComponent();
      expect(screen.getByText('Unable to connect to the configured repository. someError')).toBeInTheDocument();
    });
  });
});
