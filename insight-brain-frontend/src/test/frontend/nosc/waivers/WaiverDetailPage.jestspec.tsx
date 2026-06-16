/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, waitFor } from '@testing-library/react';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import WaiverDetailPage from 'MainRoot/nosc/waivers/WaiverDetailPage';
import { getWaiverDetailsUrl } from 'MainRoot/util/CLMLocation';
import { formatDateUtcYYYYMMDD } from 'MainRoot/util/dateUtils';
import { _setBaseUrlForTesting } from 'MainRoot/util/urlUtil';
import { renderNexusOneRoute } from 'TestRoot/nosc/renderNexusOneRoute';

const ROUTE = {
  ownerType: 'application',
  ownerId: 'app-internal-1',
  waiverId: 'w-xyz',
};

// Mount the page at its UI-Router state so it reads params from the router.
function renderDetail(params: Record<string, string> = ROUTE) {
  return renderNexusOneRoute(<WaiverDetailPage />, 'nexusOneWaiverDetail', params);
}

describe('WaiverDetailPage', () => {
  let axiosMock: any;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
    _setBaseUrlForTesting('http://localhost');
  });

  afterEach(() => {
    axiosMock.reset();
  });

  function reply(body: unknown) {
    axiosMock
      .onGet(getWaiverDetailsUrl(ROUTE.ownerType, ROUTE.ownerId, ROUTE.waiverId))
      .reply(200, body);
  }

  it('renders policy + threat + scope + constraint conditions on success', async () => {
    reply({
      id: ROUTE.waiverId,
      threatLevel: 9,
      policyName: 'Critical CVSS 9+',
      ownerId: ROUTE.ownerId,
      ownerType: ROUTE.ownerType,
      scope: 'Application: Apple - Java',
      scopeOwnerType: 'application',
      scopeOwnerName: 'Apple - Java',
      createTime: '2026-05-01T10:00:00Z',
      expiryTime: '2026-12-31T00:00:00Z',
      creatorName: 'msjohnson',
      reasonText: 'Risk accepted by AppSec',
      comment: 'Approved in CHG-12345',
      constraintFacts: [
        {
          constraintName: 'Severity 9+',
          conditionFacts: [{ reason: 'CVSS Score >= 9.0' }, { reason: 'Has fix available' }],
        },
      ],
    });

    renderDetail();

    await waitFor(() => {
      expect(screen.getByText('Critical CVSS 9+')).toBeInTheDocument();
    });
    expect(screen.getByText(/Threat 9/i)).toBeInTheDocument();
    expect(screen.getByText('Apple - Java')).toBeInTheDocument();
    // Constraint + conditions
    expect(screen.getByText('Severity 9+')).toBeInTheDocument();
    expect(screen.getByText('CVSS Score >= 9.0')).toBeInTheDocument();
    expect(screen.getByText('Has fix available')).toBeInTheDocument();
    // Reason + comment
    expect(screen.getByText('Risk accepted by AppSec')).toBeInTheDocument();
    expect(screen.getByText('Approved in CHG-12345')).toBeInTheDocument();
    // Lifecycle dates (UTC calendar day via waiverDisplayUtils)
    expect(
      screen.getByText(formatDateUtcYYYYMMDD('2026-05-01T10:00:00Z')),
    ).toBeInTheDocument();
    expect(
      screen.getByText(formatDateUtcYYYYMMDD('2026-12-31T00:00:00Z')),
    ).toBeInTheDocument();
    expect(screen.getByText('msjohnson')).toBeInTheDocument();
    // "Continue in Classic" deep-link with all 3 segments
    const classicLink = screen.getByTestId('preview-waiver-detail-classic-link');
    expect(classicLink).toHaveAttribute(
      'href',
      expect.stringContaining(`/waiver/details/${ROUTE.ownerType}/${ROUTE.ownerId}/${ROUTE.waiverId}/waiver`)
    );
  });

  it('renders an Auto-waiver badge and "Auto" expiry when isAutoWaiver=true', async () => {
    reply({
      id: ROUTE.waiverId,
      threatLevel: 5,
      policyName: 'Auto-managed waiver',
      ownerId: ROUTE.ownerId,
      ownerType: ROUTE.ownerType,
      scope: 'Application: Apple - Java',
      scopeOwnerName: 'Apple - Java',
      isAutoWaiver: true,
    });
    renderDetail();
    await waitFor(() => {
      expect(screen.getByText('Auto-waiver')).toBeInTheDocument();
    });
    expect(screen.getByText(/auto \(managed by iq\)/i)).toBeInTheDocument();
  });

  it('shows "Does not expire" when there is no expiryTime and not auto', async () => {
    reply({
      id: ROUTE.waiverId,
      threatLevel: 3,
      policyName: 'Indefinite waiver',
      ownerId: ROUTE.ownerId,
      ownerType: ROUTE.ownerType,
      scope: 'Org: Root',
      scopeOwnerName: 'Root',
    });
    renderDetail();
    await waitFor(() => {
      expect(screen.getByText('Does not expire')).toBeInTheDocument();
    });
  });

  it('renders an error card on 500 with a Retry link', async () => {
    axiosMock
      .onGet(getWaiverDetailsUrl(ROUTE.ownerType, ROUTE.ownerId, ROUTE.waiverId))
      .reply(500, 'boom');
    renderDetail();
    await waitFor(() => {
      expect(screen.getByTestId('preview-waiver-detail-error')).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
  });

  it('maps root_organization in the URL to organization for the details API', async () => {
    const ownerId = 'root-org-internal';
    const waiverId = 'w-root';
    axiosMock
      .onGet(getWaiverDetailsUrl('organization', ownerId, waiverId))
      .reply(200, {
        id: waiverId,
        threatLevel: 1,
        policyName: 'Root org waiver',
        ownerId,
        ownerType: 'organization',
        scope: 'Org: Root',
        scopeOwnerName: 'Root',
      });

    renderNexusOneRoute(<WaiverDetailPage />, 'nexusOneWaiverDetail', {
      ownerType: 'root_organization',
      ownerId,
      waiverId,
    });

    await waitFor(() => {
      expect(screen.getByText('Root org waiver')).toBeInTheDocument();
    });
    expect(axiosMock.history.get).toHaveLength(1);
    expect(axiosMock.history.get[0].url).toBe(getWaiverDetailsUrl('organization', ownerId, waiverId));
  });

  it('maps all_repositories in the URL to repository_container for the details API', async () => {
    const ownerId = 'repo-container-1';
    const waiverId = 'w-repo';
    axiosMock
      .onGet(getWaiverDetailsUrl('repository_container', ownerId, waiverId))
      .reply(200, {
        id: waiverId,
        threatLevel: 3,
        policyName: 'Repository waiver',
        ownerId,
        ownerType: 'repository_container',
        scope: 'All repositories',
        scopeOwnerName: 'All repositories',
      });

    renderNexusOneRoute(<WaiverDetailPage />, 'nexusOneWaiverDetail', {
      ownerType: 'all_repositories',
      ownerId,
      waiverId,
    });

    await waitFor(() => {
      expect(screen.getByText('Repository waiver')).toBeInTheDocument();
    });
    expect(axiosMock.history.get).toHaveLength(1);
    expect(axiosMock.history.get[0].url).toBe(
      getWaiverDetailsUrl('repository_container', ownerId, waiverId),
    );
  });

  it('back link goes to /waivers', async () => {
    reply({
      id: ROUTE.waiverId,
      threatLevel: 1,
      ownerId: ROUTE.ownerId,
      ownerType: ROUTE.ownerType,
      scope: 'Org: Root',
    });
    renderDetail();
    await waitFor(() => {
      expect(screen.getByTestId('preview-waiver-detail-back-link')).toBeInTheDocument();
    });
    const back = screen.getByTestId('preview-waiver-detail-back-link');
    expect(back).toHaveAttribute('href', '#/waivers');
  });
});
