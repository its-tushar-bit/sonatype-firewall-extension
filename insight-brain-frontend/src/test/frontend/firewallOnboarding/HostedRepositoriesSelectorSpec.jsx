/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';

import { render, screen, axiosMockAdapter, fireEvent } from 'TestRoot/SpecUtil';
import HostedRepositoriesSelector from 'MainRoot/firewallOnboarding/HostedRepositoriesSelector';
import { getRepositoryListUrl, getSupportedRepositoriesFormat } from 'MainRoot/util/CLMLocation';
import { steps } from 'MainRoot/firewallOnboarding/firewallOnboardingUtils';

let renderComponent,
  axiosMock,
  expectedRepositoriesByRepoManagerIdUrl,
  repositoriesList,
  supportedFormats,
  initialState;

describe('HostedRepositoriesSelector', function () {
  beforeAll(function () {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(function () {
    supportedFormats = {
      regexpsByRepositoryFormat: {
        maven: [],
        npm: [],
        pypi: [],
        go: [],
      },
    };
    repositoriesList = [
      {
        id: 'id1',
        repositoryManagerId: 'repoManagerId1',
        publicId: 'publicId1',
        repositoryType: 'hosted',
        auditEnabled: false,
        quarantineEnabled: false,
        policyCompliantComponentSelectionEnabled: false,
        namespaceConfusionProtectionEnabled: true,
        format: 'maven',
      },
      {
        id: 'id2',
        repositoryManagerId: 'repoManagerId2',
        publicId: 'publicId2',
        repositoryType: 'hosted',
        auditEnabled: false,
        quarantineEnabled: false,
        policyCompliantComponentSelectionEnabled: false,
        namespaceConfusionProtectionEnabled: true,
        format: 'maven',
      },
      {
        id: 'id3',
        repositoryManagerId: 'repoManagerId3',
        publicId: 'publicId3',
        repositoryType: 'hosted',
        auditEnabled: false,
        quarantineEnabled: false,
        policyCompliantComponentSelectionEnabled: false,
        namespaceConfusionProtectionEnabled: false,
        format: 'maven',
      },
      {
        id: 'id4',
        repositoryManagerId: 'repoManagerId4',
        publicId: 'publicId4',
        repositoryType: 'hosted',
        auditEnabled: false,
        quarantineEnabled: false,
        policyCompliantComponentSelectionEnabled: false,
        namespaceConfusionProtectionEnabled: false,
        format: 'npm',
      },
      {
        id: 'id5',
        repositoryManagerId: 'repoManagerId5',
        publicId: 'publicId5',
        repositoryType: 'hosted',
        auditEnabled: false,
        quarantineEnabled: false,
        policyCompliantComponentSelectionEnabled: false,
        namespaceConfusionProtectionEnabled: false,
        format: 'npm',
      },
      {
        id: 'id6',
        repositoryManagerId: 'repoManagerId6',
        publicId: 'publicId6',
        repositoryType: 'hosted',
        auditEnabled: false,
        quarantineEnabled: false,
        policyCompliantComponentSelectionEnabled: false,
        namespaceConfusionProtectionEnabled: false,
        format: 'go',
      },
      {
        id: 'id7',
        repositoryManagerId: 'repoManagerId7',
        publicId: 'publicId7',
        repositoryType: 'hosted',
        auditEnabled: false,
        quarantineEnabled: false,
        policyCompliantComponentSelectionEnabled: false,
        namespaceConfusionProtectionEnabled: false,
        format: 'pypi',
      },
    ];

    initialState = {
      firewallOnboarding: {
        loading: false,
        currentStep: steps[0],
        supportedFormats: [],
        repositories: {
          loading: false,
          loadError: null,
          saving: false,
          saveError: null,
          list: null,
        },
        unconfiguredRepoManagers: {
          repoManagers: [
            { id: 'id', instanceId: 'instanceId', userAgent: 'userAgent', configured: false, configureTime: null },
          ],
          loading: false,
          loadError: null,
        },
      },
    };

    expectedRepositoriesByRepoManagerIdUrl = getRepositoryListUrl('id');
    renderComponent = (preloadedState = initialState) => render(<HostedRepositoriesSelector />, { preloadedState });
  });

  it('renders loading messages', async () => {
    initialState.firewallOnboarding.repositories.loading = true;
    renderComponent(initialState);

    expect(screen.getByText('Loading…')).toBeVisible();
  });

  it('renders an error with the error message and a retry button when there is a failure fetching repos', async () => {
    axiosMock.onGet(getSupportedRepositoriesFormat()).reply(200, supportedFormats);
    axiosMock.onGet(expectedRepositoriesByRepoManagerIdUrl).reply(() => Promise.reject('Test error'));
    initialState.firewallOnboarding.repositories.loadError = 'Some error';
    renderComponent(initialState);

    expect(await screen.findByRole('alert')).toBeVisible();
    expect(await screen.findByText(/Some error/i)).toBeVisible();

    const retryButton = await screen.findByRole('button', { name: 'Retry' });
    expect(retryButton).toBeVisible();
    fireEvent.click(retryButton);

    expect(screen.getByText('Loading…')).toBeVisible();
    expect(await screen.findByRole('alert')).toBeVisible();
    expect(await screen.findByText(/Test error/i)).toBeVisible();
  });

  describe('when successfully loads hosted repositories selectors page', () => {
    it('renders the expected repositories grouped by format', async () => {
      initialState.firewallOnboarding.repositories.list = repositoriesList;
      initialState.firewallOnboarding.supportedFormats = Object.keys(supportedFormats.regexpsByRepositoryFormat);
      renderComponent(initialState);

      expect(await screen.findByText('maven')).toBeVisible();
      expect(await screen.findByText('npm')).toBeVisible();
      expect(await screen.findByText('go')).toBeVisible();
      expect(await screen.findByText('other')).toBeVisible();

      const repositoriesListTitles = await screen.findAllByRole('heading', { level: 2 });
      expect(await repositoriesListTitles[0]).toHaveTextContent('maven2 of 3');
      expect(await repositoriesListTitles[1]).toHaveTextContent('npm0 of 2');
      expect(await repositoriesListTitles[2]).toHaveTextContent('go0 of 1');
      expect(await repositoriesListTitles[3]).toHaveTextContent('other0 of 1');

      const repositoryItems = screen.getAllByRole('row', { name: /repository item/i });
      repositoryItems.forEach(async (repository, index) => {
        expect(await repository).toHaveTextContent(`publicId${index + 1}`);
      });
    });

    it('renders correct subtitle and link', async () => {
      renderComponent(initialState);
      const subtitleEl = await screen.getByText('Choose which hosted repositories you would like to enable', {
        exact: false,
      });
      expect(subtitleEl.textContent).toEqual(
        'Choose which hosted repositories you would like to enable namespace confusion protection on.'
      );

      const NAMESPACE_CONFUSION_PROTECTION_URL =
        'http://links.sonatype.com/products/nxiq/doc/preventing-namespace-confusion';
      expect(screen.getByRole('link', { name: 'namespace confusion protection' })).toHaveAttribute(
        'href',
        NAMESPACE_CONFUSION_PROTECTION_URL
      );
    });

    it('renders empty messages if there is no repositories to configure', async () => {
      initialState.firewallOnboarding.repositories.list = [];
      initialState.firewallOnboarding.supportedFormats = Object.keys(supportedFormats.regexpsByRepositoryFormat);
      renderComponent(initialState);

      expect(await screen.findByText('There are no hosted repositories to apply your protection rules.')).toBeVisible();
    });

    it('renders empty messages if there is no supported formats', async () => {
      initialState.firewallOnboarding.repositories.list = repositoriesList;
      initialState.firewallOnboarding.supportedFormats = [];
      renderComponent(initialState);

      expect(await screen.findByText('There are no hosted repositories to apply your protection rules.')).toBeVisible();
    });
  });
});
