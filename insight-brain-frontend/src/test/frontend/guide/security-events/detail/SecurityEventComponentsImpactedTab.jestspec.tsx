/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, waitFor } from '../../test-utils';
import { SecurityEventComponentsImpactedTab } from 'GuideRoot/security-events/detail/SecurityEventComponentsImpactedTab';
import * as securityEventsBackend from 'GuideRoot/api/securityEventsBackend';

class MockResizeObserver {
  observe = jest.fn();
  unobserve = jest.fn();
  disconnect = jest.fn();
}
global.ResizeObserver = MockResizeObserver as unknown as typeof ResizeObserver;
window.scrollTo = jest.fn();

jest.mock('GuideRoot/api/securityEventsBackend');

const mockGet =
  securityEventsBackend.getSecurityEventAffectedComponents as jest.MockedFunction<
    typeof securityEventsBackend.getSecurityEventAffectedComponents
  >;

const ROUTES = [{ path: '/security-event/:eventId/affected-components' }];

function renderAtPath(path: string) {
  return render(<SecurityEventComponentsImpactedTab />, {
    routerOptions: { initialEntries: [path], routes: ROUTES },
  });
}

const mockResponse = {
  hits: [
    { ecosystem: 'maven', namespace: 'org.apache.logging.log4j', packageName: 'log4j-core',
      version: '2.14.1', fullPackageName: 'pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1' },
    { ecosystem: 'maven', namespace: 'org.apache.logging.log4j', packageName: 'log4j-core',
      version: '2.14.0', fullPackageName: 'pkg:maven/org.apache.logging.log4j/log4j-core@2.14.0' },
  ],
  total: 2,
  offset: 0,
  limit: 25,
};

describe('SecurityEventComponentsImpactedTab', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockGet.mockResolvedValue(mockResponse);
  });

  it('shows loading skeleton on initial load', () => {
    mockGet.mockImplementation(() => new Promise(() => {}));
    renderAtPath('/security-event/SEC-1/affected-components');
    expect(document.querySelector('[aria-busy="true"]')).toBeInTheDocument();
  });

  it('renders the table after fetch succeeds', async () => {
    renderAtPath('/security-event/SEC-1/affected-components');
    await waitFor(() => {
      expect(screen.getByText('2.14.1')).toBeInTheDocument();
      expect(screen.getByText('2.14.0')).toBeInTheDocument();
    });
    expect(screen.queryByText(/failed to load affected components/i)).not.toBeInTheDocument();
  });

  it('handles an empty list without error', async () => {
    mockGet.mockResolvedValue({ hits: [], total: 0, offset: 0, limit: 25 });
    renderAtPath('/security-event/SEC-nolinks/affected-components');
    await waitFor(() => {
      expect(mockGet).toHaveBeenCalled();
      expect(screen.queryByText(/failed to load affected components/i)).not.toBeInTheDocument();
      expect(document.querySelector('[aria-busy="true"]')).not.toBeInTheDocument();
    });
  });

  it('shows an error message when the fetch fails', async () => {
    mockGet.mockRejectedValue(new Error('Network error'));
    renderAtPath('/security-event/SEC-1/affected-components');
    await waitFor(() => {
      expect(screen.getByText(/failed to load affected components/i)).toBeInTheDocument();
    });
  });

  it('passes the eventId and default limit', async () => {
    renderAtPath('/security-event/SEC-1/affected-components');
    await waitFor(() => expect(mockGet).toHaveBeenCalled());
    expect(mockGet).toHaveBeenCalledWith('SEC-1', expect.objectContaining({ limit: 25 }));
  });

  it('clamps a limit above the policy-enrichment cap to 25', async () => {
    renderAtPath('/security-event/SEC-1/affected-components?limit=50');
    await waitFor(() => expect(mockGet).toHaveBeenCalled());
    expect(mockGet).toHaveBeenCalledWith('SEC-1', expect.objectContaining({ limit: 25 }));
  });

  it('clamps an invalid sortField to "packageName"', async () => {
    renderAtPath('/security-event/SEC-1/affected-components?sortField=invalid&sortOrder=asc');
    await waitFor(() => expect(mockGet).toHaveBeenCalled());
    expect(mockGet).toHaveBeenCalledWith('SEC-1', expect.objectContaining({ sortField: 'packageName' }));
  });
});
