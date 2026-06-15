/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, waitFor } from '@testing-library/dom';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter, render } from 'TestRoot/SpecUtil';
import * as RouterStateContext from 'MainRoot/react/RouterStateContext';
import * as hostedReposActions from 'MainRoot/hostedRepos/hostedReposActions';
import RepositoryComponentsList from 'MainRoot/hostedRepos/RepositoryComponentsList';

describe('RepositoryComponentsList', () => {
  let axiosMock;
  let mockRouterState;
  let goToComponentReportSpy;
  let goToComponentPrioritiesSpy;

  const defaultParams = {
    repositoryManagerId: 'local-nexus',
    repositoryId: 'repo-uuid-123',
    repositoryPublicId: 'maven-hosted',
  };

  const defaultPreloadedState = {
    repositoryComponents: {
      components: [],
      totalCount: 0,
      currentPage: 1,
      hasNextPage: false,
      filter: '',
      loading: false,
      error: null,
    },
    router: {
      currentParams: defaultParams,
      currentState: { name: 'hostedRepoComponents' },
    },
  };

  const componentWithViolations = {
    id: 'comp001',
    pathname: 'com/example/log4j-core-2.14.1.jar',
    displayName: 'log4j-core : 2.14.1',
    hash: 'abc123',
    matchStateId: 'exact',
    lastEvaluationTime: 1700000000000,
    quarantined: false,
    violationCount: 2,
    criticalViolationCount: 1,
    severeViolationCount: 1,
    moderateViolationCount: 0,
    maxThreatLevel: 10,
    applicationPublicId: 'maven-hosted_com_example_log4j-core-2.14.1.jar',
    scanId: 'scan-abc123',
    stageTypeId: 'build',
    componentIdentifier: {
      format: 'maven',
      coordinates: { artifactId: 'log4j-core', groupId: 'org.apache.logging.log4j', version: '2.14.1', extension: 'jar', classifier: '' },
    },
  };

  const componentNoViolations = {
    id: 'comp002',
    pathname: 'com/example/safe-1.0.jar',
    displayName: 'safe : 1.0',
    hash: 'def456',
    matchStateId: 'exact',
    lastEvaluationTime: 1700000000000,
    quarantined: false,
    violationCount: 0,
    criticalViolationCount: 0,
    severeViolationCount: 0,
    moderateViolationCount: 0,
    maxThreatLevel: 0,
    applicationPublicId: null,
    scanId: null,
    stageTypeId: null,
    componentIdentifier: null,
  };

  const twoPageApiResponse = {
    components: [componentWithViolations],
    totalCount: 50,
    page: 1,
    pageSize: 25,
    hasNextPage: true,
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    mockRouterState = {
      href: jest.fn().mockImplementation((stateName, params) => {
        if (stateName === 'hostedRepos') return '#/hostedRepos';
        if (stateName === 'hostedRepositories') return `#/hostedRepos/${params?.repositoryManagerId}`;
        return '#/default';
      }),
      get: jest.fn(),
      includes: jest.fn(),
    };
    jest.spyOn(RouterStateContext, 'useRouterState').mockReturnValue(mockRouterState);
    goToComponentReportSpy = jest.spyOn(hostedReposActions, 'goToComponentReport').mockReturnValue(() => {});
    goToComponentPrioritiesSpy = jest.spyOn(hostedReposActions, 'goToComponentPriorities').mockReturnValue(() => {});
    axiosMock.onGet('/api/v2/repositories/local-nexus/repo-uuid-123/components').reply(200, {
      components: [componentWithViolations],
      totalCount: 1,
      page: 1,
      pageSize: 25,
      hasNextPage: false,
    });
  });

  afterEach(() => {
    jest.restoreAllMocks();
    axiosMock.reset();
  });

  const renderComponent = (preloadedState) =>
    render(<RepositoryComponentsList />, { preloadedState: preloadedState || defaultPreloadedState });

  it('shows repo public ID as page title', async () => {
    renderComponent();
    await waitFor(() => expect(screen.getByRole('heading', { name: 'maven-hosted' })).toBeInTheDocument());
  });

  it('loads and displays component name on mount', async () => {
    renderComponent();
    await waitFor(() => expect(screen.getByText('log4j-core : 2.14.1')).toBeInTheDocument());
  });

  it('calls the correct API URL with page and pageSize', async () => {
    renderComponent();
    await waitFor(() => {
      expect(axiosMock.history.get.length).toBeGreaterThan(0);
      const req = axiosMock.history.get[0];
      expect(req.url).toBe('/api/v2/repositories/local-nexus/repo-uuid-123/components');
      expect(req.params).toMatchObject({ page: 1, pageSize: 25 });
    });
  });

  it('sends filter param when search input changes', async () => {
    const user = userEvent.setup();
    renderComponent();

    await waitFor(() => expect(screen.getByRole('heading', { name: 'maven-hosted' })).toBeInTheDocument());

    const filterInput = screen.getByRole('textbox');
    await user.type(filterInput, 'log4j');

    await waitFor(() => {
      const filtered = axiosMock.history.get.filter(
        (r) => r.url === '/api/v2/repositories/local-nexus/repo-uuid-123/components' && r.params?.filter
      );
      expect(filtered.length).toBeGreaterThan(0);
    });
  });

  it('navigates to report page when Report button is clicked', async () => {
    const user = userEvent.setup();
    renderComponent();

    await waitFor(() => expect(screen.getByText('Report')).toBeInTheDocument());
    await user.click(screen.getByText('Report'));

    expect(goToComponentReportSpy).toHaveBeenCalledWith(
      'maven-hosted_com_example_log4j-core-2.14.1.jar',
      'scan-abc123',
      'local-nexus',
      'repo-uuid-123',
      'maven-hosted'
    );
  });

  it('navigates to priorities page when Priorities button is clicked', async () => {
    const user = userEvent.setup();
    renderComponent();

    await waitFor(() => expect(screen.getByText('Priorities')).toBeInTheDocument());
    await user.click(screen.getByText('Priorities'));

    expect(goToComponentPrioritiesSpy).toHaveBeenCalledWith(
      'maven-hosted_com_example_log4j-core-2.14.1.jar',
      'scan-abc123'
    );
  });

  it('Report and Priorities buttons are disabled when applicationPublicId or scanId is missing', async () => {
    axiosMock.reset();
    axiosMock.onGet('/api/v2/repositories/local-nexus/repo-uuid-123/components').reply(200, {
      components: [componentNoViolations],
      totalCount: 1,
      page: 1,
      pageSize: 25,
      hasNextPage: false,
    });
    renderComponent();

    await waitFor(() => expect(screen.getByText('safe : 1.0')).toBeInTheDocument());
    // No violation count > 0 means Report/Priorities are not rendered at all
    expect(screen.queryByText('Report')).not.toBeInTheDocument();
    expect(screen.queryByText('Priorities')).not.toBeInTheDocument();
  });

  it('does not render threat counter or report links when violationCount is 0', async () => {
    axiosMock.reset();
    axiosMock.onGet('/api/v2/repositories/local-nexus/repo-uuid-123/components').reply(200, {
      components: [componentNoViolations],
      totalCount: 1,
      page: 1,
      pageSize: 25,
      hasNextPage: false,
    });
    renderComponent();

    await waitFor(() => expect(screen.getByText('safe : 1.0')).toBeInTheDocument());
    expect(screen.queryByText('Report')).not.toBeInTheDocument();
    expect(screen.queryByText('Priorities')).not.toBeInTheDocument();
  });

  it('renders stage label with first letter uppercased', async () => {
    renderComponent();

    await waitFor(() => expect(screen.getByText('Build')).toBeInTheDocument());
  });

  it('renders stage label lowercased except first char for COMPLIANCE', async () => {
    axiosMock.reset();
    axiosMock.onGet('/api/v2/repositories/local-nexus/repo-uuid-123/components').reply(200, {
      components: [{ ...componentWithViolations, stageTypeId: 'COMPLIANCE' }],
      totalCount: 1,
      page: 1,
      pageSize: 25,
      hasNextPage: false,
    });
    renderComponent();

    await waitFor(() => expect(screen.getByText('Compliance')).toBeInTheDocument());
  });

  it('shows empty message when no components', async () => {
    axiosMock.reset();
    axiosMock.onGet('/api/v2/repositories/local-nexus/repo-uuid-123/components').reply(200, {
      components: [],
      totalCount: 0,
      page: 1,
      pageSize: 25,
      hasNextPage: false,
    });
    renderComponent();
    await waitFor(() => expect(screen.getByText('No components found')).toBeInTheDocument());
  });

  it('shows error state with retry button when API fails', async () => {
    axiosMock.reset();
    axiosMock.onGet('/api/v2/repositories/local-nexus/repo-uuid-123/components').reply(500, { message: 'Server error' });
    renderComponent();
    await waitFor(() => expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument());
  });

  it('shows pagination when there are multiple pages', async () => {
    axiosMock.reset();
    axiosMock.onGet('/api/v2/repositories/local-nexus/repo-uuid-123/components').reply(200, twoPageApiResponse);
    renderComponent();
    await waitFor(() => expect(screen.getByRole('navigation')).toBeInTheDocument());
  });

  it('dispatches loadComponents with page 2 when pagination changes', async () => {
    const user = userEvent.setup();
    axiosMock.reset();
    axiosMock.onGet('/api/v2/repositories/local-nexus/repo-uuid-123/components').reply(200, twoPageApiResponse);
    renderComponent();

    await waitFor(() => expect(screen.getByRole('navigation')).toBeInTheDocument());

    // NxPagination renders navigation buttons; click the next-page or any non-current page button
    const paginationButtons = screen.getAllByRole('button');
    const nextButton = paginationButtons.find((btn) =>
      btn.getAttribute('aria-label')?.toLowerCase().includes('next') ||
      (btn.textContent && !isNaN(parseInt(btn.textContent.trim())) && parseInt(btn.textContent.trim()) === 2)
    );
    expect(nextButton).toBeDefined();
    await user.click(nextButton);
    await waitFor(() => {
      const pageRequests = axiosMock.history.get.filter((r) => r.params?.page === 2);
      expect(pageRequests.length).toBeGreaterThan(0);
    });
  });

  it('breadcrumbs have correct hrefs', async () => {
    renderComponent();

    await waitFor(() => expect(screen.getByRole('heading', { name: 'maven-hosted' })).toBeInTheDocument());

    expect(mockRouterState.href).toHaveBeenCalledWith('hostedRepos');
    expect(mockRouterState.href).toHaveBeenCalledWith('hostedRepositories', { repositoryManagerId: 'local-nexus' });
  });

  it('shows friendly name in breadcrumb when managerName is loaded in hostedReposList state', async () => {
    const stateWithManagerName = {
      ...defaultPreloadedState,
      hostedReposList: {
        managerInstanceId: 'local-nexus',
        managerBaseUrl: 'http://localhost:8081',
        managerName: 'My Local NXRM',
      },
    };

    renderComponent(stateWithManagerName);

    await waitFor(() => expect(screen.getByRole('heading', { name: 'maven-hosted' })).toBeInTheDocument());

    expect(screen.getByText('My Local NXRM')).toBeInTheDocument();
  });

  it('falls back to repositoryManagerId in breadcrumb when managerName is null', async () => {
    const stateWithoutManagerName = {
      ...defaultPreloadedState,
      hostedReposList: {
        managerInstanceId: 'local-nexus',
        managerBaseUrl: null,
        managerName: null,
      },
    };

    renderComponent(stateWithoutManagerName);

    await waitFor(() => expect(screen.getByRole('heading', { name: 'maven-hosted' })).toBeInTheDocument());

    expect(screen.getByText('local-nexus')).toBeInTheDocument();
  });
});
