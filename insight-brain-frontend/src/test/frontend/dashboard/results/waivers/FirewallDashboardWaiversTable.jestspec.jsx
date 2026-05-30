/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, waitFor } from 'TestRoot/SpecUtil';
import userEvent from '@testing-library/user-event';
import { fireEvent } from '@testing-library/react';
import FirewallDashboardWaiversTable from 'MainRoot/dashboard/results/waivers/FirewallDashboardWaiversTable';
import * as dashboardResultsActions from 'MainRoot/dashboard/results/dashboardResultsActions';
import { WAIVERS_RESULTS_TYPE } from 'MainRoot/dashboard/results/dashboardResultsTypes';

import 'TestRoot/SpecUtil';

describe('FirewallDashboardWaiversTable', () => {
  const stateGoSpy = jest.fn().mockReturnValue({ type: 'STATE_GO' });
  const sortWaiversSpy = jest.fn();
  const reloadSpy = jest.fn();

  const baseWaiver = {
    id: 'waiver-1',
    threatLevel: 8,
    createTime: '2025-01-01T00:00:00Z',
    expiryTime: '2026-12-31T23:59:59Z',
    policyName: 'Security-High',
    ownerId: 'owner-1',
    ownerType: 'organization',
    scope: 'Organization - Org1',
    componentMatchStrategy: 'EXACT_COMPONENT',
    componentUpgradeAvailable: false,
    isAutoWaiver: false,
    componentIdentifier: null,
  };

  const defaultProps = {
    waivers: {
      results: [baseWaiver],
      hasNextPage: false,
      sortFields: ['-threatLevel'],
      error: null,
      hasMultiplePages: false,
      page: 0,
    },
    sortWaivers: sortWaiversSpy,
    dispatchNexPage: jest.fn(),
    dispatchPreviousPage: jest.fn(),
    stateGo: stateGoSpy,
    maxDaysOld: 30,
    needsAcknowledgement: false,
    reload: reloadSpy,
  };

  const renderComponent = (props = {}) =>
    render(<FirewallDashboardWaiversTable {...defaultProps} {...props} />);

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders all column headers including Components', () => {
    renderComponent();

    expect(screen.getByText('Threat')).toBeInTheDocument();
    expect(screen.getByText('Date Created')).toBeInTheDocument();
    expect(screen.getByText('Expiration')).toBeInTheDocument();
    expect(screen.getByText('Policy')).toBeInTheDocument();
    expect(screen.getByText('Scope')).toBeInTheDocument();
    expect(screen.getByText('Components')).toBeInTheDocument();
    expect(screen.getByText('Upgrade')).toBeInTheDocument();
  });

  it('renders Actions column header', () => {
    renderComponent();

    expect(screen.getByText('Actions')).toBeInTheDocument();
  });

  it('renders waiver data rows', () => {
    renderComponent();

    expect(screen.getByText('8')).toBeInTheDocument();
    expect(screen.getByText('Security-High')).toBeInTheDocument();
    expect(screen.getByText('Organization - Org1')).toBeInTheDocument();
  });

  it('shows empty message when no results', () => {
    renderComponent({
      waivers: {
        ...defaultProps.waivers,
        results: [],
      },
    });

    expect(
      screen.getByText('No data available for the applied filters and permissions.')
    ).toBeInTheDocument();
  });

  it('shows loading spinner when results are null', () => {
    renderComponent({
      waivers: {
        ...defaultProps.waivers,
        results: null,
        error: null,
      },
    });

    expect(screen.getByText('Loading…')).toBeInTheDocument();
  });

  it('Components column header is sortable and calls sortWaivers with component', async () => {
    const user = userEvent.setup();
    renderComponent({
      waivers: {
        ...defaultProps.waivers,
        results: [baseWaiver, { ...baseWaiver, id: 'waiver-2' }],
      },
    });

    await user.click(screen.getByText('Components'));

    expect(sortWaiversSpy).toHaveBeenCalledWith(['component']);
  });

  it('Threat column header is sortable and calls sortWaivers', async () => {
    const user = userEvent.setup();
    renderComponent({
      waivers: {
        ...defaultProps.waivers,
        results: [baseWaiver],
        sortFields: ['threatLevel'],
      },
    });

    await user.click(screen.getByText('Threat'));

    expect(sortWaiversSpy).toHaveBeenCalledWith(['-threatLevel']);
  });

  it('shows error message when error is present', () => {
    renderComponent({
      waivers: {
        ...defaultProps.waivers,
        results: null,
        error: 'Network failure',
      },
    });

    expect(screen.getByText(/Network failure/)).toBeInTheDocument();
  });

  it('shows acknowledgement info row when needsAcknowledgement is true', () => {
    renderComponent({ needsAcknowledgement: true });

    expect(
      screen.getByText(`Select your filter criteria and click 'apply' to see results.`)
    ).toBeInTheDocument();
  });

  it('renders expiration filter toggle button', () => {
    const { container } = renderComponent();

    expect(container.querySelector('.nx-dropdown__toggle')).toBeInTheDocument();
  });

  it('shows all expiration options when dropdown is open', async () => {
    const user = userEvent.setup();
    const { container } = renderComponent({ isExpiringWaiversEnabled: true });

    await user.click(container.querySelector('.nx-dropdown__toggle'));

    // Menu is portaled to document.body, so query from document
    const options = Array.from(document.querySelectorAll('.nx-dropdown-button')).map((b) => b.textContent);
    expect(options).toContain('All');
    expect(options).toContain('Auto');
    expect(options).toContain('In 7 Days');
    expect(options).toContain('In 30 Days');
    expect(options).toContain('Never');
  });

  it('updates appliedFilter.expirationDate in store and resets page when user selects an option', async () => {
    const user = userEvent.setup();
    const setPageSpy = jest.spyOn(dashboardResultsActions, 'setPage').mockReturnValue(() => {});

    const { container } = renderComponent({ isExpiringWaiversEnabled: true });

    await user.click(container.querySelector('.nx-dropdown__toggle'));
    // Menu is portaled to document.body; component uses onMouseDown for selection
    const option = Array.from(document.querySelectorAll('.nx-dropdown-button')).find(
      (b) => b.textContent === 'In 7 Days'
    );
    fireEvent.mouseDown(option);

    expect(setPageSpy).toHaveBeenCalledWith(WAIVERS_RESULTS_TYPE, 0);

    setPageSpy.mockRestore();
  });

  it('renders component filter input', () => {
    renderComponent();

    expect(screen.getByPlaceholderText('component name')).toBeInTheDocument();
  });

  it('renders repository filter input', () => {
    renderComponent();

    expect(screen.getByPlaceholderText('repository')).toBeInTheDocument();
  });

  it('updates component name filter and resets page when typing in component filter input', async () => {
    jest.useFakeTimers();
    const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
    const setPageSpy = jest.spyOn(dashboardResultsActions, 'setPage').mockReturnValue(() => {});

    renderComponent({ isExpiringWaiversEnabled: true });

    const componentInput = screen.getByPlaceholderText('component name');
    await user.type(componentInput, 'commons-io');

    // Advance timers to trigger the debounced function
    jest.advanceTimersByTime(600);

    expect(setPageSpy).toHaveBeenCalledWith(WAIVERS_RESULTS_TYPE, 0);

    setPageSpy.mockRestore();
    jest.useRealTimers();
  });

  it('updates repository filter and resets page when typing in repository filter input', async () => {
    jest.useFakeTimers();
    const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
    const setPageSpy = jest.spyOn(dashboardResultsActions, 'setPage').mockReturnValue(() => {});

    renderComponent({ isExpiringWaiversEnabled: true });

    const repoInput = screen.getByPlaceholderText('repository');
    await user.type(repoInput, 'my-repo');

    // Advance timers to trigger the debounced function
    jest.advanceTimersByTime(600);

    expect(setPageSpy).toHaveBeenCalledWith(WAIVERS_RESULTS_TYPE, 0);

    setPageSpy.mockRestore();
    jest.useRealTimers();
  });
});
