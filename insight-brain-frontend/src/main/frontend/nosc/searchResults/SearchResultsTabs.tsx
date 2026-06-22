/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Badge, Box, Tabs } from '@radix-ui/themes';
import { ITEM_TYPE_LABEL, RENDERED_ITEM_TYPES } from 'MainRoot/nosc/search/searchTypes';

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
 * counts arrive.
 */

interface SearchResultsTabsProps {
  readonly activeTab: string;
  readonly countsByType: Readonly<Record<string, number>>;
  readonly onTabChange: (tab: string) => void;
  /** When true, a fetch is in flight and count badges are suppressed. */
  readonly loading?: boolean;
}

export function SearchResultsTabs({
  activeTab,
  countsByType,
  onTabChange,
  loading = false,
}: SearchResultsTabsProps): JSX.Element {
  const renderBadge = (count: number): JSX.Element | null => {
    if (loading) return null;
    return (
      <Box pl="2">
        <Badge color="gray" variant="soft" size="1" radius="full">
          {count}
        </Badge>
      </Box>
    );
  };

  return (
    <Tabs.Root value={activeTab} onValueChange={onTabChange}>
      <Tabs.List style={{ borderBottom: '1px solid var(--gray-4)' }}>
        <Tabs.Trigger value="all" data-testid="nosc-search-tab-all">
          All
          {renderBadge(countsByType.all ?? 0)}
        </Tabs.Trigger>
        {RENDERED_ITEM_TYPES.map((type) => (
          <Tabs.Trigger key={type} value={type} data-testid={`nosc-search-tab-${type}`}>
            {ITEM_TYPE_LABEL[type]}
            {renderBadge(countsByType[type] ?? 0)}
          </Tabs.Trigger>
        ))}
      </Tabs.List>
    </Tabs.Root>
  );
}
