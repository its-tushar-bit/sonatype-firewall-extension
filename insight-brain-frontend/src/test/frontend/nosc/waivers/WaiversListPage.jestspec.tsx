/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, waitFor } from '@testing-library/react';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import WaiversListPage from 'MainRoot/nosc/waivers/WaiversListPage';
import { getWaiversAndAutoWaiversUrl } from 'MainRoot/util/CLMLocation';
import { _setBaseUrlForTesting } from 'MainRoot/util/urlUtil';
import { renderNexusOneRoute } from 'TestRoot/nosc/renderNexusOneRoute';

describe('WaiversListPage', () => {
  let axiosMock: any;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
    _setBaseUrlForTesting('http://localhost');
  });

  afterEach(() => {
    axiosMock.reset();
  });

  const renderPage = () => renderNexusOneRoute(<WaiversListPage />, 'nexusOneWaivers');

  function reply(dashboardResults: ReadonlyArray<unknown>, hasNextPage = false) {
    axiosMock
      .onPost(getWaiversAndAutoWaiversUrl())
      .reply(200, { dashboardResults, hasNextPage });
  }

  it('renders skeleton while loading, then a row per waiver', async () => {
    reply([
      {
        id: 'w1',
        threatLevel: 9,
        createTime: '2026-05-01T10:00:00Z',
        expiryTime: '2026-12-31T00:00:00Z',
        policyName: 'Critical CVSS 9+',
        ownerId: 'app-internal-1',
        ownerName: 'Apple - Java',
        ownerType: 'application',
        scope: 'Application: Apple - Java',
      },
      {
        id: 'w2',
        threatLevel: 4,
        createTime: '2026-04-15T10:00:00Z',
        expiryTime: null,
        policyName: 'Moderate licensing',
        ownerId: 'org-root',
        ownerType: 'organization',
        scope: 'Root Organization',
        isAutoWaiver: true,
      },
    ]);

    renderPage();
    expect(screen.getByTestId('nosc-waivers-list-table-loading')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Critical CVSS 9+')).toBeInTheDocument();
    });
    expect(screen.getByText('Moderate licensing')).toBeInTheDocument();
    // Auto-waiver shows up at least once (badge in Threat cell + "Auto" in
    // expiry cell when there's no expiryTime).
    expect(screen.getAllByText('Auto').length).toBeGreaterThanOrEqual(1);
  });

  it('renders a calm empty state when there are zero waivers', async () => {
    reply([]);
    renderPage();
    await waitFor(() => {
      expect(screen.getByTestId('nosc-waivers-list-table-empty')).toBeInTheDocument();
    });
    expect(screen.getByText(/no waivers in scope/i)).toBeInTheDocument();
  });

  it('renders an error state on 500 with a Retry link', async () => {
    axiosMock.onPost(getWaiversAndAutoWaiversUrl()).reply(500, 'oops');
    renderPage();
    await waitFor(() => {
      expect(screen.getByTestId('nosc-waivers-list-table-error')).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
  });

  it('every row links to the native Waiver Detail page', async () => {
    reply([
      {
        id: 'w-abc-123',
        threatLevel: 7,
        ownerId: 'app-internal-1',
        ownerType: 'application',
        scope: 'Application: Apple - Java',
        policyName: 'Severe CVSS',
        createTime: '2026-05-01T00:00:00Z',
      },
    ]);
    renderPage();
    const link = await screen.findByRole('link', { name: /view details/i });
    expect(link).toHaveAttribute(
      'href',
      expect.stringContaining('/waivers/application/app-internal-1/w-abc-123')
    );
  });

  it('shows truncation hint when hasNextPage is true', async () => {
    reply(
      Array.from({ length: 100 }, (_, i) => ({
        id: `w${i}`,
        threatLevel: 5,
        ownerId: 'org-root',
        ownerType: 'organization',
        scope: 'Root',
        policyName: 'p',
        createTime: '2026-05-01T00:00:00Z',
      })),
      true
    );
    renderPage();
    await waitFor(() => {
      expect(screen.getByTestId('preview-waivers-truncated')).toBeInTheDocument();
    });
  });
});
