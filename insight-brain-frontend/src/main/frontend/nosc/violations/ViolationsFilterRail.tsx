/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import { Badge, Box, Button, Checkbox, Flex, ScrollArea, Slider, Text, TextField } from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import {
  ViolationFilterSetGroup,
  ViolationsFilterState,
  ViolationsListFacets,
  ViolationThreatRange,
  VIOLATION_THREAT_MAX,
  VIOLATION_THREAT_MIN,
} from 'MainRoot/nosc/violations/violationListTypes';
import {
  hasActiveViolationFilters,
  stageLabel,
  threatCategoryLabel,
  violationStateLabel,
} from 'MainRoot/nosc/violations/violationsListApi';
import './ViolationsFilterRail.scss';

export interface ViolationsFilterRailProps {
  readonly facets?: ViolationsListFacets;
  /**
   * id→display-name maps for the org / app facets (facet maps are id-keyed only; org/app rows carry
   * both id and name). Stage facets are labeled by {@link stageLabel} instead — the row-side stage is
   * a display name, not the id the facet is keyed by.
   */
  readonly labels?: {
    readonly organizations: Readonly<Record<string, string>>;
    readonly applications: Readonly<Record<string, string>>;
  };
  /** Current filter selection (controlled by the list container). */
  readonly selected: ViolationsFilterState;
  /** Toggle one option in a set-valued group. */
  readonly onToggle: (group: ViolationFilterSetGroup, id: string) => void;
  /** Commit a new [min, max] policy-threat-level range. */
  readonly onThreatRangeChange: (range: ViolationThreatRange) => void;
  /** Reset every group to its default (all cleared, threat range [0, 10]). */
  readonly onReset: () => void;
  /**
   * Disambiguates data-testids/input ids when the rail is rendered twice (desktop rail + mobile
   * drawer). Defaults to the desktop instance.
   */
  readonly idPrefix?: string;
}

type FacetEntry = { readonly id: string; readonly label: string; readonly count: number };

/** Only OPEN and WAIVED are validator-safe; LEGACY_VIOLATION is excluded from the facet UI. */
const SELECTABLE_VIOLATION_STATES = new Set(['OPEN', 'WAIVED']);

/**
 * Build sorted facet entries and fold in any currently-selected id the (post-filter) facet map no
 * longer returns, so a selected option never vanishes and can always be toggled back off.
 */
function toEntries(
  counts: Readonly<Record<string, number>> | undefined,
  selected: ReadonlySet<string>,
  labelFor: (id: string) => string,
): ReadonlyArray<FacetEntry> {
  const byId = new Map<string, number>(counts ? Object.entries(counts) : []);
  selected.forEach((id) => {
    if (!byId.has(id)) byId.set(id, 0);
  });
  return Array.from(byId.entries())
    .map(([id, count]) => ({ id, label: labelFor(id), count }))
    .sort((a, b) => a.label.localeCompare(b.label));
}

function CheckboxFilterSection({
  title,
  testId,
  group,
  entries,
  selected,
  onToggle,
}: {
  readonly title: string;
  readonly testId: string;
  readonly group: ViolationFilterSetGroup;
  readonly entries: ReadonlyArray<FacetEntry>;
  readonly selected: ReadonlySet<string>;
  readonly onToggle: (group: ViolationFilterSetGroup, id: string) => void;
}): JSX.Element | null {
  if (entries.length === 0) return null;
  return (
    <fieldset className="nosc-violations-filter-group" data-testid={testId}>
      <legend className="nosc-violations-filter-legend">{title}</legend>
      {/* Same height cap as the searchable sections so a growable facet (e.g. Stages) scrolls within
          the rail instead of stretching it; short lists (State/Policy Type) never trigger the scroll. */}
      <ScrollArea type="auto" scrollbars="vertical" className="nosc-violations-filter-scroll">
        <Flex direction="column" gap="1" pr="2">
          {entries.map(({ id, label, count }) => (
            <Text key={id} as="label" size="2" color="gray" className="nosc-violations-filter-option">
              <Flex align="center" gap="2" className="nosc-violations-filter-option-row">
                <Checkbox
                  checked={selected.has(id)}
                  onCheckedChange={() => onToggle(group, id)}
                  data-testid={`${testId}-option-${id}`}
                />
                <span className="nosc-violations-filter-option-label">{label}</span>
                <Badge size="1" color="gray" variant="soft" radius="full">
                  {count}
                </Badge>
              </Flex>
            </Text>
          ))}
        </Flex>
      </ScrollArea>
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
}: {
  readonly title: string;
  readonly testId: string;
  readonly group: ViolationFilterSetGroup;
  readonly entries: ReadonlyArray<FacetEntry>;
  readonly selected: ReadonlySet<string>;
  readonly onToggle: (group: ViolationFilterSetGroup, id: string) => void;
}): JSX.Element | null {
  const [query, setQuery] = useState('');
  const selectionEmpty = selected.size === 0;
  // When the parent clears this group's selection (Reset filters, or unchecking the last item), drop
  // the local search text too — otherwise the box keeps stale text and the list stays filtered/empty
  // even though no filter is active.
  useEffect(() => {
    if (selectionEmpty) setQuery('');
  }, [selectionEmpty]);
  if (entries.length === 0) return null;

  const trimmed = query.trim().toLowerCase();
  const visible = trimmed
    ? entries.filter(
        (entry) => entry.label.toLowerCase().includes(trimmed) || selected.has(entry.id),
      )
    : entries;

  return (
    <fieldset className="nosc-violations-filter-group" data-testid={testId}>
      <legend className="nosc-violations-filter-legend">{title}</legend>
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
      {visible.length === 0 ? (
        <Text size="1" color="gray" data-testid={`${testId}-empty`}>
          No matches.
        </Text>
      ) : (
        <ScrollArea type="auto" scrollbars="vertical" className="nosc-violations-filter-scroll">
          <Flex direction="column" gap="1" pr="2">
            {visible.map(({ id, label, count }) => (
              <Text key={id} as="label" size="2" color="gray" className="nosc-violations-filter-option">
                <Flex align="center" gap="2" className="nosc-violations-filter-option-row">
                  <Checkbox
                    checked={selected.has(id)}
                    onCheckedChange={() => onToggle(group, id)}
                    data-testid={`${testId}-option-${id}`}
                  />
                  <span className="nosc-violations-filter-option-label">{label}</span>
                  <Badge size="1" color="gray" variant="soft" radius="full">
                    {count}
                  </Badge>
                </Flex>
              </Text>
            ))}
          </Flex>
        </ScrollArea>
      )}
    </fieldset>
  );
}

/**
 * Coerce a raw Radix slider payload into an in-bounds, ascending [min, max] pair. Radix's two-thumb
 * Slider already emits a sorted pair for the current config, but normalizing here hardens the handlers
 * against a future config change (e.g. a single thumb, where next[1] would be undefined).
 */
function normalizeThreatRange(next: readonly number[]): ViolationThreatRange {
  const clamp = (n: number): number =>
    Math.min(VIOLATION_THREAT_MAX, Math.max(VIOLATION_THREAT_MIN, n ?? VIOLATION_THREAT_MIN));
  const a = clamp(next[0]);
  const b = clamp(next[1] ?? next[0]);
  return [Math.min(a, b), Math.max(a, b)];
}

function ThreatLevelSection({
  range,
  onThreatRangeChange,
  testId,
}: {
  readonly range: ViolationThreatRange;
  readonly onThreatRangeChange: (range: ViolationThreatRange) => void;
  readonly testId: string;
}): JSX.Element {
  // Track the thumbs live while dragging (so the value label follows the thumb), but only propagate
  // to the container — which refetches — on commit (pointer release / keyup), avoiding a request per
  // step. Re-sync when the controlled range changes elsewhere (e.g. Reset filters).
  const [liveRange, setLiveRange] = useState<[number, number]>([range[0], range[1]]);
  useEffect(() => {
    setLiveRange([range[0], range[1]]);
  }, [range]);

  const legendId = `${testId}-legend`;
  return (
    <fieldset className="nosc-violations-filter-group" data-testid={testId}>
      <legend id={legendId} className="nosc-violations-filter-legend">
        Policy Threat Level
      </legend>
      {/* Name the control from the visible legend rather than a duplicated string. Radix Themes'
          Slider spreads aria props onto the root, not each thumb's <input type="range">, so true
          per-thumb (min/max) names need the lower-level Radix primitive — deferred as a V1 a11y nit. */}
      <Slider
        min={VIOLATION_THREAT_MIN}
        max={VIOLATION_THREAT_MAX}
        step={1}
        value={liveRange}
        onValueChange={(next) => setLiveRange(normalizeThreatRange(next))}
        onValueCommit={(next) => onThreatRangeChange(normalizeThreatRange(next))}
        data-testid={`${testId}-slider`}
        aria-labelledby={legendId}
      />
      <Flex justify="between" mt="2">
        <Text size="1" color="gray">
          {VIOLATION_THREAT_MIN}
        </Text>
        <Text size="1" color="gray" data-testid={`${testId}-value`}>
          {liveRange[0]} – {liveRange[1]}
        </Text>
        <Text size="1" color="gray">
          {VIOLATION_THREAT_MAX}
        </Text>
      </Flex>
    </fieldset>
  );
}

/**
 * Interactive filter sidebar for Martha V1 Violations. Renders violation-state, policy-type,
 * threat-level, stage, organization, and application controls from the API facet maps and lifts every
 * change to the list container, which refetches. Age, application categories, and legacy state are
 * deferred until the index supports them.
 */
export default function ViolationsFilterRail({
  facets,
  labels,
  selected,
  onToggle,
  onThreatRangeChange,
  onReset,
  idPrefix = 'violations-filter',
}: ViolationsFilterRailProps): JSX.Element {
  const orgLabel = (id: string): string => labels?.organizations[id] ?? id;
  const appLabel = (id: string): string => labels?.applications[id] ?? id;

  return (
    <Box asChild className="nosc-violations-filter-rail" data-testid={`${idPrefix}-rail`}>
      <aside aria-label="Violation filters">
        <Flex align="center" justify="start" mb="4">
          <Button
            variant="outline"
            color="gray"
            size="2"
            disabled={!hasActiveViolationFilters(selected)}
            onClick={onReset}
            data-testid={`${idPrefix}-reset`}
          >
            <ActionIcons.Refresh size={12} />
            Reset filters
          </Button>
        </Flex>

        <Flex direction="column" gap="4">
          <CheckboxFilterSection
            title="Violation State"
            testId={`${idPrefix}-state`}
            group="states"
            entries={toEntries(facets?.states, selected.states, violationStateLabel).filter((entry) =>
              SELECTABLE_VIOLATION_STATES.has(entry.id),
            )}
            selected={selected.states}
            onToggle={onToggle}
          />
          <CheckboxFilterSection
            title="Policy Type"
            testId={`${idPrefix}-policy-type`}
            group="threatCategories"
            entries={toEntries(facets?.threatCategories, selected.threatCategories, threatCategoryLabel)}
            selected={selected.threatCategories}
            onToggle={onToggle}
          />
          <ThreatLevelSection
            range={selected.threatRange}
            onThreatRangeChange={onThreatRangeChange}
            testId={`${idPrefix}-threat`}
          />
          <CheckboxFilterSection
            title="Stages"
            testId={`${idPrefix}-stages`}
            group="stageIds"
            entries={toEntries(facets?.stages, selected.stageIds, stageLabel)}
            selected={selected.stageIds}
            onToggle={onToggle}
          />
          <SearchableFilterSection
            title="Organizations"
            testId={`${idPrefix}-organizations`}
            group="organizationIds"
            entries={toEntries(facets?.organizations, selected.organizationIds, orgLabel)}
            selected={selected.organizationIds}
            onToggle={onToggle}
          />
          <SearchableFilterSection
            title="Applications"
            testId={`${idPrefix}-applications`}
            group="applicationIds"
            entries={toEntries(facets?.applications, selected.applicationIds, appLabel)}
            selected={selected.applicationIds}
            onToggle={onToggle}
          />
        </Flex>
      </aside>
    </Box>
  );
}
