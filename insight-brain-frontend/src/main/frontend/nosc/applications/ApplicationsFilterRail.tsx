/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import {
  Badge,
  Box,
  Button,
  Checkbox,
  Flex,
  Slider,
  Text,
  TextField,
} from '@radix-ui/themes';
import { ActionIcons, NavIcons } from 'MainRoot/nosc/icons';
import { ApplicationsFilterFacetCounts } from 'MainRoot/nosc/applications/applicationListTypes';
import {
  APPLICATIONS_THREAT_MAX,
  APPLICATIONS_THREAT_MIN,
  ApplicationsListFilterSetField,
  ApplicationsListFilterState,
  ApplicationsThreatRange,
  normalizeApplicationsThreatRange,
} from 'MainRoot/nosc/applications/applicationsListFilters';
import './ApplicationsFilterRail.scss';

/** Collapsed facet rows before See more (prototype parity). */
const FACET_COLLAPSE_LIMIT = 8;
/** Cap rendered facet rows while searching so large org/app estates stay responsive. */
const FACET_SEARCH_RESULT_LIMIT = 50;

export interface ApplicationsFilterRailProps {
  readonly facets: ApplicationsFilterFacetCounts;
  readonly filters: ApplicationsListFilterState;
  readonly hasActiveFilters: boolean;
  readonly onToggleFilter: (
    field: ApplicationsListFilterSetField,
    id: string,
  ) => void;
  readonly onThreatRangeChange: (range: ApplicationsThreatRange) => void;
  readonly onResetFilters: () => void;
}

type FacetEntry = {
  readonly id: string;
  readonly label: string;
  readonly count: number;
};

function FilterOption({
  label,
  count,
  checked,
  onToggle,
  testId,
}: {
  readonly label: string;
  readonly count: number;
  readonly checked: boolean;
  readonly onToggle: () => void;
  readonly testId?: string;
}): JSX.Element {
  return (
    <Text as="label" size="2" color="gray" className="nosc-applications-filter-option-label">
      <span className="nosc-applications-filter-option">
        <span className="nosc-applications-filter-option__checkbox">
          <Checkbox checked={checked} onCheckedChange={onToggle} data-testid={testId} />
        </span>
        <span className="nosc-applications-filter-option__label">{label}</span>
        {count > 0 && (
          <Badge
            className="nosc-applications-filter-option__count"
            size="1"
            color="gray"
            variant="soft"
            radius="full"
          >
            {count}
          </Badge>
        )}
      </span>
    </Text>
  );
}

function ThreatLevelSection({
  range,
  onThreatRangeChange,
}: {
  readonly range: ApplicationsThreatRange;
  readonly onThreatRangeChange: (range: ApplicationsThreatRange) => void;
}): JSX.Element {
  const [liveRange, setLiveRange] = useState<[number, number]>([range[0], range[1]]);
  useEffect(() => {
    setLiveRange((current) => (
      current[0] === range[0] && current[1] === range[1]
        ? current
        : [range[0], range[1]]
    ));
  }, [range[0], range[1]]);

  const legendId = 'applications-filter-threat-level-legend';
  return (
    <fieldset
      className="nosc-applications-filter-group"
      data-testid="applications-filter-threat-level"
    >
      <legend id={legendId} className="nosc-applications-filter-legend">
        Policy Threat Level
      </legend>
      <Slider
        min={APPLICATIONS_THREAT_MIN}
        max={APPLICATIONS_THREAT_MAX}
        step={1}
        value={liveRange}
        onValueChange={(next) => setLiveRange([...normalizeApplicationsThreatRange(next)])}
        onValueCommit={(next) => onThreatRangeChange(normalizeApplicationsThreatRange(next))}
        data-testid="applications-filter-threat-level-slider"
        aria-labelledby={legendId}
      />
      <Flex justify="between" mt="2">
        <Text size="1" color="gray">
          {APPLICATIONS_THREAT_MIN}
        </Text>
        <Text size="1" color="gray" data-testid="applications-filter-threat-level-value">
          {liveRange[0]} – {liveRange[1]}
        </Text>
        <Text size="1" color="gray">
          {APPLICATIONS_THREAT_MAX}
        </Text>
      </Flex>
    </fieldset>
  );
}

function collapseFacetEntries(
  entries: ReadonlyArray<FacetEntry>,
  selected: ReadonlySet<string>,
  limit: number,
): ReadonlyArray<FacetEntry> {
  if (entries.length <= limit) return entries;
  const selectedEntries = entries.filter((entry) => selected.has(entry.id));
  if (selectedEntries.length >= limit) return selectedEntries;
  const remaining = limit - selectedEntries.length;
  const unselected = entries.filter((entry) => !selected.has(entry.id));
  return [...selectedEntries, ...unselected.slice(0, remaining)];
}

function SearchableFilterSection({
  title,
  testId,
  field,
  entries,
  selected,
  onToggle,
}: {
  readonly title: string;
  readonly testId: string;
  readonly field: ApplicationsListFilterSetField;
  readonly entries: ReadonlyArray<FacetEntry>;
  readonly selected: ReadonlySet<string>;
  readonly onToggle: (field: ApplicationsListFilterSetField, id: string) => void;
}): JSX.Element | null {
  const [query, setQuery] = useState('');
  const [expanded, setExpanded] = useState(false);

  if (entries.length === 0) return null;

  const trimmed = query.trim().toLowerCase();
  const filtered = trimmed
    ? entries.filter(
        (entry) => entry.label.toLowerCase().includes(trimmed) || selected.has(entry.id),
      )
    : entries;

  const searching = trimmed.length > 0;
  const searchCapped = searching && filtered.length > FACET_SEARCH_RESULT_LIMIT;
  const visible = searching
    ? filtered.slice(0, FACET_SEARCH_RESULT_LIMIT)
    : expanded
      ? filtered
      : collapseFacetEntries(filtered, selected, FACET_COLLAPSE_LIMIT);
  const canToggleCollapse = !searching && filtered.length > FACET_COLLAPSE_LIMIT;

  return (
    <fieldset className="nosc-applications-filter-group" data-testid={testId}>
      <legend className="nosc-applications-filter-legend">{title}</legend>
      <TextField.Root
        size="1"
        placeholder="Search..."
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
      {filtered.length === 0 ? (
        <Text size="1" color="gray" data-testid={`${testId}-empty`}>
          No matches.
        </Text>
      ) : (
        <Flex direction="column" gap="1">
          {visible.map(({ id, label, count }) => (
            <FilterOption
              key={id}
              label={label}
              count={count}
              checked={selected.has(id)}
              onToggle={() => onToggle(field, id)}
              testId={`${testId}-option-${id}`}
            />
          ))}
          {searchCapped && (
            <Text size="1" color="gray" data-testid={`${testId}-search-capped`}>
              Showing first {FACET_SEARCH_RESULT_LIMIT} matches. Refine your search to see more.
            </Text>
          )}
          {canToggleCollapse && (
            <Button
              type="button"
              variant="ghost"
              color="blue"
              size="1"
              className="nosc-applications-filter-see-more"
              onClick={() => setExpanded((current) => !current)}
              data-testid={`${testId}-see-more`}
            >
              {expanded ? 'See less' : 'See more'}
              {expanded ? <NavIcons.Collapse size={14} /> : <NavIcons.Expand size={14} />}
            </Button>
          )}
        </Flex>
      )}
    </fieldset>
  );
}

/**
 * Filter sidebar for the Martha V1 Applications page.
 *
 * Selection state is local to the list page and refreshes via POST
 * /rest/dashboard/applications/list with stage/org/app/threat filters.
 */
export default function ApplicationsFilterRail({
  facets,
  filters,
  hasActiveFilters,
  onToggleFilter,
  onThreatRangeChange,
  onResetFilters,
}: ApplicationsFilterRailProps): JSX.Element {
  return (
    <Box asChild className="nosc-applications-filter-rail" data-testid="applications-filter-rail">
      <aside aria-label="Application filters">
        <Flex align="center" justify="start" mb="4">
          <Button
            variant="outline"
            color="gray"
            size="2"
            disabled={!hasActiveFilters}
            onClick={onResetFilters}
            data-testid="applications-filter-reset"
          >
            <ActionIcons.Refresh size={12} />
            Reset filters
          </Button>
        </Flex>

        <Flex direction="column" gap="4">
          <ThreatLevelSection
            range={filters.threatRange}
            onThreatRangeChange={onThreatRangeChange}
          />

          <fieldset className="nosc-applications-filter-group" data-testid="applications-filter-stages">
            <legend className="nosc-applications-filter-legend">Stages</legend>
            <Flex direction="column" gap="1">
              {facets.stages.map(({ id, label, count }) => (
                <FilterOption
                  key={id}
                  label={label}
                  count={count}
                  checked={filters.stageIds.has(id)}
                  onToggle={() => onToggleFilter('stageIds', id)}
                  testId={`applications-filter-stages-option-${id}`}
                />
              ))}
            </Flex>
          </fieldset>

          <SearchableFilterSection
            title="Organizations"
            testId="applications-filter-organizations"
            field="organizationIds"
            entries={facets.organizations}
            selected={filters.organizationIds}
            onToggle={onToggleFilter}
          />

          <SearchableFilterSection
            title="Applications"
            testId="applications-filter-applications"
            field="applicationIds"
            entries={facets.applications}
            selected={filters.applicationIds}
            onToggle={onToggleFilter}
          />
        </Flex>
      </aside>
    </Box>
  );
}
