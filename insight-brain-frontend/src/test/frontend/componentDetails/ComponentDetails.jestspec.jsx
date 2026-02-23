/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import ComponentDetails from 'MainRoot/componentDetails/ComponentDetails';
import { axiosMockAdapter, render, screen } from 'TestRoot/SpecUtil';
import {
  getApplicableLabelsUrl,
  getComponentLabels,
  getDependenciesUrl,
  getReportBomUrl,
  getReportDataUrl,
  getReportMetadataUrl,
  getReportPartialMatchedUrl,
  getReportPolicyThreatsUrl,
  getReportUnknownJsUrl,
} from 'MainRoot/util/CLMLocation';

import {
  bomData,
  metadata,
  policyThreatsData,
  reportData,
  dependenciesData,
  labelsData,
  dependenciesDataTransitive,
} from './data';
import { compose, lensPath, set } from 'ramda';
import router from 'MainRoot/router/routerInstance';

describe('ComponentDetails', () => {
  let axiosMock;
  let defaultPreloadedState;
  let renderComponent;
  const publicId = 'publicId';
  const hash = '67137314736c6a2f39a8';
  const scanId = 'scanId';
  const tabId = 'overview';

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    defaultPreloadedState = {
      router: {
        currentParams: {
          publicId,
          hash,
          scanId,
          tabId,
        },
        currentState: { name: 'component name' },
      },
      applicationReport: {
        reportParameters: { appId: publicId, scanId, isUnknownJs: false },
        pendingLoads: [],
      },
    };

    axiosMock.onGet(getReportBomUrl(publicId, scanId)).reply(200, bomData);
    axiosMock.onGet(getReportMetadataUrl(publicId, scanId)).reply(200, metadata);
    axiosMock.onGet(getReportPolicyThreatsUrl(publicId, scanId)).reply(200, policyThreatsData);
    axiosMock.onGet(getReportDataUrl(publicId, scanId)).reply(200, reportData);
    axiosMock.onGet(getReportPartialMatchedUrl(publicId, scanId)).reply(200, { aaData: [] });
    axiosMock.onGet(getDependenciesUrl(publicId, scanId)).reply(200, dependenciesData);
    axiosMock.onGet(getComponentLabels(publicId, hash, 'application')).reply(200, { labelsByOwner: [] });
    axiosMock.onGet(getApplicableLabelsUrl('application', publicId)).reply(200, { labelsByOwner: [] });

    jest.spyOn(router.stateService, 'href').mockReturnValue('#');
    jest.spyOn(router.stateService, 'get').mockReturnValue('#');
    jest.spyOn(router.stateService, 'includes').mockReturnValue(false);

    renderComponent = (preloadedState = defaultPreloadedState) => render(<ComponentDetails />, { preloadedState });
  });

  it('renders a loading indicator and title', async () => {
    renderComponent();
    const loading = screen.getAllByText('Loading…');
    expect(loading[1]).toBeVisible();
    const titles = await screen.findAllByText('org.seleniumhq.selenium : selenium-chrome-driver : 3.141.59');
    const title = titles[0];
    expect(title.parentNode.tagName).toBe('H1');
    expect(title).toBeVisible();
  });

  it('renders an error message', async () => {
    axiosMock.onGet(getReportBomUrl(publicId, scanId)).reply(500, 'some error');
    renderComponent();
    const error = await screen.findAllByRole('alert', /An error occurred loading data. some error/i);
    expect(error[0]).toBeVisible();
  });

  describe('Pagination', () => {
    it('renders pagination if it comes from report', async () => {
      renderComponent();
      const footer = await screen.findByRole('contentinfo');
      expect(footer).toBeVisible();
      expect(footer).toHaveTextContent('Previous Component1 of 3Next Component');
    });

    it('does not render pagination if it comes from the dependency tree', async () => {
      const tabIdLens = lensPath(['applicationReport', 'dependencyTreePageRouterParams']);
      const newState = set(tabIdLens, 'Routed Params', defaultPreloadedState);
      renderComponent(newState);
      const titles = await screen.findAllByText('org.seleniumhq.selenium : selenium-chrome-driver : 3.141.59');
      const title = titles[0];
      expect(title).toBeVisible();
      const footer = screen.queryByRole('contentinfo');
      expect(footer).not.toBeInTheDocument();
    });

    it('does not render pagination if it comes from the priorities page', async () => {
      const currentStateNameLens = lensPath(['router', 'currentState', 'name']);
      const newState = set(
        currentStateNameLens,
        'componentDetailsPageWithinPrioritiesPageContainerFromDashboards',
        defaultPreloadedState
      );
      renderComponent(newState);
      const titles = await screen.findAllByText('org.seleniumhq.selenium : selenium-chrome-driver : 3.141.59');
      const title = titles[0];
      expect(title).toBeVisible();
      const footer = screen.queryByRole('contentinfo');
      expect(footer).not.toBeInTheDocument();
    });
  });

  describe('Tags', () => {
    it('does not render application tags, but renders direct dependency and format tags', async () => {
      renderComponent();
      const tags = await screen.findAllByTestId('component-details-tag');
      expect(tags.length).toBe(2);
      expect(tags[0]).toHaveTextContent('Maven');
      expect(tags[1]).toHaveTextContent('Direct Dependency');
    });

    it('renders transitive dependency tag', async () => {
      const directDependencyLens = lensPath(['aaData', 1, 'directDependency']);
      const newBomData = set(directDependencyLens, false, bomData);
      axiosMock.onGet(getReportBomUrl(publicId, scanId)).reply(200, newBomData);
      axiosMock.onGet(getDependenciesUrl(publicId, scanId)).reply(200, dependenciesDataTransitive);
      renderComponent();
      const tags = await screen.findAllByTestId('component-details-tag');
      expect(tags.length).toBe(2);
      expect(tags[0]).toHaveTextContent('Maven');
      expect(tags[1]).toHaveTextContent('Transitive Dependency');
    });

    it('renders application tags', async () => {
      axiosMock.onGet(getComponentLabels(publicId, hash, 'application')).reply(200, labelsData);
      renderComponent();
      const tags = await screen.findAllByTestId('component-details-tag');
      expect(tags.length).toBe(4);
      expect(tags[0]).toHaveTextContent('Maven');
      expect(tags[1]).toHaveTextContent('Direct Dependency');
      expect(tags[2]).toHaveTextContent('Architecture-Cleanup');
      expect(tags[3]).toHaveTextContent('new label');
    });

    it('renders inner source tag', async () => {
      const innerSourceLens = lensPath(['aaData', 1, 'innerSource']);
      const newBomData = set(innerSourceLens, true, bomData);
      axiosMock.onGet(getReportBomUrl(publicId, scanId)).reply(200, newBomData);
      renderComponent();
      const tags = await screen.findAllByTestId('component-details-tag');
      expect(tags.length).toBe(3);
      expect(tags[0]).toHaveTextContent('Maven');
      expect(tags[1]).toHaveTextContent('Direct Dependency');
      expect(tags[2]).toHaveTextContent('InnerSource');
    });
  });

  describe('Unknown component', () => {
    const innerSourceLens = lensPath(['aaData', 1, 'matchState']);
    const newBomData = set(innerSourceLens, 'unknown', bomData);
    beforeEach(() => {
      axiosMock.onGet(getReportBomUrl(publicId, scanId)).reply(200, newBomData);
      axiosMock.onGet(getReportUnknownJsUrl(publicId, scanId)).reply(200);
    });

    it('Shows the unknown component alert and claim tab', async () => {
      renderComponent();
      const alert = await screen.findByRole('alert');
      const tabs = screen.getAllByRole('tab');
      expect(alert).toBeVisible();
      expect(alert).toHaveTextContent('The component is unknown.');
      expect(tabs.length).toBe(3);
      expect(tabs[0]).toHaveTextContent('Overview');
      expect(tabs[1]).toHaveTextContent('Policy Violations');
      expect(tabs[2]).toHaveTextContent('Claim');
    });

    it('renders claim tab', async () => {
      const tabIdLens = lensPath(['router', 'currentParams', 'tabId']);
      const newState = set(tabIdLens, 'claim', defaultPreloadedState);
      renderComponent(newState);
      const titles = await screen.findAllByText('org.seleniumhq.selenium : selenium-chrome-driver : 3.141.59');
      const title = titles[0];
      expect(title).toBeVisible();
      const headers = screen.getAllByRole('heading');
      expect(headers[1]).toHaveTextContent('Claim Component');
    });

    it('Shows the proprietary component alert when unknown component has been claimed', async () => {
      const proprietaryLens = lensPath(['aaData', 1, 'proprietary']);
      const newestBomData = compose(set(innerSourceLens, 'unknown'), set(proprietaryLens, true))(newBomData);
      axiosMock.onGet(getReportBomUrl(publicId, scanId)).reply(200, newestBomData);
      renderComponent();
      const alert = await screen.findByRole('alert');
      expect(alert).toBeVisible();
      expect(alert).toHaveTextContent('This component has been matched as a Proprietary Component. Learn more here');
    });
  });

  describe('Tabs', () => {
    it('renders the tabs', async () => {
      renderComponent();
      const titles = await screen.findAllByText('org.seleniumhq.selenium : selenium-chrome-driver : 3.141.59');
      const title = titles[0];
      expect(title).toBeVisible();
      const tabs = screen.getAllByRole('tab');
      expect(tabs.length).toBe(6);
      expect(tabs[0]).toHaveTextContent('Overview');
      expect(tabs[1]).toHaveTextContent('Policy Violations');
      expect(tabs[2]).toHaveTextContent('Security');
      expect(tabs[3]).toHaveTextContent('Legal');
      expect(tabs[4]).toHaveTextContent('Labels');
      expect(tabs[5]).toHaveTextContent('Audit Log');
    });

    it('renders overview tab', async () => {
      renderComponent();
      const titles = await screen.findAllByText('org.seleniumhq.selenium : selenium-chrome-driver : 3.141.59');
      const title = titles[0];
      expect(title).toBeVisible();
      const headers = screen.getAllByRole('heading');
      expect(headers[1]).toHaveTextContent('Component Information');
      expect(headers[2]).toHaveTextContent('Version Explorer');
      expect(headers[3]).toHaveTextContent('Suggested Version Change');
      expect(headers[4]).toHaveTextContent('Compare Versions');
      expect(headers[5]).toHaveTextContent('Dependency Tree');
    });

    it('renders policy violations tab', async () => {
      const tabIdLens = lensPath(['router', 'currentParams', 'tabId']);
      const newState = set(tabIdLens, 'violations', defaultPreloadedState);
      renderComponent(newState);
      const titles = await screen.findAllByText('org.seleniumhq.selenium : selenium-chrome-driver : 3.141.59');
      const title = titles[0];
      expect(title).toBeVisible();
      const headers = screen.getAllByRole('heading');
      expect(headers[1]).toHaveTextContent('Policy Violations');
    });

    it('renders security violations tab', async () => {
      const tabIdLens = lensPath(['router', 'currentParams', 'tabId']);
      const newState = set(tabIdLens, 'security', defaultPreloadedState);
      renderComponent(newState);
      const titles = await screen.findAllByText('org.seleniumhq.selenium : selenium-chrome-driver : 3.141.59');
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
      const titles = await screen.findAllByText('org.seleniumhq.selenium : selenium-chrome-driver : 3.141.59');
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

    it('renders audit log tab', async () => {
      const tabIdLens = lensPath(['router', 'currentParams', 'tabId']);
      const newState = set(tabIdLens, 'audit', defaultPreloadedState);
      renderComponent(newState);
      const titles = await screen.findAllByText('org.seleniumhq.selenium : selenium-chrome-driver : 3.141.59');
      const title = titles[0];
      expect(title).toBeVisible();
      const table = screen.getByRole('table');
      expect(table).toBeVisible();
    });
  });
});
