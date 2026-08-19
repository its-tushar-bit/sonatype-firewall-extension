/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter, render, screen, waitFor } from 'TestRoot/SpecUtil';
import BulkWaiveButton from 'MainRoot/waivers/BulkWaiveButton';
import { getApplicationSummaryUrl, getPermissionContextTestUrl } from 'MainRoot/util/CLMLocation';
import * as RouterActions from 'MainRoot/reduxUiRouter/routerActions';

describe('BulkWaiveButton', () => {
  let axiosMock, stateGoSpy;

  const defaultPreloadedState = {
    productFeatures: {
      productFeatures: { 'bulk-waivers': true },
    },
    waivers: {
      bulkWaive: {
        checkboxState: {},
        selectAllChecked: false,
        selectedViolations: [],
      },
      permissions: {
        loading: {},
        error: {},
        byApplicationId: {},
      },
    },
    router: {
      currentState: { name: 'applicationReport' },
      currentParams: { publicId: 'test-app-id' },
    },
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    stateGoSpy = jest.spyOn(RouterActions, 'stateGo');

    // Default mock: user has permission
    axiosMock.onGet(getApplicationSummaryUrl('test-app-id')).reply(200, { id: 'internal-id-123' });
    axiosMock
      .onPut(getPermissionContextTestUrl('application', 'internal-id-123'))
      .reply(200, ['WAIVE_POLICY_VIOLATIONS']);
  });

  const renderComponent = (props = {}, preloadedState) => {
    const defaultProps = {
      disabled: false,
      publicId: 'test-app-id',
      className: '',
      skipPermissionCheck: false,
    };

    return render(<BulkWaiveButton {...defaultProps} {...props} />, {
      preloadedState: preloadedState || defaultPreloadedState,
    });
  };

  describe('Permission loading and caching', () => {
    it('shows loading spinner while fetching permissions', () => {
      renderComponent();

      expect(screen.getByRole('img', { hidden: true })).toBeInTheDocument();
    });

    it('renders button when user has permission', async () => {
      renderComponent();

      expect(await screen.findByRole('button', { name: 'Bulk Waive' })).toBeInTheDocument();

      expect(axiosMock.history.get.length).toBe(1);
      expect(axiosMock.history.put.length).toBe(1);
    });

    it('caches permission and does not make duplicate API calls', async () => {
      const stateWithCache = {
        ...defaultPreloadedState,
        waivers: {
          ...defaultPreloadedState.waivers,
          permissions: {
            loading: {},
            error: {},
            byApplicationId: { 'test-app-id': true },
          },
        },
      };

      renderComponent({}, stateWithCache);

      expect(await screen.findByRole('button', { name: 'Bulk Waive' })).toBeInTheDocument();

      // No API calls should be made when cached
      expect(axiosMock.history.get.length).toBe(0);
      expect(axiosMock.history.put.length).toBe(0);
    });

    it('tracks loading state per publicId', async () => {
      const stateWithOtherAppLoading = {
        ...defaultPreloadedState,
        waivers: {
          ...defaultPreloadedState.waivers,
          permissions: {
            loading: { 'other-app-id': true },
            error: {},
            byApplicationId: {},
          },
        },
      };

      renderComponent({}, stateWithOtherAppLoading);

      // Should show loading spinner for test-app-id
      expect(screen.getByRole('img', { hidden: true })).toBeInTheDocument();

      expect(await screen.findByRole('button', { name: 'Bulk Waive' })).toBeInTheDocument();
    });

    it('handles multiple apps with different permissions independently', async () => {
      const stateWithMultipleApps = {
        ...defaultPreloadedState,
        waivers: {
          ...defaultPreloadedState.waivers,
          permissions: {
            loading: {},
            error: {},
            byApplicationId: {
              'app-1': true,
              'app-2': false,
            },
          },
        },
      };

      // Render for app-1 (has permission)
      const { unmount } = renderComponent({ publicId: 'app-1' }, stateWithMultipleApps);
      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Bulk Waive' })).toBeInTheDocument();
      });
      unmount();

      // Render for app-2 (no permission)
      renderComponent({ publicId: 'app-2' }, stateWithMultipleApps);
      expect(screen.queryByRole('button', { name: 'Bulk Waive' })).not.toBeInTheDocument();
    });
  });

  describe('Permission denied scenarios', () => {
    it('returns null when user does not have permission', async () => {
      axiosMock.onPut(getPermissionContextTestUrl('application', 'internal-id-123')).reply(200, []);

      renderComponent();

      await waitFor(() => {
        expect(axiosMock.history.put.length).toBe(1);
      });

      expect(screen.queryByRole('button', { name: 'Bulk Waive' })).not.toBeInTheDocument();
    });

    it('uses cached permission denial', () => {
      const stateWithNoPermission = {
        ...defaultPreloadedState,
        waivers: {
          ...defaultPreloadedState.waivers,
          permissions: {
            loading: {},
            error: {},
            byApplicationId: { 'test-app-id': false },
          },
        },
      };

      renderComponent({}, stateWithNoPermission);

      expect(screen.queryByRole('button', { name: 'Bulk Waive' })).not.toBeInTheDocument();
      expect(axiosMock.history.get.length).toBe(0);
    });
  });

  describe('Error handling', () => {
    it('logs error and returns null when permission check fails', async () => {
      const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();
      axiosMock.onGet(getApplicationSummaryUrl('test-app-id')).reply(500, { message: 'Server Error' });

      renderComponent();

      await waitFor(() => {
        expect(axiosMock.history.get.length).toBe(1);
      });

      expect(consoleErrorSpy).toHaveBeenCalledWith('Failed to load waiver permissions.');
      expect(screen.queryByRole('button', { name: 'Bulk Waive' })).not.toBeInTheDocument();

      consoleErrorSpy.mockRestore();
    });

    it('caches error state and does not retry', async () => {
      const stateWithError = {
        ...defaultPreloadedState,
        waivers: {
          ...defaultPreloadedState.waivers,
          permissions: {
            loading: {},
            error: { 'test-app-id': 'Failed to load' },
            byApplicationId: { 'test-app-id': false },
          },
        },
      };

      const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();

      renderComponent({}, stateWithError);

      expect(consoleErrorSpy).toHaveBeenCalledWith('Failed to load waiver permissions.');
      expect(screen.queryByRole('button', { name: 'Bulk Waive' })).not.toBeInTheDocument();
      expect(axiosMock.history.get.length).toBe(0);

      consoleErrorSpy.mockRestore();
    });

    it('tracks errors per publicId independently', async () => {
      const stateWithOtherAppError = {
        ...defaultPreloadedState,
        waivers: {
          ...defaultPreloadedState.waivers,
          permissions: {
            loading: {},
            error: { 'other-app-id': 'Some error' },
            byApplicationId: { 'other-app-id': false },
          },
        },
      };

      renderComponent({}, stateWithOtherAppError);

      // Should load normally for test-app-id despite other app having error
      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Bulk Waive' })).toBeInTheDocument();
      });

      expect(axiosMock.history.get.length).toBe(1);
    });
  });

  describe('Button behavior', () => {
    it('renders enabled button when disabled prop is false', async () => {
      renderComponent({ disabled: false });

      const button = await screen.findByRole('button', { name: 'Bulk Waive' });
      expect(button).not.toBeDisabled();
    });

    it('renders disabled button when disabled prop is true', async () => {
      renderComponent({ disabled: true });

      const button = await screen.findByRole('button', { name: 'Bulk Waive' });
      expect(button).toBeDisabled();
    });

    it('navigates to bulk waive page when clicked', async () => {
      const user = userEvent.setup();
      renderComponent({ disabled: false });

      const button = await screen.findByRole('button', { name: 'Bulk Waive' });
      await user.click(button);

      expect(stateGoSpy).toHaveBeenCalledWith('applicationReport.bulkWaive', expect.any(Object));
    });

    it('clears checkbox state when navigating to bulk waive page', async () => {
      const user = userEvent.setup();
      const stateWithCheckboxes = {
        ...defaultPreloadedState,
        waivers: {
          ...defaultPreloadedState.waivers,
          bulkWaive: {
            checkboxState: { 'violation-1': true, 'violation-2': true },
            selectAllChecked: true,
            selectedViolations: [],
          },
          permissions: {
            loading: {},
            error: {},
            byApplicationId: { 'test-app-id': true },
          },
        },
      };

      const { store } = renderComponent({ disabled: false }, stateWithCheckboxes);

      const button = await screen.findByRole('button', { name: 'Bulk Waive' });
      await user.click(button);

      await waitFor(() => {
        const state = store.getState();
        expect(state.waivers.bulkWaive.checkboxState).toEqual({});
        expect(state.waivers.bulkWaive.selectAllChecked).toBe(false);
      });
    });

    it('applies custom className prop', async () => {
      renderComponent({ className: 'custom-class' });

      const button = await screen.findByRole('button', { name: 'Bulk Waive' });
      expect(button).toHaveClass('custom-class');
    });
  });

  describe('skipPermissionCheck prop', () => {
    it('does not load permissions when skipPermissionCheck is true', () => {
      const stateWithPermission = {
        ...defaultPreloadedState,
        waivers: {
          ...defaultPreloadedState.waivers,
          permissions: {
            loading: {},
            error: {},
            byApplicationId: { 'test-app-id': true },
          },
        },
      };

      renderComponent({ skipPermissionCheck: true }, stateWithPermission);

      // Should render immediately without loading
      expect(screen.getByRole('button', { name: 'Bulk Waive' })).toBeInTheDocument();
      expect(axiosMock.history.get.length).toBe(0);
    });

    it('returns null when skipPermissionCheck is true but no cached permission', () => {
      renderComponent({ skipPermissionCheck: true });

      expect(screen.queryByRole('button', { name: 'Bulk Waive' })).not.toBeInTheDocument();
      expect(axiosMock.history.get.length).toBe(0);
    });
  });

  describe('publicId handling', () => {
    it('does not load permissions when publicId is not provided', () => {
      renderComponent({ publicId: null });

      expect(screen.queryByRole('button', { name: 'Bulk Waive' })).not.toBeInTheDocument();
      expect(axiosMock.history.get.length).toBe(0);
    });

    it('handles different publicIds correctly', async () => {
      axiosMock.onGet(getApplicationSummaryUrl('app-123')).reply(200, { id: 'internal-123' });
      axiosMock
        .onPut(getPermissionContextTestUrl('application', 'internal-123'))
        .reply(200, ['WAIVE_POLICY_VIOLATIONS']);

      renderComponent({ publicId: 'app-123' });

      expect(await screen.findByRole('button', { name: 'Bulk Waive' })).toBeInTheDocument();

      expect(axiosMock.history.get[0].url).toBe(getApplicationSummaryUrl('app-123'));
    });
  });

  describe('Pro Tier Gating', () => {
    it('shows EnterpriseLockButton with Preview Bulk Waive when bulk-waivers feature is absent', async () => {
      const proState = {
        ...defaultPreloadedState,
        productFeatures: { productFeatures: {} },
        productLicense: { license: { products: ['Sonatype Lifecycle Pro'] } },
        waivers: {
          ...defaultPreloadedState.waivers,
          permissions: {
            loading: {},
            error: {},
            byApplicationId: { 'test-app-id': true },
          },
        },
      };

      renderComponent({ skipPermissionCheck: true }, proState);
      expect(await screen.findByRole('button', { name: 'Preview Bulk Waive' })).toBeVisible();
    });
  });
});
