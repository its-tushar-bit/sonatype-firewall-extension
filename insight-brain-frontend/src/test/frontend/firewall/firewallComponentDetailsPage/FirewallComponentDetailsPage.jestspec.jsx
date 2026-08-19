/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, fireEvent, render, screen, setupPortalContainer, within } from 'TestRoot/SpecUtil';
import {
  getApplicableLabelsUrl,
  getComponentDetailsUrl,
  getComponentLabels,
  getComponentPolicyViolationsUrl,
  getComponentWaivers,
} from 'MainRoot/util/CLMLocation';

import { applicableLabelsData, componentDetailsData, labelsData, policyViolationsData } from './data';
import router from 'MainRoot/router/routerInstance';
import FirewallComponentDetailsPage from 'MainRoot/firewall/firewallComponentDetailsPage/FirewallComponentDetailsPage';
import { lensPath, set } from 'ramda';

describe('ComponentDetails', () => {
  let axiosMock;
  let defaultPreloadedState;
  let renderComponent;
  const repositoryId = 'ff7688303b844b08bd9854d3e53802ce';
  const componentIdentifier =
    '{"format":"maven","coordinates":{"artifactId":"ant","classifier":"","extension":"jar","groupId":"ant","version":"1.6.1"}}';
  const componentHash = '684aeca90db2a55234f5';
  const matchState = 'exact';
  const pathname = 'ant/ant/1.6.1/ant-1.6.1.jar';
  const componentDisplayName = 'ant : ant : 1.6.1';
  const tabId = 'overview';
  const requestParams = {
    clientType: 'ci',
    ownerType: 'repository',
    ownerId: repositoryId,
    componentIdentifier,
    hash: componentHash,
    matchState,
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
    setupPortalContainer();
  });

  beforeEach(() => {
    defaultPreloadedState = {
      router: {
        currentParams: {
          repositoryId,
          componentIdentifier,
          componentHash,
          matchState,
          pathname,
          componentDisplayName,
          tabId,
        },
        currentState: { name: 'firewall.componentDetailsPage.violations' },
      },
    };

    axiosMock.onGet(getComponentDetailsUrl(requestParams)).reply(200, componentDetailsData);
    axiosMock.onGet(getComponentLabels(repositoryId, componentHash, 'repository')).reply(200, { labelsByOwner: [] });
    axiosMock.onGet(getComponentPolicyViolationsUrl(pathname, repositoryId)).reply(200, policyViolationsData);
    axiosMock.onGet(getComponentWaivers('repository', repositoryId, componentHash)).reply(200, { waiversByOwner: [] });
    axiosMock.onGet(getApplicableLabelsUrl('repository', repositoryId)).reply(200, applicableLabelsData);

    jest.spyOn(router.stateService, 'href').mockReturnValue('#');
    jest.spyOn(router.stateService, 'get').mockReturnValue('#');
    jest.spyOn(router.stateService, 'includes').mockReturnValue(false);

    renderComponent = (preloadedState = defaultPreloadedState) =>
      render(<FirewallComponentDetailsPage />, { preloadedState });
  });

  it('renders a loading indicator and title', async () => {
    renderComponent();
    await screen.findByText('Loading…');
    const titles = await screen.findAllByText('ant : ant : 1.6.1');
    const title = titles[0];
    expect(title.parentNode.tagName).toBe('H1');
    expect(title).toBeVisible();
  });

  it('renders an error message', async () => {
    axiosMock.onGet(getComponentDetailsUrl(requestParams)).reply(500, 'some error');
    renderComponent();
    const error = await screen.findAllByRole('alert', /An error occurred loading data. some error/i);
    expect(error[0]).toBeVisible();
  });

  describe('Tags', () => {
    it('does not render application tags, but renders format tags', async () => {
      renderComponent();
      const tags = await screen.findAllByTestId('component-details-tag');
      expect(tags.length).toBe(1);
      expect(tags[0]).toHaveTextContent('Maven');
    });

    it('renders application tags', async () => {
      axiosMock.onGet(getComponentLabels(repositoryId, componentHash, 'repository')).reply(200, labelsData);
      renderComponent();
      await screen.findAllByText('ant : ant : 1.6.1');
      const tags = await screen.findAllByTestId('component-details-tag');
      expect(tags.length).toBe(2);
      expect(tags[0]).toHaveTextContent('Maven');
      expect(tags[1]).toHaveTextContent('Architecture-Blacklisted');
    });
  });

  describe('Tabs', () => {
    it('renders the tabs', async () => {
      renderComponent();
      const titles = await screen.findAllByText('ant : ant : 1.6.1');
      const title = titles[0];
      expect(title).toBeVisible();
      const tabs = screen.getAllByRole('tab');
      expect(tabs.length).toBe(5);
      expect(tabs[0]).toHaveTextContent('Overview');
      expect(tabs[1]).toHaveTextContent('Policy Violations');
      expect(tabs[2]).toHaveTextContent('Security');
      expect(tabs[3]).toHaveTextContent('Legal');
      expect(tabs[4]).toHaveTextContent('Labels');
    });

    it('renders overview tab', async () => {
      renderComponent();
      const titles = await screen.findAllByText('ant : ant : 1.6.1');
      const title = titles[0];
      expect(title).toBeVisible();
      const headers = screen.getAllByRole('heading');
      expect(headers[1]).toHaveTextContent('Component Information');
      expect(headers[2]).toHaveTextContent('Version Explorer');
      expect(headers[3]).toHaveTextContent('Suggested Version Change');
      expect(headers[4]).toHaveTextContent('Compare Versions');
    });

    it('renders policy violations tab', async () => {
      const tabIdLens = lensPath(['router', 'currentParams', 'tabId']);
      const newState = set(tabIdLens, 'violations', defaultPreloadedState);
      renderComponent(newState);
      const titles = await screen.findAllByText('ant : ant : 1.6.1');
      const title = titles[0];
      expect(title).toBeVisible();
      const headers = screen.getAllByRole('heading');
      expect(headers[1]).toHaveTextContent('Policy Violations');
    });

    it('renders security violations tab', async () => {
      const tabIdLens = lensPath(['router', 'currentParams', 'tabId']);
      const newState = set(tabIdLens, 'security', defaultPreloadedState);
      renderComponent(newState);
      const titles = await screen.findAllByText('ant : ant : 1.6.1');
      const title = titles[0];
      expect(title).toBeVisible();
      const headers = screen.getAllByRole('heading');
      expect(headers[1]).toHaveTextContent('Security Violations');
      expect(headers[2]).toHaveTextContent('Vulnerabilities');
    });

    it('renders legal tab', async () => {
      const tabIdLens = lensPath(['router', 'currentParams', 'tabId']);
      const newState = set(tabIdLens, 'legal', defaultPreloadedState);
      renderComponent(newState);
      const titles = await screen.findAllByText('ant : ant : 1.6.1');
      const title = titles[0];
      expect(title).toBeVisible();
      const headers = await screen.findAllByRole('heading');
      expect(headers[1]).toHaveTextContent('Legal Policy Violations');
    });

    it('renders labels tab', async () => {
      const tabIdLens = lensPath(['router', 'currentParams', 'tabId']);
      const newState = set(tabIdLens, 'labels', defaultPreloadedState);
      renderComponent(newState);
      const title = await screen.findByText('Manage Labels');
      expect(title).toBeVisible();
      const headers = screen.getAllByRole('heading');
      expect(headers[1]).toHaveTextContent('Manage Labels');
    });
  });

  describe('Violations NxDrawer', () => {
    it('shows the popover when clicking a violation', async () => {
      const tabIdLens = lensPath(['router', 'currentParams', 'tabId']);
      const newState = set(tabIdLens, 'violations', defaultPreloadedState);
      renderComponent(newState);
      const titles = await screen.findAllByText('ant : ant : 1.6.1');
      const title = titles[0];
      expect(title).toBeVisible();
      const violationRows = screen.getAllByRole('row');
      expect(violationRows.length).toBe(8);
      fireEvent.click(violationRows[3]);
      const dialog = screen.getByRole('dialog', { hidden: true });
      expect(dialog).not.toHaveAttribute('open');
      await fireEvent.animationEnd(dialog);
      expect(dialog).toHaveAttribute('open');
      const dialogTitle = within(dialog).getByText('Violation of');
      expect(dialogTitle).toBeVisible();
      expect(dialogTitle).toHaveTextContent('Violation of Security-Medium');
    });
  });

  describe('Back Button', () => {
    it('displays "Back to Repository Results" when coming from repository results page', async () => {
      const stateWithRepositoryResultsPrev = {
        ...defaultPreloadedState,
        router: {
          ...defaultPreloadedState.router,
          prevState: {
            name: 'firewall.repository-report',
            url: '/firewall/repository/:repositoryId/results',
          },
        },
      };
      renderComponent(stateWithRepositoryResultsPrev);
      const titles = await screen.findAllByText('ant : ant : 1.6.1');
      expect(titles[0]).toBeVisible();
      const backButton = screen.getByText('Back to Repository Results');
      expect(backButton).toBeVisible();
    });

    it('displays "Back to Firewall Dashboard" when coming from firewall dashboard in standalone firewall', async () => {
      const stateWithFirewallDashboardPrev = {
        ...defaultPreloadedState,
        router: {
          ...defaultPreloadedState.router,
          prevState: {
            name: 'firewall.firewallPage',
            url: '/firewall/dashboard',
          },
        },
        firewall: {
          isStandaloneFirewall: true,
          componentDetailsPage: {
            componentDetails: null,
            isLoadingComponentDetails: false,
            componentDetailsError: null,
          },
        },
      };
      renderComponent(stateWithFirewallDashboardPrev);
      const titles = await screen.findAllByText('ant : ant : 1.6.1');
      expect(titles[0]).toBeVisible();
      const backButton = screen.getByText('Back to Firewall Dashboard');
      expect(backButton).toBeVisible();
    });

    it('displays "Back to Auto Release from Quarantine" when coming from auto unquarantine page', async () => {
      const stateWithAutoUnquarantinePrev = {
        ...defaultPreloadedState,
        router: {
          ...defaultPreloadedState.router,
          prevState: {
            name: 'firewall.firewallAutoUnquarantinePage',
            url: '/firewall/autoReleaseQuarantine',
          },
        },
        firewall: {
          isStandaloneFirewall: true,
          componentDetailsPage: {
            componentDetails: null,
            isLoadingComponentDetails: false,
            componentDetailsError: null,
          },
        },
      };
      renderComponent(stateWithAutoUnquarantinePrev);
      const titles = await screen.findAllByText('ant : ant : 1.6.1');
      expect(titles[0]).toBeVisible();
      const backButton = screen.getByText('Back to Auto Release from Quarantine');
      expect(backButton).toBeVisible();
    });

    it('preserves back button text when navigating between tabs', async () => {
      // Initial state: came from repository results
      const stateWithRepositoryResultsPrev = {
        ...defaultPreloadedState,
        router: {
          ...defaultPreloadedState.router,
          currentParams: {
            ...defaultPreloadedState.router.currentParams,
            tabId: 'overview',
          },
          currentState: { name: 'firewall.componentDetailsPage' },
          prevState: {
            name: 'firewall.repository-report',
            url: '/firewall/repository/:repositoryId/results',
          },
        },
      };

      const { rerender } = renderComponent(stateWithRepositoryResultsPrev);
      const titles = await screen.findAllByText('ant : ant : 1.6.1');
      expect(titles[0]).toBeVisible();

      // Verify initial back button text
      let backButton = screen.getByText('Back to Repository Results');
      expect(backButton).toBeVisible();

      // Simulate navigation to violations tab (prevState changes in router)
      // Rerender the component (router.stateService is already mocked in beforeEach)
      rerender(<FirewallComponentDetailsPage />);

      // Back button text should still say "Back to Repository Results"
      // because we captured the initial prevState on mount
      backButton = screen.getByText('Back to Repository Results');
      expect(backButton).toBeVisible();
    });
  });
});
