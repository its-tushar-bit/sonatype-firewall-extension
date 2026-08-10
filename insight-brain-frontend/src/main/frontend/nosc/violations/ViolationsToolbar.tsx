/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useMemo, useState } from 'react';
import { Button, Flex, Select, Text, TextField, VisuallyHidden } from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { getNewestRisksExportUrl } from 'MainRoot/util/CLMLocation';
import { ViolationsFilterState } from 'MainRoot/nosc/violations/violationListTypes';
import { buildViolationsListExportPayload } from 'MainRoot/nosc/violations/violationsListExport';
import { hasExportableViolationFilters } from 'MainRoot/nosc/violations/violationsListApi';
import type { ViolationsListOrderBy } from 'MainRoot/nosc/violations/violationsListQuery';
import { violationsListOrderByLabel } from 'MainRoot/nosc/violations/violationsListQuery';

export interface ViolationsToolbarProps {
  readonly totalCount: number;
  /** Current committed search term (controlled from the container). */
  readonly searchValue: string;
  /** Called with the trimmed term when the user submits the search (Enter). */
  readonly onSearchSubmit: (term: string) => void;
  /** When omitted (e.g. Legal), the threat sort control is hidden. */
  readonly orderBy?: ViolationsListOrderBy;
  readonly onOrderByChange?: (orderBy: ViolationsListOrderBy) => void;
  /** Sidebar filter selection — drives the CSV export payload. */
  readonly filters: ViolationsFilterState;
  /** When true, hide the Classic violations CSV export (Legal V1 has no legal CSV yet). */
  readonly hideCsvExport?: boolean;
  /**
   * Plural result noun for the count label and search aria (default: violation/violations).
   * Legal passes {@code "license risk findings"}.
   */
  readonly resultNoun?: string;
  /** Singular form used when {@link resultNoun} is set and {@code totalCount === 1}. */
  readonly resultNounSingular?: string;
}

/**
 * Toolbar row for Martha V1 Violations. Search submits on Enter and drives the server query via the
 * list API's {@code search} field. Sort exposes backend {@code ±policyThreatLevel}. CSV export posts
 * the active sidebar filters to Classic {@code /rest/dashboard/export/newestRisks}.
 */
export default function ViolationsToolbar({
  totalCount,
  searchValue,
  onSearchSubmit,
  orderBy,
  onOrderByChange,
  filters,
  hideCsvExport = false,
  resultNoun,
  resultNounSingular,
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

  const countLabel = resultNoun
    ? `${totalCount} ${
        totalCount === 1 ? (resultNounSingular ?? resultNoun) : resultNoun
      }`
    : `${totalCount} ${totalCount === 1 ? 'violation' : 'violations'}`;
  const searchAriaLabel = resultNoun ? `Search ${resultNoun}` : 'Search violations';

  return (
    <Flex align="center" justify="between" gap="3" wrap="wrap" data-testid="violations-toolbar">
      <Flex align="center" gap="3" flexGrow="1" minWidth="240px">
        <form
          role="search"
          onSubmit={(event) => {
            event.preventDefault();
            onSearchSubmit(draft.trim());
          }}
          // Span the full content width (no maxWidth cap) so the search bar fills the row per the
          // Lifecycle V1 prototype; flex:1 lets it grow next to the sort control.
          style={{ flex: 1 }}
        >
          <TextField.Root
            placeholder="Search component, application, organization, policy..."
            aria-label={searchAriaLabel}
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
        {orderBy != null && onOrderByChange != null && (
          <Flex align="center" gap="2" data-testid="violations-toolbar-sort">
            <Text size="2" color="gray" as="label" htmlFor="violations-toolbar-sort-select">
              Sort
            </Text>
            <Select.Root
              value={orderBy}
              onValueChange={(value) => onOrderByChange(value as ViolationsListOrderBy)}
            >
              <Select.Trigger
                id="violations-toolbar-sort-select"
                variant="soft"
                color="gray"
              />
              {/* Popper: item-aligned Select collapses to ~1 option inside this page's overflow shell. */}
              <Select.Content position="popper">
                <Select.Item value="-policyThreatLevel">
                  {violationsListOrderByLabel('-policyThreatLevel')}
                </Select.Item>
                <Select.Item value="policyThreatLevel">
                  {violationsListOrderByLabel('policyThreatLevel')}
                </Select.Item>
              </Select.Content>
            </Select.Root>
          </Flex>
        )}
      </Flex>

      <Flex align="center" gap="3">
        {/* CSV export posts the active sidebar filters (not the free-text search) to the Classic
            violations export, which returns the full filtered set. Disabled with an explanatory hint
            when there is nothing to export. Hidden entirely when the host list has no CSV (Legal V1). */}
        {!hideCsvExport && (
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
        )}
        {!hideCsvExport && !canExport && (
          <VisuallyHidden id="violations-toolbar-csv-empty-hint">
            CSV export is unavailable when there are no violations.
          </VisuallyHidden>
        )}
        {!hideCsvExport && canExport && hasUnexportedNarrowing && (
          <VisuallyHidden id="violations-toolbar-csv-caveat-hint">{exportCaveat}</VisuallyHidden>
        )}
        <Text size="2" color="gray" data-testid="violations-toolbar-count">
          {countLabel}
        </Text>
      </Flex>
    </Flex>
  );
}
