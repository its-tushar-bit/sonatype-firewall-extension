/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';

import { render, screen, axiosMockAdapter, fireEvent } from 'TestRoot/SpecUtil';
import RepositoriesSelector from 'MainRoot/firewallOnboarding/RepositoriesSelector';
import { getRepositoryListUrl, getSupportedRepositoriesFormat } from 'MainRoot/util/CLMLocation';
import { hostedRepoItems, proxyRepoItems } from './repositoriesSelectorListItems';
import {
  stepsById,
  MALICIOUS_COMPONENTS_PROTECTION_URL,
  NAMESPACE_CONFUSION_PROTECTION_URL,
} from 'MainRoot/firewallOnboarding/firewallOnboardingUtils';

let renderComponent,
  axiosMock,
  expectedRepositoriesByRepoManagerIdUrl,
  repositoriesList,
  supportedFormats,
  initialState;

describe('RepositoriesSelector', function () {
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

    repositoriesList = {
      proxy: proxyRepoItems,
      hosted: hostedRepoItems,
    };

    initialState = {
      firewallOnboarding: {
        loading: false,
        currentStep: stepsById.selectProxy,
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
        protectionRules: {
          supplyChainAttacksProtectionEnabled: true,
          namespaceConfusionProtectionEnabled: true,
        },
        launchFirewall: {
          saving: false,
          saveError: null,
        },
      },
    };

    expectedRepositoriesByRepoManagerIdUrl = getRepositoryListUrl('id');
    renderComponent = (preloadedState = initialState) => render(<RepositoriesSelector />, { preloadedState });
  });

  it('renders loading messages', async () => {
    initialState.firewallOnboarding.repositories.loading = true;
    renderComponent();

    expect(screen.getByText('Loading…')).toBeVisible();
  });

  it('renders an error with the error message and a retry button when there is a failure fetching repos', async () => {
    axiosMock.onGet(getSupportedRepositoriesFormat()).reply(200, supportedFormats);
    axiosMock.onGet(expectedRepositoriesByRepoManagerIdUrl).reply(() => Promise.reject('Test error'));
    initialState.firewallOnboarding.repositories.loadError = 'Some error';
    renderComponent();

    expect(await screen.findByRole('alert')).toBeVisible();
    expect(await screen.findByText(/Some error/i)).toBeVisible();

    const retryButton = await screen.findByRole('button', { name: 'Retry' });
    expect(retryButton).toBeVisible();
    fireEvent.click(retryButton);

    expect(screen.getByText('Loading…')).toBeVisible();
    expect(await screen.findByRole('alert')).toBeVisible();
    expect(await screen.findByText(/Test error/i)).toBeVisible();
  });

  describe('successfully loads proxy repositories selectors page', () => {
    it('renders correct title', async () => {
      renderComponent(initialState);
      const titleEl = await screen.getByText('Enable protection from malicious components');
      expect(titleEl).toBeVisible();
    });

    it('renders correct title when protenctionRules are disabled', async () => {
      initialState.firewallOnboarding.protectionRules.supplyChainAttacksProtectionEnabled = false;
      initialState.firewallOnboarding.protectionRules.namespaceConfusionProtectionEnabled = false;
      renderComponent(initialState);
      const titleEl = await screen.getByText('You have not enabled recommended protection');
      expect(titleEl).toBeVisible();
    });

    it('renders correct subtitle when both supplyChainAttacksProtection and namespaceConfusionProtection are enabled', async () => {
      renderComponent(initialState);
      const subtitleEl = await screen.getByText('The selected proxy repositories will have', {
        exact: false,
      });

      expect(subtitleEl.textContent).toEqual(
        'The selected proxy repositories will have supply chain attacks protection and namespace confusion protection enabled.'
      );
      expect(screen.getByRole('link', { name: 'namespace confusion protection' })).toHaveAttribute(
        'href',
        NAMESPACE_CONFUSION_PROTECTION_URL
      );
      expect(screen.getByRole('link', { name: 'supply chain attacks protection' })).toHaveAttribute(
        'href',
        MALICIOUS_COMPONENTS_PROTECTION_URL
      );
    });

    it('renders correct subtitle when only supplyChainAttacksProtection is enabled', async () => {
      initialState.firewallOnboarding.protectionRules.namespaceConfusionProtectionEnabled = false;
      renderComponent(initialState);
      const subtitleEl = await screen.getByText('The selected proxy repositories will have', {
        exact: false,
      });

      expect(subtitleEl.textContent).toEqual(
        'The selected proxy repositories will have supply chain attacks protection enabled. You can also enable namespace confusion protection by going back to the previous step.'
      );
      expect(screen.getByRole('link', { name: 'namespace confusion protection' })).toHaveAttribute(
        'href',
        NAMESPACE_CONFUSION_PROTECTION_URL
      );
      expect(screen.getByRole('link', { name: 'supply chain attacks protection' })).toHaveAttribute(
        'href',
        MALICIOUS_COMPONENTS_PROTECTION_URL
      );
    });

    it('renders correct subtitle when only namespaceConfusionProtection is enabled', async () => {
      initialState.firewallOnboarding.protectionRules.supplyChainAttacksProtectionEnabled = false;
      renderComponent(initialState);
      const subtitleEl = await screen.getByText('The selected proxy repositories will have', {
        exact: false,
      });

      expect(subtitleEl.textContent).toEqual(
        'The selected proxy repositories will have namespace confusion protection enabled. You can also enable supply chain attacks protection by going back to the previous step.'
      );
      expect(screen.getByRole('link', { name: 'namespace confusion protection' })).toHaveAttribute(
        'href',
        NAMESPACE_CONFUSION_PROTECTION_URL
      );
      expect(screen.getByRole('link', { name: 'supply chain attacks protection' })).toHaveAttribute(
        'href',
        MALICIOUS_COMPONENTS_PROTECTION_URL
      );
    });

    it('renders correct subtitle when both supplyChainAttacksProtection and namespaceConfusionProtection are DISABLED', async () => {
      initialState.firewallOnboarding.protectionRules.namespaceConfusionProtectionEnabled = false;
      initialState.firewallOnboarding.protectionRules.supplyChainAttacksProtectionEnabled = false;
      renderComponent(initialState);
      const subtitleEl = await screen.getByText('The selected proxy repositories will not have', {
        exact: false,
      });

      expect(subtitleEl.textContent).toEqual(
        'The selected proxy repositories will not have supply chain attacks protection or namespace confusion protection enabled. You can enable protection by going back to the previous step.'
      );
      expect(screen.getByRole('link', { name: 'namespace confusion protection' })).toHaveAttribute(
        'href',
        NAMESPACE_CONFUSION_PROTECTION_URL
      );
      expect(screen.getByRole('link', { name: 'supply chain attacks protection' })).toHaveAttribute(
        'href',
        MALICIOUS_COMPONENTS_PROTECTION_URL
      );
    });

    it('renders the expected proxy repositories grouped by format', async () => {
      initialState.firewallOnboarding.repositories.list = repositoriesList.proxy;
      initialState.firewallOnboarding.supportedFormats = Object.keys(supportedFormats.regexpsByRepositoryFormat);
      renderComponent(initialState);

      expect(await screen.findByText('maven')).toBeVisible();
      expect(await screen.findByText('npm')).toBeVisible();
      expect(await screen.findByText('go')).toBeVisible();
      expect(await screen.findByText('pypi')).toBeVisible();

      const repositoriesListTitles = await screen.findAllByRole('heading', { level: 2 });
      expect(await repositoriesListTitles[0]).toHaveTextContent('maven2 of 3');
      expect(await repositoriesListTitles[1]).toHaveTextContent('npm0 of 2');
      expect(await repositoriesListTitles[2]).toHaveTextContent('go0 of 1');
      expect(await repositoriesListTitles[3]).toHaveTextContent('pypi0 of 1');

      const repositoryItems = screen.getAllByRole('row', { name: /repository item/i });
      repositoryItems.forEach(async (repository, index) => {
        expect(await repository).toHaveTextContent(`publicId${index + 1}`);
      });
    });

    it('renders empty messages if there is no repositories', async () => {
      initialState.firewallOnboarding.repositories.list = [];
      initialState.firewallOnboarding.supportedFormats = [];
      renderComponent(initialState);

      expect(await screen.findByText('There are no proxy repositories to apply your protection rules.')).toBeVisible();
    });

    it('renders empty messages if there is no proxy repositories to configure', async () => {
      initialState.firewallOnboarding.repositories.list = [];
      initialState.firewallOnboarding.supportedFormats = Object.keys(supportedFormats.regexpsByRepositoryFormat);
      renderComponent(initialState);

      expect(await screen.findByText('There are no proxy repositories to apply your protection rules.')).toBeVisible();
    });

    it('successfully loads proxy repositories selectors page when there are no supported repositories', async () => {
      initialState.firewallOnboarding.repositories.list = repositoriesList.proxy;
      initialState.firewallOnboarding.supportedFormats = [];
      renderComponent(initialState);

      // then all checkboxes are disabled
      screen
        .getAllByRole('checkbox', { name: /^firewall publicId[0-9]? repository item$/i })
        .forEach(async (checkbox) => {
          expect(await checkbox).toBeDisabled();
        });
    });
  });

  describe('loads hosted repositories selectors page', () => {
    it('renders correct title', async () => {
      initialState.firewallOnboarding.currentStep = stepsById.selectHosted;
      renderComponent(initialState);
      const titleEl = await screen.getByText('Protect your internal components from namespace attacks');
      expect(titleEl).toBeVisible();
    });

    it('renders correct subtitle when both supplyChainAttacksProtection and namespaceConfusionProtection are enabled', async () => {
      initialState.firewallOnboarding.currentStep = stepsById.selectHosted;
      renderComponent(initialState);
      const subtitleEl = await screen.getByText('The component names from the selected hosted repositories', {
        exact: false,
      });
      expect(subtitleEl.textContent).toEqual(
        'The component names from the selected hosted repositories will be used to protect against namespace confusion attacks against your proxy repositories.' +
          ' This capability should only be turned on for repositories with proprietary components only.' +
          ' Enabling it on hosted repositories containing open-source components will cause those namespaces to be quarantined.'
      );

      expect(screen.getByRole('link', { name: 'namespace confusion' })).toHaveAttribute(
        'href',
        NAMESPACE_CONFUSION_PROTECTION_URL
      );
    });

    it('renders correct subtitle when only namespaceConfusionProtection is enabled', async () => {
      initialState.firewallOnboarding.protectionRules.supplyChainAttacksProtectionEnabled = false;
      initialState.firewallOnboarding.currentStep = stepsById.selectHosted;
      renderComponent(initialState);
      const subtitleEl = await screen.getByText('The component names from the selected hosted repositories', {
        exact: false,
      });
      expect(subtitleEl.textContent).toEqual(
        'The component names from the selected hosted repositories will be used to protect against namespace confusion attacks against your proxy repositories.' +
          ' This capability should only be turned on for repositories with proprietary components only.' +
          ' Enabling it on hosted repositories containing open-source components will cause those namespaces to be quarantined.'
      );

      expect(screen.getByRole('link', { name: 'namespace confusion' })).toHaveAttribute(
        'href',
        NAMESPACE_CONFUSION_PROTECTION_URL
      );
    });

    it('renders correct subtitle when only supplyChainAttacksProtection is enabled', async () => {
      initialState.firewallOnboarding.protectionRules.namespaceConfusionProtectionEnabled = false;
      initialState.firewallOnboarding.currentStep = stepsById.selectHosted;
      renderComponent(initialState);
      const subtitleEl = await screen.getByText('The component names from the selected hosted repositories', {
        exact: false,
      });

      expect(subtitleEl.textContent).toEqual(
        'The component names from the selected hosted repositories will not be used to protect against namespace confusion attacks against your proxy repositories.' +
          ' You can enable namespace confusion protection by going back to the previous step.' +
          ' This capability should only be turned on for repositories with proprietary components only.' +
          ' Enabling it on hosted repositories containing open-source components will cause those namespaces to be quarantined.'
      );
      expect(screen.getByRole('link', { name: 'namespace confusion' })).toHaveAttribute(
        'href',
        NAMESPACE_CONFUSION_PROTECTION_URL
      );
    });

    it('renders correct subtitle when both supplyChainAttacksProtection and namespaceConfusionProtection are DISABLED', async () => {
      initialState.firewallOnboarding.protectionRules.namespaceConfusionProtectionEnabled = false;
      initialState.firewallOnboarding.protectionRules.supplyChainAttacksProtectionEnabled = false;
      initialState.firewallOnboarding.currentStep = stepsById.selectHosted;
      renderComponent(initialState);
      const subtitleEl = await screen.getByText('The component names from the selected hosted repositories', {
        exact: false,
      });

      expect(subtitleEl.textContent).toEqual(
        'The component names from the selected hosted repositories will not be used to protect against namespace confusion attacks against your proxy repositories.' +
          ' You can enable namespace confusion protection by going back to the previous step.' +
          ' This capability should only be turned on for repositories with proprietary components only.' +
          ' Enabling it on hosted repositories containing open-source components will cause those namespaces to be quarantined.'
      );
      expect(screen.getByRole('link', { name: 'namespace confusion' })).toHaveAttribute(
        'href',
        NAMESPACE_CONFUSION_PROTECTION_URL
      );
    });

    it('renders the expected hosted repositories grouped by format', async () => {
      initialState.firewallOnboarding.currentStep = stepsById.selectHosted;
      initialState.firewallOnboarding.repositories.list = repositoriesList.hosted;
      initialState.firewallOnboarding.supportedFormats = Object.keys(supportedFormats.regexpsByRepositoryFormat);
      initialState.firewallOnboarding.currentStep = stepsById.selectHosted;
      renderComponent(initialState);

      expect(await screen.findByText('maven')).toBeVisible();
      expect(await screen.findByText('npm')).toBeVisible();
      expect(await screen.findByText('go')).toBeVisible();
      expect(await screen.findByText('pypi')).toBeVisible();

      const repositoriesListTitles = await screen.findAllByRole('heading', { level: 2 });
      expect(await repositoriesListTitles[0]).toHaveTextContent('maven2 of 3');
      expect(await repositoriesListTitles[1]).toHaveTextContent('npm0 of 2');
      expect(await repositoriesListTitles[2]).toHaveTextContent('go0 of 1');
      expect(await repositoriesListTitles[3]).toHaveTextContent('pypi0 of 1');

      const repositoryItems = screen.getAllByRole('row', { name: /repository item/i });
      repositoryItems.forEach(async (repository, index) => {
        expect(await repository).toHaveTextContent(`publicId${index + 1}`);
      });
    });

    it('renders empty messages if there are no repositories', async () => {
      initialState.firewallOnboarding.currentStep = stepsById.selectHosted;
      initialState.firewallOnboarding.repositories.list = [];
      initialState.firewallOnboarding.supportedFormats = [];
      initialState.firewallOnboarding.currentStep = stepsById.selectHosted;
      renderComponent(initialState);

      expect(await screen.findByText('There are no hosted repositories to apply your protection rules.')).toBeVisible();
    });

    it('renders empty messages if there is no hosted repositories to configure', async () => {
      initialState.firewallOnboarding.currentStep = stepsById.selectHosted;
      initialState.firewallOnboarding.repositories.list = [];
      initialState.firewallOnboarding.supportedFormats = Object.keys(supportedFormats.regexpsByRepositoryFormat);
      initialState.firewallOnboarding.currentStep = stepsById.selectHosted;
      renderComponent(initialState);

      expect(await screen.findByText('There are no hosted repositories to apply your protection rules.')).toBeVisible();
    });
  });
});
