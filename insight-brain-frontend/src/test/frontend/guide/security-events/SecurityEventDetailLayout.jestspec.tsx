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

  it('renders header, sections and populated tag groups on success', async () => {
    mockGet.mockResolvedValue(mockEvent);

    renderAtPath('/security-event/sonatype-2024-0001');

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Overview' })).toBeInTheDocument();
    });
    expect(screen.getByRole('heading', { name: 'Details' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Guidance' })).toBeInTheDocument();
    // title appears in the header and the breadcrumb
    expect(screen.getAllByText('Malicious package xyz').length).toBeGreaterThan(0);
    // Overview section renders the `detail` markdown; guidance card renders `guidance`
    expect(screen.getByText(/Detailed markdown analysis/)).toBeInTheDocument();
    expect(screen.getByText(/Remove the package immediately/)).toBeInTheDocument();
    // tag groups (only populated ones)
    expect(screen.getByText('CWEs')).toBeInTheDocument();
    expect(screen.getByText('CVE-2024-0001')).toBeInTheDocument();
    expect(screen.getByText('Ecosystems')).toBeInTheDocument();
  });

  it('shows "Known to be exploited in the wild" when isKnownExploited is true', async () => {
    mockGet.mockResolvedValue(mockEvent);

    renderAtPath('/security-event/sonatype-2024-0001');

    await waitFor(() => {
      expect(screen.getByText('Known to be exploited in the wild')).toBeInTheDocument();
    });
  });

  it('shows the KEV-negative message when isKnownExploited is false', async () => {
    mockGet.mockResolvedValue({ ...mockEvent, isKnownExploited: false });

    renderAtPath('/security-event/sonatype-2024-0001');

    await waitFor(() => {
      expect(
        screen.getByText('Not in KEV Catalog: No known exploits')
      ).toBeInTheDocument();
    });
  });

  it('shows "Not available." for Known Exploited when isKnownExploited is undefined', async () => {
    mockGet.mockResolvedValue({ ...mockEvent, isKnownExploited: undefined });

    renderAtPath('/security-event/sonatype-2024-0001');

    // guidance is non-empty in mockEvent, so the only "Not available." rendered is the KEV row
    await waitFor(() => {
      expect(screen.getByText('Not available.')).toBeInTheDocument();
    });
    expect(screen.queryByText('Known to be exploited in the wild')).not.toBeInTheDocument();
    expect(screen.queryByText('Not in KEV Catalog: No known exploits')).not.toBeInTheDocument();
  });

  it('shows the Affected Component Versions row when the count is present', async () => {
    mockGet.mockResolvedValue(mockEvent);

    renderAtPath('/security-event/sonatype-2024-0001');

    await waitFor(() => {
      expect(screen.getByText('Affected Component Versions')).toBeInTheDocument();
    });
  });

  it('hides the Affected Component Versions row when the count is undefined', async () => {
    mockGet.mockResolvedValue({ ...mockEvent, affectedComponentVersionsCount: undefined });

    renderAtPath('/security-event/sonatype-2024-0001');

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Details' })).toBeInTheDocument();
    });
    expect(screen.queryByText('Affected Component Versions')).not.toBeInTheDocument();
  });

  it('falls back to "Not available." when guidance is empty', async () => {
    mockGet.mockResolvedValue({ ...mockEvent, guidance: '   ' });

    renderAtPath('/security-event/sonatype-2024-0001');

    await waitFor(() => {
      expect(screen.getByText('Not available.')).toBeInTheDocument();
    });
    expect(screen.queryByText(/Remove the package immediately/)).not.toBeInTheDocument();
  });

  it('shows the blog link when a blog url exists', async () => {
    mockGet.mockResolvedValue(mockEvent);

    renderAtPath('/security-event/sonatype-2024-0001');

    await waitFor(() => {
      expect(
        screen.getByRole('link', { name: /Read the full Sonatype analysis/i })
      ).toHaveAttribute('href', 'https://blog.sonatype.com/xyz');
    });
  });

  it('hides the blog link when no blog url exists', async () => {
    mockGet.mockResolvedValue({ ...mockEvent, sonatypeBlogUrl: undefined });

    renderAtPath('/security-event/sonatype-2024-0001');

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Guidance' })).toBeInTheDocument();
    });
    expect(
      screen.queryByRole('link', { name: /Read the full Sonatype analysis/i })
    ).not.toBeInTheDocument();
  });

  it('shows not-found state when the event is null', async () => {
    mockGet.mockResolvedValue(null);

    renderAtPath('/security-event/does-not-exist');

    await waitFor(() => {
      expect(
        screen.getByRole('heading', { name: /security event not found/i })
      ).toBeInTheDocument();
    });
    expect(screen.getByText(/does-not-exist/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /go to home/i })).toBeInTheDocument();
  });

  it('shows not-found without the "check the ID" hint when no eventId is provided', async () => {
    renderAtPath('/security-event');

    await waitFor(() => {
      expect(
        screen.getByRole('heading', { name: /security event not found/i })
      ).toBeInTheDocument();
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

  it('renders a breadcrumb back to the list', async () => {
    mockGet.mockResolvedValue(mockEvent);

    renderAtPath('/security-event/sonatype-2024-0001');

    await waitFor(() => {
      expect(screen.getByRole('link', { name: 'Security Events' })).toBeInTheDocument();
    });
  });
});
