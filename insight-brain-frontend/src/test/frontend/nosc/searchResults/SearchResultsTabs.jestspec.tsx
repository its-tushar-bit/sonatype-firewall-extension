/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { render } from 'TestRoot/SpecUtil';
import { SearchResultsTabs } from 'MainRoot/nosc/searchResults/SearchResultsTabs';

const COUNTS = { all: 7, APPLICATION: 5, COMPONENT: 2 };

function renderTabs(
  loading: boolean,
  visibleTypes?: readonly ('VULNERABILITY' | 'COMPONENT' | 'APPLICATION' | 'VIOLATION' | 'WAIVER')[]
) {
  return render(
    <Theme>
      <SearchResultsTabs
        activeTab="all"
        countsByType={COUNTS}
        onTabChange={() => {}}
        loading={loading}
        visibleTypes={visibleTypes}
        panelId="test-panel"
      />
    </Theme>
  );
}

describe('SearchResultsTabs', () => {
  it('shows count badges once results have loaded', () => {
    renderTabs(false);
    // The "All" tab label is followed by its count badge.
    expect(screen.getByTestId('nosc-search-tab-all')).toHaveTextContent('7');
    expect(screen.getByTestId('nosc-search-tab-APPLICATION')).toHaveTextContent('5');
    expect(screen.getByTestId('nosc-search-tab-COMPONENT')).toHaveTextContent('2');
  });

  it('suppresses count badges while a fetch is in flight (no transient 0/stale count)', () => {
    renderTabs(true);
    // Labels still render, but no numeric badge is shown during loading.
    const allTab = screen.getByTestId('nosc-search-tab-all');
    expect(allTab).toHaveTextContent('All');
    expect(allTab).not.toHaveTextContent('7');
    expect(screen.getByTestId('nosc-search-tab-APPLICATION')).not.toHaveTextContent('5');
  });

  it('formats badge counts like the hit summary (separators, and "+" at the cap)', () => {
    render(
      <Theme>
        <SearchResultsTabs
          activeTab="all"
          countsByType={{ all: 10000, APPLICATION: 1234 }}
          onTabChange={() => {}}
          panelId="test-panel"
        />
      </Theme>
    );
    expect(screen.getByTestId('nosc-search-tab-all')).toHaveTextContent('10,000+');
    expect(screen.getByTestId('nosc-search-tab-APPLICATION')).toHaveTextContent('1,234');
  });

  it('flags arrow-key activation so the caller can replace history instead of pushing', async () => {
    const user = userEvent.setup();
    const onTabChange = jest.fn();
    render(
      <Theme>
        <SearchResultsTabs
          activeTab="all"
          countsByType={COUNTS}
          onTabChange={onTabChange}
          panelId="test-panel"
        />
      </Theme>
    );
    const allTab = screen.getByTestId('nosc-search-tab-all');
    allTab.focus();
    await user.keyboard('{ArrowRight}');
    expect(onTabChange).toHaveBeenCalledWith(expect.any(String), true);

    onTabChange.mockClear();
    await user.click(screen.getByTestId('nosc-search-tab-COMPONENT'));
    expect(onTabChange).toHaveBeenCalledWith('COMPONENT');
  });

  it('points every tab aria-controls at the shared panel id (no dangling reference)', () => {
    renderTabs(false);
    for (const tab of screen.getAllByRole('tab')) {
      expect(tab).toHaveAttribute('aria-controls', 'test-panel');
    }
  });

  it('renders only the given visibleTypes (catalog hides App/Violation/Waiver)', () => {
    renderTabs(false, ['VULNERABILITY', 'COMPONENT']);
    expect(screen.getByTestId('nosc-search-tab-all')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-search-tab-COMPONENT')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-search-tab-VULNERABILITY')).toBeInTheDocument();
    expect(screen.queryByTestId('nosc-search-tab-APPLICATION')).not.toBeInTheDocument();
    expect(screen.queryByTestId('nosc-search-tab-VIOLATION')).not.toBeInTheDocument();
    expect(screen.queryByTestId('nosc-search-tab-WAIVER')).not.toBeInTheDocument();
  });
});
