/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useMemo, useState } from 'react';
import { Button, Flex, Text, TextField, VisuallyHidden } from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { getNewestRisksExportUrl } from 'MainRoot/util/CLMLocation';
import { ViolationsFilterState } from 'MainRoot/nosc/violations/violationListTypes';
import { buildViolationsListExportPayload } from 'MainRoot/nosc/violations/violationsListExport';
import { hasActiveViolationFilters } from 'MainRoot/nosc/violations/violationsListApi';

export interface ViolationsToolbarProps {
  readonly totalCount: number;
  /** Current committed search term (controlled from the container). */
  readonly searchValue: string;
  /** Called with the trimmed term when the user submits the search (Enter). */
  readonly onSearchSubmit: (term: string) => void;
  /** Sidebar filter selection — drives the CSV export payload. */
  readonly filters: ViolationsFilterState;
}

/**
 * Toolbar row for Martha V1 Violations. Search submits on Enter and drives the server query via the
 * list API's {@code search} field. CSV export posts the active sidebar filters to the Classic
 * {@code /rest/dashboard/export/newestRisks} endpoint, which streams the full filtered result set as
 * the canonical 9-column violations CSV. Free-text search is index-only and is not included in the
 * export (a title + screen-reader hint says so). Because the export ignores search, disable/title
 * must not key off the search-narrowed {@code totalCount} alone — an active sidebar filter set still
 * yields a valid filter-only export even when search matches zero rows. Sort is fixed to
 * highest-threat-first (the only order the list API supports), so it stays an informational label
 * rather than a control.
 */
export default function ViolationsToolbar({
  totalCount,
  searchValue,
  onSearchSubmit,
  filters,
}: ViolationsToolbarProps): JSX.Element {
  const [draft, setDraft] = useState(searchValue);

  // Keep the local draft in sync when the committed value changes elsewhere (e.g. reset).
  useEffect(() => {
    setDraft(searchValue);
  }, [searchValue]);

  const exportPayloadJson = useMemo(
    () => JSON.stringify(buildViolationsListExportPayload(filters)),
    [filters],
  );
  const hasResults = totalCount > 0;
  const hasActiveSearch = searchValue.trim().length > 0;
  const hasFilters = hasActiveViolationFilters(filters);
  // Export omits free-text search and streams the filter-only set, so disable/title must not key off the
  // search-narrowed totalCount alone. Enable when the visible list has rows OR the sidebar has active
  // filters (whose export still yields rows even if search narrowed the view to zero). Deliberately stay
  // disabled when a zero-result search has no filters — an active search alone must not enable an
  // export-everything action (that would stream the entire unfiltered set the user never asked for).
  const canExport = hasResults || hasFilters;
  // When a search is active the caveat also flags the fully-unfiltered case, where the export would
  // include every violation rather than just what the search suggested.
  const searchCaveat = hasFilters
    ? 'Exports sidebar filters only — search term is not included in the CSV'
    : 'Search is not included in the CSV and no sidebar filters are active, so all violations are exported';
  const csvExportTitle = !canExport
    ? 'No violations to export'
    : hasActiveSearch
      ? searchCaveat
      : undefined;
  // Screen readers get the same caveat/empty explanation the sighted `title` conveys, in both states.
  const csvHintId = !canExport
    ? 'violations-toolbar-csv-empty-hint'
    : hasActiveSearch
      ? 'violations-toolbar-csv-search-hint'
      : undefined;

  return (
    <Flex align="center" justify="between" gap="3" wrap="wrap" data-testid="violations-toolbar">
      <Flex align="center" gap="3" flexGrow="1" minWidth="240px">
        {/* The sort indicator is informational, so it lives outside role="search" — a screen reader
            should not announce it as part of the search form's accessible name/description. The list
            API supports only the highest-threat-first order, so this stays a label, not a control. */}
        <form
          role="search"
          onSubmit={(event) => {
            event.preventDefault();
            onSearchSubmit(draft.trim());
          }}
          // Span the full content width (no maxWidth cap) so the search bar fills the row per the
          // Lifecycle V1 prototype; flex:1 lets it grow next to the sort text.
          style={{ flex: 1 }}
        >
          <TextField.Root
            placeholder="Search component, application, organization, policy..."
            aria-label="Search violations"
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            data-testid="violations-toolbar-search"
            style={{ width: '100%' }}
          >
            <TextField.Slot>
              <ActionIcons.Search size={16} />
            </TextField.Slot>
          </TextField.Root>
        </form>
        <Text size="2" color="gray" data-testid="violations-toolbar-sort">
          Sort: Threat (highest first)
        </Text>
      </Flex>

      <Flex align="center" gap="3">
        {/* CSV export posts the active sidebar filters (not the free-text search) to the Classic
            violations export, which returns the full filtered set. Disabled with an explanatory hint
            when there is nothing to export. */}
        <form
          action={getNewestRisksExportUrl()}
          method="post"
          encType="multipart/form-data"
          data-testid="violations-toolbar-export-form"
        >
          <input type="hidden" name="filter" value={exportPayloadJson} />
          <Button
            variant="outline"
            color="gray"
            size="2"
            type="submit"
            disabled={!canExport}
            title={csvExportTitle}
            aria-describedby={csvHintId}
            data-testid="violations-toolbar-csv"
          >
            <ActionIcons.Download size={14} />
            CSV
          </Button>
        </form>
        {!canExport && (
          <VisuallyHidden id="violations-toolbar-csv-empty-hint">
            CSV export is unavailable when there are no violations.
          </VisuallyHidden>
        )}
        {canExport && hasActiveSearch && (
          <VisuallyHidden id="violations-toolbar-csv-search-hint">{searchCaveat}</VisuallyHidden>
        )}
        <Text size="2" color="gray" data-testid="violations-toolbar-count">
          {totalCount} {totalCount === 1 ? 'violation' : 'violations'}
        </Text>
      </Flex>
    </Flex>
  );
}
