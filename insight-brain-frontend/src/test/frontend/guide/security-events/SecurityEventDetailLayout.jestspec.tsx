/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { render, screen, waitFor } from '../test-utils';
import { SecurityEventDetailLayout } from 'GuideRoot/security-events/SecurityEventDetailLayout';
import * as securityEventsBackend from 'GuideRoot/api/securityEventsBackend';

jest.mock('GuideRoot/api/securityEventsBackend');
jest.mock('GuideRoot/utils/navigation', () => ({
  reloadPage: jest.fn(),
  clearErrorRetries: jest.fn(),
  getErrorRetryCount: jest.fn().mockReturnValue(0),
}));

const SE_ROUTES = [{ path: '/security-event/:eventId' }, { path: '/security-event' }];

function renderAtPath(path: string) {
  return render(<SecurityEventDetailLayout />, {
    routerOptions: { initialEntries: [path], routes: SE_ROUTES },
  });
}

const mockEvent = {
  eventId: 'sonatype-2024-0001',
  title: 'Malicious package xyz',
  overview: 'A malicious package was published to npm.',
  publishedDate: '2024-01-01T00:00:00Z',
  lastUpdatedDate: '2024-01-02T00:00:00Z',
  eventSeverityCategory: 'Critical',
  eventThreatType: 'MALICIOUS_OSS',
  isKnownExploited: true,
  detail: 'Detailed markdown analysis of the event.',
  guidance: 'Remove the package immediately.',
  sonatypeBlogUrl: 'https://blog.sonatype.com/xyz',
  advisoryReferenceIds: ['CVE-2024-0001'],
  cwes: ['CWE-506'],
  malwareThreatTypes: ['DATA_EXFILTRATION'],
  malwareAttackVectors: ['TYPOSQUATTING'],
  affectedEcosystems: ['npm'],
  affectedComponentVersionsCount: 3,
};

const mockGet = securityEventsBackend.getSecurityEventDetails as jest.Mock;

describe('SecurityEventDetailLayout', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('shows loading state while fetching', () => {
    mockGet.mockImplementation(() => new Promise(() => {}));
    renderAtPath('/security-event/sonatype-2024-0001');
    expect(screen.getByRole('status')).toBeInTheDocument();
  });

  it('renders the header, breadcrumb and both tabs on success', async () => {
    mockGet.mockResolvedValue(mockEvent);
    renderAtPath('/security-event/sonatype-2024-0001');

    await waitFor(() => {
      expect(screen.getByRole('tab', { name: /Overview/i })).toBeInTheDocument();
    });
    expect(screen.getByRole('tab', { name: /Impacted Components/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Security Events' })).toBeInTheDocument();
    expect(screen.getAllByText('Malicious package xyz').length).toBeGreaterThan(0);
  });

  it('shows the impacted-components count badge when present', async () => {
    mockGet.mockResolvedValue(mockEvent);
    renderAtPath('/security-event/sonatype-2024-0001');

    await waitFor(() => {
      expect(screen.getByRole('tab', { name: /Impacted Components/i })).toBeInTheDocument();
    });
    // Radix Tabs.Trigger renders its content twice (visible + hidden layout spacer), so the
    // count badge appears in more than one node; scope the assertion to the tab itself.
    expect(screen.getByRole('tab', { name: /Impacted Components/i })).toHaveTextContent('3');
  });

  it('shows the not-found state when the event is null', async () => {
    mockGet.mockResolvedValue(null);
    renderAtPath('/security-event/does-not-exist');

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /security event not found/i })).toBeInTheDocument();
    });
    expect(screen.getByText(/does-not-exist/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /go to home/i })).toBeInTheDocument();
  });

  it('shows not-found without the "check the ID" hint when no eventId is provided', async () => {
    renderAtPath('/security-event');

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /security event not found/i })).toBeInTheDocument();
    });
    expect(screen.getByText(/No security event ID provided/i)).toBeInTheDocument();
    expect(screen.queryByText(/Please check the ID and try again/i)).not.toBeInTheDocument();
  });

  it('shows the error state when the fetch throws', async () => {
    mockGet.mockRejectedValue(new Error('Network error'));
    renderAtPath('/security-event/sonatype-2024-0001');

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /we hit a snag/i })).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
  });
});
