/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { screen } from '@testing-library/react';
import { axiosMockAdapter, render, setupPortalContainer } from 'TestRoot/SpecUtil';
import { _setBaseUrlForTesting } from 'MainRoot/util/urlUtil';
import PreviewDashboardViolationsTable from 'MainRoot/nosc/dashboard/tabs/PreviewDashboardViolationsTable';

const NOW = Date.UTC(2026, 5, 18, 12, 0, 0);

const sampleViolation = {
  policyViolationId: 'pv-1',
  threatLevel: 9,
  policyName: 'GPL Policy',
  applicationName: 'apple-java1',
  derivedComponentName: 'log4j:log4j-core',
  firstOccurrenceTime: NOW - 5 * 24 * 60 * 60 * 1000,
};

const loadedState = {
  dashboard: {
    violations: {
      results: [sampleViolation],
      hasNextPage: false,
      error: null,
      page: 0,
    },
  },
};

const loadingState = {
  dashboard: {
    violations: {
      results: null,
      hasNextPage: false,
      error: null,
      page: null,
    },
  },
};

const errorState = {
  dashboard: {
    violations: {
      results: null,
      hasNextPage: false,
      error: 'Backend unavailable',
      page: null,
    },
  },
};

const emptyState = {
  dashboard: {
    violations: {
      results: [],
      hasNextPage: false,
      error: null,
      page: 0,
    },
  },
};

function renderWrapped(preloadedState: object) {
  return render(
    <Theme appearance="light" accentColor="blue" radius="medium">
      <PreviewDashboardViolationsTable />
    </Theme>,
    { preloadedState },
  );
}

describe('PreviewDashboardViolationsTable', () => {
  let axiosMock: ReturnType<typeof axiosMockAdapter>;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
    // Classic deep-links resolve through the real bundleIndexUrl; pin a base URL so the href is deterministic.
    _setBaseUrlForTesting('http://localhost');
  });

  beforeEach(() => {
    setupPortalContainer();
    axiosMock.onAny().reply(() => new Promise(() => {}));
    jest.spyOn(Date, 'now').mockReturnValue(NOW);
  });

  afterEach(() => {
    axiosMock.reset();
    jest.restoreAllMocks();
  });

  it('shows loading skeleton while the violations slice is unloaded', () => {
    renderWrapped(loadingState);
    expect(screen.getByTestId('nosc-dashboard-violations-table-loading')).toBeInTheDocument();
  });

  it('renders an error Callout when the slice has an error', () => {
    renderWrapped(errorState);
    expect(screen.getByTestId('nosc-dashboard-violations-table-error')).toBeInTheDocument();
    expect(screen.getByText(/backend unavailable/i)).toBeInTheDocument();
  });

  it('renders an empty-state message when there are zero violations', () => {
    renderWrapped(emptyState);
    expect(screen.getByTestId('nosc-dashboard-violations-table-empty')).toBeInTheDocument();
    expect(screen.getByText(/no violations match/i)).toBeInTheDocument();
  });

  it('renders one row per violation with policy and age labels', () => {
    renderWrapped(loadedState);
    expect(screen.getAllByTestId('nosc-dashboard-violations-row')).toHaveLength(1);
    expect(screen.getByText('GPL Policy')).toBeInTheDocument();
    expect(screen.getByText('apple-java1')).toBeInTheDocument();
    expect(screen.getByText('log4j:log4j-core')).toBeInTheDocument();
    expect(screen.getByText('5d')).toBeInTheDocument();
    expect(screen.getByText('9')).toBeInTheDocument();
  });

  it('links each row to the Classic violation detail sidebar', () => {
    renderWrapped(loadedState);
    expect(screen.getByTestId('nosc-dashboard-violations-row-detail-link')).toHaveAttribute(
      'href',
      'http://localhost/assets/index.html#/sidebarView/violation/pv-1',
    );
  });
});
