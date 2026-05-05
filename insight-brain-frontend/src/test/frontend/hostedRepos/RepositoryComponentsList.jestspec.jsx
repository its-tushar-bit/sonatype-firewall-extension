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

  const mockComponents = [
    {
      id: 'comp001',
      pathname: 'log4j-core-2.14.1.jar',
      displayName: 'log4j-core : 2.14.1',
      hash: 'abc123',
      matchStateId: 'exact',
      lastEvaluationTime: 1700000000000,
      quarantined: false,
      violationCount: 1,
      maxThreatLevel: 10,
      componentIdentifier: { format: 'maven', coordinates: { artifactId: 'log4j-core', groupId: 'org.apache.logging.log4j', version: '2.14.1', extension: 'jar', classifier: '' } },
    },
    {
      id: 'comp002',
      pathname: 'commons-text-1.9.0.jar',
      displayName: 'commons-text : 1.9.0',
      hash: 'def456',
      matchStateId: 'exact',
      lastEvaluationTime: 1700000000000,
      quarantined: false,
      violationCount: 1,
      maxThreatLevel: 7,
      componentIdentifier: { format: 'maven', coordinates: { artifactId: 'commons-text', groupId: 'org.apache.commons', version: '1.9.0', extension: 'jar', classifier: '' } },
    },
  ];

  const mockApiResponse = {
    components: mockComponents,
    totalCount: 2,
    page: 1,
    pageSize: 25,
    hasNextPage: false,
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    mockRouterState = {
      href: jest.fn().mockReturnValue('#/hostedRepos/local-nexus'),
      get: jest.fn(),
      includes: jest.fn(),
    };
    jest.spyOn(RouterStateContext, 'useRouterState').mockReturnValue(mockRouterState);
    goToComponentReportSpy = jest.spyOn(hostedReposActions, 'goToComponentReport').mockReturnValue(() => {});
    axiosMock.onGet('/api/v2/repositories/local-nexus/repo-uuid-123/components').reply(200, mockApiResponse);
  });

  afterEach(() => {
    jest.restoreAllMocks();
    axiosMock.reset();
  });

  const renderComponent = (preloadedState) =>
    render(<RepositoryComponentsList />, { preloadedState: preloadedState || defaultPreloadedState });

  it('shows repo public ID as title', async () => {
    renderComponent();
    await waitFor(() => expect(screen.getByRole('heading', { name: 'maven-hosted' })).toBeInTheDocument());
  });

  it('loads and displays components on mount', async () => {
    renderComponent();
    await waitFor(() => {
      expect(screen.getByText('log4j-core : 2.14.1')).toBeInTheDocument();
      expect(screen.getByText('commons-text : 1.9.0')).toBeInTheDocument();
    });
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

  it('sends filter param when search changes', async () => {
    const user = userEvent.setup();
    renderComponent();

    await waitFor(() => expect(screen.getByRole('heading', { name: 'maven-hosted' })).toBeInTheDocument());

    const filterInput = screen.getByRole('textbox');
    await user.type(filterInput, 'log4j');

    await waitFor(() => {
      const requests = axiosMock.history.get.filter(
        (r) => r.url === '/api/v2/repositories/local-nexus/repo-uuid-123/components' && r.params?.filter
      );
      expect(requests.length).toBeGreaterThan(0);
    });
  });

  it('navigates to repository report on Report button click', async () => {
    const user = userEvent.setup();

    const componentsWithViolations = [
      { ...mockComponents[0], violationCount: 1, criticalViolationCount: 1 },
    ];
    axiosMock.reset();
    axiosMock.onGet('/api/v2/repositories/local-nexus/repo-uuid-123/components').reply(200, {
      ...mockApiResponse,
      components: componentsWithViolations,
    });

    renderComponent();

    await waitFor(() => expect(screen.getByText('Report')).toBeInTheDocument());
    await user.click(screen.getByText('Report'));

    expect(goToComponentReportSpy).toHaveBeenCalledWith('repo-uuid-123');
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

  it('shows error state when API fails', async () => {
    axiosMock.reset();
    axiosMock.onGet('/api/v2/repositories/local-nexus/repo-uuid-123/components').reply(500, { message: 'Server error' });
    renderComponent();
    await waitFor(() => expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument());
  });

  it('shows pagination when there are multiple pages', async () => {
    axiosMock.reset();
    axiosMock.onGet('/api/v2/repositories/local-nexus/repo-uuid-123/components').reply(200, {
      ...mockApiResponse,
      totalCount: 50,
      hasNextPage: true,
    });
    renderComponent();
    await waitFor(() => {
      expect(screen.getByRole('navigation')).toBeInTheDocument();
    });
  });
});
