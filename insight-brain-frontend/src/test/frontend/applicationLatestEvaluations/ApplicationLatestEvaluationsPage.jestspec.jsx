/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { axiosMockAdapter, render, within } from 'TestRoot/SpecUtil';
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
          scanTriggerInternal: true,
          scannerVersion: 'someScannerVersion',
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

    renderComponent = (preloadedState) => render(<ApplicationLatestEvaluationsPage />, { preloadedState });
  });

  it('renders the page', async () => {
    const user = userEvent.setup();
    renderComponent(state);

    expect(await screen.findByRole('heading', { name: 'appName Latest Evaluations' })).toBeVisible();
    expect(screen.getByText('Stage:')).toBeVisible();
    expect(screen.getByText('Build')).toBeVisible();

    const table = screen.getByRole('table');
    const rows = within(table).getAllByRole('rowgroup');
    expect(rows.length).toBe(2);

    const headers = within(rows[0]).getAllByRole('columnheader');
    expect(headers.length).toBe(6);
    expect(headers[0]).toHaveTextContent('Evaluation Date');
    expect(headers[1]).toHaveTextContent('Trigger');
    expect(headers[2]).toHaveTextContent('Version');
    expect(headers[3]).toHaveTextContent('Violations');
    expect(headers[4]).toHaveTextContent('Components');
    expect(headers[5]).toHaveTextContent('');

    const versionText = screen.getByText('Version');
    await user.hover(versionText);
    const tooltip = await screen.findByRole('tooltip');
    expect(tooltip).toHaveTextContent('The integration version that triggered the evaluation.');

    const cells = within(rows[1]).getAllByRole('cell');
    expect(cells.length).toBe(6);
    expect(cells[0]).toHaveTextContent('2025-01-21 05:32:51');
    expect(cells[1]).toHaveTextContent(/^Web UI$/);
    expect(cells[2]).toHaveTextContent(/^someScannerVersion$/);
    expect(within(cells[3]).getByText('Critical').closest('.nx-small-threat-counter')).toHaveTextContent('1');
    expect(within(cells[3]).getByText('Severe').closest('.nx-small-threat-counter')).toHaveTextContent('2');
    expect(within(cells[3]).getByText('Moderate').closest('.nx-small-threat-counter')).toHaveTextContent('3');
    expect(cells[4]).toHaveTextContent('10');
    expect(cells[5]).toHaveTextContent('View Report');
    expect(mockRouterState.href).toHaveBeenCalledWith('applicationReport.policy', {
      publicId: 'appPublicId',
      scanId: 'someScanId',
    });
    expect(within(cells[5]).getByRole('link', { name: 'View Report' })).toHaveAttribute('href', 'someHref');

    expect(axiosMock.history.get.length).toBe(2);
    expect(axiosMock.history.get[0].url).toBe(getApplicationUrl('appPublicId'));
    expect(axiosMock.history.get[1].url).toBe(getApplicationReportHistoryUrl('appId', 'build'));
  });

  it('renders an em dash when the scanner version is not available', async () => {
    axiosMock.onGet(getApplicationReportHistoryUrl('appId', 'build')).reply(200, {
      applicationId: 'appId',
      reports: [
        {
          evaluationDate: '2025-01-21T10:32:51.641Z',
          scanTriggerTypeDisplayName: 'Web UI',
          scanTriggerInternal: true,
          scannerVersion: null,
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
    renderComponent(state);

    expect(await screen.findByRole('heading', { name: 'appName Latest Evaluations' })).toBeVisible();
    const table = screen.getByRole('table');
    const rows = within(table).getAllByRole('rowgroup');
    const cells = within(rows[1]).getAllByRole('cell');
    expect(cells[2]).toHaveTextContent(/^—$/);
  });

  it('renders a scanner version without a qualifier when not internal', async () => {
    axiosMock.onGet(getApplicationReportHistoryUrl('appId', 'build')).reply(200, {
      applicationId: 'appId',
      reports: [
        {
          evaluationDate: '2025-01-21T10:32:51.641Z',
          scanTriggerTypeDisplayName: 'CLI',
          scanTriggerInternal: false,
          scannerVersion: '1.2.3-01',
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
    renderComponent(state);

    expect(await screen.findByRole('heading', { name: 'appName Latest Evaluations' })).toBeVisible();
    const table = screen.getByRole('table');
    const rows = within(table).getAllByRole('rowgroup');
    const cells = within(rows[1]).getAllByRole('cell');
    expect(cells[2]).toHaveTextContent(/^1.2.3$/);
  });

  it('renders a scanner version as the release version when internal', async () => {
    axiosMock.onGet(getApplicationReportHistoryUrl('appId', 'build')).reply(200, {
      applicationId: 'appId',
      reports: [
        {
          evaluationDate: '2025-01-21T10:32:51.641Z',
          scanTriggerTypeDisplayName: 'Web Ui',
          scanTriggerInternal: true,
          scannerVersion: '1.189.0-01',
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
    renderComponent(state);

    expect(await screen.findByRole('heading', { name: 'appName Latest Evaluations' })).toBeVisible();
    const table = screen.getByRole('table');
    const rows = within(table).getAllByRole('rowgroup');
    const cells = within(rows[1]).getAllByRole('cell');
    expect(cells[2]).toHaveTextContent(/^189$/);
  });

  it('renders continuous monitoring', async () => {
    axiosMock.onGet(getApplicationReportHistoryUrl('appId', 'build')).reply(200, {
      applicationId: 'appId',
      reports: [
        {
          evaluationDate: '2025-01-21T10:32:51.641Z',
          scanTriggerTypeDisplayName: 'Web UI',
          scanTriggerInternal: true,
          scannerVersion: 'someScannerVersion',
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
    expect(cells.length).toBe(6);
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
          scanTriggerInternal: true,
          scannerVersion: 'someScannerVersion',
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

  // The legacy origin='hostedRepoComponents' → "Back to Repository Component Report" flow
  // was removed alongside the CLM-44275 entry-point rewire (goToHrcReport routes users to the
  // native HRC report state, no synthetic-app detour). componentDisplayName is now forwarded
  // from the HRC route params — HRC coverage lives in this file's isHrcMode block above.

  it('historical View Report links do NOT carry hosted-repo params on the application-report path (regression guard)', async () => {
    renderComponent(state);

    await screen.findByRole('heading', { name: 'appName Latest Evaluations' });
    expect(mockRouterState.href).toHaveBeenCalledWith('applicationReport.policy', {
      publicId: 'appPublicId',
      scanId: 'someScanId',
    });
  });
});
