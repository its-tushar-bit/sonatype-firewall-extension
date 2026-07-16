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
import { hasExportableViolationFilters } from 'MainRoot/nosc/violations/violationsListApi';

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
 * the canonical 9-column violations CSV. Free-text search and the auto/manual waiver-type filter are
 * index-only and are not included in the export (a title + screen-reader hint says so). Because the
 * export ignores both, disable/title must not key off the search/waiver-narrowed {@code totalCount}
 * alone — an active exportable sidebar filter still yields a valid filter-only export even when the
 * view is narrowed to zero rows. Sort is fixed to highest-threat-first (the only order the list API
 * supports), so it stays an informational label rather than a control.
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
  const hasWaiverTypeFilter = filters.waiverType !== 'ANY';
  // Filters the Classic export can actually apply (everything except waiverType, which is index-only).
  const hasExportableFilters = hasExportableViolationFilters(filters);
  // Narrowings the CSV can't honor — free-text search and the auto/manual waiver-type radio.
  const hasUnexportedNarrowing = hasActiveSearch || hasWaiverTypeFilter;
  // Export omits search + waiverType and streams the exportable-filter set, so disable/title must not key
  // off the (search/waiver-narrowed) totalCount alone. Enable when the visible list has rows OR an
  // exportable sidebar filter is active (whose export still yields rows even if search/waiver narrowed the
  // view to zero). Deliberately stay disabled when only unexported narrowing is active with zero results —
  // that must not enable an export-everything action the user never asked for.
  const canExport = hasResults || hasExportableFilters;
  // Name the active unexported narrowings so the caveat is specific about what the CSV drops.
  const unexported = [
    ...(hasActiveSearch ? ['search term'] : []),
    ...(hasWaiverTypeFilter ? ['waiver-type filter'] : []),
  ].join(' and ');
  // Flags the fully-unfiltered case too: with no exportable filters the CSV would stream every violation
  // rather than the narrowed view the search/waiver suggested.
  const exportCaveat = hasExportableFilters
    ? `Exports sidebar filters only — the ${unexported} won't be applied to the CSV`
    : `The ${unexported} won't be applied to the CSV and no other sidebar filters are active, so all violations are exported`;
  const csvExportTitle = !canExport
    ? 'No violations to export'
    : hasUnexportedNarrowing
      ? exportCaveat
      : undefined;
  // Screen readers get the same caveat/empty explanation the sighted `title` conveys, in both states.
  const csvHintId = !canExport
    ? 'violations-toolbar-csv-empty-hint'
    : hasUnexportedNarrowing
      ? 'violations-toolbar-csv-caveat-hint'
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
        {canExport && hasUnexportedNarrowing && (
          <VisuallyHidden id="violations-toolbar-csv-caveat-hint">{exportCaveat}</VisuallyHidden>
        )}
        <Text size="2" color="gray" data-testid="violations-toolbar-count">
          {totalCount} {totalCount === 1 ? 'violation' : 'violations'}
        </Text>
      </Flex>
    </Flex>
  );
}
