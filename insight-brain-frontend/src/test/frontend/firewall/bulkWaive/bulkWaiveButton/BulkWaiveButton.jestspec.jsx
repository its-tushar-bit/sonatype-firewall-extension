/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import userEvent from '@testing-library/user-event';
import { render, screen, waitFor } from 'TestRoot/SpecUtil';
import BulkWaiveButton from 'MainRoot/firewall/bulkWaive/bulkWaiveButton/BulkWaiveButton';
import * as RouterActions from 'MainRoot/reduxUiRouter/routerActions';
import * as authorizationUtil from 'MainRoot/util/authorizationUtil';

describe('BulkWaiveButton', () => {
  let stateGoSpy;
  let checkPermissionsSpy;

  beforeEach(() => {
    stateGoSpy = jest.spyOn(RouterActions, 'stateGo');
    checkPermissionsSpy = jest.spyOn(authorizationUtil, 'checkPermissions').mockResolvedValue();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should render the button with correct text', () => {
    renderComponent();

    expect(screen.getByRole('button', { name: 'Bulk Waive' })).toBeVisible();
  });

  it('should be enabled when disabled prop is false', () => {
    renderComponent({ disabled: false });

    expect(screen.getByRole('button', { name: 'Bulk Waive' })).not.toBeDisabled();
  });

  it('should be disabled when disabled prop is true', () => {
    renderComponent({ disabled: true });

    expect(screen.getByRole('button', { name: 'Bulk Waive' })).toBeDisabled();
  });

  it('should apply custom className', () => {
    renderComponent({ className: 'custom-class' });

    const button = screen.getByRole('button', { name: 'Bulk Waive' });
    expect(button).toHaveClass('custom-class');
  });

  it('should navigate to bulk waive page when clicked', async () => {
    const user = userEvent.setup();
    renderComponent({ disabled: false });

    const button = screen.getByRole('button', { name: 'Bulk Waive' });
    await user.click(button);

    expect(stateGoSpy).toHaveBeenCalledWith('firewall.bulkWaive', {
      repositoryId: 'test-repo-id',
    });
  });

  it('should store repository-report source context when clicked from repository report', async () => {
    const user = userEvent.setup();
    const { store } = renderComponent({
      disabled: false,
      source: 'repository-report',
    });

    const button = screen.getByRole('button', { name: 'Bulk Waive' });
    await user.click(button);

    const state = store.getState();
    expect(state.firewallBulkWaiver.sourceContext).toMatchObject({
      source: 'repository-report',
      repositoryId: 'test-repo-id',
    });
  });

  it('should store component-details source context when clicked from component details', async () => {
    const user = userEvent.setup();
    const { store } = renderComponent({
      disabled: false,
      source: 'component-details',
      componentIdentifier: 'test-component',
      componentHash: 'test-hash',
      matchState: 'exact',
      tabId: 'violations',
      pathname: '/test/path',
      componentDisplayName: 'Test Component',
    });

    const button = screen.getByRole('button', { name: 'Bulk Waive' });
    await user.click(button);

    const state = store.getState();
    expect(state.firewallBulkWaiver.sourceContext).toMatchObject({
      source: 'component-details',
      repositoryId: 'test-repo-id',
      componentIdentifier: 'test-component',
      componentHash: 'test-hash',
      matchState: 'exact',
      tabId: 'violations',
      pathname: '/test/path',
      componentDisplayName: 'Test Component',
    });
  });

  it('should be disabled when all provided violations are already waived', () => {
    renderComponent({
      disabled: false,
      violations: [
        { waived: true },
        { waived: true },
      ],
    });

    expect(screen.getByRole('button', { name: 'Bulk Waive' })).toBeDisabled();
  });

  it('should be enabled when at least one violation is not waived', () => {
    renderComponent({
      disabled: false,
      violations: [
        { waived: true },
        { waived: false },
      ],
    });

    expect(screen.getByRole('button', { name: 'Bulk Waive' })).not.toBeDisabled();
  });

  it('should not navigate when button is disabled', async () => {
    const user = userEvent.setup();
    renderComponent({ disabled: true });

    const button = screen.getByRole('button', { name: 'Bulk Waive' });
    await user.click(button);

    // Button is disabled, so click should not trigger navigation
    expect(stateGoSpy).not.toHaveBeenCalled();
  });

  it('should clear filters when navigating to bulk waive page', async () => {
    const user = userEvent.setup();
    const { store } = renderComponent({
      disabled: false,
    });

    const button = screen.getByRole('button', { name: 'Bulk Waive' });
    await user.click(button);

    const state = store.getState();
    // Verify filters are cleared
    expect(state.repositoryResultsSummaryPage.searchFiltersValues).toEqual({
      POLICY_NAME: '',
      EVALUATION_TIME: '',
      QUARANTINE_TIME: '',
      COMPONENT_COORDINATES: '',
    });
    expect(state.repositoryResultsSummaryPage.componentsRequestBody.searchFilters).toEqual([]);
  });

  it('should not render button when user lacks WAIVE_POLICY_VIOLATIONS permission', async () => {
    checkPermissionsSpy.mockRejectedValue(new Error('Forbidden'));

    renderComponent({ disabled: false });

    await waitFor(() => {
      expect(screen.queryByRole('button', { name: 'Bulk Waive' })).not.toBeInTheDocument();
    });
  });

  it('should check permissions for the repository', async () => {
    renderComponent({ repositoryId: 'my-repo-123' });

    await waitFor(() => {
      expect(checkPermissionsSpy).toHaveBeenCalledWith(['WAIVE_POLICY_VIOLATIONS'], 'repository', 'my-repo-123');
    });
  });

  function renderComponent(props = {}) {
    const defaultProps = {
      repositoryId: 'test-repo-id',
      disabled: false,
      ...props,
    };

    const preloadedState = {
      firewallBulkWaiver: {
        sourceContext: null,
      },
      repositoryResultsSummaryPage: {
        searchFiltersValues: {
          POLICY_NAME: '',
          QUARANTINE_TIME: '',
          COMPONENT_COORDINATES: '',
        },
        componentsRequestBody: {
          searchFilters: [],
          matchStateFilters: [],
          violationStateFilters: [],
          threatLevelFilters: [0, 10],
        },
      },
    };

    return render(<BulkWaiveButton {...defaultProps} />, { preloadedState });
  }
});
