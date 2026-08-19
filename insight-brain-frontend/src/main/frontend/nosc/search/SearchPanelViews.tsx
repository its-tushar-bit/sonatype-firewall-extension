/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Badge, Box, Flex, Text, VisuallyHidden } from '@radix-ui/themes';
import { ActionIcons, DomainIcons } from 'MainRoot/nosc/icons';
import { threatColorFor } from 'MainRoot/nosc/applications/applicationDetailUtils';
import { IconSlot, JumpToHint, ListboxOption } from 'MainRoot/nosc/search/SearchPanelParts';
import { SearchPanelTabs } from 'MainRoot/nosc/search/SearchPanelTabs';
import { SearchPanelTab } from 'MainRoot/nosc/search/searchPanelModel';
import { RecentSearchEntry } from 'MainRoot/nosc/search/useRecentSearches';
import { SearchRow, displayNameFor, isApplication, isComponent, isViolation, isWaiver } from 'MainRoot/nosc/search/searchTypes';

/**
 * The three panel body views: Recent Searches (empty / too-short query), the
 * placeholder (loading / no results), and the loaded results view.
 *
 * Every row is a ListboxOption participating in the input's listbox composite —
 * the parent owns the highlight and passes the active option's DOM id down.
 */

export const SHOW_RESULTS_OPTION_ID = 'nosc-search-show-results';
export const VIEW_MORE_OPTION_ID = 'nosc-search-view-more';
export const PLACEHOLDER_OPTION_ID = 'nosc-search-placeholder';

/**
 * The lead listbox, holding the "Show results for" option (and, in the recent /
 * placeholder views, every option). Always present while the panel is open.
 */
export const LEAD_LISTBOX_ID = 'nosc-search-listbox';

/**
 * The rows listbox, holding the result rows and "View more results". Only the
 * loaded view renders it, so the input names both ids in aria-controls and AT
 * following aria-controls reaches the rows as well as the lead option.
 */
export const ROWS_LISTBOX_ID = 'nosc-search-rows-listbox';

/** DOM id for the option at a given recent-search index. */
export function recentOptionId(index: number): string {
  return `nosc-search-recent-${index}`;
}

/** DOM id for the option at a given result-row index. */
export function rowOptionId(index: number): string {
  return `nosc-search-row-${index}`;
}

interface ViewCommonProps {
  /** DOM id of the highlighted option, owned by the parent composite. */
  readonly highlightedItemId: string | undefined;
  readonly onHighlight: (id: string) => void;
}

// ---------------------------------------------------------------------------
// Recent Searches
// ---------------------------------------------------------------------------

export interface RecentSearchesViewProps extends ViewCommonProps {
  readonly entries: readonly RecentSearchEntry[];
  readonly onActivate: (query: string) => void;
}

export function RecentSearchesView({
  entries,
  highlightedItemId,
  onActivate,
  onHighlight,
}: RecentSearchesViewProps): JSX.Element {
  if (entries.length === 0) {
    // Carries the lead id without role="listbox": the combobox must always name a
    // popup via aria-controls, but an empty listbox would violate the
    // listbox-requires-options rule, so the message container owns the id.
    return (
      <Box px="4" py="4" id={LEAD_LISTBOX_ID} data-testid="nosc-search-recent-empty">
        <Text size="2" color="gray">
          No recent searches yet.
        </Text>
      </Box>
    );
  }

  return (
    <Box data-testid="nosc-search-recent">
      <Box px="4" py="2">
        <Text size="1" color="gray">
          Recent Searches
        </Text>
      </Box>
      <Box role="listbox" aria-label="Recent searches" id={LEAD_LISTBOX_ID}>
        {entries.map((entry, index) => {
          const id = recentOptionId(index);
          return (
            <ListboxOption
              key={`${entry.q}:${entry.ts}`}
              id={id}
              active={id === highlightedItemId}
              onHighlight={onHighlight}
              onActivate={() => onActivate(entry.q)}
              testId={`nosc-search-recent-row-${index}`}
            >
              <Flex align="center" gap="3">
                <IconSlot>
                  <ActionIcons.Clock size={16} />
                </IconSlot>
                <Text size="2" weight="medium" truncate style={{ flex: 1, minWidth: 0 }}>
                  {entry.q}
                </Text>
              </Flex>
            </ListboxOption>
          );
        })}
      </Box>
    </Box>
  );
}

// ---------------------------------------------------------------------------
// Placeholder (loading / no results)
// ---------------------------------------------------------------------------

export interface PlaceholderViewProps extends ViewCommonProps {
  readonly query: string;
  readonly variant: 'loading' | 'empty' | 'error';
  readonly onActivate: () => void;
  readonly errorMessage?: string | null;
}

/**
 * Shown while a fetch is in flight and when nothing matched. Both variants keep
 * the same "Show results for" row so the panel never collapses to a bare
 * "Loading" or "Empty" message and the row stays activatable.
 */
export function PlaceholderView({
  query,
  variant,
  highlightedItemId,
  onHighlight,
  onActivate,
  errorMessage,
}: PlaceholderViewProps): JSX.Element {
  return (
    <Box data-testid={`nosc-search-placeholder-${variant}`}>
      <Box role="listbox" aria-label="Search results" id={LEAD_LISTBOX_ID}>
        <ListboxOption
          id={PLACEHOLDER_OPTION_ID}
          active={highlightedItemId === PLACEHOLDER_OPTION_ID}
          onHighlight={onHighlight}
          onActivate={onActivate}
          testId="nosc-search-show-results-row"
        >
          <ShowResultsForRow query={query} />
        </ListboxOption>
      </Box>
      {variant === 'loading' && (
        <Box px="4" pb="2">
          <Text size="1" color="gray" data-testid="nosc-search-loading-text">
            Searching…
          </Text>
        </Box>
      )}
      {variant === 'empty' && (
        <Box px="4" pb="2">
          <Text size="1" color="gray">
            No matches yet — press Enter to search everything.
          </Text>
        </Box>
      )}
      {variant === 'error' && (
        <Box px="4" pb="2">
          <Text size="1" color="gray">
            {errorMessage || 'Search is unavailable. Try again in a moment.'}
          </Text>
        </Box>
      )}
    </Box>
  );
}

// ---------------------------------------------------------------------------
// Loaded results
// ---------------------------------------------------------------------------

export interface ResultsViewProps extends ViewCommonProps {
  readonly query: string;
  readonly tabs: readonly SearchPanelTab[];
  readonly activeTab: string;
  readonly onActiveTabChange: (tab: string) => void;
  /** Hidden when the query already carries itemType: tokens. */
  readonly hideTabs: boolean;
  readonly rows: readonly SearchRow[];
  readonly onActivateRow: (row: SearchRow) => void;
  /** Activated by "Show results for" and "View more results". */
  readonly onShowAll: () => void;
  readonly showViewMore: boolean;
}

export function ResultsView({
  query,
  tabs,
  activeTab,
  onActiveTabChange,
  hideTabs,
  rows,
  highlightedItemId,
  onHighlight,
  onActivateRow,
  onShowAll,
  showViewMore,
}: ResultsViewProps): JSX.Element {
  return (
    <Box data-testid="nosc-search-results-view">
      {/* One listbox holds every option: the tab strip is a separate widget and
          cannot be a listbox child (a listbox admits only options), so it is
          rendered after the listbox and positioned above the rows with CSS
          order. That keeps the DOM valid for ARIA while preserving the visual
          order "Show results for" -> tabs -> rows. */}
      <Box className="nosc-search-results-stack">
        {!hideTabs && tabs.length > 1 && (
          <Box className="nosc-search-results-stack__tabs">
            <SearchPanelTabs tabs={tabs} activeTab={activeTab} onActiveTabChange={onActiveTabChange} />
          </Box>
        )}

        <Box
          role="listbox"
          aria-label="Search results"
          id={LEAD_LISTBOX_ID}
          className="nosc-search-results-stack__lead"
        >
          <ListboxOption
            id={SHOW_RESULTS_OPTION_ID}
            active={highlightedItemId === SHOW_RESULTS_OPTION_ID}
            onHighlight={onHighlight}
            onActivate={onShowAll}
            testId="nosc-search-show-results-row"
          >
            <ShowResultsForRow query={query} />
          </ListboxOption>
        </Box>

        <Box
          role="listbox"
          aria-label="Results"
          id={ROWS_LISTBOX_ID}
          className="nosc-search-results-stack__rows"
        >
          {rows.map((row, index) => {
            const id = rowOptionId(index);
            return (
              // Keyed on the positional id, not the row identity: two rows can
              // legitimately share an entity id across sources, and React would
              // otherwise drop one with a duplicate-key warning.
              <ListboxOption
                key={id}
                id={id}
                active={id === highlightedItemId}
                onHighlight={onHighlight}
                onActivate={() => onActivateRow(row)}
                testId={`nosc-search-result-row-${index}`}
              >
                <Flex align="center" gap="3" width="100%">
                  <Box flexGrow="1" minWidth="0">
                    <SearchRowContent row={row} />
                  </Box>
                  <JumpToHint />
                </Flex>
              </ListboxOption>
            );
          })}

          {showViewMore && (
            <ListboxOption
              id={VIEW_MORE_OPTION_ID}
              active={highlightedItemId === VIEW_MORE_OPTION_ID}
              onHighlight={onHighlight}
              onActivate={onShowAll}
              testId="nosc-search-view-more"
            >
              <Text size="2" weight="medium" color="indigo">
                View more results
              </Text>
            </ListboxOption>
          )}
        </Box>
      </Box>

      <VisuallyHidden aria-live="polite">
        {rows.length} result{rows.length === 1 ? '' : 's'} for {query}
      </VisuallyHidden>
    </Box>
  );
}

/** Leading row of the results / placeholder views: the escape hatch to the full results page. */
function ShowResultsForRow({ query }: { readonly query: string }): JSX.Element {
  return (
    <Flex align="center" gap="3">
      <IconSlot>
        <ActionIcons.Search size={16} />
      </IconSlot>
      <Text size="2" weight="medium" style={{ flex: 1, minWidth: 0 }}>
        Show results for:{' '}
        <Text size="2" weight="regular">
          {query}
        </Text>
      </Text>
      <JumpToHint />
    </Flex>
  );
}

/**
 * Row body: leading type icon (or numeric severity pill for threat-bearing
 * rows), bold title, gray subtitle. The backend already formats title and
 * subtitle per entity type, so this only picks the leading slot.
 */
export function SearchRowContent({ row }: { readonly row: SearchRow }): JSX.Element {
  return (
    <Flex align="center" gap="3" width="100%">
      <IconSlot>
        <RowLeadingIcon row={row} />
      </IconSlot>
      <Flex direction="column" gap="1" flexGrow="1" minWidth="0">
        <Text size="2" weight="medium" truncate>
          {displayNameFor(row)}
        </Text>
        {row.subtitle && (
          <Text size="1" color="gray" truncate>
            {row.subtitle}
          </Text>
        )}
      </Flex>
    </Flex>
  );
}

/**
 * Numeric threat level for a row, when the backend supplied one. Violations and
 * vulnerabilities render it as a colored severity pill in place of a type icon,
 * matching how threat is shown everywhere else in the Preview UI.
 */
function threatLevelOf(row: SearchRow): number | null {
  for (const key of ['threatLevel', 'policyThreatLevel', 'maxThreatLevel']) {
    const value = row.fields[key];
    if (typeof value === 'number' && Number.isFinite(value)) return value;
  }
  return null;
}

function RowLeadingIcon({ row }: { readonly row: SearchRow }): JSX.Element {
  const threat = threatLevelOf(row);
  if (threat !== null) {
    return (
      <Badge color={threatColorFor(threat)} variant="soft" radius="full" size="1" aria-label={`Threat level ${threat}`}>
        {threat}
      </Badge>
    );
  }
  if (isApplication(row)) return <DomainIcons.Applications size={16} />;
  if (isComponent(row)) return <DomainIcons.Component size={16} />;
  if (isViolation(row)) return <DomainIcons.Policies size={16} />;
  if (isWaiver(row)) return <DomainIcons.Waivers size={16} />;
  return <DomainIcons.Vulnerability size={16} />;
}
