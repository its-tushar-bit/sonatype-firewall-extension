/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import { Badge, Box, Button, Checkbox, Flex, Text, TextField } from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import type {
  WaiversFilterFacetCounts,
  WaiversFilterFacetEntry,
} from 'MainRoot/nosc/waivers/waiversListTypes';
import type {
  WaiversFilterSetGroup,
  WaiversListFilterState,
} from 'MainRoot/nosc/waivers/waiversListFilters';
import './WaiversFilterRail.scss';

/** Collapsed facet rows before "Show more" for searchable high-cardinality facets. */
export const WAIVERS_FILTER_COLLAPSED_COUNT = 5;

export interface WaiversFilterRailProps {
  readonly facets: WaiversFilterFacetCounts;
  readonly filters: WaiversListFilterState;
  readonly hasActiveFilters: boolean;
  readonly onToggleFilter: (group: WaiversFilterSetGroup, id: string) => void;
  readonly onResetFilters: () => void;
}

function FacetOptionRow({
  testId,
  group,
  id,
  label,
  count,
  selected,
  onToggle,
  showCount,
}: {
  readonly testId: string;
  readonly group: WaiversFilterSetGroup;
  readonly id: string;
  readonly label: string;
  readonly count: number;
  readonly selected: boolean;
  readonly onToggle: (group: WaiversFilterSetGroup, id: string) => void;
  readonly showCount: boolean;
}): JSX.Element {
  return (
    <Text as="label" size="2" color="gray" className="nosc-waivers-filter-option">
      <Flex align="start" gap="2" className="nosc-waivers-filter-option-row">
        <Checkbox
          checked={selected}
          onCheckedChange={() => onToggle(group, id)}
          data-testid={`${testId}-checkbox-${id}`}
        />
        <span className="nosc-waivers-filter-option-label">{label}</span>
        {showCount && count > 0 && (
          <Badge size="1" color="gray" variant="soft" radius="full" className="nosc-waivers-filter-option-count">
            {count}
          </Badge>
        )}
      </Flex>
    </Text>
  );
}

function CheckboxFilterSection({
  title,
  testId,
  group,
  entries,
  selected,
  onToggle,
  showCount,
  hint,
}: {
  readonly title: string;
  readonly testId: string;
  readonly group: WaiversFilterSetGroup;
  readonly entries: ReadonlyArray<WaiversFilterFacetEntry>;
  readonly selected: ReadonlySet<string>;
  readonly onToggle: (group: WaiversFilterSetGroup, id: string) => void;
  readonly showCount: boolean;
  readonly hint?: string;
}): JSX.Element | null {
  if (entries.length === 0) return null;
  return (
    <fieldset className="nosc-waivers-filter-group" data-testid={testId}>
      <legend className="nosc-waivers-filter-legend">{title}</legend>
      {hint && (
        <Text size="1" color="gray" mb="1" data-testid={`${testId}-hint`}>
          {hint}
        </Text>
      )}
      <Flex direction="column" gap="1">
        {entries.map(({ id, label, count }) => (
          <FacetOptionRow
            key={id}
            testId={testId}
            group={group}
            id={id}
            label={label}
            count={count}
            selected={selected.has(id)}
            onToggle={onToggle}
            showCount={showCount}
          />
        ))}
      </Flex>
    </fieldset>
  );
}

function SearchableFilterSection({
  title,
  testId,
  group,
  entries,
  selected,
  onToggle,
  showCount,
}: {
  readonly title: string;
  readonly testId: string;
  readonly group: WaiversFilterSetGroup;
  readonly entries: ReadonlyArray<WaiversFilterFacetEntry>;
  readonly selected: ReadonlySet<string>;
  readonly onToggle: (group: WaiversFilterSetGroup, id: string) => void;
  readonly showCount: boolean;
}): JSX.Element | null {
  const [query, setQuery] = useState('');
  const [expanded, setExpanded] = useState(false);
  const selectionEmpty = selected.size === 0;
  useEffect(() => {
    if (selectionEmpty) setQuery('');
  }, [selectionEmpty]);
  // New search results start collapsed so the rail stays short.
  useEffect(() => {
    setExpanded(false);
  }, [query]);
  if (entries.length === 0) return null;

  const trimmed = query.trim().toLowerCase();
  const matched = trimmed
    ? entries.filter(
        (entry) => entry.label.toLowerCase().includes(trimmed) || selected.has(entry.id),
      )
    : entries;

  const canToggle = matched.length > WAIVERS_FILTER_COLLAPSED_COUNT;
  const visible =
    !trimmed && !expanded && canToggle
      ? matched.slice(0, WAIVERS_FILTER_COLLAPSED_COUNT)
      : matched;
  const hiddenCount = matched.length - visible.length;

  return (
    <fieldset className="nosc-waivers-filter-group" data-testid={testId}>
      <legend className="nosc-waivers-filter-legend">{title}</legend>
      <TextField.Root
        size="1"
        placeholder={`Search ${title.toLowerCase()}...`}
        aria-label={`Search ${title.toLowerCase()}`}
        value={query}
        onChange={(event) => setQuery(event.target.value)}
        data-testid={`${testId}-search`}
        mb="2"
      >
        <TextField.Slot>
          <ActionIcons.Search size={14} />
        </TextField.Slot>
      </TextField.Root>
      {matched.length === 0 ? (
        <Text size="1" color="gray" data-testid={`${testId}-empty`}>
          No matches.
        </Text>
      ) : (
        <Flex direction="column" gap="1">
          {visible.map(({ id, label, count }) => (
            <FacetOptionRow
              key={id}
              testId={testId}
              group={group}
              id={id}
              label={label}
              count={count}
              selected={selected.has(id)}
              onToggle={onToggle}
              showCount={showCount}
            />
          ))}
          {canToggle && !trimmed && (
            <Button
              type="button"
              variant="ghost"
              color="gray"
              size="1"
              className="nosc-waivers-filter-show-more"
              onClick={() => setExpanded((value) => !value)}
              data-testid={`${testId}-show-more`}
            >
              {expanded ? 'Show less' : `Show more (${hiddenCount})`}
            </Button>
          )}
        </Flex>
      )}
    </fieldset>
  );
}

/**
 * Filter sidebar for the Ana Waivers list (CLM-43204 / CLM-43962). Vision-ordered facet groups
 * for existing Ana facets only (no new facet kinds). Never offers an Excluded chip.
 * High-cardinality facets use in-section search + Show more/less (no clipped scrollbar labels).
 */
export default function WaiversFilterRail({
  facets,
  filters,
  hasActiveFilters,
  onToggleFilter,
  onResetFilters,
}: WaiversFilterRailProps): JSX.Element {
  return (
    <Box asChild className="nosc-waivers-filter-rail" data-testid="waivers-filter-rail">
      <aside aria-label="Waiver filters">
        <Flex align="center" justify="start" mb="4">
          <Button
            variant="outline"
            color="gray"
            size="2"
            disabled={!hasActiveFilters}
            onClick={onResetFilters}
            data-testid="waivers-filter-reset"
          >
            <ActionIcons.Refresh size={12} />
            Reset to default view
          </Button>
        </Flex>

        <Flex direction="column" gap="4">
          <CheckboxFilterSection
            title="Waiver State"
            testId="waivers-filter-state"
            group="waiverStateIds"
            entries={facets.waiverStates}
            selected={filters.waiverStateIds}
            onToggle={onToggleFilter}
            showCount={false}
          />

          <CheckboxFilterSection
            title="Status"
            testId="waivers-filter-status"
            group="lifecycleStatusIds"
            entries={facets.lifecycleStatuses}
            selected={filters.lifecycleStatusIds}
            onToggle={onToggleFilter}
            showCount={false}
          />

          <CheckboxFilterSection
            title="Auto vs Manual"
            testId="waivers-filter-auto"
            group="autoStatusIds"
            entries={facets.autoStatuses}
            selected={filters.autoStatusIds}
            onToggle={onToggleFilter}
            showCount={false}
          />

          <CheckboxFilterSection
            title="Policy Threat"
            testId="waivers-filter-threat-level"
            group="threatLevelIds"
            entries={facets.threatLevels}
            selected={filters.threatLevelIds}
            onToggle={onToggleFilter}
            showCount={false}
            hint="Applied as a continuous min–max range of the selected buckets."
          />

          <SearchableFilterSection
            title="Organizations"
            testId="waivers-filter-organizations"
            group="organizationIds"
            entries={facets.organizations}
            selected={filters.organizationIds}
            onToggle={onToggleFilter}
            showCount
          />

          <SearchableFilterSection
            title="Applications"
            testId="waivers-filter-applications"
            group="applicationIds"
            entries={facets.applications}
            selected={filters.applicationIds}
            onToggle={onToggleFilter}
            showCount
          />

          <CheckboxFilterSection
            title="Scope"
            testId="waivers-filter-scope"
            group="scopeIds"
            entries={facets.scopes}
            selected={filters.scopeIds}
            onToggle={onToggleFilter}
            showCount
          />

          <CheckboxFilterSection
            title="Policy Types"
            testId="waivers-filter-policy-type"
            group="policyTypeIds"
            entries={facets.policyTypes}
            selected={filters.policyTypeIds}
            onToggle={onToggleFilter}
            showCount
          />

          <SearchableFilterSection
            title="Policies"
            testId="waivers-filter-policies"
            group="policyIds"
            entries={facets.policies}
            selected={filters.policyIds}
            onToggle={onToggleFilter}
            showCount
          />
        </Flex>
      </aside>
    </Box>
  );
}
