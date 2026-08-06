/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useRef } from 'react';
import { Badge, Box, Flex } from '@radix-ui/themes';
import { ITEM_TYPE_LABEL, RENDERED_ITEM_TYPES, SearchEntityType } from 'MainRoot/nosc/search/searchTypes';
import { formatTabCount } from 'MainRoot/nosc/searchResults/searchResultsCounts';

/**
 * P1-F13: entity-type tabs for the search results page. Mirrors
 * Sonatype Guide's SearchTabs (apps/seaworthy/ui/src/components/search/
 * SearchTabs.tsx) and the design-system convention of tab labels
 * followed by a soft gray rounded-full count badge.
 *
 * Tabs: All + one per RENDERED_ITEM_TYPES that has at least 1 result.
 * Tabs with 0 results are still rendered but disabled-looking (gray-7
 * label) so the user knows the type was searched but didn't match.
 *
 * While a fetch is in flight (`loading`), count badges are suppressed
 * rather than showing a transient `0` for every type — a stale `0`
 * misleads the user into thinking a type has no matches before the
 * counts arrive. A badge is likewise suppressed when a tab's count is
 * unknown (undefined), so an uncounted tab shows no badge instead of a
 * misleading `0`.
 *
 * A plain WAI-ARIA tablist (role=tab/tablist with roving tabindex and
 * arrow-key navigation) is used instead of Radix `Tabs` because the
 * results list is a single shared panel rendered by the parent page,
 * not one Radix `Tabs.Content` per tab. Radix would auto-emit an
 * `aria-controls` pointing at a tabpanel it never renders, leaving a
 * dangling reference (axe `aria-valid-attr-value`). Here every tab
 * points `aria-controls` at the parent-owned panel id instead.
 */

interface SearchResultsTabsProps {
  readonly activeTab: string;
  readonly countsByType: Readonly<Record<string, number>>;
  /**
   * Called with the newly selected tab. `viaKeyboard` is true for arrow/Home/End
   * navigation, where activation follows focus per WAI-ARIA — the caller should
   * replace the history entry instead of pushing one per keypress.
   */
  readonly onTabChange: (tab: string, viaKeyboard?: boolean) => void;
  /** When true, a fetch is in flight and count badges are suppressed. */
  readonly loading?: boolean;
  /**
   * Entity-type tabs to render (in addition to the always-present "All" tab), in
   * presentation order. Defaults to all rendered types; the results page passes a
   * source-filtered subset so catalog mode hides the IQ-only tabs.
   */
  readonly visibleTypes?: readonly SearchEntityType[];
  /** id of the results tabpanel this tablist controls (for aria-controls). */
  readonly panelId: string;
}

export function SearchResultsTabs({
  activeTab,
  countsByType,
  onTabChange,
  loading = false,
  visibleTypes = RENDERED_ITEM_TYPES,
  panelId,
}: SearchResultsTabsProps): JSX.Element {
  const tabValues: readonly string[] = ['all', ...visibleTypes];
  const tabRefs = useRef<Array<HTMLButtonElement | null>>([]);

  const renderBadge = (count: number | undefined): JSX.Element | null => {
    if (loading || typeof count !== 'number') return null;
    // Formatted through the same helper as the hit summary so a capped count
    // reads "10,000+" in both places rather than a bare 10000 here.
    return (
      <Box pl="2" asChild>
        <Badge color="gray" variant="soft" size="1" radius="full">
          {formatTabCount(count)}
        </Badge>
      </Box>
    );
  };

  // Roving-tabindex arrow navigation, matching the WAI-ARIA tabs pattern:
  // Left/Right (and Home/End) move focus between tabs and activate them.
  const onKeyDown = (e: React.KeyboardEvent, index: number): void => {
    let next = index;
    if (e.key === 'ArrowRight') next = (index + 1) % tabValues.length;
    else if (e.key === 'ArrowLeft') next = (index - 1 + tabValues.length) % tabValues.length;
    else if (e.key === 'Home') next = 0;
    else if (e.key === 'End') next = tabValues.length - 1;
    else return;
    e.preventDefault();
    onTabChange(tabValues[next], true);
    tabRefs.current[next]?.focus();
  };

  const renderTab = (value: string, label: string, index: number): JSX.Element => {
    const selected = value === activeTab;
    const count = value === 'all' ? countsByType.all : countsByType[value];
    return (
      <button
        key={value}
        ref={(el) => {
          tabRefs.current[index] = el;
        }}
        type="button"
        role="tab"
        id={`${panelId}-tab-${value}`}
        aria-selected={selected}
        aria-controls={panelId}
        tabIndex={selected ? 0 : -1}
        data-testid={`nosc-search-tab-${value}`}
        onClick={() => onTabChange(value)}
        onKeyDown={(e) => onKeyDown(e, index)}
        style={{
          appearance: 'none',
          background: 'none',
          border: 'none',
          borderBottom: selected ? '2px solid var(--accent-9)' : '2px solid transparent',
          marginBottom: '-1px',
          padding: 'var(--space-2) var(--space-3)',
          cursor: 'pointer',
          font: 'inherit',
          fontWeight: selected ? 500 : 400,
          color: selected ? 'var(--gray-12)' : 'var(--gray-11)',
          display: 'inline-flex',
          alignItems: 'center',
        }}
      >
        {label}
        {renderBadge(count)}
      </button>
    );
  };

  return (
    <Flex
      role="tablist"
      aria-label="Result types"
      align="center"
      style={{ borderBottom: '1px solid var(--gray-4)' }}
    >
      {renderTab('all', 'All', 0)}
      {visibleTypes.map((type, i) => renderTab(type, ITEM_TYPE_LABEL[type], i + 1))}
    </Flex>
  );
}
