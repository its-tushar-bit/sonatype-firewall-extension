/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { screen, within } from '@testing-library/react';
import { axiosMockAdapter, render, setupPortalContainer } from 'TestRoot/SpecUtil';
import PreviewDashboardComponentsTable from 'MainRoot/nosc/dashboard/tabs/PreviewDashboardComponentsTable';

const sampleComponent = {
  hash: 'hash-1',
  derivedComponentName: 'lodash',
  affectedApplications: 3,
  score: 12,
  scoreCritical: 2,
  scoreSevere: 3,
  scoreModerate: 4,
  scoreLow: 3,
};

const loadedState = {
  dashboard: {
    components: {
      results: [sampleComponent],
      hasNextPage: false,
      error: null,
      page: 0,
    },
  },
};

const loadingState = {
  dashboard: {
    components: {
      results: null,
      hasNextPage: false,
      error: null,
      page: null,
    },
  },
};

const errorState = {
  dashboard: {
    components: {
      results: null,
      hasNextPage: false,
      error: 'Backend unavailable',
      page: null,
    },
  },
};

const emptyState = {
  dashboard: {
    components: {
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
      <PreviewDashboardComponentsTable />
    </Theme>,
    { preloadedState },
  );
}

describe('PreviewDashboardComponentsTable', () => {
  let axiosMock: ReturnType<typeof axiosMockAdapter>;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    setupPortalContainer();
    axiosMock.onAny().reply(() => new Promise(() => {}));
  });

  afterEach(() => {
    axiosMock.reset();
  });

  it('shows loading skeleton while the components slice is unloaded', () => {
    renderWrapped(loadingState);
    expect(screen.getByTestId('nosc-dashboard-components-table-loading')).toBeInTheDocument();
  });

  it('renders an error Callout when the slice has an error', () => {
    renderWrapped(errorState);
    expect(screen.getByTestId('nosc-dashboard-components-table-error')).toBeInTheDocument();
    expect(screen.getByText(/backend unavailable/i)).toBeInTheDocument();
  });

  it('renders an empty-state message when there are zero components', () => {
    renderWrapped(emptyState);
    expect(screen.getByTestId('nosc-dashboard-components-table-empty')).toBeInTheDocument();
    expect(screen.getByText(/no components match/i)).toBeInTheDocument();
  });

  it('renders one row per component with score badges', () => {
    renderWrapped(loadedState);
    const row = screen.getByTestId('nosc-dashboard-components-row');
    expect(screen.getAllByTestId('nosc-dashboard-components-row')).toHaveLength(1);
    expect(within(row).getByText('lodash')).toBeInTheDocument();
    expect(within(row).getByText('12')).toBeInTheDocument();
    expect(within(row).getByText('2')).toBeInTheDocument();
    expect(within(row).getByText('4')).toBeInTheDocument();
    expect(within(row).getAllByText('3')).toHaveLength(3);
  });
});
