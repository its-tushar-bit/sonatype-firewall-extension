/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, waitFor, axiosMockAdapter } from 'TestRoot/SpecUtil';
import userEvent from '@testing-library/user-event';
import FirewallRenewWaiverPage from 'MainRoot/firewall/renewWaiver/FirewallRenewWaiverPage';
import * as RouterStateContext from 'MainRoot/react/RouterStateContext';
import * as RouterActions from 'MainRoot/reduxUiRouter/routerActions';
import { getWaiverDetailsUrl, renewWaiverUrl, getPolicyWaiverReasonsUrl } from 'MainRoot/util/CLMLocation';

jest.mock('MainRoot/ComponentDisplay/ReactComponentDisplay', () => {
  return function MockComponentDisplay({ component }) {
    return <span data-testid="component-display">{component?.packageId || 'component'}</span>;
  };
});

describe('FirewallRenewWaiverPage', () => {
  let axiosMock, stateGoSpy, mockRouterState;

  const OWNER_TYPE = 'repository';
  const OWNER_ID = 'repo-123';
  const WAIVER_ID = 'waiver-456';

  const mockWaiver = {
    policyWaiverId: WAIVER_ID,
    policyName: 'Security-High',
    scopeOwnerType: 'repository',
    scopeOwnerName: 'pypi-tenant',
    scopeOwnerId: OWNER_ID,
    expiryTime: '2026-09-04T23:59:59.999Z',
    creatorName: 'Admin BuiltIn',
    matcherStrategy: 'EXACT_COMPONENT',
    packageId: 'pkg:pypi/urllib3@1.25',
  };

  const mockReasons = [
    { id: 'reason-1', reasonText: 'False Positive' },
    { id: 'reason-2', reasonText: 'Risk Accepted' },
  ];

  const defaultPreloadedState = {
    firewallRenewWaiver: {
      loading: false,
      loadError: null,
      waiver: mockWaiver,
      newExpiryTime: '30',
      customExpiryTime: { value: '', isPristine: true },
      comment: { value: '', isPristine: true },
      reasonId: null,
      submitMaskState: null,
      submitError: null,
      isDirty: false,
      waiverReasons: mockReasons,
      waiverReasonsLoading: false,
      waiverReasonsError: null,
    },
    router: {
      currentParams: { ownerType: OWNER_TYPE, ownerId: OWNER_ID, waiverId: WAIVER_ID },
      currentState: { name: 'firewall.renewWaiver' },
      prevState: { name: 'firewall.waiver.details' },
      prevParams: {},
    },
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    axiosMock.onGet(getWaiverDetailsUrl(OWNER_TYPE, OWNER_ID, WAIVER_ID)).reply(200, mockWaiver);
    axiosMock.onGet(getPolicyWaiverReasonsUrl()).reply(200, mockReasons);
    axiosMock.onPost(renewWaiverUrl()).reply(200, {});

    mockRouterState = {
      href: jest.fn().mockImplementation((stateName) => `#/${stateName}`),
      get: jest.fn(),
      includes: jest.fn(),
    };
    jest.spyOn(RouterStateContext, 'useRouterState').mockReturnValue(mockRouterState);
    stateGoSpy = jest.spyOn(RouterActions, 'stateGo');
  });

  afterEach(() => {
    jest.clearAllMocks();
    axiosMock.reset();
  });

  afterAll(() => {
    axiosMock.restore();
  });

  const renderPage = (preloadedState = defaultPreloadedState) => {
    return render(<FirewallRenewWaiverPage />, { preloadedState });
  };

  const waitForPage = async (preloadedState) => {
    const result = renderPage(preloadedState);
    await waitFor(() => {
      expect(screen.getByText('Renew Waiver')).toBeInTheDocument();
      expect(screen.queryByText('Loading…')).not.toBeInTheDocument();
    });
    return result;
  };

  describe('page rendering', () => {
    it('should render page title', async () => {
      await waitForPage();
      expect(screen.getByRole('heading', { name: 'Renew Waiver' })).toBeInTheDocument();
    });

    it('should render back button', async () => {
      await waitForPage();
      expect(screen.getByRole('link', { name: /back to waiver details/i })).toBeInTheDocument();
    });

    it('should render waiver policy name', async () => {
      await waitForPage();
      expect(screen.getByText('Security-High')).toBeInTheDocument();
    });

    it('should render waiver scope', async () => {
      await waitForPage();
      expect(screen.getByText('pypi-tenant')).toBeInTheDocument();
    });

    it('should render current expiry date', async () => {
      await waitForPage();
      expect(screen.getByText(/2026-09-04/)).toBeInTheDocument();
    });

    it('should render Cancel and Renew buttons', async () => {
      await waitForPage();
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Renew' })).toBeInTheDocument();
    });

    it('should render New Expiry Date fieldset', async () => {
      await waitForPage();
      expect(screen.getByText('New Expiry Date')).toBeInTheDocument();
    });

    it('should render Reason fieldset', async () => {
      await waitForPage();
      expect(screen.getByText('Reason')).toBeInTheDocument();
    });

    it('should render Comment fieldset', async () => {
      await waitForPage();
      expect(screen.getByText('Comment')).toBeInTheDocument();
    });

    it('should render Updated By with creator name', async () => {
      await waitForPage();
      expect(screen.getByText('Admin BuiltIn')).toBeInTheDocument();
    });
  });

  describe('loading state', () => {
    it('should show loading spinner while fetching waiver', async () => {
      const loadingState = {
        ...defaultPreloadedState,
        firewallRenewWaiver: { ...defaultPreloadedState.firewallRenewWaiver, loading: true, waiver: null },
      };
      renderPage(loadingState);
      expect(screen.getByRole('status')).toBeInTheDocument();
    });

    it.skip('should show error and retry button on load failure', async () => {
      // TODO: NxLoadWrapper error state needs to be properly initialized
      const errorState = {
        ...defaultPreloadedState,
        firewallRenewWaiver: {
          ...defaultPreloadedState.firewallRenewWaiver,
          loading: false,
          loadError: 'Failed to load waiver',
          waiver: null,
        },
      };
      renderPage(errorState);
      await waitFor(() => {
        expect(screen.getByText(/failed to load waiver/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
      });
    });
  });

  describe('expiry selection', () => {
    it('should default to 30 days when waiver has expiry', async () => {
      await waitForPage();
      const select = screen.getByDisplayValue('30 Days');
      expect(select.value).toBe('30');
    });

    it('should default to never when waiver has no expiry', async () => {
      const neverWaiver = { ...mockWaiver, expiryTime: null };
      axiosMock.onGet(getWaiverDetailsUrl(OWNER_TYPE, OWNER_ID, WAIVER_ID)).reply(200, neverWaiver);

      const neverState = {
        ...defaultPreloadedState,
        firewallRenewWaiver: {
          ...defaultPreloadedState.firewallRenewWaiver,
          waiver: neverWaiver,
          newExpiryTime: 'never',
        },
      };
      await waitForPage(neverState);
      const select = screen.getByDisplayValue('Never');
      expect(select.value).toBe('never');
    });

    it.skip('should show date picker when Custom is selected', async () => {
      // TODO: This test needs to interact with the component via user actions
      // because loadWaiverForRenewal resets newExpiryTime based on the fetched waiver
      const customState = {
        ...defaultPreloadedState,
        firewallRenewWaiver: {
          ...defaultPreloadedState.firewallRenewWaiver,
          newExpiryTime: 'custom',
          customExpiryTime: { value: '', isPristine: true },
        },
      };
      await waitForPage(customState);
      expect(screen.getByRole('textbox', { name: /expiry date/i }) ||
             document.querySelector('input[type="date"]')).toBeTruthy();
    });

    it('should show renewal message for preset days selection', async () => {
      await waitForPage();
      expect(screen.getByText(/days added from current expiry date/i)).toBeInTheDocument();
      expect(screen.getByText(/extends current expiry by 30 days/i)).toBeInTheDocument();
    });

    it('should show no-op notice when waiver never expires and Never is selected', async () => {
      const noOpWaiver = { ...mockWaiver, expiryTime: null };
      axiosMock.onGet(getWaiverDetailsUrl(OWNER_TYPE, OWNER_ID, WAIVER_ID)).reply(200, noOpWaiver);

      const noOpState = {
        ...defaultPreloadedState,
        firewallRenewWaiver: {
          ...defaultPreloadedState.firewallRenewWaiver,
          waiver: noOpWaiver,
          newExpiryTime: 'never',
        },
      };
      await waitForPage(noOpState);
      expect(screen.getByText(/waiver already has no expiry date/i)).toBeInTheDocument();
    });
  });

  describe('Renew button disabled state', () => {
    it('should enable Renew button with valid 30-day selection', async () => {
      await waitForPage();
      expect(screen.getByRole('button', { name: 'Renew' })).not.toBeDisabled();
    });

    it('should disable Renew button for no-op renewal (never + no expiry)', async () => {
      const noOpWaiver = { ...mockWaiver, expiryTime: null };
      axiosMock.onGet(getWaiverDetailsUrl(OWNER_TYPE, OWNER_ID, WAIVER_ID)).reply(200, noOpWaiver);

      const noOpState = {
        ...defaultPreloadedState,
        firewallRenewWaiver: {
          ...defaultPreloadedState.firewallRenewWaiver,
          waiver: noOpWaiver,
          newExpiryTime: 'never',
        },
      };
      await waitForPage(noOpState);
      expect(screen.getByRole('button', { name: 'Renew' })).toBeDisabled();
    });

    it.skip('should disable Renew button when custom date is empty', async () => {
      // TODO: These tests need to interact with the component via user actions
      // because loadWaiverForRenewal resets newExpiryTime based on the fetched waiver
      const customEmptyState = {
        ...defaultPreloadedState,
        firewallRenewWaiver: {
          ...defaultPreloadedState.firewallRenewWaiver,
          newExpiryTime: 'custom',
          customExpiryTime: { value: '', isPristine: true },
        },
      };
      await waitForPage(customEmptyState);
      expect(screen.getByRole('button', { name: 'Renew' })).toBeDisabled();
    });

    it.skip('should enable Renew button when custom date is a valid future date', async () => {
      // TODO: These tests need to interact with the component via user actions
      // because loadWaiverForRenewal resets newExpiryTime based on the fetched waiver
      const futureDate = '2027-12-31';
      const customValidState = {
        ...defaultPreloadedState,
        firewallRenewWaiver: {
          ...defaultPreloadedState.firewallRenewWaiver,
          newExpiryTime: 'custom',
          customExpiryTime: { value: futureDate, isPristine: false },
        },
      };
      await waitForPage(customValidState);
      expect(screen.getByRole('button', { name: 'Renew' })).not.toBeDisabled();
    });
  });

  describe('expired waiver', () => {
    it('should show EXPIRED tag when waiver is expired', async () => {
      const expiredWaiver = { ...mockWaiver, expiryTime: '2020-01-01T00:00:00.000Z' };
      axiosMock.onGet(getWaiverDetailsUrl(OWNER_TYPE, OWNER_ID, WAIVER_ID)).reply(200, expiredWaiver);

      const expiredState = {
        ...defaultPreloadedState,
        firewallRenewWaiver: {
          ...defaultPreloadedState.firewallRenewWaiver,
          waiver: expiredWaiver,
          newExpiryTime: '30',
        },
      };
      await waitForPage(expiredState);
      expect(screen.getByText('EXPIRED')).toBeInTheDocument();
    });

    it('should show "Days added from today" hint for expired waiver', async () => {
      const expiredWaiver = { ...mockWaiver, expiryTime: '2020-01-01T00:00:00.000Z' };
      axiosMock.onGet(getWaiverDetailsUrl(OWNER_TYPE, OWNER_ID, WAIVER_ID)).reply(200, expiredWaiver);

      const expiredState = {
        ...defaultPreloadedState,
        firewallRenewWaiver: {
          ...defaultPreloadedState.firewallRenewWaiver,
          waiver: expiredWaiver,
          newExpiryTime: '30',
        },
      };
      await waitForPage(expiredState);
      expect(screen.getByText(/days added from today \(waiver is expired\)/i)).toBeInTheDocument();
    });
  });

  describe('submit error', () => {
    it('should display submit error message when present', async () => {
      const errorState = {
        ...defaultPreloadedState,
        firewallRenewWaiver: {
          ...defaultPreloadedState.firewallRenewWaiver,
          submitError: 'Renewal failed: server error',
        },
      };
      await waitForPage(errorState);
      expect(screen.getByText('Renewal failed: server error')).toBeInTheDocument();
    });
  });

  describe('Cancel button', () => {
    it('should navigate back when Cancel is clicked', async () => {
      const user = userEvent.setup();
      await waitForPage();
      await user.click(screen.getByRole('button', { name: 'Cancel' }));
      expect(stateGoSpy).toHaveBeenCalled();
    });
  });

  describe('Renew button submission', () => {
    it('should dispatch submitRenewal when Renew is clicked', async () => {
      const user = userEvent.setup();
      await waitForPage();
      await user.click(screen.getByRole('button', { name: 'Renew' }));
      await waitFor(() => {
        expect(axiosMock.history.post.length).toBe(1);
        expect(axiosMock.history.post[0].url).toBe(renewWaiverUrl());
      });
    });

    it('should show submit mask during submission', async () => {
      const submittingState = {
        ...defaultPreloadedState,
        firewallRenewWaiver: {
          ...defaultPreloadedState.firewallRenewWaiver,
          submitMaskState: false,
        },
      };
      renderPage(submittingState);
      await waitFor(() => {
        expect(screen.getByText(/renewing waiver/i)).toBeInTheDocument();
      });
    });
  });
});
