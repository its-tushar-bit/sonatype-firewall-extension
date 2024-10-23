/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen, waitFor } from 'TestRoot/SpecUtil';

import SbomApplicationsTable from 'MainRoot/sbomManager/features/sbomApplicationsPage/sbomApplicationsTable/SbomApplicationsTable';

import {
  APPLICATIONS_PER_PAGE,
  SORT_BY_FIELDS,
  SORT_DIRECTION,
} from 'MainRoot/sbomManager/features/sbomApplicationsPage/sbomApplicationsTable/sbomApplicationsTableSlice';

describe('SbomApplicationsTable', () => {
  const initialState = Object.freeze({
    sbomApplicationsPage: {
      sbomApplicationsTable: {
        loading: false,
        errorMessage: null,
        applications: [],
        totalApplicationsCount: 0,
        sortConfiguration: { sortBy: SORT_BY_FIELDS[0], sortDirection: SORT_DIRECTION.ASC },
        pagination: { page: 1, pageSize: APPLICATIONS_PER_PAGE },
      },
    },
  });

  const renderComponent = (props, preloadedState) => render(<SbomApplicationsTable {...props} />, { preloadedState });

  // TODO: This is a placeholder test until SbomApplicationsTable is fully implemented.
  it('renders a table', async () => {
    renderComponent(initialState);

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    expect(screen.getByTestId('sbom-manager-applications-table')).toBeVisible();
  });
});
