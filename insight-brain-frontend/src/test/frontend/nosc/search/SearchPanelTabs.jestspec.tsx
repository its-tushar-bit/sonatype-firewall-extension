/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { SearchPanelTabs } from 'MainRoot/nosc/search/SearchPanelTabs';
import { ALL_TAB_ID, SearchPanelTab, buildPanelTabs } from 'MainRoot/nosc/search/searchPanelModel';

/**
 * jsdom reports every element's offsetWidth/clientWidth as 0 and has no
 * ResizeObserver, so the strip's measure-and-overflow pass cannot run untouched.
 * These helpers give tabs a real measured width and the container a real budget
 * so recompute() actually decides what overflows.
 */
const TAB_WIDTH = 100;
const OVERFLOW_BUTTON_WIDTH = 36;

let resizeCallbacks: ResizeObserverCallback[] = [];

class ControllableResizeObserver {
  constructor(callback: ResizeObserverCallback) {
    resizeCallbacks.push(callback);
  }
  observe(): void {
    // The real observer fires once on observe; tests drive it explicitly instead.
  }
  unobserve(): void {
    /* no-op */
  }
  disconnect(): void {
    /* no-op */
  }
}

/** Width the container reports, i.e. how much room the strip has. */
let containerWidth = 1000;

function stubLayout(): void {
  Object.defineProperty(HTMLElement.prototype, 'clientWidth', {
    configurable: true,
    get(this: HTMLElement): number {
      // Only the strip's own flex container is asked for a budget.
      return this.dataset.testid === 'nosc-search-panel-tabs' ? containerWidth : 0;
    },
  });
  Object.defineProperty(HTMLElement.prototype, 'offsetWidth', {
    configurable: true,
    get(this: HTMLElement): number {
      if (this.dataset.testid === 'nosc-search-panel-tabs-overflow') return OVERFLOW_BUTTON_WIDTH;
      if (this.dataset.testid?.startsWith('nosc-search-panel-tab-')) return TAB_WIDTH;
      return 0;
    },
  });
}

function renderTabs(
  tabs: readonly SearchPanelTab[],
  activeTab = ALL_TAB_ID
): { onActiveTabChange: jest.Mock; rerenderWith: (next: string) => void } {
  const onActiveTabChange = jest.fn();
  const ui = (active: string): JSX.Element => (
    <Theme>
      <SearchPanelTabs tabs={tabs} activeTab={active} onActiveTabChange={onActiveTabChange} />
    </Theme>
  );
  const { rerender } = render(ui(activeTab));
  return { onActiveTabChange, rerenderWith: (next: string) => rerender(ui(next)) };
}

/** Fire the observed resize so the strip re-measures at the current width. */
function triggerResize(): void {
  for (const callback of resizeCallbacks) {
    callback([], {} as ResizeObserver);
  }
}

describe('SearchPanelTabs', () => {
  const originalResizeObserver = globalThis.ResizeObserver;

  beforeEach(() => {
    resizeCallbacks = [];
    containerWidth = 1000;
    globalThis.ResizeObserver = ControllableResizeObserver as unknown as typeof ResizeObserver;
    stubLayout();
  });

  afterEach(() => {
    globalThis.ResizeObserver = originalResizeObserver;
    jest.restoreAllMocks();
  });

  const localTabs = buildPanelTabs('local', { VULNERABILITY: 3, COMPONENT: 9, APPLICATION: 2, VIOLATION: 1, WAIVER: 4 }, 19);

  it('renders every tab with its count badge when they all fit', () => {
    // 6 tabs * 100px fits inside 1000px, so nothing overflows.
    renderTabs(localTabs);

    expect(screen.getByTestId(`nosc-search-panel-tab-${ALL_TAB_ID}`)).toHaveTextContent('All');
    expect(screen.getByTestId('nosc-search-panel-tab-COMPONENT')).toHaveTextContent('9');
    expect(screen.queryByTestId('nosc-search-panel-tabs-overflow')).not.toBeInTheDocument();
  });

  it('counts the tablist gap and padding, not just the tab widths', async () => {
    // 6 tabs * 100px = 600px fits 700px on width alone, but with 16px gaps and
    // 16px side padding the strip really needs 600 + 5*16 + 32 = 712px. Ignoring
    // the spacing would report "fits" and let the parent clip the strip instead.
    containerWidth = 700;
    const realGetComputedStyle = window.getComputedStyle.bind(window);
    jest.spyOn(window, 'getComputedStyle').mockImplementation(((element: Element) => {
      const style = realGetComputedStyle(element as HTMLElement);
      if ((element as HTMLElement).classList?.contains('nosc-search-tablist')) {
        return { ...style, columnGap: '16px', gap: '16px', paddingLeft: '16px', paddingRight: '16px' };
      }
      return style;
    }) as typeof window.getComputedStyle);

    renderTabs(localTabs);
    triggerResize();

    await waitFor(() => expect(screen.getByTestId('nosc-search-panel-tabs-overflow')).toBeInTheDocument());
  });

  it('moves the tabs that do not fit into an overflow menu', async () => {
    // Budget fits about 3 tabs, so the trailing ones must overflow.
    containerWidth = 360;
    renderTabs(localTabs);
    triggerResize();

    await waitFor(() => expect(screen.getByTestId('nosc-search-panel-tabs-overflow')).toBeInTheDocument());
    // The first tab is always kept visible so the strip is never empty.
    expect(screen.getByTestId(`nosc-search-panel-tab-${ALL_TAB_ID}`)).toBeInTheDocument();
    expect(screen.queryByTestId('nosc-search-panel-tab-WAIVER')).not.toBeInTheDocument();
  });

  it('names how many tabs the overflow button holds', async () => {
    containerWidth = 360;
    renderTabs(localTabs);
    triggerResize();

    const overflow = await screen.findByTestId('nosc-search-panel-tabs-overflow');
    expect(overflow).toHaveAccessibleName(/more tabs?$/);
  });

  it('keeps the active tab visible by swapping it with the last visible tab', async () => {
    containerWidth = 360;
    // WAIVER would overflow at this width, but it is the active tab.
    renderTabs(localTabs, 'WAIVER');
    triggerResize();

    await waitFor(() => expect(screen.getByTestId('nosc-search-panel-tabs-overflow')).toBeInTheDocument());
    const activeTab = screen.getByTestId('nosc-search-panel-tab-WAIVER');
    expect(activeTab).toBeInTheDocument();
    expect(activeTab).toHaveAttribute('aria-selected', 'true');
  });

  it('brings tabs back out of the overflow menu when the strip grows', async () => {
    containerWidth = 360;
    renderTabs(localTabs);
    triggerResize();
    await waitFor(() => expect(screen.getByTestId('nosc-search-panel-tabs-overflow')).toBeInTheDocument());

    containerWidth = 1000;
    triggerResize();

    await waitFor(() =>
      expect(screen.queryByTestId('nosc-search-panel-tabs-overflow')).not.toBeInTheDocument()
    );
    expect(screen.getByTestId('nosc-search-panel-tab-WAIVER')).toBeInTheDocument();
  });

  it('selects a tab on click', async () => {
    const user = userEvent.setup();
    const { onActiveTabChange } = renderTabs(localTabs);

    await user.click(screen.getByTestId('nosc-search-panel-tab-COMPONENT'));

    expect(onActiveTabChange).toHaveBeenCalledWith('COMPONENT');
  });

  it('disables a tab whose count is known to be zero', () => {
    const tabs = buildPanelTabs('local', { COMPONENT: 0 }, 5);
    renderTabs(tabs);

    expect(screen.getByTestId('nosc-search-panel-tab-COMPONENT')).toBeDisabled();
  });

  it('leaves a tab enabled while its count is still unknown', () => {
    const tabs = buildPanelTabs('local', {}, undefined);
    renderTabs(tabs);

    expect(screen.getByTestId('nosc-search-panel-tab-COMPONENT')).toBeEnabled();
  });

  it('moves selection with left and right arrows, skipping disabled tabs', async () => {
    const user = userEvent.setup();
    // APPLICATION is the first tab after All and is empty, so arrowing off All must skip it.
    const tabs = buildPanelTabs('local', { APPLICATION: 0, COMPONENT: 4 }, 4);
    const { onActiveTabChange } = renderTabs(tabs);

    screen.getByTestId(`nosc-search-panel-tab-${ALL_TAB_ID}`).focus();
    await user.keyboard('{ArrowRight}');

    expect(onActiveTabChange).toHaveBeenCalledWith('COMPONENT');
  });

  it('only makes the selected tab a Tab stop', () => {
    renderTabs(localTabs, 'COMPONENT');

    expect(screen.getByTestId('nosc-search-panel-tab-COMPONENT')).toHaveAttribute('tabindex', '0');
    expect(screen.getByTestId(`nosc-search-panel-tab-${ALL_TAB_ID}`)).toHaveAttribute('tabindex', '-1');
  });

  it('caps a large count rather than printing the raw number', () => {
    const tabs = buildPanelTabs('local', { COMPONENT: 4321 }, 4321);
    renderTabs(tabs);

    expect(screen.getByTestId('nosc-search-panel-tab-COMPONENT')).toHaveTextContent('1,000+');
  });
});
