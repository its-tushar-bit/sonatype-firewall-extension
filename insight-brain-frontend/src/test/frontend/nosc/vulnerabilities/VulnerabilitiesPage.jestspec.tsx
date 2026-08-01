/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { screen } from '@testing-library/react';
import { renderNexusOneRoute } from 'TestRoot/nosc/renderNexusOneRoute';
import { installRadixJsdomShims } from 'TestRoot/nosc/shell/radixJsdomShims';
import VulnerabilitiesPage, {
  type VulnerabilitiesPageProps,
} from 'MainRoot/nosc/vulnerabilities/VulnerabilitiesPage';
import { createDefaultVulnerabilitiesFilterState } from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesListApi';
import { NEXUS_ONE_VULNERABILITIES_STATE_NAME } from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesRoute';

beforeAll(() => {
  installRadixJsdomShims();
});

const user = userEvent.setup();

function renderPage(overrides: Partial<VulnerabilitiesPageProps> = {}) {
  return renderNexusOneRoute(
    <VulnerabilitiesPage
      tab="myScanData"
      onTabChange={jest.fn()}
      vulnerabilities={[
        {
          vulnerabilityId: 'CVE-2024-0001',
          title: 'Example vulnerability',
          cvssScore: 9.8,
          severity: 'critical',
          ecosystem: 'maven',
        },
      ]}
      facets={{
        totalVulnerabilities: 1,
        severities: { critical: 1 },
        ecosystems: { maven: 1 },
      }}
      filters={createDefaultVulnerabilitiesFilterState()}
      onFilterToggle={jest.fn()}
      onCvssRangeChange={jest.fn()}
      onFiltersReset={jest.fn()}
      totalCount={1}
      searchValue=""
      onSearchSubmit={jest.fn()}
      orderBy="-cvssScore"
      onOrderByChange={jest.fn()}
      page={1}
      pageSize={25}
      onPageChange={jest.fn()}
      {...overrides}
    />,
    NEXUS_ONE_VULNERABILITIES_STATE_NAME,
  );
}

describe('VulnerabilitiesPage', () => {
  it('renders My Scan Data, filter rail, and switches to Catalog', async () => {
    const onTabChange = jest.fn();
    renderPage({ onTabChange });

    expect(screen.getByTestId('preview-vulnerabilities-page')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Vulnerabilities' })).toBeInTheDocument();
    expect(screen.getByText('CVE-2024-0001')).toBeInTheDocument();
    expect(screen.getByTestId('vulnerability-card-cvss')).toHaveTextContent('9.8');
    expect(screen.getByTestId('vulnerability-card-severity')).toHaveTextContent('Critical');
    expect(screen.getByTestId('vulnerabilities-toolbar-sort')).toBeInTheDocument();
    expect(screen.getByTestId('vulnerabilities-toolbar-csv')).toBeEnabled();
    expect(screen.getByTestId('vulnerabilities-toolbar-export-form')).toHaveAttribute(
      'action',
      expect.stringContaining('/rest/dashboard/vulnerabilities/export'),
    );
    expect(screen.getByTestId('vulnerabilities-filter-rail-desktop')).toBeInTheDocument();
    expect(screen.getByTestId('vulnerabilities-filter-severity-desktop')).toBeInTheDocument();
    expect(screen.getByTestId('vulnerabilities-filter-cvss-desktop-slider')).toBeInTheDocument();
    expect(screen.getByTestId('vulnerabilities-tab-my-scan-data')).toHaveAttribute(
      'aria-selected',
      'true',
    );

    await user.click(screen.getByTestId('vulnerabilities-tab-catalog'));
    expect(onTabChange).toHaveBeenCalledWith('catalog');
  });

  it('renders scope sections with names and toggles by id (CLM-43211)', async () => {
    const onFilterToggle = jest.fn();
    renderPage({
      onFilterToggle,
      facets: {
        totalVulnerabilities: 4,
        severities: { critical: 4 },
        ecosystems: { maven: 4 },
        organizations: { 'org-1': 4 },
        applications: { 'app-1': 3, 'app-2': 1 },
        stages: { build: 4 },
        organizationNames: { 'org-1': 'Platform' },
        applicationNames: { 'app-1': 'Checkout' },
        stageNames: { build: 'Build' },
      },
    });

    expect(screen.getByTestId('vulnerabilities-filter-organizations-desktop')).toHaveTextContent(
      'Platform',
    );
    expect(screen.getByTestId('vulnerabilities-filter-stages-desktop')).toHaveTextContent('Build');
    // An application with no resolved name still renders, labelled by its id, so it stays pickable.
    const applications = screen.getByTestId('vulnerabilities-filter-applications-desktop');
    expect(applications).toHaveTextContent('Checkout');
    expect(applications).toHaveTextContent('app-2');

    await user.click(
      screen.getByTestId('vulnerabilities-filter-applications-desktop-option-app-1'),
    );
    expect(onFilterToggle).toHaveBeenCalledWith('applications', 'app-1');
  });

  it('omits scope sections the backend could not aggregate', () => {
    renderPage();
    expect(
      screen.queryByTestId('vulnerabilities-filter-organizations-desktop'),
    ).not.toBeInTheDocument();
    expect(screen.queryByTestId('vulnerabilities-filter-stages-desktop')).not.toBeInTheDocument();
  });

  it('opens the mobile filter drawer with the same filter rail on demand', async () => {
    renderPage();
    expect(screen.queryByTestId('vulnerabilities-filters-mobile-drawer')).not.toBeInTheDocument();
    await user.click(screen.getByTestId('vulnerabilities-filters-mobile-trigger'));
    expect(await screen.findByTestId('vulnerabilities-filters-mobile-drawer')).toBeInTheDocument();
    expect(screen.getByTestId('vulnerabilities-filter-rail-mobile')).toBeInTheDocument();
  });

  it('announces active filters on the mobile trigger via aria-label', () => {
    renderPage({
      filters: {
        ...createDefaultVulnerabilitiesFilterState(),
        severities: new Set(['critical']),
      },
    });
    expect(screen.getByTestId('vulnerabilities-filters-mobile-trigger')).toHaveAttribute(
      'aria-label',
      'Filters (active)',
    );
  });

  it('disables CSV and hides sort on Catalog', () => {
    renderPage({ tab: 'catalog', totalCount: 5 });
    expect(screen.getByTestId('vulnerabilities-toolbar-csv')).toBeDisabled();
    expect(screen.getByTestId('vulnerabilities-toolbar-csv')).toHaveAttribute(
      'title',
      'Sonatype Catalog export is not available',
    );
    expect(screen.queryByTestId('vulnerabilities-toolbar-sort')).not.toBeInTheDocument();
  });

  it('hides estate scope sections on Catalog even when facet maps are present', () => {
    renderPage({
      tab: 'catalog',
      facets: {
        totalVulnerabilities: 4,
        severities: { critical: 4 },
        ecosystems: { maven: 4 },
        organizations: { 'org-1': 4 },
        applications: { 'app-1': 3 },
        stages: { build: 4 },
        organizationNames: { 'org-1': 'Platform' },
        applicationNames: { 'app-1': 'Checkout' },
        stageNames: { build: 'Build' },
      },
    });
    expect(
      screen.queryByTestId('vulnerabilities-filter-organizations-desktop'),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId('vulnerabilities-filter-applications-desktop'),
    ).not.toBeInTheDocument();
    expect(screen.queryByTestId('vulnerabilities-filter-stages-desktop')).not.toBeInTheDocument();
    expect(screen.getByTestId('vulnerabilities-filter-severity-desktop')).toBeInTheDocument();
  });

  it('disables CSV when My Scan Data has zero results', () => {
    renderPage({ vulnerabilities: [], totalCount: 0 });
    expect(screen.getByTestId('vulnerabilities-toolbar-csv')).toBeDisabled();
    expect(screen.getByTestId('vulnerabilities-toolbar-csv')).toHaveAttribute(
      'title',
      'No vulnerabilities to export',
    );
    expect(screen.getByTestId('vulnerabilities-toolbar-sort')).toBeInTheDocument();
  });

  it('distinguishes filter-empty copy from estate-empty copy', () => {
    renderPage({
      vulnerabilities: [],
      totalCount: 0,
      filters: {
        ...createDefaultVulnerabilitiesFilterState(),
        severities: new Set(['critical']),
      },
    });
    expect(screen.getByTestId('vulnerabilities-list-empty')).toHaveTextContent(
      'No vulnerabilities match your filters.',
    );
  });

  it('distinguishes Catalog search+filters empty copy', () => {
    renderPage({
      tab: 'catalog',
      vulnerabilities: [],
      totalCount: 0,
      searchValue: 'log4j',
      filters: {
        ...createDefaultVulnerabilitiesFilterState(),
        severities: new Set(['critical']),
      },
    });
    expect(screen.getByTestId('vulnerabilities-list-empty')).toHaveTextContent(
      'No catalog vulnerabilities match your search and filters.',
    );
  });

  it('hides pagination while an error is shown', () => {
    renderPage({
      error: 'Unable to load vulnerabilities.',
      totalCount: 120,
      page: 3,
      pageSize: 25,
    });
    expect(screen.queryByTestId('vulnerabilities-pagination')).not.toBeInTheDocument();
  });

  it('orders severity facets critical → high → medium → low (not A–Z)', () => {
    renderPage({
      facets: {
        totalVulnerabilities: 4,
        severities: { low: 1, medium: 1, high: 1, critical: 1 },
        ecosystems: {},
      },
    });
    const labels = screen
      .getAllByTestId(/^vulnerabilities-filter-severity-desktop-option-/)
      .map((node) => node.getAttribute('data-testid'));
    expect(labels).toEqual([
      'vulnerabilities-filter-severity-desktop-option-critical',
      'vulnerabilities-filter-severity-desktop-option-high',
      'vulnerabilities-filter-severity-desktop-option-medium',
      'vulnerabilities-filter-severity-desktop-option-low',
      'vulnerabilities-filter-severity-desktop-option-none',
      'vulnerabilities-filter-severity-desktop-option-unknown',
    ]);
  });
});
