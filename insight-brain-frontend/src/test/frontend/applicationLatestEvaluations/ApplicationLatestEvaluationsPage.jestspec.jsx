/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { axiosMockAdapter, render, setupPortalContainer, within } from 'TestRoot/SpecUtil';
import React from 'react';
import ApplicationLatestEvaluationsPage from 'MainRoot/applicationLatestEvaluations/ApplicationLatestEvaluationsPage';
import { screen } from '@testing-library/dom';
import { getApplicationReportHistoryUrl, getApplicationUrl } from 'MainRoot/util/CLMLocation';
import * as RouterStateContextModule from 'MainRoot/react/RouterStateContext';
import userEvent from '@testing-library/user-event';

describe('ApplicationLatestEvaluationsPage', () => {
  let axiosMock, mockRouterState, state, renderComponent;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    state = {
      router: {
        currentParams: {
          applicationPublicId: 'appPublicId',
          stageId: 'build',
        },
      },
    };

    axiosMock.onGet(getApplicationUrl('appPublicId')).reply(200, {
      id: 'appId',
      publicId: 'appPublicId',
      name: 'appName',
    });

    axiosMock.onGet(getApplicationReportHistoryUrl('appId', 'build')).reply(200, {
      applicationId: 'appId',
      reports: [
        {
          evaluationDate: '2025-01-21T10:32:51.641Z',
          scanTriggerTypeDisplayName: 'Web UI',
          scanId: 'someScanId',
          isForMonitoring: false,
          policyEvaluationResult: {
            criticalPolicyViolationCount: 1,
            severePolicyViolationCount: 2,
            moderatePolicyViolationCount: 3,
            totalComponentCount: 10,
          },
        },
      ],
    });

    mockRouterState = {
      href: jest.fn().mockImplementation(() => 'someHref'),
      includes: jest.fn(),
    };
    jest.spyOn(RouterStateContextModule, 'useRouterState').mockImplementation(() => mockRouterState);

    setupPortalContainer();

    renderComponent = (preloadedState) => render(<ApplicationLatestEvaluationsPage />, { preloadedState });
  });

  it('renders the page', async () => {
    renderComponent(state);

    expect(await screen.findByRole('heading', { name: 'appName Latest Evaluations' })).toBeVisible();
    expect(screen.getByText('Stage:')).toBeVisible();
    expect(screen.getByText('Build')).toBeVisible();

    const table = screen.getByRole('table');
    const rows = within(table).getAllByRole('rowgroup');
    expect(rows.length).toBe(2);

    const headers = within(rows[0]).getAllByRole('columnheader');
    expect(headers.length).toBe(5);
    expect(headers[0]).toHaveTextContent('Evaluation Date');
    expect(headers[1]).toHaveTextContent('Trigger');
    expect(headers[2]).toHaveTextContent('Violations');
    expect(headers[3]).toHaveTextContent('Components');
    expect(headers[4]).toHaveTextContent('');

    const cells = within(rows[1]).getAllByRole('cell');
    expect(cells.length).toBe(5);
    expect(cells[0]).toHaveTextContent('2025-01-21 05:32:51');
    expect(cells[1]).toHaveTextContent(/^Web UI$/);
    expect(within(cells[2]).getByText('Critical').closest('.nx-small-threat-counter')).toHaveTextContent('1');
    expect(within(cells[2]).getByText('Severe').closest('.nx-small-threat-counter')).toHaveTextContent('2');
    expect(within(cells[2]).getByText('Moderate').closest('.nx-small-threat-counter')).toHaveTextContent('3');
    expect(cells[3]).toHaveTextContent('10');
    expect(cells[4]).toHaveTextContent('View Report');
    expect(mockRouterState.href).toHaveBeenCalledWith('applicationReport.policy', {
      publicId: 'appPublicId',
      scanId: 'someScanId',
    });
    expect(within(cells[4]).getByRole('link', { name: 'View Report' })).toHaveAttribute('href', 'someHref');

    expect(axiosMock.history.get.length).toBe(2);
    expect(axiosMock.history.get[0].url).toBe(getApplicationUrl('appPublicId'));
    expect(axiosMock.history.get[1].url).toBe(getApplicationReportHistoryUrl('appId', 'build'));
  });

  it('renders continuous monitoring', async () => {
    axiosMock.onGet(getApplicationReportHistoryUrl('appId', 'build')).reply(200, {
      applicationId: 'appId',
      reports: [
        {
          evaluationDate: '2025-01-21T10:32:51.641Z',
          scanTriggerTypeDisplayName: 'Web UI',
          scanId: 'someScanId',
          isForMonitoring: true,
          policyEvaluationResult: {
            criticalPolicyViolationCount: 1,
            severePolicyViolationCount: 2,
            moderatePolicyViolationCount: 3,
            totalComponentCount: 10,
          },
        },
      ],
    });
    renderComponent(state);

    const table = await screen.findByRole('table');
    const rows = within(table).getAllByRole('rowgroup');
    const cells = within(rows[1]).getAllByRole('cell');
    expect(cells.length).toBe(5);
    expect(cells[1]).toHaveTextContent(/^Web UI \(Continuous Monitoring\)$/);
  });

  it('renders a page error for failing to load the application', async () => {
    const user = userEvent.setup();
    axiosMock.onGet(getApplicationUrl('appPublicId')).reply(500, 'Some error');

    renderComponent(state);

    const alert = await screen.findByRole('alert');
    expect(alert).toBeVisible();
    expect(alert).toHaveTextContent('An error occurred loading data. Some error');
    expect(axiosMock.history.get.length).toBe(1);
    expect(axiosMock.history.get[0].url).toBe(getApplicationUrl('appPublicId'));

    axiosMock.onGet(getApplicationUrl('appPublicId')).reply(200, {
      id: 'appId',
      publicId: 'appPublicId',
      name: 'appName',
    });

    const retryButton = within(alert).getByRole('button', { name: 'Retry' });
    expect(retryButton).toBeVisible();
    await user.click(retryButton);
    expect(await screen.findByRole('heading', { name: 'appName Latest Evaluations' })).toBeVisible();
  });

  it('renders a page error for failing to load the application report history', async () => {
    const user = userEvent.setup();
    axiosMock.onGet(getApplicationReportHistoryUrl('appId', 'build')).reply(500, 'Some error');

    renderComponent(state);

    const alert = await screen.findByRole('alert');
    expect(alert).toBeVisible();
    expect(alert).toHaveTextContent('An error occurred loading data. Some error');
    expect(axiosMock.history.get.length).toBe(2);
    expect(axiosMock.history.get[0].url).toBe(getApplicationUrl('appPublicId'));
    expect(axiosMock.history.get[1].url).toBe(getApplicationReportHistoryUrl('appId', 'build'));

    axiosMock.onGet(getApplicationReportHistoryUrl('appId', 'build')).reply(200, {
      applicationId: 'appId',
      reports: [
        {
          evaluationDate: '2025-01-21T10:32:51.641Z',
          scanTriggerTypeDisplayName: 'Web UI',
          scanId: 'someScanId',
          policyEvaluationResult: {
            criticalPolicyViolationCount: 1,
            severePolicyViolationCount: 2,
            moderatePolicyViolationCount: 3,
            totalComponentCount: 10,
          },
        },
      ],
    });

    const retryButton = within(alert).getByRole('button', { name: 'Retry' });
    expect(retryButton).toBeVisible();
    await user.click(retryButton);
    expect(await screen.findByRole('heading', { name: 'appName Latest Evaluations' })).toBeVisible();
  });

  it('renders back button to reports when no previous route', async () => {
    renderComponent(state);

    const backBtn = await screen.findByRole('link', { name: /^All Reports$/ });
    expect(backBtn).toBeVisible();
    expect(mockRouterState.href).toHaveBeenCalledWith('violations');
  });

  it('renders back button to application report when coming from report', async () => {
    const stateWithPrevRoute = {
      ...state,
      router: {
        ...state.router,
        prevState: {
          name: 'applicationReport.policy',
        },
        prevParams: {
          publicId: 'testId',
          scanId: 'testScan',
        },
      },
    };

    renderComponent(stateWithPrevRoute);

    const backBtn = await screen.findByRole('link', { name: /^Back to Application Report$/ });
    expect(backBtn).toBeVisible();
    expect(mockRouterState.href).toHaveBeenCalledWith('applicationReport.policy', {
      publicId: 'testId',
      scanId: 'testScan',
    });
  });
});
