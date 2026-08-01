/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, waitFor, within } from '@testing-library/react';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import WaiverDetailPage from 'MainRoot/nosc/waivers/WaiverDetailPage';
import { getWaiverDetailsUrl, getAutoWaiversConfigurationURLWaiver } from 'MainRoot/util/CLMLocation';
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

  it('renders policy, threat, constraint blurb and the Waiver Details card on success', async () => {
    reply({
      id: ROUTE.waiverId,
      threatLevel: 9,
      policyName: 'Critical CVSS 9+',
      ownerId: ROUTE.ownerId,
      ownerType: ROUTE.ownerType,
      scopeOwnerType: 'application',
      scopeOwnerName: 'Apple - Java',
      createTime: '2026-05-01T10:00:00Z',
      expiryTime: '2036-12-31T00:00:00Z',
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
    expect(screen.getByLabelText('Threat level 9')).toBeInTheDocument();

    // Constraint blurb reads "{constraint} is in violation for:" + condition bullets.
    const constraint = screen.getByTestId('preview-waiver-detail-constraint');
    expect(constraint).toHaveTextContent('Severity 9+ is in violation for:');
    expect(within(constraint).getByText('CVSS Score >= 9.0')).toBeInTheDocument();
    expect(within(constraint).getByText('Has fix available')).toBeInTheDocument();

    // Waiver Details card rows.
    expect(screen.getByTestId('preview-waiver-detail-created')).toHaveTextContent(
      `${formatDateUtcYYYYMMDD('2026-05-01T10:00:00Z')} by msjohnson`,
    );
    expect(screen.getByTestId('preview-waiver-detail-reason')).toHaveTextContent(
      'Risk accepted by AppSec',
    );
    expect(screen.getByTestId('preview-waiver-detail-comments')).toHaveTextContent(
      'Approved in CHG-12345',
    );

    // Wave C: Classic escapes live in TopNav only — no per-entity Classic CTA.
    expect(screen.queryByTestId('preview-waiver-detail-classic-link')).not.toBeInTheDocument();
  });

  it('composes the Scope, Component and Expires meta strip from the v2 payload', async () => {
    reply({
      id: ROUTE.waiverId,
      threatLevel: 8,
      policyName: 'Security-High',
      ownerId: ROUTE.ownerId,
      ownerType: ROUTE.ownerType,
      scopeOwnerType: 'repository_manager',
      scopeOwnerName: 'Repository Manager',
      expiryTime: '2036-07-02T00:00:00Z',
      componentIdentifier: {
        format: 'maven',
        coordinates: { artifactId: 'shiro-core', version: '1.13.0' },
      },
    });

    renderDetail();

    await screen.findByTestId('preview-waiver-detail-meta');
    expect(screen.getByTestId('preview-waiver-detail-meta-scope')).toHaveTextContent(
      'Repository Manager (Repository Manager)',
    );
    expect(screen.getByTestId('preview-waiver-detail-meta-component')).toHaveTextContent(
      'shiro-core:1.13.0',
    );
    expect(screen.getByTestId('preview-waiver-detail-meta-expires')).toHaveTextContent(
      formatDateUtcYYYYMMDD('2036-07-02T00:00:00Z'),
    );
  });

  it('flags a past expiry as Expired in both the meta strip and the details card', async () => {
    reply({
      id: ROUTE.waiverId,
      threatLevel: 8,
      policyName: 'Lapsed waiver',
      ownerId: ROUTE.ownerId,
      ownerType: ROUTE.ownerType,
      scopeOwnerName: 'Apple - Java',
      expiryTime: '2020-01-01T00:00:00Z',
    });

    renderDetail();

    await waitFor(() => {
      expect(screen.getByTestId('preview-waiver-detail-meta-expired-badge')).toBeInTheDocument();
    });
    expect(screen.getByTestId('preview-waiver-detail-expired-badge')).toBeInTheDocument();
    expect(screen.getByTestId('preview-waiver-detail-expires')).toHaveTextContent('Expired');
  });

  it('hides the Component meta chip for container-image waivers', async () => {
    reply({
      id: ROUTE.waiverId,
      threatLevel: 8,
      policyName: 'Container waiver',
      ownerId: ROUTE.ownerId,
      ownerType: ROUTE.ownerType,
      scopeOwnerName: 'Apple - Java',
      forContainerImage: true,
      componentIdentifier: {
        format: 'maven',
        coordinates: { artifactId: 'ignored', version: '1.0.0' },
      },
    });

    renderDetail();

    await waitFor(() => {
      expect(screen.getByText('Container image')).toBeInTheDocument();
    });
    expect(screen.queryByTestId('preview-waiver-detail-meta-component')).not.toBeInTheDocument();
  });

  it('shows "Never" when there is no expiry and the waiver is not auto', async () => {
    reply({
      id: ROUTE.waiverId,
      threatLevel: 3,
      policyName: 'Indefinite waiver',
      ownerId: ROUTE.ownerId,
      ownerType: ROUTE.ownerType,
      scopeOwnerName: 'Root',
    });
    renderDetail();
    await waitFor(() => {
      expect(screen.getByTestId('preview-waiver-detail-expires')).toHaveTextContent('Never');
    });
    expect(screen.queryByTestId('preview-waiver-detail-expired-badge')).not.toBeInTheDocument();
    expect(screen.queryByTestId('preview-waiver-detail-meta-expired-badge')).not.toBeInTheDocument();
  });

  it('renders an Auto-waiver badge and Auto expiry, fetched from the autoPolicyWaivers API', async () => {
    // Auto-waivers live in a different table/API than manual waivers, so the
    // route must carry `type=autoWaiver` for the page to fetch the right one
    // (see the dedicated 404-reproduction test below for what happens without it).
    axiosMock.onGet(getAutoWaiversConfigurationURLWaiver(ROUTE.ownerType, ROUTE.ownerId, ROUTE.waiverId)).reply(200, {
      autoPolicyWaiverId: ROUTE.waiverId,
      ownerId: ROUTE.ownerId,
      ownerType: ROUTE.ownerType,
      ownerName: 'Apple - Java',
      threatLevel: 5,
      createTime: '2026-01-01T00:00:00Z',
      creatorName: 'admin',
    });

    renderNexusOneRoute(<WaiverDetailPage />, 'nexusOneWaiverDetail', { ...ROUTE, type: 'autoWaiver' });

    await waitFor(() => {
      expect(screen.getByText('Auto-waiver')).toBeInTheDocument();
    });
    expect(screen.getByTestId('preview-waiver-detail-meta-expires')).toHaveTextContent(
      'Auto (managed by IQ)',
    );
    expect(screen.getByTestId('preview-waiver-detail-expires')).toHaveTextContent(
      'Auto (managed by IQ)',
    );
    expect(screen.queryByTestId('preview-waiver-detail-expired-badge')).not.toBeInTheDocument();
    expect(screen.queryByTestId('preview-waiver-detail-meta-expired-badge')).not.toBeInTheDocument();
    // Never hit the manual-waiver endpoint — that's the 404 this fix avoids.
    expect(
      axiosMock.history.get.some(
        (r) => r.url === getWaiverDetailsUrl(ROUTE.ownerType, ROUTE.ownerId, ROUTE.waiverId),
      ),
    ).toBe(false);
    // Wave C: Classic escapes live in TopNav only — no per-entity Classic CTA.
    expect(screen.queryByTestId('preview-waiver-detail-classic-link')).not.toBeInTheDocument();
  });

  it('loads a root-org auto-waiver from autoPolicyWaivers without 404ing (CLM-43502)', async () => {
    // Reproduces the reported bug: opening a root-org auto-waiver used to always
    // call /api/v2/policyWaivers/organization/{id}/{waiverId} (404, auto-waivers
    // aren't in that table) regardless of the already-correct owner-type mapping.
    const ownerId = 'ROOT_ORGANIZATION_ID';
    const waiverId = 'auto-w-root';
    axiosMock.onGet(getAutoWaiversConfigurationURLWaiver('organization', ownerId, waiverId)).reply(200, {
      autoPolicyWaiverId: waiverId,
      ownerId,
      ownerType: 'organization',
      ownerName: 'Root Organization',
      threatLevel: 7,
      createTime: '2026-01-01T00:00:00Z',
      creatorName: 'admin',
    });
    axiosMock.onGet(getWaiverDetailsUrl('organization', ownerId, waiverId)).reply(404);

    renderNexusOneRoute(<WaiverDetailPage />, 'nexusOneWaiverDetail', {
      ownerType: 'root_organization',
      ownerId,
      waiverId,
      type: 'autoWaiver',
    });

    await waitFor(() => {
      expect(screen.getByText('Auto-waiver')).toBeInTheDocument();
    });
    expect(screen.queryByTestId('preview-waiver-detail-header-error')).not.toBeInTheDocument();
    expect(axiosMock.history.get).toHaveLength(1);
    expect(axiosMock.history.get[0].url).toBe(
      getAutoWaiversConfigurationURLWaiver('organization', ownerId, waiverId),
    );
  });

  it('falls back to an empty-state blockquote when there is no comment', async () => {
    reply({
      id: ROUTE.waiverId,
      threatLevel: 1,
      policyName: 'Quiet waiver',
      ownerId: ROUTE.ownerId,
      ownerType: ROUTE.ownerType,
      scopeOwnerName: 'Root',
    });
    renderDetail();
    await waitFor(() => {
      expect(screen.getByTestId('preview-waiver-detail-comments')).toHaveTextContent(
        'No additional comments',
      );
    });
  });

  it('renders an error card on 500 with a Retry link', async () => {
    axiosMock
      .onGet(getWaiverDetailsUrl(ROUTE.ownerType, ROUTE.ownerId, ROUTE.waiverId))
      .reply(500, 'boom');
    renderDetail();
    await waitFor(() => {
      expect(screen.getByTestId('preview-waiver-detail-header-error')).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
    expect(screen.queryByTestId('preview-waiver-detail-body')).not.toBeInTheDocument();
  });

  it('maps root_organization in the URL to organization for the details API', async () => {
    const ownerId = 'root-org-internal';
    const waiverId = 'w-root';
    axiosMock.onGet(getWaiverDetailsUrl('organization', ownerId, waiverId)).reply(200, {
      id: waiverId,
      threatLevel: 1,
      policyName: 'Root org waiver',
      ownerId,
      ownerType: 'organization',
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
    axiosMock.onGet(getWaiverDetailsUrl('repository_container', ownerId, waiverId)).reply(200, {
      id: waiverId,
      threatLevel: 3,
      policyName: 'Repository waiver',
      ownerId,
      ownerType: 'repository_container',
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

  it('lowercases enum-style owner types coming from list deep-links', async () => {
    const ownerId = 'app-from-ana';
    const waiverId = 'w-ana';
    axiosMock.onGet(getWaiverDetailsUrl('application', ownerId, waiverId)).reply(200, {
      id: waiverId,
      threatLevel: 5,
      policyName: 'Ana deep-link waiver',
      ownerId,
      ownerType: 'application',
      scopeOwnerName: 'Apple - Java',
    });

    renderNexusOneRoute(<WaiverDetailPage />, 'nexusOneWaiverDetail', {
      ownerType: 'APPLICATION',
      ownerId,
      waiverId,
    });

    await waitFor(() => {
      expect(screen.getByText('Ana deep-link waiver')).toBeInTheDocument();
    });
    expect(axiosMock.history.get[0].url).toBe(
      getWaiverDetailsUrl('application', ownerId, waiverId),
    );
  });

  it('breadcrumb links back to /waivers and names the current page', async () => {
    reply({
      id: ROUTE.waiverId,
      threatLevel: 1,
      ownerId: ROUTE.ownerId,
      ownerType: ROUTE.ownerType,
      scopeOwnerName: 'Root',
    });
    renderDetail();
    await waitFor(() => {
      expect(screen.getByTestId('preview-waiver-detail-back-link')).toBeInTheDocument();
    });
    expect(screen.getByTestId('preview-waiver-detail-back-link')).toHaveAttribute(
      'href',
      '#/waivers',
    );
    expect(screen.getByTestId('preview-waiver-detail-breadcrumb')).toHaveTextContent(
      /Waivers\s*\/\s*Waiver Details/,
    );
  });

  it('renders Overview as the only tab without Classic escapes', async () => {
    reply({
      id: ROUTE.waiverId,
      threatLevel: 1,
      policyName: 'Layout waiver',
      ownerId: ROUTE.ownerId,
      ownerType: ROUTE.ownerType,
      scopeOwnerName: 'Apple - Java',
    });
    renderDetail();

    expect(screen.getByTestId('preview-waiver-detail-page')).toBeInTheDocument();
    const tabs = screen.getByTestId('preview-waiver-detail-tabs');
    expect(within(tabs).getAllByRole('tab')).toHaveLength(1);
    expect(screen.getByTestId('preview-waiver-detail-tab-overview')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByTestId('preview-waiver-detail-header')).toBeInTheDocument();
    });

    expect(screen.queryByTestId('preview-waiver-detail-create-classic')).not.toBeInTheDocument();
    expect(screen.queryByTestId('preview-waiver-detail-classic-link')).not.toBeInTheDocument();
    expect(screen.queryByText(/classic/i)).not.toBeInTheDocument();
  });

  it('links vulnerability to native Nexus One vulnerability detail', async () => {
    reply({
      id: ROUTE.waiverId,
      threatLevel: 9,
      policyName: 'Vuln waiver',
      ownerId: ROUTE.ownerId,
      ownerType: ROUTE.ownerType,
      scopeOwnerName: 'Apple - Java',
      vulnerabilityId: 'CVE-2024-1234',
    });
    renderDetail();

    const vulnLink = await screen.findByTestId('preview-waiver-detail-vuln-link');
    expect(vulnLink).toHaveAttribute('href', '#/vulnerabilities/CVE-2024-1234');
  });

  it('related-risk context rail marks the vulnerability as current after metadata loads', async () => {
    reply({
      id: ROUTE.waiverId,
      threatLevel: 9,
      policyName: 'Rail waiver',
      ownerId: ROUTE.ownerId,
      ownerType: ROUTE.ownerType,
      scopeOwnerName: 'Apple - Java',
      vulnerabilityId: 'CVE-2024-9999',
    });
    renderDetail();

    await screen.findByTestId('preview-waiver-detail-header');
    const rail = await screen.findByTestId('preview-waiver-detail-context-rail');
    expect(within(rail).getByText('CVE-2024-9999')).toHaveAttribute('aria-current', 'page');
    expect(within(rail).queryByRole('link', { name: 'CVE-2024-9999' })).not.toBeInTheDocument();
    for (const placeholder of ['Application', 'Component', 'Violation'] as const) {
      expect(within(rail).getByText(placeholder)).toBeInTheDocument();
      expect(within(rail).queryByRole('link', { name: placeholder })).not.toBeInTheDocument();
    }
  });

  it('issues exactly one detail GET and no estate fan-out', async () => {
    reply({
      id: ROUTE.waiverId,
      threatLevel: 4,
      policyName: 'Single GET waiver',
      ownerId: ROUTE.ownerId,
      ownerType: ROUTE.ownerType,
      scopeOwnerName: 'Apple - Java',
    });
    renderDetail();

    await screen.findByTestId('preview-waiver-detail-body');
    expect(axiosMock.history.get).toHaveLength(1);
    expect(axiosMock.history.post).toHaveLength(0);
  });
});
