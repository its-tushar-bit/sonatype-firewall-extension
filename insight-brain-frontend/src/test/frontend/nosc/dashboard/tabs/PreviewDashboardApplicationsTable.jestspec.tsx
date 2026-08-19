/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import userEvent from '@testing-library/user-event';
import { render, screen } from 'TestRoot/SpecUtil';
import PreviewDashboardApplicationsTable from 'MainRoot/nosc/dashboard/tabs/PreviewDashboardApplicationsTable';
import { PREVIEW_APPLICATIONS_COLUMNS } from 'MainRoot/nosc/dashboard/tabs/previewDashboardApplicationsColumns';

// jsdom does not implement several browser APIs that Radix's Tooltip
// (and similar pop-out primitives) rely on during open / close.
// Without these shims `userEvent.hover` on the trigger throws an
// opaque AggregateError. Mirrors the shim block already in use by
// `PreviewSolutionSwitcher.jestspec.tsx`.
beforeAll(() => {
  if (typeof (globalThis as any).ResizeObserver === 'undefined') {
    (globalThis as any).ResizeObserver = class {
      observe(): void {}
      unobserve(): void {}
      disconnect(): void {}
    };
  }
  if (!Element.prototype.hasPointerCapture) {
    Element.prototype.hasPointerCapture = (): boolean => false;
  }
  if (!Element.prototype.setPointerCapture) {
    Element.prototype.setPointerCapture = (): void => undefined;
  }
  if (!Element.prototype.releasePointerCapture) {
    Element.prototype.releasePointerCapture = (): void => undefined;
  }
  if (!Element.prototype.scrollIntoView) {
    Element.prototype.scrollIntoView = (): void => undefined;
  }
});

const apps = [
  {
    applicationId: 'apple-java1',
    applicationName: 'apple-java1',
    totalApplicationRisk: {
      totalRisk: 47, criticalRisk: 3, severeRisk: 8, moderateRisk: 21, lowRisk: 15,
    },
    stageRisks: [
      {
        scanId: 'scan-build-1',
        stageTypeName: 'Build',
        risk: { totalRisk: 47, criticalRisk: 3, severeRisk: 8, moderateRisk: 21, lowRisk: 15 },
      },
    ],
  },
  {
    applicationId: 'banana-java2',
    applicationName: 'banana-java2',
    totalApplicationRisk: {
      totalRisk: 12, criticalRisk: 0, severeRisk: 2, moderateRisk: 4, lowRisk: 6,
    },
    stageRisks: [],
  },
];

const loadedState = {
  dashboard: {
    currentTab: 'applications',
    applications: {
      results: apps,
      hasNextPage: false,
      classyBrew: null,
      error: null,
      sortFields: ['-totalApplicationRisk.totalRisk'],
      hasMultiplePages: false,
      page: 0,
    },
  },
};

const loadingState = {
  dashboard: {
    currentTab: 'applications',
    applications: {
      results: null, hasNextPage: false, classyBrew: null, error: null,
      sortFields: ['-totalApplicationRisk.totalRisk'], hasMultiplePages: false, page: null,
    },
  },
};

const errorState = {
  dashboard: {
    currentTab: 'applications',
    applications: {
      results: null, hasNextPage: false, classyBrew: null,
      error: 'Backend unavailable',
      sortFields: ['-totalApplicationRisk.totalRisk'], hasMultiplePages: false, page: null,
    },
  },
};

const emptyState = {
  dashboard: {
    currentTab: 'applications',
    applications: {
      results: [], hasNextPage: false, classyBrew: null, error: null,
      sortFields: ['-totalApplicationRisk.totalRisk'], hasMultiplePages: false, page: 0,
    },
  },
};

function renderWrapped(preloadedState: object) {
  return render(
    <Theme appearance="light" accentColor="blue" radius="medium">
      <PreviewDashboardApplicationsTable />
    </Theme>,
    { preloadedState }
  );
}

describe('PreviewDashboardApplicationsTable', () => {
  it('renders one row per application plus stage rows', () => {
    renderWrapped(loadedState);
    expect(screen.getAllByTestId('nosc-dashboard-applications-row')).toHaveLength(2);
    expect(screen.getAllByTestId('nosc-dashboard-applications-stage-row')).toHaveLength(1);
  });

  it('renders column headers in the order declared by PREVIEW_APPLICATIONS_COLUMNS', () => {
    renderWrapped(loadedState);
    const headers = screen.getAllByRole('columnheader');
    PREVIEW_APPLICATIONS_COLUMNS.forEach((col, i) => {
      expect(headers[i]).toHaveTextContent(col.title);
    });
  });

  it('renders the app name as a link to the Preview app-detail view', () => {
    renderWrapped(loadedState);
    expect(
      screen.getByRole('link', { name: 'apple-java1' })
    ).toHaveAttribute('href', '#/applications/apple-java1');
  });

  it('renders stage-row link with the Classic deep-link shape', () => {
    renderWrapped(loadedState);
    expect(
      screen.getByRole('link', { name: 'Build' })
    ).toHaveAttribute(
      'href',
      '#/management/view/application/apple-java1/report/scan-build-1/policy'
    );
  });

  it('renders Skeleton rows when loading', () => {
    renderWrapped(loadingState);
    expect(
      screen.getAllByTestId('nosc-dashboard-applications-skeleton-row').length
    ).toBeGreaterThanOrEqual(3);
  });

  it('renders an error Callout when the slice has an error', () => {
    renderWrapped(errorState);
    expect(screen.getByText(/backend unavailable/i)).toBeInTheDocument();
  });

  it('renders an empty-state message when there are zero apps', () => {
    renderWrapped(emptyState);
    expect(screen.getByText(/no applications/i)).toBeInTheDocument();
  });

  it('does not render any Nx* (RSC) primitive', () => {
    const { container } = renderWrapped(loadedState);
    expect(container.querySelectorAll('[class^="nx-"]').length).toBe(0);
  });

  it('exposes Coming-Soon affordances for CSV export and pagination', () => {
    renderWrapped(loadedState);
    expect(screen.getByRole('button', { name: /csv export/i })).toBeDisabled();
    expect(screen.getByTestId('nosc-dashboard-applications-csv')).toBeDisabled();
    expect(screen.getByRole('button', { name: /pagination/i })).toBeDisabled();
    expect(screen.getByTestId('nosc-dashboard-applications-pagination')).toBeDisabled();
  });
});
