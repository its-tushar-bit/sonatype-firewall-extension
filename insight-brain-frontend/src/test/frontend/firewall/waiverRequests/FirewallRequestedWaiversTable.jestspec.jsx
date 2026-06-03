/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, waitFor, axiosMockAdapter } from 'TestRoot/SpecUtil';
import userEvent from '@testing-library/user-event';
import FirewallRequestedWaiversTable from 'MainRoot/firewall/waiverRequests/FirewallRequestedWaiversTable';
import * as RouterActions from 'MainRoot/reduxUiRouter/routerActions';
import { getListPolicyWaiverRequestsUrl } from 'MainRoot/util/CLMLocation';

const LIST_URL = getListPolicyWaiverRequestsUrl('organization', 'ROOT_ORGANIZATION_ID');

// Matches ApiPolicyWaiverRequestDTO field names from the backend
const mockRequests = [
  {
    policyWaiverRequestId: 'req-1',
    scopeOwnerType: 'repository',
    scopeOwnerId: 'npm-central',
    requesterName: 'john.doe',
    policyName: 'Security-Critical',
    threatLevel: 10,
    comment: 'Unblock CI pipeline for Q2 release',
    requestTime: '2026-05-24T10:00:00Z',
    status: 'REQUESTED',
  },
  {
    policyWaiverRequestId: 'req-2',
    scopeOwnerType: 'repository',
    scopeOwnerId: 'maven-central',
    requesterName: 'jane.smith',
    policyName: 'License-Moderate',
    threatLevel: 5,
    comment: 'Legal reviewed and approved',
    requestTime: '2026-05-23T09:00:00Z',
    status: 'APPROVED',
  },
];

const defaultPreloadedState = {
  router: {
    currentState: { name: 'firewall.waivers.components.requested' },
    currentParams: {},
  },
  firewallWaiverRequests: {
    loading: false,
    error: null,
    waiverRequests: mockRequests,
    reviewPage: { loading: false, error: null, waiverRequest: null, isSubmitting: false, submitError: null, rejectionReason: '' },
  },
};

describe('FirewallRequestedWaiversTable', () => {
  let axiosMock, stateGoSpy;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    stateGoSpy = jest.spyOn(RouterActions, 'stateGo');
    axiosMock.onGet(LIST_URL).reply(200, mockRequests);
  });

  afterEach(() => {
    axiosMock.reset();
    jest.restoreAllMocks();
  });

  it('renders the table with expected column headers', () => {
    render(<FirewallRequestedWaiversTable repositoryFormat="component" />, { preloadedState: defaultPreloadedState });

    expect(screen.getByRole('columnheader', { name: /threat/i })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: /date requested/i })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: /requester/i })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: /policy/i })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: /scope/i })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: /components/i })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: /status/i })).toBeInTheDocument();
  });

  it('renders a row for each waiver request', async () => {
    render(<FirewallRequestedWaiversTable repositoryFormat="component" />, { preloadedState: defaultPreloadedState });

    await waitFor(() => expect(screen.getByText('john.doe')).toBeInTheDocument());
    expect(screen.getByText('Security-Critical')).toBeInTheDocument();
    expect(screen.getByText('jane.smith')).toBeInTheDocument();
    expect(screen.getByText('License-Moderate')).toBeInTheDocument();
  });

  it('shows "Requested" status badge for REQUESTED row', async () => {
    render(<FirewallRequestedWaiversTable repositoryFormat="component" />, { preloadedState: defaultPreloadedState });
    await waitFor(() => expect(screen.getByText('Requested')).toBeInTheDocument());
  });

  it('shows "Approved" status badge for APPROVED row', async () => {
    render(<FirewallRequestedWaiversTable repositoryFormat="component" />, { preloadedState: defaultPreloadedState });
    await waitFor(() => expect(screen.getByText('Approved')).toBeInTheDocument());
  });

  it('navigates to review page when row is clicked', async () => {
    const user = userEvent.setup();
    render(<FirewallRequestedWaiversTable repositoryFormat="component" />, { preloadedState: defaultPreloadedState });

    // Wait for rows to render after API response
    await waitFor(() => expect(screen.getByText('john.doe')).toBeInTheDocument());

    const rows = screen.getAllByRole('row');
    // First row is header, second is first data row
    await user.click(rows[1]);

    expect(stateGoSpy).toHaveBeenCalledWith('firewall.reviewWaiverRequest', {
      waiverRequestId: 'req-1',
      ownerType: 'repository',
      ownerId: 'npm-central',
      origin: 'firewall.waivers.components.requested',
    });
  });

  it('passes containers origin when repositoryFormat is docker', async () => {
    const user = userEvent.setup();
    const dockerRequests = [{ ...mockRequests[0], scopeOwnerType: 'all_repositories', scopeOwnerId: 'REPOSITORY_CONTAINER_ID' }];
    const dockerState = { ...defaultPreloadedState, firewallWaiverRequests: { ...defaultPreloadedState.firewallWaiverRequests, waiverRequests: dockerRequests } };
    axiosMock.onGet(LIST_URL).reply(200, dockerRequests);
    render(<FirewallRequestedWaiversTable repositoryFormat="docker" />, { preloadedState: dockerState });

    await waitFor(() => expect(screen.getByText('john.doe')).toBeInTheDocument());
    const rows = screen.getAllByRole('row');
    await user.click(rows[1]);

    expect(stateGoSpy).toHaveBeenCalledWith('firewall.reviewWaiverRequest', expect.objectContaining({
      origin: 'firewall.waivers.containers.requested',
    }));
  });

  it('shows empty message when no requests exist', async () => {
    axiosMock.reset();
    axiosMock.onGet(LIST_URL).reply(200, []);
    const emptyState = {
      ...defaultPreloadedState,
      firewallWaiverRequests: { ...defaultPreloadedState.firewallWaiverRequests, waiverRequests: [] },
    };
    render(<FirewallRequestedWaiversTable repositoryFormat="component" />, { preloadedState: emptyState });
    await waitFor(() => expect(screen.getByText(/no waiver requests/i)).toBeInTheDocument());
  });

  it('shows loading state', () => {
    const loadingState = {
      ...defaultPreloadedState,
      firewallWaiverRequests: { ...defaultPreloadedState.firewallWaiverRequests, loading: true, waiverRequests: [] },
    };
    render(<FirewallRequestedWaiversTable repositoryFormat="component" />, { preloadedState: loadingState });
    expect(screen.getByRole('status')).toBeInTheDocument();
  });

});
