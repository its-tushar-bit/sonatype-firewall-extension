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
  ScrollArea,
  Slider,
  Text,
  TextField,
} from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import {
  ComponentsFilterFacetCounts,
  ComponentsFilterFacetEntry,
} from 'MainRoot/nosc/componentsList/componentListTypes';
import {
  collapseFacetEntries,
  COMPONENTS_THREAT_MAX,
  COMPONENTS_THREAT_MIN,
  ComponentsFilterSetGroup,
  ComponentsListFilterState,
  ComponentsThreatRange,
  FACET_COLLAPSE_LIMIT,
  normalizeComponentsThreatRange,
} from 'MainRoot/nosc/componentsList/componentsListFilters';
import type { ComponentsTab } from 'MainRoot/nosc/componentsList/componentsRoute';
import './ComponentsFilterRail.scss';

export interface ComponentsFilterRailProps {
  readonly tab: ComponentsTab;
  readonly facets: ComponentsFilterFacetCounts;
  readonly filters: ComponentsListFilterState;
  readonly hasActiveFilters: boolean;
  readonly onToggleFilter: (group: ComponentsFilterSetGroup, id: string) => void;
  readonly onThreatRangeChange: (range: ComponentsThreatRange) => void;
  readonly onResetFilters: () => void;
}

function ThreatLevelSection({
  range,
  onThreatRangeChange,
}: {
  readonly range: ComponentsThreatRange;
  readonly onThreatRangeChange: (range: ComponentsThreatRange) => void;
}): JSX.Element {
  const [liveRange, setLiveRange] = useState<[number, number]>([range[0], range[1]]);
  useEffect(() => {
    setLiveRange((current) => (
      current[0] === range[0] && current[1] === range[1]
        ? current
        : [range[0], range[1]]
    ));
  }, [range[0], range[1]]);

  const legendId = 'components-filter-threat-level-legend';
  return (
    <fieldset
      className="nosc-components-filter-group"
      data-testid="components-filter-threat-level"
    >
      <legend id={legendId} className="nosc-components-filter-legend">
        Policy Threat Level
      </legend>
      <Slider
        min={COMPONENTS_THREAT_MIN}
        max={COMPONENTS_THREAT_MAX}
        step={1}
        value={liveRange}
        onValueChange={(next) => setLiveRange([...normalizeComponentsThreatRange(next)])}
        onValueCommit={(next) => onThreatRangeChange(normalizeComponentsThreatRange(next))}
        data-testid="components-filter-threat-level-slider"
        aria-labelledby={legendId}
      />
      <Flex justify="between" mt="2">
        <Text size="1" color="gray">
          {COMPONENTS_THREAT_MIN}
        </Text>
        <Text size="1" color="gray" data-testid="components-filter-threat-level-value">
          {liveRange[0]} – {liveRange[1]}
        </Text>
        <Text size="1" color="gray">
          {COMPONENTS_THREAT_MAX}
        </Text>
      </Flex>
    </fieldset>
  );
}

/**
 * Searchable facet section with Applications-pattern See more / See less (limit 8).
 */
function SearchableFilterSection({
  title,
  testId,
  group,
  entries,
  selected,
  onToggle,
}: {
  readonly title: string;
  readonly testId: string;
  readonly group: ComponentsFilterSetGroup;
  readonly entries: ReadonlyArray<ComponentsFilterFacetEntry>;
  readonly selected: ReadonlySet<string>;
  readonly onToggle: (group: ComponentsFilterSetGroup, id: string) => void;
}): JSX.Element | null {
  const [query, setQuery] = useState('');
  const [expanded, setExpanded] = useState(false);
  const selectionEmpty = selected.size === 0;

  useEffect(() => {
    if (selectionEmpty) setQuery('');
  }, [selectionEmpty]);

  useEffect(() => {
    setExpanded(false);
  }, [query]);

  if (entries.length === 0) return null;

  const trimmed = query.trim().toLowerCase();
  const filtered = trimmed
    ? entries.filter((entry) => entry.label.toLowerCase().includes(trimmed))
    : entries;
  const visible = collapseFacetEntries(filtered, selected, FACET_COLLAPSE_LIMIT, expanded);
  const canToggleExpand = filtered.length > FACET_COLLAPSE_LIMIT;

  return (
    <fieldset className="nosc-components-filter-group" data-testid={testId}>
      <legend className="nosc-components-filter-legend">{title}</legend>
      <TextField.Root
        size="1"
        placeholder={`Search ${title.toLowerCase()}…`}
        value={query}
        onChange={(event) => setQuery(event.target.value)}
        data-testid={`${testId}-search`}
        aria-label={`Search ${title}`}
        mb="2"
      >
        <TextField.Slot>
          <ActionIcons.Search size={12} />
        </TextField.Slot>
      </TextField.Root>
      <ScrollArea type="auto" scrollbars="vertical" className="nosc-components-filter-scroll">
        <Flex direction="column" gap="1" pr="2">
          {visible.map(({ id, label, count }) => (
            <Text key={id} as="label" size="2" color="gray" className="nosc-components-filter-option">
              <Flex align="start" gap="2" className="nosc-components-filter-option-row">
                <Checkbox
                  checked={selected.has(id)}
                  onCheckedChange={() => onToggle(group, id)}
                  data-testid={`${testId}-option-${id}`}
                />
                <span className="nosc-components-filter-option-label">{label}</span>
                {count > 0 && (
                  <Badge size="1" color="gray" variant="soft" radius="full" style={{ flexShrink: 0 }}>
                    {count}
                  </Badge>
                )}
              </Flex>
            </Text>
          ))}
        </Flex>
      </ScrollArea>
      {canToggleExpand && (
        <>
          {!expanded ? (
            <Button
              variant="ghost"
              color="gray"
              size="1"
              mt="1"
              onClick={() => setExpanded(true)}
              data-testid={`${testId}-see-more`}
            >
              See more
            </Button>
          ) : (
            <Button
              variant="ghost"
              color="gray"
              size="1"
              mt="1"
              onClick={() => setExpanded(false)}
              data-testid={`${testId}-see-less`}
            >
              See less
            </Button>
          )}
        </>
      )}
    </fieldset>
  );
}

/**
 * Filter sidebar for the Martha V1 Components page (CLM-42214 / CLM-43960).
 * Ecosystems on Catalog only (My Scan Data dashboard list has no ecosystem facets yet).
 * Organizations, Applications, Stages, and Policy Threat Level on My Scan Data only — the Catalog
 * source rejects those estate dimensions outright (CLM-43211).
 */
export default function ComponentsFilterRail({
  tab,
  facets,
  filters,
  hasActiveFilters,
  onToggleFilter,
  onThreatRangeChange,
  onResetFilters,
}: ComponentsFilterRailProps): JSX.Element {
  const showEcosystems = tab === 'catalog';
  const showEstateScope = tab === 'myScanData';

  return (
    <Box asChild className="nosc-components-filter-rail" data-testid="components-filter-rail">
      <aside aria-label="Component filters">
        <Flex align="center" justify="start" mb="4">
          <Button
            variant="outline"
            color="gray"
            size="2"
            disabled={!hasActiveFilters}
            onClick={onResetFilters}
            data-testid="components-filter-reset"
          >
            <ActionIcons.Refresh size={12} />
            Reset filters
          </Button>
        </Flex>

        <Flex direction="column" gap="4">
          {showEcosystems && (
            <SearchableFilterSection
              title="Ecosystems"
              testId="components-filter-ecosystems"
              group="ecosystems"
              entries={facets.ecosystems}
              selected={filters.ecosystems}
              onToggle={onToggleFilter}
            />
          )}
          {showEstateScope && (
            <>
              <ThreatLevelSection
                range={filters.threatRange}
                onThreatRangeChange={onThreatRangeChange}
              />
              <SearchableFilterSection
                title="Organizations"
                testId="components-filter-organizations"
                group="organizations"
                entries={facets.organizations}
                selected={filters.organizations}
                onToggle={onToggleFilter}
              />
              <SearchableFilterSection
                title="Applications"
                testId="components-filter-applications"
                group="applications"
                entries={facets.applications}
                selected={filters.applications}
                onToggle={onToggleFilter}
              />
              <SearchableFilterSection
                title="Stages"
                testId="components-filter-stages"
                group="stages"
                entries={facets.stages}
                selected={filters.stages}
                onToggle={onToggleFilter}
              />
            </>
          )}
        </Flex>
      </aside>
    </Box>
  );
}
