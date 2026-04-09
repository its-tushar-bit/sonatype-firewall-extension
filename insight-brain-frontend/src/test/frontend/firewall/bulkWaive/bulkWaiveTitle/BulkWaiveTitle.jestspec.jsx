/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import BulkWaiveTitle from 'MainRoot/firewall/bulkWaive/bulkWaiveTitle/BulkWaiveTitle';

describe('BulkWaiveTitle', () => {
  let preloadedState;

  beforeEach(() => {
    preloadedState = getDefaultPreloadedState();
  });

  it('should render the page title', () => {
    renderComponent();

    expect(screen.getByRole('heading', { name: 'Bulk Waiver' })).toBeVisible();
  });

  it('should render subtitle with repository public ID', () => {
    renderComponent();

    expect(screen.getByTestId('bulk-waiver-subtitle')).toHaveTextContent('test-public-id Repository Results');
  });

  it('should render empty subtitle when repository info is not available', () => {
    const stateWithoutRepoInfo = {
      ...preloadedState,
      repositoryResultsSummaryPage: {
        repositoryInfo: null,
      },
    };

    renderComponent(stateWithoutRepoInfo);

    expect(screen.getByTestId('bulk-waiver-subtitle')).toHaveTextContent('');
  });

  function renderComponent(additionalState = {}) {
    const finalState = { ...preloadedState, ...additionalState };
    return render(<BulkWaiveTitle />, { preloadedState: finalState });
  }

  function getDefaultPreloadedState() {
    return {
      repositoryResultsSummaryPage: {
        repositoryInfo: {
          repositoryId: 'test-repo-id',
          publicId: 'test-public-id',
          repositoryName: 'Test Repository',
        },
      },
    };
  }
});
