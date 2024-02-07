/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as routerContext from 'MainRoot/react/RouterStateContext';
import TransitiveViolationsPage from 'MainRoot/violation/TransitiveViolationsPage';
import { render, axiosMockAdapter, screen, fireEvent } from 'TestRoot/SpecUtil';
import {
  getOwnerHierarchyUrl,
  getReportMetadataUrl,
  getTransitiveViolationsUrl,
  getWaiveTransitiveViolationsUrl,
} from 'MainRoot/util/CLMLocation';
import {
  noTransitiveViolationsData,
  ownerHierarchyData,
  reportMockMetaData,
  transitiveData,
  waiveTransitiveData,
} from 'TestRoot/componentDetails/violations/data';
import { mergeDeepRight } from 'ramda';

describe('TransitiveViolationPage', function () {
  let renderComponent, defaultPreloadedState, axiosMock;

  const ownerType = 'application';
  const ownerId = 'ACME-CONSUMER';
  const scanId = 'a2e3c6037a6a46bd8b769729c76cbb20';
  const hash = '03ff80065de60b9287f4';

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    defaultPreloadedState = {
      router: {
        currentParams: {
          ownerType,
          ownerId,
          scanId,
          hash,
        },
        currentState: { name: 'component name' },
        prevState: {
          name: 'applicationReport.policy',
        },
      },
    };
    jest.spyOn(routerContext, 'useRouterState').mockReturnValue({
      get: jest.fn((stateName) => stateName),
      href: jest.fn((stateName, stateParams) => {
        if (stateParams) {
          return `${stateName}-${JSON.stringify(stateParams)}`;
        }
        return stateName;
      }),
    });

    axiosMock.onGet(getTransitiveViolationsUrl(ownerType, ownerId, scanId, hash)).reply(200, transitiveData);
    axiosMock.onGet(getReportMetadataUrl(ownerId, scanId)).reply(200, reportMockMetaData);
    axiosMock.onGet(getOwnerHierarchyUrl(ownerType, ownerId)).reply(200, ownerHierarchyData);
    axiosMock.onGet(getWaiveTransitiveViolationsUrl(ownerId, scanId, hash)).reply(200, waiveTransitiveData);
    renderComponent = (preloadedState = defaultPreloadedState) =>
      render(
        <>
          <div id="menu-bar__back-button-container"></div>
          <TransitiveViolationsPage />
        </>,
        { preloadedState }
      );
  });

  it('Renders a loading indicator and title ', async () => {
    renderComponent();
    const loading = screen.getAllByText('Loading…');
    expect(loading[0]).toBeVisible();
    const title = await screen.findByRole('heading', { name: 'org.example : ACME-business : 1.0-SNAPSHOT' });
    expect(title).toBeVisible();
  });

  it('Renders an error message', async () => {
    axiosMock.onGet(getTransitiveViolationsUrl(ownerType, ownerId, scanId, hash)).reply(500, 'some error');
    renderComponent();
    const error = await screen.findAllByRole('alert', /An error occurred loading data. some error/i);
    expect(error[0]).toBeVisible();
  });

  it('Loads transitive violations table with all the information', async () => {
    renderComponent();
    const loading = screen.getAllByText('Loading…');
    expect(loading[0]).toBeVisible();
    const policyName = await screen.findAllByText('Security-Medium');
    const displayName = await screen.findAllByText('commons-io : commons-io : 2.6');
    const threatLevel = await screen.findAllByText(7);
    expect(policyName[0]).toBeVisible();
    expect(displayName[0]).toBeVisible();
    expect(threatLevel[0]).toBeVisible();
  });

  it('Opens Request Waivers component if clicked', async () => {
    renderComponent();
    const requestWaiverButton = await screen.findByRole('button', { name: 'Request Waiver' });
    fireEvent.click(requestWaiverButton);
    const requestWaiverTitle = await screen.findByRole('heading', {
      name: 'Request Waivers for Transitive Violations',
    });
    expect(requestWaiverTitle).toBeVisible();
  });

  it('Opens Waive Transitive Violations Component is clicked  ', async () => {
    renderComponent();
    const waiverTransitiveViolationsButton = await screen.findByRole('button', { name: 'Waive Transitive Violations' });
    fireEvent.click(waiverTransitiveViolationsButton);
    const waiveTransitiveViolationsTitle = await screen.findByRole('heading', {
      name: 'Add Waivers to Transitive Violations',
    });
    expect(waiveTransitiveViolationsTitle).toBeVisible();
  });

  it('Opens Existing Waivers Component if clicked', async () => {
    renderComponent();
    const viewExistingWaiversButton = await screen.findByRole('button', { name: 'View Existing Waivers' });
    fireEvent.click(viewExistingWaiversButton);
    const viewExistingWaiversTitle = await screen.findByRole('heading', { name: 'Transitive Component Waivers' });
    expect(viewExistingWaiversTitle).toBeVisible();
  });

  describe('Back Button', function () {
    it('links to the component details if needed', async () => {
      renderComponent(
        mergeDeepRight(defaultPreloadedState, {
          router: { prevState: { name: 'applicationReport.componentDetails.violations' } },
        })
      );
      const backButton = await screen.findByRole('link', { name: 'Back' });
      expect(backButton).toBeVisible();
      expect(backButton).toHaveAttribute(
        'href',
        'applicationReport.componentDetails.violations-{"publicId":"ACME-CONSUMER","scanId":"a2e3c6037a6a46bd8b769729c76cbb20","hash":"03ff80065de60b9287f4"}'
      );
    });

    it('links to the app report if needed', async () => {
      renderComponent();
      const backButton = await screen.findByRole('link', { name: 'Back' });
      expect(backButton).toBeVisible();
      expect(backButton).toHaveAttribute(
        'href',
        'applicationReport.policy-{"publicId":"ACME-CONSUMER","scanId":"a2e3c6037a6a46bd8b769729c76cbb20","componentHash":"03ff80065de60b9287f4","tabId":"policy"}'
      );
    });
  });

  it('is shown if the queried component is InnerSource', async () => {
    renderComponent();
    const innerSource = await screen.findByText('InnerSource');
    expect(innerSource).toBeVisible();
  });

  it('disables the waive transitive violations button if there are no transitive violations', async () => {
    axiosMock
      .onGet(getTransitiveViolationsUrl(ownerType, ownerId, scanId, hash))
      .reply(200, noTransitiveViolationsData);
    renderComponent();
    const waiverTransitiveViolationsButton = await screen.findByRole('button', { name: 'Waive Transitive Violations' });
    expect(waiverTransitiveViolationsButton).toBeVisible();
    expect(waiverTransitiveViolationsButton).toBeDisabled();
  });
});
