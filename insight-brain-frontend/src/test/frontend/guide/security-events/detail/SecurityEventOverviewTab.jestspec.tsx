/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Routes, Route, Outlet } from 'react-router';
import { render, screen } from '../../test-utils';
import { SecurityEventOverviewTab } from 'GuideRoot/security-events/detail/SecurityEventOverviewTab';
import type { SecurityEventDetailDocument } from '@guide/ui-core/types';

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
} as unknown as SecurityEventDetailDocument;

function renderWithEvent(event: SecurityEventDetailDocument | Partial<SecurityEventDetailDocument>) {
  return render(
    <Routes>
      <Route path="/security-event/:eventId" element={<Outlet context={event} />}>
        <Route index element={<SecurityEventOverviewTab />} />
      </Route>
    </Routes>,
    { routerOptions: { initialEntries: ['/security-event/sonatype-2024-0001'] } }
  );
}

describe('SecurityEventOverviewTab', () => {
  it('renders the Overview, Details and Guidance sections', () => {
    renderWithEvent(mockEvent);
    expect(screen.getByRole('heading', { name: 'Overview' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Details' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Guidance' })).toBeInTheDocument();
    expect(screen.getByText(/Detailed markdown analysis/)).toBeInTheDocument();
    expect(screen.getByText(/Remove the package immediately/)).toBeInTheDocument();
  });

  it('shows the KEV-positive text when isKnownExploited is true', () => {
    renderWithEvent(mockEvent);
    expect(screen.getByText('Known to be exploited in the wild')).toBeInTheDocument();
  });

  it('shows the KEV-negative text when isKnownExploited is false', () => {
    renderWithEvent({ ...mockEvent, isKnownExploited: false });
    expect(screen.getByText('Not in KEV Catalog: No known exploits')).toBeInTheDocument();
  });

  it('renders the blog link when a url exists', () => {
    renderWithEvent(mockEvent);
    expect(screen.getByRole('link', { name: /Read the full Sonatype analysis/i }))
      .toHaveAttribute('href', 'https://blog.sonatype.com/xyz');
  });

  it('falls back to "Not available." when guidance is empty', () => {
    renderWithEvent({ ...mockEvent, guidance: '   ' });
    expect(screen.getByText('Not available.')).toBeInTheDocument();
    expect(screen.queryByText(/Remove the package immediately/)).not.toBeInTheDocument();
  });

  it('shows "Not available." for Known Exploited when isKnownExploited is undefined', () => {
    // Keep guidance populated so the empty-guidance fallback does not also render
    // "Not available." — the only occurrence should be the Known Exploited row.
    renderWithEvent({ ...mockEvent, isKnownExploited: undefined });
    expect(screen.getByText('Not available.')).toBeInTheDocument();
    expect(screen.queryByText('Known to be exploited in the wild')).not.toBeInTheDocument();
    expect(screen.queryByText('Not in KEV Catalog: No known exploits')).not.toBeInTheDocument();
  });

  it('does not render an Affected Component Versions row (the count lives on the tab badge)', () => {
    renderWithEvent(mockEvent);
    expect(screen.getByRole('heading', { name: 'Details' })).toBeInTheDocument();
    expect(screen.queryByText('Affected Component Versions')).not.toBeInTheDocument();
  });

  it('does not render the blog link when no url exists', () => {
    renderWithEvent({ ...mockEvent, sonatypeBlogUrl: undefined });
    expect(
      screen.queryByRole('link', { name: /Read the full Sonatype analysis/i })
    ).not.toBeInTheDocument();
  });

  it('renders a populated TagGroup with its label and values', () => {
    renderWithEvent(mockEvent);
    expect(screen.getByText('CWEs')).toBeInTheDocument();
    expect(screen.getByText('CWE-506')).toBeInTheDocument();
  });
});
