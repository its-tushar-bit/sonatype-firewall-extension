/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, axiosMockAdapter, mockInterceptionObserver } from 'TestRoot/SpecUtil';
import RepositoriesSummaryView from 'MainRoot/OrgsAndPolicies/repositories/RepositoriesSummaryView';
import { getRepositoryContainer, getPermissionContextTestUrl, getAccessPageRolesUrl } from 'MainRoot/util/CLMLocation';

describe('RepositoriesSummaryView', () => {
  let renderComponent, axiosMock, preloadedState;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
    mockInterceptionObserver();
  });

  beforeEach(() => {
    preloadedState = {
      router: {
        currentState: {
          name: 'management.view.repository_container',
          url: '/repository_container/{repositoryContainerId}',
          data: {
            title: 'Repository Managers Management',
            viewportSized: true,
          },
        },
        currentParams: {
          repositoryContainerId: 'REPOSITORY_CONTAINER_ID',
        },
      },
      orgsAndPolicies: {
        sourceControl: {
          data: {
            repositoryUrl: null,
            provider: {
              value: null,
              parentValue: 'github',
            },
            token: {
              value: null,
            },
          },
        },
      },
    };

    axiosMock.onGet(getRepositoryContainer()).reply(200, {
      id: 'REPOSITORY_CONTAINER_ID',
      name: 'Repository Managers',
    });

    axiosMock
      .onPut(getPermissionContextTestUrl('repository_container', 'REPOSITORY_CONTAINER_ID'))
      .reply(200, ['WRITE']);
    renderComponent = () => render(<RepositoriesSummaryView />, { preloadedState });

    axiosMock
      .onGet(getAccessPageRolesUrl('repository_container', 'REPOSITORY_CONTAINER_ID'))
      .reply(200, { membersByRole: [] });
  });

  it('renders the page title', async () => {
    renderComponent();

    expect(await screen.findByRole('heading', { name: /Repository Managers/ })).toBeVisible();
  });

  it('renders Access and Configuration tiles', async () => {
    renderComponent();

    expect(await screen.findByTestId('repositories_configuration')).toBeVisible();
    expect(screen.getByTestId('repositories_access')).toBeVisible();
    expect(screen.getByTestId('policies-tile')).toBeVisible();
    expect(screen.getByTestId('namespace-confusion-protection-pill-configuration')).toBeVisible();
  });

  it('renders NavPills', async () => {
    renderComponent();

    expect(await screen.findByTestId('repositories-pill-configuration-button')).toBeVisible();
    expect(screen.getByTestId('owner-pill-policy-button')).toBeVisible();
    expect(screen.getByTestId('access-tile-pill-access-button')).toBeVisible();
    expect(screen.getByTestId('namespace-confusion-protection-pill-configuration-button')).toBeVisible();
  });

  describe('Limited Firewall Access Alert', () => {
    it('shows limited firewall access alert when showLimitedFirewallAccessAlert is true', async () => {
      preloadedState.orgsAndPolicies.root = {
        ...preloadedState.orgsAndPolicies.root,
        showLimitedFirewallAccessAlert: true,
        selectedOwner: {
          id: 'REPOSITORY_CONTAINER_ID',
          name: 'Repository Managers',
        },
      };

      renderComponent();

      expect(
        await screen.findByText(/You have limited access to Repository Firewall based on your current permissions/)
      ).toBeVisible();
      expect(screen.getByText(/Some data or settings may not be visible. Contact your administrator/)).toBeVisible();
    });

    it('does not show limited firewall access alert when showLimitedFirewallAccessAlert is false', async () => {
      preloadedState.orgsAndPolicies.root = {
        ...preloadedState.orgsAndPolicies.root,
        showLimitedFirewallAccessAlert: false,
        selectedOwner: {
          id: 'REPOSITORY_CONTAINER_ID',
          name: 'Repository Managers',
        },
      };

      renderComponent();

      expect(await screen.findByRole('heading', { name: /Repository Managers/ })).toBeVisible();
      expect(
        screen.queryByText(/You have limited access to Repository Firewall based on your current permissions/)
      ).not.toBeInTheDocument();
    });

    it('hides tiles when showing limited firewall access alert', async () => {
      preloadedState.orgsAndPolicies.root = {
        ...preloadedState.orgsAndPolicies.root,
        showLimitedFirewallAccessAlert: true,
        selectedOwner: {
          id: 'REPOSITORY_CONTAINER_ID',
          name: 'Repository Managers',
        },
      };

      renderComponent();

      expect(
        await screen.findByText(/You have limited access to Repository Firewall based on your current permissions/)
      ).toBeVisible();

      // Verify tiles are not rendered when alert is shown
      expect(screen.queryByTestId('repositories_configuration')).not.toBeInTheDocument();
      expect(screen.queryByTestId('repositories_access')).not.toBeInTheDocument();
      expect(screen.queryByTestId('policies-tile')).not.toBeInTheDocument();
    });
  });
});
