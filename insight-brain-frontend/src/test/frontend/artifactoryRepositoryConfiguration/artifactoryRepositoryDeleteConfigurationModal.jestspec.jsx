/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ArtifactoryRepositoryDeleteConfigurationModal from 'MainRoot/artifactoryRepositoryConfiguration/ArtifactoryRepositoryDeleteConfigurationModal';
import { getInitialState } from 'TestRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryConfigurationModalTestData';
import * as artifactoryRepositoryConfigurationModalSelectors from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryDeleteConfigurationModalSelectors';
import { render, screen } from 'TestRoot/SpecUtil';
import React from 'react';

import 'TestRoot/SpecUtil';

describe('ArtifactoryRepositoryDeleteConfigurationModal', function () {
  let renderComponent, spySelectArtifactoryRepositoryDeleteConfigurationModalSlice;

  beforeEach(() => {
    spySelectArtifactoryRepositoryDeleteConfigurationModalSlice = jest.spyOn(
      artifactoryRepositoryConfigurationModalSelectors,
      'selectArtifactoryRepositoryDeleteConfigurationModalSlice'
    );

    spySelectArtifactoryRepositoryDeleteConfigurationModalSlice.mockReturnValue({
      ...getInitialState(),
      showModal: true,
    });

    renderComponent = () => render(<ArtifactoryRepositoryDeleteConfigurationModal />);
  });

  it('renders the delete configuration modal', function () {
    spySelectArtifactoryRepositoryDeleteConfigurationModalSlice.mockReturnValue({
      ...getInitialState(),
      showModal: true,
    });
    renderComponent();
    expect(screen.getByText('Delete Repository Configuration?')).toBeInTheDocument();
  });

  it('renders the delete configuration error', function () {
    spySelectArtifactoryRepositoryDeleteConfigurationModalSlice.mockReturnValue({
      ...getInitialState(),
      showModal: true,
      deleteConfigurationError: 'someError',
    });
    renderComponent();
    expect(screen.getByText('Unable to delete the configured repository. someError')).toBeInTheDocument();
  });
});
