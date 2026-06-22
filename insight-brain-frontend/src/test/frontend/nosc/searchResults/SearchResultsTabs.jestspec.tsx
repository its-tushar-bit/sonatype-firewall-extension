/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { screen } from '@testing-library/react';
import { render } from 'TestRoot/SpecUtil';
import { SearchResultsTabs } from 'MainRoot/nosc/searchResults/SearchResultsTabs';

const COUNTS = { all: 7, APPLICATION: 5, NON_VULNERABLE_COMPONENT: 2 };

function renderTabs(loading: boolean) {
  return render(
    <Theme>
      <SearchResultsTabs
        activeTab="all"
        countsByType={COUNTS}
        onTabChange={() => {}}
        loading={loading}
      />
    </Theme>,
  );
}

describe('SearchResultsTabs', () => {
  it('shows count badges once results have loaded', () => {
    renderTabs(false);
    // The "All" tab label is followed by its count badge.
    expect(screen.getByTestId('nosc-search-tab-all')).toHaveTextContent('7');
    expect(screen.getByTestId('nosc-search-tab-APPLICATION')).toHaveTextContent('5');
  });

  it('suppresses count badges while a fetch is in flight (no transient 0/stale count)', () => {
    renderTabs(true);
    // Labels still render, but no numeric badge is shown during loading.
    const allTab = screen.getByTestId('nosc-search-tab-all');
    expect(allTab).toHaveTextContent('All');
    expect(allTab).not.toHaveTextContent('7');
    expect(screen.getByTestId('nosc-search-tab-APPLICATION')).not.toHaveTextContent('5');
  });
});
