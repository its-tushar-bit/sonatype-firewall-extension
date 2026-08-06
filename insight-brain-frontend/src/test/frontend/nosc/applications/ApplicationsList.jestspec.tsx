/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, act, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ApplicationsList from 'MainRoot/nosc/applications/ApplicationsList';
import ApplicationsPage from 'MainRoot/nosc/applications/ApplicationsPage';
import { EMPTY_APPLICATIONS_LIST_FILTERS } from 'MainRoot/nosc/applications/applicationsListFilters';
import {
  MOCK_APPLICATION_RISK_SCORES,
  MOCK_APPLICATIONS_FILTER_FACETS,
} from 'MainRoot/nosc/applications/mockApplicationsListData';
import { getApplicationsListUrl } from 'MainRoot/util/CLMLocation';
import { _setBaseUrlForTesting } from 'MainRoot/util/urlUtil';
import router from 'MainRoot/router/routerInstance';
import { nexusOneApplicationReportStates } from 'MainRoot/nexus-one/nexusOneApplicationReportStates';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { renderNexusOneRoute } from 'TestRoot/nosc/renderNexusOneRoute';
import { installRadixJsdomShims } from 'TestRoot/nosc/shell/radixJsdomShims';

const API_LIST_RESPONSE = {
  applications: [
    {
      organizationName: 'Java-team',
      organizationId: 'org-java',
      applicationName: 'Apple - Java',
      applicationId: 'apple-java',
      totalApplicationRisk: {
        totalRisk: 47,
        criticalRisk: 3,
        severeRisk: 8,
        moderateRisk: 21,
        lowRisk: 15,
      },
      stageRisks: [
        {
          stageTypeId: 'develop',
          stageTypeName: 'Develop',
          scanId: 'scan-apple-develop',
          evaluationTime: Date.parse('2026-07-08T14:22:00Z'),
          risk: { totalRisk: 12, criticalRisk: 1, severeRisk: 2, moderateRisk: 5, lowRisk: 4 },
        },
        {
          stageTypeId: 'build',
          stageTypeName: 'Build',
          scanId: 'scan-apple-build',
          evaluationTime: Date.parse('2026-07-09T09:05:00Z'),
          risk: { totalRisk: 47, criticalRisk: 3, severeRisk: 8, moderateRisk: 21, lowRisk: 15 },
        },
      ],
    },
    {
      organizationName: 'Java-team',
      organizationId: 'org-java',
      applicationName: 'Banana - Java',
      applicationId: 'banana-java',
      totalApplicationRisk: {
        totalRisk: 12,
        criticalRisk: 0,
        severeRisk: 2,
        moderateRisk: 4,
        lowRisk: 6,
      },
      stageRisks: [
        {
          stageTypeId: 'build',
          stageTypeName: 'Build',
          scanId: 'scan-banana-build',
          evaluationTime: Date.parse('2026-07-06T11:30:00Z'),
          risk: { totalRisk: 12, criticalRisk: 0, severeRisk: 2, moderateRisk: 4, lowRisk: 6 },
        },
      ],
    },
    {
      organizationName: 'Platform',
      organizationId: 'org-platform',
      applicationName: 'Cherry - Platform',
      applicationId: 'cherry-platform',
      totalApplicationRisk: {
        totalRisk: 0,
        criticalRisk: 0,
        severeRisk: 0,
        moderateRisk: 0,
        lowRisk: 0,
      },
      stageRisks: [
        {
          stageTypeId: 'develop',
          stageTypeName: 'Develop',
          scanId: 'scan-cherry-develop',
          evaluationTime: Date.parse('2026-07-05T08:00:00Z'),
          risk: { totalRisk: 0, criticalRisk: 0, severeRisk: 0, moderateRisk: 0, lowRisk: 0 },
        },
      ],
    },
  ],
  facets: { totalApplications: 3 },
  total: 3,
  page: 0,
  pageSize: 50,
  hasNextPage: false,
  source: 'index',
};

describe('ApplicationsList (CLM-42224)', () => {
  let axiosMock: ReturnType<typeof axiosMockAdapter>;
  let listUrl: string;

  beforeAll(() => {
    installRadixJsdomShims();
    _setBaseUrlForTesting('http://localhost');
    listUrl = getApplicationsListUrl();
    axiosMock = axiosMockAdapter();
    nexusOneApplicationReportStates().forEach((state) => {
      if (!router.stateRegistry.get(state.name!)) {
        router.stateRegistry.register(state);
      }
    });
  });

  afterEach(() => {
    axiosMock.reset();
  });

  const renderList = () => renderNexusOneRoute(<ApplicationsList />, 'nexusOneApplications');

  it('hydrates list state from deep-linked route params on the first fetch', async () => {
    axiosMock.onPost(listUrl).reply(200, API_LIST_RESPONSE);

    renderNexusOneRoute(<ApplicationsList />, 'nexusOneApplications', {
      q: 'apple',
      sort: 'oldest',
      page: '2',
      stage: 'build',
    });

    await waitFor(() => {
      const hydrated = axiosMock.history.post.find((request) => {
        const body = JSON.parse(String(request.data));
        return (
          body.search === 'apple' &&
          body.orderBy === 'lastEvaluationTime' &&
          body.page === 1 &&
          body.stageIds?.includes('build')
        );
      });
      expect(hydrated).toBeDefined();
    });
  });

  it('replaces invalid threat tokens in the address bar on hydrate', async () => {
    axiosMock.onPost(listUrl).reply(200, API_LIST_RESPONSE);
    const goSpy = jest.spyOn(router.stateService, 'go');

    renderNexusOneRoute(<ApplicationsList />, 'nexusOneApplications', { threat: 'Bogus,Critical' });

    await waitFor(() => {
      expect(goSpy).toHaveBeenCalledWith(
        'nexusOneApplications',
        // Legacy bucket tokens are not valid min-max ranges; hydrate drops them.
        expect.objectContaining({ threat: undefined }),
        expect.objectContaining({ notify: false, location: 'replace' })
      );
    });

    goSpy.mockRestore();
  });

  it('does not replace the URL when violationState is already clean', async () => {
    axiosMock.onPost(listUrl).reply(200, API_LIST_RESPONSE);
    const goSpy = jest.spyOn(router.stateService, 'go');

    renderNexusOneRoute(
      <ApplicationsList />,
      'nexusOneApplications',
      { violationState: 'OPEN' },
    );

    await waitFor(() => {
      expect(screen.getByTestId('preview-applications-page')).toBeInTheDocument();
    });

    expect(goSpy).not.toHaveBeenCalled();
    goSpy.mockRestore();
  });

  it('updates the list request when search is submitted from a deep-linked view', async () => {
    axiosMock.onPost(listUrl).reply(200, API_LIST_RESPONSE);

    renderNexusOneRoute(<ApplicationsList />, 'nexusOneApplications', { q: 'apple' });

    await waitFor(() => {
      expect(screen.getByTestId('applications-toolbar-search')).toBeInTheDocument();
    });

    const searchInput = screen.getByTestId('applications-toolbar-search');
    await waitFor(() => expect(searchInput).toHaveValue('apple'));
    await userEvent.clear(searchInput);
    await userEvent.type(searchInput, 'banana{enter}');

    await waitFor(() => {
      const lastRequest = axiosMock.history.post.at(-1);
      const body = JSON.parse(String(lastRequest?.data));
      expect(body).toEqual(
        expect.objectContaining({
          search: 'banana',
          page: 0,
        })
      );
    });
  });

  it('renders the two-column page shell with filter rail and content area', async () => {
    axiosMock.onPost(listUrl).reply(200, API_LIST_RESPONSE);
    renderList();
    await waitFor(() => {
      expect(screen.getByTestId('preview-applications-page')).toBeInTheDocument();
    });
    expect(screen.getByTestId('applications-page-layout')).toBeInTheDocument();
    expect(screen.getByTestId('applications-filter-rail')).toBeInTheDocument();
    expect(screen.getByTestId('applications-page-content')).toBeInTheDocument();
  });

  it('exposes the filter rail on small screens via the mobile drawer trigger', async () => {
    axiosMock.onPost(listUrl).reply(200, API_LIST_RESPONSE);
    renderList();
    await waitFor(() => {
      expect(screen.getByTestId('preview-applications-page')).toBeInTheDocument();
    });
    // The skeleton renders the mobile trigger when both renderFilterRail and
    // renderMobileFilterDrawer are supplied.
    const trigger = screen.getByTestId('applications-filters-mobile-trigger');
    expect(trigger).toBeInTheDocument();
    // Opening the drawer mounts a second rail instance under the prefixed testid namespace,
    // so it does not collide with the desktop rail's ids.
    await userEvent.click(trigger);
    expect(await screen.findByTestId('applications-filter-mobile-applications-filter-rail')).toBeInTheDocument();
  });

  it('renders filter rail sections with facet labels derived from list rows', async () => {
    axiosMock.onPost(listUrl).reply(200, API_LIST_RESPONSE);
    renderList();
    const filterRail = await screen.findByTestId('applications-filter-rail');
    // The facet sections keep a stable shape while facets load, so the fieldsets exist before
    // the first response resolves — wait on the row labels themselves, not the containers.
    await waitFor(() => {
      expect(filterRail).toHaveTextContent('Develop');
    });
    expect(filterRail).toHaveTextContent('Java-team');
    expect(filterRail).toHaveTextContent('Apple - Java');
    expect(screen.getByTestId('applications-filter-stages')).toBeInTheDocument();
    expect(screen.getByTestId('applications-filter-threat-level')).toBeInTheDocument();
  });

  it('posts stage filter ids when a stage checkbox is toggled', async () => {
    axiosMock.onPost(listUrl).reply(200, API_LIST_RESPONSE);
    renderList();
    const stagesGroup = await screen.findByTestId('applications-filter-stages');
    await waitFor(() => {
      expect(within(stagesGroup).getByText('Build')).toBeInTheDocument();
    });

    const buildCheckbox = within(stagesGroup).getByRole('checkbox', { name: /build/i });
    await userEvent.click(buildCheckbox);

    await waitFor(() => {
      const lastRequest = axiosMock.history.post.at(-1);
      const body = JSON.parse(String(lastRequest?.data));
      expect(body).toEqual(
        expect.objectContaining({
          stageIds: ['build'],
          page: 0,
        })
      );
    });

    expect(screen.getByTestId('applications-toolbar-csv')).toHaveAttribute(
      'title',
      'CSV export caveat: sorted by total risk (not evaluation time); stage filter uses Classic matching and may differ from this list'
    );
  });

  it('reset filters clears selection and posts an unfiltered request', async () => {
    axiosMock.onPost(listUrl).reply(200, API_LIST_RESPONSE);
    renderList();
    const stagesGroup = await screen.findByTestId('applications-filter-stages');
    await waitFor(() => {
      expect(within(stagesGroup).getByText('Build')).toBeInTheDocument();
    });

    await userEvent.click(within(stagesGroup).getByRole('checkbox', { name: /build/i }));
    await waitFor(() => {
      expect(screen.getByTestId('applications-filter-reset')).not.toBeDisabled();
    });

    await userEvent.click(screen.getByTestId('applications-filter-reset'));

    await waitFor(() => {
      const lastRequest = axiosMock.history.post.at(-1);
      const body = JSON.parse(String(lastRequest?.data));
      expect(body).toEqual(
        expect.objectContaining({
          page: 0,
          pageSize: 50,
          includeFacets: true,
          orderBy: '-lastEvaluationTime',
        })
      );
      expect(body.stageIds).toBeUndefined();
    });
  });

  it('renders toolbar controls and total count from the list API', async () => {
    axiosMock.onPost(listUrl).reply(200, API_LIST_RESPONSE);
    renderList();
    await waitFor(() => {
      expect(screen.getByTestId('applications-toolbar-count')).toHaveTextContent('3 applications');
    });
    expect(screen.getByTestId('applications-toolbar-search')).toBeInTheDocument();
    expect(screen.getByTestId('applications-toolbar-sort')).toBeInTheDocument();
    expect(screen.getByLabelText('Sort')).toHaveTextContent('Latest evaluation');
    expect(screen.getByTestId('applications-toolbar-csv')).toBeEnabled();
    expect(screen.getByTestId('applications-toolbar-export-form')).toHaveAttribute(
      'action',
      expect.stringContaining('/rest/dashboard/export/applicationRisks')
    );
  });

  it('submits search to the list API on Enter and resets page', async () => {
    axiosMock.onPost(listUrl).reply(200, API_LIST_RESPONSE);
    renderList();
    await waitFor(() => {
      expect(screen.getByTestId('applications-toolbar-search')).toBeInTheDocument();
    });

    const searchInput = screen.getByLabelText('Search applications');
    await userEvent.type(searchInput, 'apple{enter}');

    await waitFor(() => {
      const lastRequest = axiosMock.history.post.at(-1);
      const body = JSON.parse(String(lastRequest?.data));
      expect(body).toEqual(
        expect.objectContaining({
          search: 'apple',
          page: 0,
          orderBy: '-lastEvaluationTime',
        })
      );
    });

    expect(screen.getByTestId('applications-toolbar-csv')).toHaveAttribute(
      'title',
      'CSV export caveat: sorted by total risk (not evaluation time); search term is not included'
    );
  });

  it('renders evaluation cards from the list API instead of a data table', async () => {
    axiosMock.onPost(listUrl).reply(200, API_LIST_RESPONSE);
    renderList();
    await waitFor(() => {
      expect(screen.getAllByTestId('evaluation-card')).toHaveLength(3);
    });
    expect(screen.queryByTestId('applications-list-table')).not.toBeInTheDocument();
    expect(screen.getByTestId('evaluation-card-grid')).toBeInTheDocument();
    expect(screen.getAllByTestId('nosc-dashboard-app-link')).toHaveLength(3);
    expect(screen.getByRole('link', { name: /apple - java/i })).toBeInTheDocument();
  });

  it('card app name links to the Preview Application Detail page', async () => {
    axiosMock.onPost(listUrl).reply(200, API_LIST_RESPONSE);
    renderList();
    const nameLink = await screen.findByRole('link', { name: /apple - java/i });
    expect(nameLink).toHaveAttribute('href', expect.stringContaining('/applications/apple-java'));
  });

  it('stage tiles open that stage\u2019s own Classic report, not the latest', async () => {
    axiosMock.onPost(listUrl).reply(200, API_LIST_RESPONSE);
    renderList();
    await waitFor(() => {
      expect(screen.getAllByTestId('evaluation-card')).toHaveLength(3);
    });
    const appleCard = screen.getAllByTestId('evaluation-card')[0];
    expect(
      within(appleCard).getByRole('link', {
        name: /open the build report for this application/i,
      }),
    ).toHaveAttribute('href', '#/applications/apple-java/report/scan-apple-build');
    expect(
      within(appleCard).getByRole('link', {
        name: /open the develop report for this application/i,
      }),
    ).toHaveAttribute('href', '#/applications/apple-java/report/scan-apple-develop');
  });

  it('page wrapper offsets reflow when LeftNav collapses', async () => {
    axiosMock.onPost(listUrl).reply(200, API_LIST_RESPONSE);
    window.localStorage.removeItem('nosc.leftnav.collapsed');
    renderList();
    const pageMain = (await screen.findByTestId('preview-applications-page')) as HTMLElement;
    expect(pageMain.style.left).toBe('256px');

    await act(async () => {
      window.dispatchEvent(new CustomEvent('nosc.leftnav.collapsed.change', { detail: { collapsed: true } }));
    });
    expect(pageMain.style.left).toBe('64px');

    await act(async () => {
      window.dispatchEvent(new CustomEvent('nosc.leftnav.collapsed.change', { detail: { collapsed: false } }));
    });
    expect(pageMain.style.left).toBe('256px');
  });
});

describe('ApplicationsPage async states', () => {
  const filterProps = {
    filters: EMPTY_APPLICATIONS_LIST_FILTERS,
    hasActiveFilters: false,
    onToggleFilter: jest.fn(),
    onThreatRangeChange: jest.fn(),
    onResetFilters: jest.fn(),
  };
  const toolbarProps = {
    searchValue: '',
    onSearchSubmit: jest.fn(),
    orderBy: '-lastEvaluationTime' as const,
    onOrderByChange: jest.fn(),
  };
  const pageProps = {
    totalCount: 0,
    page: 1,
    pageSize: 50,
    onPageChange: jest.fn(),
    ...filterProps,
    ...toolbarProps,
  };

  it('renders loading skeleton when loading', () => {
    renderNexusOneRoute(
      <ApplicationsPage
        applications={[]}
        facets={{ ...MOCK_APPLICATIONS_FILTER_FACETS, totalApplications: 0 }}
        loading
        {...pageProps}
      />,
      'nexusOneApplications'
    );
    expect(screen.getByTestId('applications-list-loading')).toBeInTheDocument();
  });

  it('renders error banner with retry when error is set', async () => {
    const onRetry = jest.fn();
    renderNexusOneRoute(
      <ApplicationsPage
        applications={[]}
        facets={{ ...MOCK_APPLICATIONS_FILTER_FACETS, totalApplications: 0 }}
        error="Backend unavailable"
        onRetry={onRetry}
        {...pageProps}
      />,
      'nexusOneApplications'
    );
    expect(screen.getByTestId('applications-list-error')).toBeInTheDocument();
    const retryButton = await screen.findByRole('button', { name: /retry/i });
    await userEvent.click(retryButton);
    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  it('renders empty state when there are no applications', () => {
    renderNexusOneRoute(
      <ApplicationsPage
        applications={[]}
        facets={{ ...MOCK_APPLICATIONS_FILTER_FACETS, totalApplications: 0 }}
        {...pageProps}
      />,
      'nexusOneApplications'
    );
    expect(screen.getByTestId('applications-list-empty')).toBeInTheDocument();
    expect(screen.getByText('No applications to display.')).toBeInTheDocument();
  });

  it('renders pagination when total exceeds page size', () => {
    renderNexusOneRoute(
      <ApplicationsPage
        applications={MOCK_APPLICATION_RISK_SCORES}
        facets={MOCK_APPLICATIONS_FILTER_FACETS}
        totalCount={120}
        page={1}
        pageSize={50}
        onPageChange={jest.fn()}
        {...filterProps}
        {...toolbarProps}
      />,
      'nexusOneApplications'
    );
    expect(screen.getByTestId('applications-pagination')).toBeInTheDocument();
    expect(screen.getByText(/showing 1–50 of 120/i)).toBeInTheDocument();
  });

  it('keeps pagination visible on page 2 when total is within page size', () => {
    renderNexusOneRoute(
      <ApplicationsPage
        applications={MOCK_APPLICATION_RISK_SCORES}
        facets={MOCK_APPLICATIONS_FILTER_FACETS}
        totalCount={3}
        page={2}
        pageSize={50}
        onPageChange={jest.fn()}
        {...filterProps}
        {...toolbarProps}
      />,
      'nexusOneApplications'
    );
    expect(screen.getByTestId('applications-pagination')).toBeInTheDocument();
  });

  it('shows pagination on page 1 when hasNextPage is true but total is within page size', () => {
    renderNexusOneRoute(
      <ApplicationsPage
        applications={MOCK_APPLICATION_RISK_SCORES}
        facets={MOCK_APPLICATIONS_FILTER_FACETS}
        totalCount={3}
        page={1}
        pageSize={50}
        hasNextPage
        onPageChange={jest.fn()}
        {...filterProps}
        {...toolbarProps}
      />,
      'nexusOneApplications'
    );
    expect(screen.getByTestId('applications-pagination')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Next page' })).not.toBeDisabled();
  });

  it('renders index-not-ready info panel when info is set', () => {
    renderNexusOneRoute(
      <ApplicationsPage
        applications={[]}
        facets={{ ...MOCK_APPLICATIONS_FILTER_FACETS, totalApplications: 0 }}
        info={{
          title: 'Search index building',
          message: 'The search index is still building. Please try again shortly.',
          testId: 'applications-list-not-ready',
        }}
        onRetry={jest.fn()}
        {...pageProps}
      />,
      'nexusOneApplications'
    );
    expect(screen.getByTestId('applications-list-not-ready')).toBeInTheDocument();
    expect(screen.queryByTestId('applications-list-loading')).not.toBeInTheDocument();
    expect(screen.queryByTestId('applications-list-error')).not.toBeInTheDocument();
  });
});
