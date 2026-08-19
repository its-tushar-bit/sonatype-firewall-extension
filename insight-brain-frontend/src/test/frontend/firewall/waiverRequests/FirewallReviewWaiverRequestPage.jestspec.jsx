/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, waitFor } from 'TestRoot/SpecUtil';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import * as RouterActions from 'MainRoot/reduxUiRouter/routerActions';
import * as RouterStateContext from 'MainRoot/react/RouterStateContext';
import * as authorizationUtil from 'MainRoot/util/authorizationUtil';
import FirewallReviewWaiverRequestPage from 'MainRoot/firewall/waiverRequests/FirewallReviewWaiverRequestPage';
import { getViewOrUpdatePolicyWaiverRequestUrl, getReviewPolicyWaiverRequestUrl } from 'MainRoot/util/CLMLocation';

jest.mock('MainRoot/util/authorizationUtil', () => ({
  ...jest.requireActual('MainRoot/util/authorizationUtil'),
  checkPermissions: jest.fn(),
}));

const waiverRequest = {
  id: 'req-1',
  requesterName: 'Alice Dev',
  requestTime: '2026-05-01T10:00:00Z',
  displayName: { parts: [{ value: 'log4j-core 2.14.1' }] },
  scopeOwnerId: 'npm-central',
  scopeOwnerName: 'npm-central',
  scopeOwnerType: 'repository',
  policyName: 'Security-Critical',
  threatLevel: 10,
  constraintFacts: [{ constraintName: 'CVE Score > 9.0' }],
  noteToReviewer: 'Needed to unblock CI pipeline',
  status: 'REQUESTED',
};

const baseState = {
  router: {
    currentState: { name: 'firewall.reviewWaiverRequest' },
    currentParams: {
      waiverRequestId: 'req-1',
      ownerType: 'repository',
      ownerId: 'npm-central',
      origin: 'firewall.waivers.components.requested',
    },
  },
  firewallWaiverRequests: {
    loading: false,
    error: null,
    waiverRequests: [],
    counts: { requestedCount: 0, existingCount: 0 },
    requestModal: { isOpen: false, violationContext: null, isSubmitting: false, submitError: null },
    reviewPage: {
      loading: false,
      error: null,
      waiverRequest,
      hasWaivePermission: true,
      isSubmitting: false,
      submitError: null,
      rejectionReason: '',
    },
    isSubmitting: false,
    submitError: null,
  },
};

const nonAdminState = {
  ...baseState,
  firewallWaiverRequests: {
    ...baseState.firewallWaiverRequests,
    reviewPage: { ...baseState.firewallWaiverRequests.reviewPage, hasWaivePermission: false },
  },
};

describe('FirewallReviewWaiverRequestPage', () => {
  let axiosMock, stateGoSpy;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    stateGoSpy = jest.spyOn(RouterActions, 'stateGo');
    jest.spyOn(RouterStateContext, 'useRouterState').mockReturnValue({
      href: jest.fn().mockReturnValue('#/mocked'),
      get: jest.fn(),
      includes: jest.fn(),
    });
    // Mock checkPermissions to resolve (grant permission) by default
    authorizationUtil.checkPermissions.mockResolvedValue(true);
    // Respond to the GET triggered by useEffect on mount. The backend now returns canReview
    // on the DTO, and the slice uses it to derive hasWaivePermission — default to true here so
    // existing "user has waive permission" assertions continue to hold.
    axiosMock
      .onGet(getViewOrUpdatePolicyWaiverRequestUrl('repository', 'npm-central', 'req-1'))
      .reply(200, { ...waiverRequest, canReview: true });
    // Also handle the POST for review actions
    axiosMock.onPost(getReviewPolicyWaiverRequestUrl('repository', 'npm-central', 'req-1')).reply(200, {});
  });

  afterEach(() => {
    axiosMock.reset();
    jest.restoreAllMocks();
  });

  it('shows the page heading', () => {
    render(<FirewallReviewWaiverRequestPage />, { preloadedState: baseState });
    expect(screen.getByRole('heading', { name: /review requested waiver/i })).toBeInTheDocument();
  });

  it('shows the requester name', async () => {
    render(<FirewallReviewWaiverRequestPage />, { preloadedState: baseState });
    await waitFor(() => expect(screen.getByText('Alice Dev')).toBeInTheDocument());
  });

  it('shows the component display name', async () => {
    render(<FirewallReviewWaiverRequestPage />, { preloadedState: baseState });
    await waitFor(() => expect(screen.getAllByText('log4j-core 2.14.1').length).toBeGreaterThan(0));
  });

  it('shows the repository scope', async () => {
    render(<FirewallReviewWaiverRequestPage />, { preloadedState: baseState });
    // The scope field renders as "Repository - npm-central" via formatScopeOwnerType
    await waitFor(() => expect(screen.getByText(/npm-central/)).toBeInTheDocument());
  });

  it('shows the policy name', async () => {
    render(<FirewallReviewWaiverRequestPage />, { preloadedState: baseState });
    await waitFor(() => expect(screen.getByText('Security-Critical')).toBeInTheDocument());
  });

  it('shows the note to reviewer content', async () => {
    render(<FirewallReviewWaiverRequestPage />, { preloadedState: baseState });
    await waitFor(() => expect(screen.getByText('Needed to unblock CI pipeline')).toBeInTheDocument());
  });

  it('shows the note to reviewer', async () => {
    render(<FirewallReviewWaiverRequestPage />, { preloadedState: baseState });
    await waitFor(() => expect(screen.getByText('Needed to unblock CI pipeline')).toBeInTheDocument());
  });

  it('approves the waiver request when Approve is clicked', async () => {
    const user = userEvent.setup();
    render(<FirewallReviewWaiverRequestPage />, { preloadedState: baseState });

    // Wait for content to load
    await waitFor(() => expect(screen.getByRole('button', { name: /approve/i })).toBeInTheDocument());
    await user.click(screen.getByRole('button', { name: /approve/i }));

    await waitFor(() => {
      expect(axiosMock.history.post.length).toBe(1);
      const body = JSON.parse(axiosMock.history.post[0].data);
      expect(body.status).toBe('APPROVED');
    });
  });

  it('preserves expireWhenRemediationAvailable=true on approve when the loaded request has the flag set', async () => {
    const remediationWaiverRequest = { ...waiverRequest, expireWhenRemediationAvailable: true, expiryTime: null };
    axiosMock
      .onGet(getViewOrUpdatePolicyWaiverRequestUrl('repository', 'npm-central', 'req-1'))
      .reply(200, { ...remediationWaiverRequest, canReview: true });

    const user = userEvent.setup();
    render(<FirewallReviewWaiverRequestPage />, {
      preloadedState: {
        ...baseState,
        firewallWaiverRequests: {
          ...baseState.firewallWaiverRequests,
          reviewPage: { ...baseState.firewallWaiverRequests.reviewPage, waiverRequest: remediationWaiverRequest },
        },
      },
    });

    await waitFor(() => expect(screen.getByRole('button', { name: /approve/i })).toBeInTheDocument());

    // Verify dropdown displays "When Remediation Available" (not "Never")
    const expiryDropdown = screen.getByRole('combobox', { name: /waiver expiration/i });
    const selectedOption = Array.from(expiryDropdown.options).find((option) => option.selected);
    expect(selectedOption.value).toBe('remediationAvailable');

    await user.click(screen.getByRole('button', { name: /approve/i }));

    await waitFor(() => {
      expect(axiosMock.history.post.length).toBe(1);
      const body = JSON.parse(axiosMock.history.post[0].data);
      expect(body.status).toBe('APPROVED');
      expect(body.expireWhenRemediationAvailable).toBe(true);
      expect(body.expiryTime).toBeNull();
    });
  });

  it('preserves expiryTime=null on approve when the loaded request was submitted as Never', async () => {
    // useState('30') initial + no else branch = a Never request re-opens showing "30 Days" and
    // approve-as-is silently issues a 30-day waiver. The load useEffect must reset to 'never'.
    const neverWaiverRequest = { ...waiverRequest, expireWhenRemediationAvailable: false, expiryTime: null };
    axiosMock
      .onGet(getViewOrUpdatePolicyWaiverRequestUrl('repository', 'npm-central', 'req-1'))
      .reply(200, { ...neverWaiverRequest, canReview: true });

    const user = userEvent.setup();
    render(<FirewallReviewWaiverRequestPage />, {
      preloadedState: {
        ...baseState,
        firewallWaiverRequests: {
          ...baseState.firewallWaiverRequests,
          reviewPage: { ...baseState.firewallWaiverRequests.reviewPage, waiverRequest: neverWaiverRequest },
        },
      },
    });

    await waitFor(() => expect(screen.getByRole('button', { name: /approve/i })).toBeInTheDocument());

    // Verify dropdown displays "Never" (not "30 Days")
    const expiryDropdown = screen.getByRole('combobox', { name: /waiver expiration/i });
    const selectedOption = Array.from(expiryDropdown.options).find((option) => option.selected);
    expect(selectedOption.value).toBe('never');

    await user.click(screen.getByRole('button', { name: /approve/i }));

    await waitFor(() => {
      expect(axiosMock.history.post.length).toBe(1);
      const body = JSON.parse(axiosMock.history.post[0].data);
      expect(body.status).toBe('APPROVED');
      expect(body.expiryTime).toBeNull();
      expect(body.expireWhenRemediationAvailable).toBe(false);
    });
  });

  it('shows rejection reason field when Reject is clicked', async () => {
    const user = userEvent.setup();
    render(<FirewallReviewWaiverRequestPage />, { preloadedState: baseState });

    await waitFor(() => expect(screen.getByRole('button', { name: /reject/i })).toBeInTheDocument());
    await user.click(screen.getByRole('button', { name: /reject/i }));

    expect(screen.getByRole('textbox', { name: /rejection reason/i })).toBeInTheDocument();
  });

  it('submits rejection with reason', async () => {
    const user = userEvent.setup();
    render(<FirewallReviewWaiverRequestPage />, { preloadedState: baseState });

    await waitFor(() => expect(screen.getByRole('button', { name: /reject/i })).toBeInTheDocument());
    await user.click(screen.getByRole('button', { name: /reject/i }));
    await user.type(screen.getByRole('textbox', { name: /rejection reason/i }), 'Not acceptable');
    await user.click(screen.getByRole('button', { name: /^send$/i }));

    await waitFor(() => {
      expect(axiosMock.history.post.length).toBe(1);
      const body = JSON.parse(axiosMock.history.post[0].data);
      expect(body.status).toBe('REJECTED');
      expect(body.rejectionReason).toBe('Not acceptable');
    });
  });

  it('navigates back to origin state when Back link is clicked', async () => {
    const user = userEvent.setup();
    render(<FirewallReviewWaiverRequestPage />, { preloadedState: baseState });

    await user.click(screen.getByRole('link', { name: /back to requested waivers/i }));

    expect(stateGoSpy).toHaveBeenCalledWith('firewall.waivers.components.requested');
  });

  it('navigates back to containers tab when origin is firewall.waivers.containers.requested', async () => {
    const user = userEvent.setup();
    const containersState = {
      ...baseState,
      router: {
        ...baseState.router,
        currentParams: { ...baseState.router.currentParams, origin: 'firewall.waivers.containers.requested' },
      },
    };
    render(<FirewallReviewWaiverRequestPage />, { preloadedState: containersState });

    await user.click(screen.getByRole('link', { name: /back to requested waivers/i }));

    expect(stateGoSpy).toHaveBeenCalledWith('firewall.waivers.containers.requested');
  });

  it('shows Approve and Reject buttons when user has waive permission', async () => {
    render(<FirewallReviewWaiverRequestPage />, { preloadedState: baseState });
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /approve/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /reject/i })).toBeInTheDocument();
    });
  });

  it('hides Approve and Reject buttons and shows only Cancel for non-admin user', async () => {
    // Backend signals no review permission by returning canReview=false on the DTO.
    axiosMock
      .onGet(getViewOrUpdatePolicyWaiverRequestUrl('repository', 'npm-central', 'req-1'))
      .reply(200, { ...waiverRequest, canReview: false });
    render(<FirewallReviewWaiverRequestPage />, { preloadedState: nonAdminState });
    await waitFor(() => expect(screen.getByRole('button', { name: /cancel/i })).toBeInTheDocument());
    expect(screen.queryByRole('button', { name: /approve/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /reject/i })).not.toBeInTheDocument();
  });

  it('shows loading state when reviewPage.loading is true', () => {
    const loadingState = {
      ...baseState,
      firewallWaiverRequests: {
        ...baseState.firewallWaiverRequests,
        reviewPage: { ...baseState.firewallWaiverRequests.reviewPage, loading: true, waiverRequest: null },
      },
    };
    render(<FirewallReviewWaiverRequestPage />, { preloadedState: loadingState });
    expect(screen.getByRole('status')).toBeInTheDocument();
  });
});
