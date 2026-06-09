/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { render, screen, waitFor } from '@testing-library/react';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { AppsScannedTile } from 'MainRoot/nosc/dashboard/tiles/AppsScannedTile';
import { dashboardApplicationsHref } from 'MainRoot/nosc/dashboard/dashboardBundleUrls';
import { setupNexusOneBundleLocation } from 'TestRoot/nosc/dashboard/dashboardTestHrefs';
import { getApplicationsUrl } from 'MainRoot/util/CLMLocation';

describe('AppsScannedTile', () => {
  let axiosMock: any;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    setupNexusOneBundleLocation();
  });

  afterEach(() => {
    axiosMock.reset();
  });

  const renderTile = () =>
    render(
      <Theme>
        <AppsScannedTile />
      </Theme>,
    );

  it('shows skeleton, then renders the count once /rest/application resolves', async () => {
    axiosMock.onGet(getApplicationsUrl()).reply(200, [
      { id: 'a1', publicId: 'apple', name: 'Apple' },
      { id: 'a2', publicId: 'banana', name: 'Banana' },
      { id: 'a3', publicId: 'cherry', name: 'Cherry' },
    ]);

    renderTile();
    expect(screen.getByTestId('dashboard-tile-skeleton')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('3')).toBeInTheDocument();
    });

    expect(screen.getByText(/apps scanned/i)).toBeInTheDocument();
  });

  it('renders a zero count when there are no applications', async () => {
    axiosMock.onGet(getApplicationsUrl()).reply(200, []);
    renderTile();
    await waitFor(() => {
      expect(screen.getByText('0')).toBeInTheDocument();
    });
  });

  it('renders the error state when the endpoint returns 500', async () => {
    axiosMock.onGet(getApplicationsUrl()).reply(500, {});
    renderTile();
    await waitFor(() => {
      expect(screen.getByTestId('dashboard-tile-error')).toBeInTheDocument();
    });
  });

  it('tile body click-target points to /preview/dashboard/applications (S2-PR-D-5 IA wiring)', async () => {
    axiosMock.onGet(getApplicationsUrl()).reply(200, []);
    renderTile();
    await waitFor(() => {
      expect(screen.getByTestId('apps-scanned-tile-body')).toBeInTheDocument();
    });
    expect(screen.getByTestId('apps-scanned-tile-body')).toHaveAttribute(
      'href',
      dashboardApplicationsHref(),
    );
  });
});
