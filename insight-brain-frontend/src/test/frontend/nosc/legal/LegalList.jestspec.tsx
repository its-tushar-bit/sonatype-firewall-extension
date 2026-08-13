/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { _setBaseUrlForTesting } from 'MainRoot/util/urlUtil';
import { renderNexusOneRoute } from 'TestRoot/nosc/renderNexusOneRoute';
import LegalList from 'MainRoot/nosc/legal/LegalList';
import { NEXUS_ONE_LEGAL_STATE_NAME } from 'MainRoot/nosc/legal/legalRoute';
import { createDefaultLegalFilterState, hasActiveLegalFilters } from 'MainRoot/nosc/legal/legalListApi';
import { getLegalListUrl } from 'MainRoot/util/CLMLocation';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import { installRadixJsdomShims } from 'TestRoot/nosc/shell/radixJsdomShims';

const MOCK_LEGAL_LIST_RESPONSE = {
  findings: [
    {
      legalFindingId: 'app|hash|lic|build',
      threatLevel: 8,
      severity: 'severe',
      licenseName: 'GPL-2.0-only',
      licenseThreatGroupName: 'Copyleft',
      organizationId: 'org-1',
      organizationName: 'Java-team',
      applicationId: 'app-1',
      applicationPublicId: 'apple-java1',
      applicationName: 'Apple - Java',
      componentName: 'com.example : lib : 1.0',
      componentVersion: '1.0',
      componentHash: 'abc123hash',
      reportId: 'scan-report-1',
      stage: 'build',
    },
  ],
  facets: {
    totalFindings: 1,
    licenseThreatGroups: { Copyleft: 1 },
    organizations: { 'org-1': 1 },
    applications: { 'app-1': 1 },
    organizationNames: { 'org-1': 'Java-team' },
    applicationNames: { 'app-1': 'Apple - Java' },
  },
  total: 1,
  page: 0,
  pageSize: 25,
  hasNextPage: false,
  source: 'index',
};

describe('LegalList (CLM-43207)', () => {
  let axiosMock: ReturnType<typeof axiosMockAdapter>;
  let user: ReturnType<typeof userEvent.setup>;
  let alpSpy: jest.SpyInstance;

  beforeAll(() => {
    _setBaseUrlForTesting('http://localhost');
    installRadixJsdomShims();
  });

  beforeEach(() => {
    axiosMock = axiosMockAdapter();
    user = userEvent.setup();
    alpSpy = jest
      .spyOn(productFeaturesSelectors, 'selectIsAdvancedLegalPackSupported')
      .mockReturnValue(false);
  });

  afterEach(() => {
    axiosMock.restore();
    alpSpy.mockRestore();
  });

  const renderList = (params: Record<string, unknown> = {}) =>
    renderNexusOneRoute(<LegalList />, NEXUS_ONE_LEGAL_STATE_NAME, params);

  it('renders Legal title and page shell', async () => {
    axiosMock.onPost(getLegalListUrl()).reply(200, MOCK_LEGAL_LIST_RESPONSE);
    renderList();

    expect(await screen.findByTestId('preview-legal-page')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Legal' })).toBeInTheDocument();
    expect(screen.getByTestId('violations-filter-rail')).toBeInTheDocument();
  });

  it('renders finding cards with LTG title, component, org, app, and threat badge', async () => {
    axiosMock.onPost(getLegalListUrl()).reply(200, MOCK_LEGAL_LIST_RESPONSE);
    renderList();

    const card = await screen.findByTestId('violation-card');
    expect(within(card).getByText('Copyleft')).toBeInTheDocument();
    expect(within(card).getByText('com.example : lib : 1.0')).toBeInTheDocument();
    expect(within(card).getByText('Java-team')).toBeInTheDocument();
    expect(within(card).getByText('Apple - Java')).toBeInTheDocument();
    expect(within(card).getByTestId('violation-threat-badge')).toHaveTextContent('8');
  });

  it('hides OPEN/WAIVED state badges on Legal cards', async () => {
    axiosMock.onPost(getLegalListUrl()).reply(200, MOCK_LEGAL_LIST_RESPONSE);
    renderList();

    const card = await screen.findByTestId('violation-card');
    expect(within(card).queryByText('Open')).not.toBeInTheDocument();
    expect(within(card).queryByText('Waived')).not.toBeInTheDocument();
    expect(within(card).queryByTestId('violation-card-waiver')).not.toBeInTheDocument();
    expect(within(card).queryByTestId('violation-card-auto-waiver')).not.toBeInTheDocument();
  });

  it('hides Classic violations CSV export and brands toolbar count / search aria', async () => {
    axiosMock.onPost(getLegalListUrl()).reply(200, MOCK_LEGAL_LIST_RESPONSE);
    renderList();

    await screen.findByTestId('violation-card-grid');
    expect(screen.queryByTestId('violations-toolbar-csv')).not.toBeInTheDocument();
    expect(screen.getByTestId('violations-toolbar-count')).toHaveTextContent('1 license risk finding');
    expect(screen.getByTestId('violations-toolbar-search')).toHaveAttribute(
      'aria-label',
      'Search license risk findings',
    );
  });

  it('links cards to Classic report Component Legal tab without ALP', async () => {
    alpSpy.mockReturnValue(false);
    axiosMock.onPost(getLegalListUrl()).reply(200, MOCK_LEGAL_LIST_RESPONSE);
    renderList();

    await screen.findByTestId('violation-card-grid');
    const link = screen.getByTestId('violation-card-link');
    expect(link).toHaveAttribute(
      'href',
      '#/applicationReport/apple-java1/scan-report-1/componentDetails/abc123hash/legal',
    );
  });

  it('links cards to Classic ALP Legal component overview when ALP is licensed', async () => {
    alpSpy.mockReturnValue(true);
    axiosMock.onPost(getLegalListUrl()).reply(200, MOCK_LEGAL_LIST_RESPONSE);
    renderList();

    await screen.findByTestId('violation-card-grid');
    expect(screen.getByTestId('violation-card-link')).toHaveAttribute(
      'href',
      '#/legal/component/abc123hash',
    );
  });

  it('shows License Threat Group filter and hides state / waiver filters', async () => {
    axiosMock.onPost(getLegalListUrl()).reply(200, MOCK_LEGAL_LIST_RESPONSE);
    renderList();

    await screen.findByTestId('violation-card-grid');
    const rail = screen.getByTestId('violations-filter-rail');
    expect(within(rail).getByTestId('violations-filter-policy-type')).toBeInTheDocument();
    expect(within(rail).getByText('License Threat Group')).toBeInTheDocument();
    expect(within(rail).getByText('Copyleft')).toBeInTheDocument();
    expect(within(rail).queryByTestId('violations-filter-state')).not.toBeInTheDocument();
    expect(within(rail).queryByTestId('violations-filter-waiver-type')).not.toBeInTheDocument();
  });

  it('writes selected LTG names to the category URL param', async () => {
    axiosMock.onPost(getLegalListUrl()).reply(200, MOCK_LEGAL_LIST_RESPONSE);
    const { router } = renderList();

    await screen.findByTestId('violation-card-grid');
    await user.click(screen.getByTestId('violations-filter-policy-type-option-Copyleft'));

    await waitFor(() => expect(router.urlService.url()).toContain('category=Copyleft'));
    await waitFor(() => {
      const last = JSON.parse(axiosMock.history.post[axiosMock.history.post.length - 1].data);
      expect(last.licenseThreatGroupNames).toEqual(['Copyleft']);
    });
  });

  it('hydrates LTG selection from a bookmarked category URL', async () => {
    axiosMock.onPost(getLegalListUrl()).reply(200, MOCK_LEGAL_LIST_RESPONSE);
    renderList({ category: 'Copyleft' });

    await screen.findByTestId('violation-card-grid');
    expect(screen.getByTestId('violations-filter-policy-type-option-Copyleft')).toBeChecked();
    await waitFor(() => {
      const hydrated = axiosMock.history.post.find((request) => {
        const body = JSON.parse(String(request.data));
        return Array.isArray(body.licenseThreatGroupNames) && body.licenseThreatGroupNames.includes('Copyleft');
      });
      expect(hydrated).toBeDefined();
    });
  });

  it('posts to legal/list with license-threat sort (not policyThreatCategories)', async () => {
    axiosMock.onPost(getLegalListUrl()).reply(200, MOCK_LEGAL_LIST_RESPONSE);
    renderList();

    await screen.findByTestId('violation-card-grid');
    expect(axiosMock.history.post[0].url).toContain('/rest/dashboard/legal/list');
    const body = JSON.parse(axiosMock.history.post[0].data);
    expect(body.orderBy).toBe('-licenseThreatLevel');
    expect(body.policyThreatCategories).toBeUndefined();
  });

  it('shows Legal-branded error title on list failure', async () => {
    axiosMock.onPost(getLegalListUrl()).reply(500, { message: 'boom' });
    renderList();

    expect(await screen.findByTestId('violations-list-error')).toBeInTheDocument();
    // Banner variant concatenates title + message into one text node.
    expect(screen.getByText(/Failed to load license risk findings/i)).toBeInTheDocument();
  });

  it('shows filter-aware Legal empty copy when active filters return nothing', async () => {
    axiosMock.onPost(getLegalListUrl()).reply(200, {
      ...MOCK_LEGAL_LIST_RESPONSE,
      findings: [],
      total: 0,
      facets: { ...MOCK_LEGAL_LIST_RESPONSE.facets, totalFindings: 0 },
    });
    renderList({ category: 'Copyleft' });

    expect(await screen.findByTestId('violations-list-empty')).toBeInTheDocument();
    expect(screen.getByText('No license risk findings match your filters.')).toBeInTheDocument();
    expect(screen.getByText('Try adjusting or resetting your filters.')).toBeInTheDocument();
    expect(screen.getByTestId('violations-empty-reset-filters')).toBeInTheDocument();
  });

  it('shows search-aware Legal empty copy when a committed search returns nothing', async () => {
    axiosMock.onPost(getLegalListUrl()).reply(200, {
      ...MOCK_LEGAL_LIST_RESPONSE,
      findings: [],
      total: 0,
      facets: { ...MOCK_LEGAL_LIST_RESPONSE.facets, totalFindings: 0 },
    });
    renderList({ q: 'gpl' });

    expect(await screen.findByTestId('violations-list-empty')).toBeInTheDocument();
    expect(screen.getByText('No license risk findings match your search.')).toBeInTheDocument();
    expect(screen.getByText('Try adjusting or clearing your search.')).toBeInTheDocument();
  });
});

describe('hasActiveLegalFilters', () => {
  it('is false for defaults and true when LTG / stage / org / app / threat are narrowed', () => {
    expect(hasActiveLegalFilters(createDefaultLegalFilterState())).toBe(false);
    expect(
      hasActiveLegalFilters({
        ...createDefaultLegalFilterState(),
        threatCategories: new Set(['Copyleft']),
      }),
    ).toBe(true);
    expect(
      hasActiveLegalFilters({
        ...createDefaultLegalFilterState(),
        stageIds: new Set(['build']),
      }),
    ).toBe(true);
    expect(
      hasActiveLegalFilters({
        ...createDefaultLegalFilterState(),
        organizationIds: new Set(['org-1']),
      }),
    ).toBe(true);
    expect(
      hasActiveLegalFilters({
        ...createDefaultLegalFilterState(),
        applicationIds: new Set(['app-1']),
      }),
    ).toBe(true);
    expect(
      hasActiveLegalFilters({
        ...createDefaultLegalFilterState(),
        threatRange: [7, 10],
      }),
    ).toBe(true);
    // Violation-state narrowing must not count for Legal (LEGAL_VIOLATION has no OPEN/WAIVED).
    expect(
      hasActiveLegalFilters({
        ...createDefaultLegalFilterState(),
        states: new Set(['OPEN']),
      }),
    ).toBe(false);
  });
});
