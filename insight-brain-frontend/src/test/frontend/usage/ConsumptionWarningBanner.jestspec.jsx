/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter, render, screen, waitFor } from 'TestRoot/SpecUtil';
import ConsumptionWarningBanner, { resetBannerDismissed } from 'MainRoot/usage/ConsumptionWarningBanner';
import * as RouterStateContext from 'MainRoot/react/RouterStateContext';

describe('ConsumptionWarningBanner', () => {
  let axiosMock, mockRouterState;

  const defaultPreloadedState = {
    router: {
      currentState: { name: 'dashboard.overview.violations' },
      currentParams: {},
    },
  };

  const warningData = {
    consumed: 40078,
    limit: 50000,
    warningThresholdPct: 80,
    percentUsed: 80.2,
    remaining: 9922,
    resetDate: '2026-06-01',
    tier: 'PRO',
  };

  const limitReachedData = {
    consumed: 50000,
    limit: 50000,
    warningThresholdPct: 80,
    percentUsed: 100,
    remaining: 0,
    resetDate: '2026-06-01',
    tier: 'PRO',
  };

  const limitExceededData = {
    consumed: 1234567,
    limit: 1000000,
    warningThresholdPct: 80,
    percentUsed: 123.46,
    remaining: -234567,
    resetDate: '2026-05-01',
    tier: 'PRO',
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    resetBannerDismissed();
    mockRouterState = {
      href: jest.fn().mockReturnValue('#/usage'),
      get: jest.fn(),
      includes: jest.fn(),
      go: jest.fn(),
    };
    jest.spyOn(RouterStateContext, 'useRouterState').mockReturnValue(mockRouterState);
    axiosMock.reset();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  const renderComponent = (preloadedState) => {
    return render(<ConsumptionWarningBanner />, { preloadedState: preloadedState || defaultPreloadedState });
  };

  function getAlertContent() {
    const el = document.querySelector('.nx-alert__content');
    return el?.textContent ?? '';
  }

  describe('rendering behavior', () => {
    it('should not render when consumption is below threshold (79%)', async () => {
      axiosMock.onGet('/api/v2/consumption/summary').reply(200, {
        consumed: 790000,
        limit: 1000000,
        warningThresholdPct: 80,
        percentUsed: 79,
        remaining: 210000,
        resetDate: '2026-05-01',
        tier: 'PRO',
      });

      renderComponent();

      await waitFor(() => {
        expect(axiosMock.history.get.length).toBe(1);
      });

      expect(document.querySelector('.nx-alert')).not.toBeInTheDocument();
    });

    it('should render warning banner when consumption is at threshold (80%)', async () => {
      axiosMock.onGet('/api/v2/consumption/summary').reply(200, warningData);

      renderComponent();

      await waitFor(() => {
        expect(document.querySelector('.nx-alert--warning')).toBeInTheDocument();
      });

      const content = getAlertContent();
      expect(content).toContain('Usage Warning.');
      expect(content).toContain('80%');
      expect(content).toContain('40,078 of 50,000 components consumed');
      expect(content).toContain('Resets June 1, 2026');
    });

    it('should render NxWarningAlert for warning state (80-99%)', async () => {
      axiosMock.onGet('/api/v2/consumption/summary').reply(200, warningData);

      renderComponent();

      await waitFor(() => {
        const alertEl = document.querySelector('.nx-alert');
        expect(alertEl).toBeInTheDocument();
        expect(alertEl).toHaveClass('nx-alert--warning');
      });
    });

    it('should render NxErrorAlert when consumption is at 100%', async () => {
      axiosMock.onGet('/api/v2/consumption/summary').reply(200, limitReachedData);

      renderComponent();

      await waitFor(() => {
        const alertEl = document.querySelector('.nx-alert');
        expect(alertEl).toBeInTheDocument();
        expect(alertEl).toHaveClass('nx-alert--error');
      });

      const content = getAlertContent();
      expect(content).toContain('Usage Limit Reached.');
      expect(content).toContain('reached your monthly limit');
      expect(content).toContain('50,000 of 50,000 components consumed');
    });

    it('should render alert with overage message when consumption exceeds 100%', async () => {
      axiosMock.onGet('/api/v2/consumption/summary').reply(200, limitExceededData);

      renderComponent();

      await waitFor(() => {
        expect(document.querySelector('.nx-alert--error')).toBeInTheDocument();
      });

      const content = getAlertContent();
      expect(content).toContain('Usage Limit Exceeded.');
      expect(content).toContain('exceeded your monthly limit by 234,567 components');
      expect(content).toContain('1,234,567 components consumed of 1,000,000 limit');
    });

    it('should not render when limit is not configured (null)', async () => {
      axiosMock.onGet('/api/v2/consumption/summary').reply(200, {
        consumed: 100000,
        limit: null,
        percentUsed: null,
        remaining: null,
        resetDate: '2026-05-01',
        tier: 'PRO',
      });

      renderComponent();

      await waitFor(() => {
        expect(axiosMock.history.get.length).toBe(1);
      });

      expect(document.querySelector('.nx-alert')).not.toBeInTheDocument();
    });

    it('should not render when limit is 0', async () => {
      axiosMock.onGet('/api/v2/consumption/summary').reply(200, {
        consumed: 100000,
        limit: 0,
        percentUsed: 0,
        remaining: 0,
        resetDate: '2026-05-01',
        tier: 'PRO',
      });

      renderComponent();

      await waitFor(() => {
        expect(axiosMock.history.get.length).toBe(1);
      });

      expect(document.querySelector('.nx-alert')).not.toBeInTheDocument();
    });

    it('should not render when API returns error', async () => {
      axiosMock.onGet('/api/v2/consumption/summary').reply(404);

      renderComponent();

      await waitFor(() => {
        expect(axiosMock.history.get.length).toBe(1);
      });

      expect(document.querySelector('.nx-alert')).not.toBeInTheDocument();
    });

    it('U-45: custom threshold 60% triggers warning banner when percentUsed reaches 60', async () => {
      axiosMock.onGet('/api/v2/consumption/summary').reply(200, {
        consumed: 600000,
        limit: 1000000,
        warningThresholdPct: 60,
        percentUsed: 60,
        remaining: 400000,
        resetDate: '2026-06-01',
        tier: 'PRO',
      });

      renderComponent();

      await waitFor(() => {
        expect(document.querySelector('.nx-alert--warning')).toBeInTheDocument();
      });
      expect(getAlertContent()).toContain('60%');
    });

    it('U-46: custom threshold 90% does not trigger warning banner at 80%', async () => {
      axiosMock.onGet('/api/v2/consumption/summary').reply(200, {
        consumed: 800000,
        limit: 1000000,
        warningThresholdPct: 90,
        percentUsed: 80,
        remaining: 200000,
        resetDate: '2026-06-01',
        tier: 'PRO',
      });

      renderComponent();

      await waitFor(() => {
        expect(axiosMock.history.get.length).toBe(1);
      });
      expect(document.querySelector('.nx-alert')).not.toBeInTheDocument();
    });

    it('null warningThresholdPct: suppresses warning banner but still renders over-limit alert', async () => {
      axiosMock.onGet('/api/v2/consumption/summary').reply(200, {
        consumed: 900000,
        limit: 1000000,
        warningThresholdPct: null,
        percentUsed: 90,
        remaining: 100000,
        resetDate: '2026-06-01',
        tier: 'PRO',
      });

      const { unmount } = renderComponent();

      await waitFor(() => {
        expect(axiosMock.history.get.length).toBe(1);
      });
      expect(document.querySelector('.nx-alert')).not.toBeInTheDocument();

      unmount();
      resetBannerDismissed();
      axiosMock.reset();
      axiosMock.onGet('/api/v2/consumption/summary').reply(200, {
        consumed: 1100000,
        limit: 1000000,
        warningThresholdPct: null,
        percentUsed: 110,
        remaining: -100000,
        resetDate: '2026-06-01',
        tier: 'PRO',
      });

      renderComponent();

      await waitFor(() => {
        expect(document.querySelector('.nx-alert--error')).toBeInTheDocument();
      });
    });

    it('should not render on usage page', async () => {
      axiosMock.onGet('/api/v2/consumption/summary').reply(200, warningData);

      renderComponent({
        router: {
          currentState: { name: 'usage' },
          currentParams: {},
        },
      });

      await waitFor(() => {
        expect(document.querySelector('.nx-alert')).not.toBeInTheDocument();
      });
    });
  });

  describe('dismissible behavior', () => {
    it('should be dismissible via close button', async () => {
      const user = userEvent.setup();

      axiosMock.onGet('/api/v2/consumption/summary').reply(200, warningData);

      renderComponent();

      await waitFor(() => {
        expect(document.querySelector('.nx-alert--warning')).toBeInTheDocument();
      });

      const closeButton = screen.getByRole('button', { name: /close/i });
      await user.click(closeButton);

      await waitFor(() => {
        expect(document.querySelector('.nx-alert')).not.toBeInTheDocument();
      });
    });
  });

  describe('navigation actions', () => {
    it('should navigate to usage dashboard when View Dashboard is clicked', async () => {
      const user = userEvent.setup();

      axiosMock.onGet('/api/v2/consumption/summary').reply(200, warningData);

      renderComponent();

      await waitFor(() => {
        expect(document.querySelector('.nx-alert--warning')).toBeInTheDocument();
      });

      const viewDashboardLink = screen.getByRole('link', { name: /view dashboard/i });
      await user.click(viewDashboardLink);

      expect(mockRouterState.go).toHaveBeenCalledWith('usage');
    });

    it('should have Contact Sales mailto link', async () => {
      axiosMock.onGet('/api/v2/consumption/summary').reply(200, warningData);

      renderComponent();

      await waitFor(() => {
        expect(document.querySelector('.nx-alert--warning')).toBeInTheDocument();
      });

      const contactSalesLink = screen.getByRole('link', { name: /contact sales/i });
      expect(contactSalesLink).toHaveAttribute('href', expect.stringContaining('mailto:sales@sonatype.com'));
    });
  });

  describe('number formatting', () => {
    it('should format numbers with commas', async () => {
      axiosMock.onGet('/api/v2/consumption/summary').reply(200, limitExceededData);

      renderComponent();

      await waitFor(() => {
        expect(document.querySelector('.nx-alert--error')).toBeInTheDocument();
      });

      const content = getAlertContent();
      expect(content).toContain('1,234,567');
      expect(content).toContain('1,000,000');
    });
  });

  describe('date formatting', () => {
    it('should format reset date in readable format', async () => {
      axiosMock.onGet('/api/v2/consumption/summary').reply(200, warningData);

      renderComponent();

      await waitFor(() => {
        expect(document.querySelector('.nx-alert--warning')).toBeInTheDocument();
      });

      const content = getAlertContent();
      expect(content).toContain('Resets June 1, 2026');
    });
  });

  describe('sessionStorage persistence', () => {
    it('should persist dismiss state in sessionStorage', async () => {
      const user = userEvent.setup();
      axiosMock.onGet('/api/v2/consumption/summary').reply(200, warningData);

      renderComponent();

      await waitFor(() => {
        expect(document.querySelector('.nx-alert--warning')).toBeInTheDocument();
      });

      const closeButton = screen.getByRole('button', { name: /close/i });
      await user.click(closeButton);

      expect(sessionStorage.getItem('iq-consumption-banner-dismissed')).toBe('true');
    });

    it('should not show banner when sessionStorage has dismissed flag', async () => {
      sessionStorage.setItem('iq-consumption-banner-dismissed', 'true');
      axiosMock.onGet('/api/v2/consumption/summary').reply(200, warningData);

      renderComponent();

      await waitFor(() => {
        expect(document.querySelector('.nx-alert')).not.toBeInTheDocument();
      });
    });

    it('should show banner after resetBannerDismissed clears sessionStorage', async () => {
      sessionStorage.setItem('iq-consumption-banner-dismissed', 'true');
      resetBannerDismissed();

      expect(sessionStorage.getItem('iq-consumption-banner-dismissed')).toBeNull();
    });
  });
});
