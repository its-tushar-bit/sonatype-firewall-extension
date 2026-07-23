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
  RadioGroup,
  ScrollArea,
  Slider,
  Text,
  TextField,
} from '@radix-ui/themes';
import { ActionIcons, NavIcons } from 'MainRoot/nosc/icons';
import {
  ViolationFilterSetGroup,
  ViolationsFilterState,
  ViolationsListFacets,
  ViolationThreatRange,
  ViolationWaiverType,
  VIOLATION_THREAT_MAX,
  VIOLATION_THREAT_MIN,
} from 'MainRoot/nosc/violations/violationListTypes';
import {
  hasActiveViolationFilters,
  stageLabel,
  threatCategoryLabel,
  violationStateLabel,
  waiverTypeLabel,
  WAIVER_TYPE_AUTO,
  WAIVER_TYPE_MANUAL,
} from 'MainRoot/nosc/violations/violationsListApi';
import './ViolationsFilterRail.scss';

/** Collapsed facet rows before See more (keeps long org/app facet lists usable). */
export const FACET_COLLAPSE_LIMIT = 8;

/**
 * Matches {@code ViolationsListFacetsBuilder.MAX_ORGANIZATION_FACETS} /
 * {@code MAX_APPLICATION_FACETS} (default 15). When the facet map reaches this size the list is
 * likely truncated server-side — surface that so estate-scale users are not left guessing.
 */
export const FACET_SERVER_CAP = 15;

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
  /** Select a waiver-type narrowing (Any / Auto-waived / Manually waived). */
  readonly onWaiverTypeChange: (waiverType: ViolationWaiverType) => void;
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
              <Flex align="start" gap="2" className="nosc-violations-filter-option-row">
                <Checkbox
                  checked={selected.has(id)}
                  onCheckedChange={() => onToggle(group, id)}
                  data-testid={`${testId}-option-${id}`}
                />
                <span className="nosc-violations-filter-option-label" title={label}>
                  {label}
                </span>
                {count > 0 && (
                  <Badge size="1" color="gray" variant="soft" radius="full">
                    {count}
                  </Badge>
                )}
              </Flex>
            </Text>
          ))}
        </Flex>
      </ScrollArea>
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
  group,
  entries,
  selected,
  filtersActive,
  onToggle,
}: {
  readonly title: string;
  readonly testId: string;
  readonly group: ViolationFilterSetGroup;
  readonly entries: ReadonlyArray<FacetEntry>;
  readonly selected: ReadonlySet<string>;
  /** True when any Violations filter is active (not only this group's selection). */
  readonly filtersActive: boolean;
  readonly onToggle: (group: ViolationFilterSetGroup, id: string) => void;
}): JSX.Element | null {
  const [query, setQuery] = useState('');
  const [expanded, setExpanded] = useState(false);
  const selectionEmpty = selected.size === 0;
  // Intentional (differs from Applications keeping mid-search text): when this group's selection
  // empties, drop local search so Reset / uncheck-last cannot leave a filtered-empty facet list.
  useEffect(() => {
    if (selectionEmpty) setQuery('');
  }, [selectionEmpty]);
  // Collapse See more whenever the whole rail returns to the default filter state. Depends on
  // filtersActive (not this group's selectionEmpty) so Reset still collapses when Orgs/Apps were
  // expanded with zero items selected in that group.
  useEffect(() => {
    if (!filtersActive) setExpanded(false);
  }, [filtersActive]);
  if (entries.length === 0) return null;

  const trimmed = query.trim().toLowerCase();
  const filtered = trimmed
    ? entries.filter(
        (entry) => entry.label.toLowerCase().includes(trimmed) || selected.has(entry.id),
      )
    : entries;

  const searching = trimmed.length > 0;
  // Intentional: clearing search keeps `expanded` so the user is not forced to re-click See more.
  const visible = searching || expanded
    ? filtered
    : collapseFacetEntries(filtered, selected, FACET_COLLAPSE_LIMIT);
  const canToggleCollapse = !searching && filtered.length > FACET_COLLAPSE_LIMIT;
  // Server returns at most FACET_SERVER_CAP org/app buckets; search only filters that set.
  const serverCapped = entries.length >= FACET_SERVER_CAP;

  return (
    <fieldset className="nosc-violations-filter-group" data-testid={testId}>
      <legend className="nosc-violations-filter-legend">{title}</legend>
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
          {/* Scroll cap for both collapsed and expanded lists so estate-scale org/app facets
              cannot blow past the rail when the user opens See more. */}
          <ScrollArea type="auto" scrollbars="vertical" className="nosc-violations-filter-scroll">
            <Flex direction="column" gap="1" pr="2">
              {visible.map(({ id, label, count }) => (
                <Text key={id} as="label" size="2" color="gray" className="nosc-violations-filter-option">
                  <Flex align="start" gap="2" className="nosc-violations-filter-option-row">
                    <Checkbox
                      checked={selected.has(id)}
                      onCheckedChange={() => onToggle(group, id)}
                      data-testid={`${testId}-option-${id}`}
                    />
                    <span className="nosc-violations-filter-option-label" title={label}>
                      {label}
                    </span>
                    {count > 0 && (
                      <Badge size="1" color="gray" variant="soft" radius="full">
                        {count}
                      </Badge>
                    )}
                  </Flex>
                </Text>
              ))}
            </Flex>
          </ScrollArea>
          {serverCapped && (
            <Text size="1" color="gray" data-testid={`${testId}-server-capped`}>
              Showing top {FACET_SERVER_CAP} {title.toLowerCase()} by violation count. Narrow other
              filters to surface more; search only filters this list.
            </Text>
          )}
          {canToggleCollapse && (
            <Button
              type="button"
              variant="ghost"
              color="blue"
              size="1"
              className="nosc-violations-filter-see-more"
              aria-expanded={expanded}
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
 * Waiver-type single-select (CLM-42261). A radio (not checkboxes) because the three options are
 * mutually exclusive: Any (no narrowing), Auto-waived only, or Manually waived only. The Auto/Manual
 * options carry facet counts; counts narrow with the rest of the query (V1 narrowing), so a selected
 * option's sibling count can drop to zero — the option stays selectable so the user can switch back.
 * Hidden entirely when there are no waived violations in scope and nothing is selected.
 */
/** One waiver-type radio row. Hoisted (not defined inside WaiverTypeSection) so it isn't re-allocated
 * per render and can be unit-tested/reasoned about in isolation. */
function waiverTypeOptionRow(
  testId: string,
  optionValue: ViolationWaiverType,
  label: string,
  count: number | undefined,
  idSuffix: string,
): JSX.Element {
  return (
    <Text as="label" size="2" color="gray" className="nosc-violations-filter-option">
      <Flex align="center" gap="2" className="nosc-violations-filter-option-row">
        <RadioGroup.Item value={optionValue} data-testid={`${testId}-option-${idSuffix}`} />
        <span className="nosc-violations-filter-option-label">{label}</span>
        {count !== undefined && (
          <Badge size="1" color="gray" variant="soft" radius="full">
            {count}
          </Badge>
        )}
      </Flex>
    </Text>
  );
}

function WaiverTypeSection({
  facets,
  value,
  onWaiverTypeChange,
  testId,
}: {
  readonly facets?: ViolationsListFacets;
  readonly value: ViolationWaiverType;
  readonly onWaiverTypeChange: (waiverType: ViolationWaiverType) => void;
  readonly testId: string;
}): JSX.Element | null {
  const autoCount = facets?.waiverTypes?.[WAIVER_TYPE_AUTO];
  const manualCount = facets?.waiverTypes?.[WAIVER_TYPE_MANUAL];
  const hasWaiverData = autoCount !== undefined || manualCount !== undefined;
  if (!hasWaiverData && value === 'ANY') return null;

  const legendId = `${testId}-legend`;
  return (
    <fieldset className="nosc-violations-filter-group" data-testid={testId}>
      <legend id={legendId} className="nosc-violations-filter-legend">
        Waiver Type
      </legend>
      <RadioGroup.Root
        value={value}
        onValueChange={(next) => onWaiverTypeChange(next as ViolationWaiverType)}
        aria-labelledby={legendId}
        data-testid={`${testId}-radio`}
      >
        <Flex direction="column" gap="1">
          {waiverTypeOptionRow(testId, 'ANY', 'Any', undefined, 'any')}
          {waiverTypeOptionRow(testId, 'AUTO', waiverTypeLabel(WAIVER_TYPE_AUTO), autoCount, 'auto')}
          {waiverTypeOptionRow(testId, 'MANUAL', waiverTypeLabel(WAIVER_TYPE_MANUAL), manualCount, 'manual')}
        </Flex>
      </RadioGroup.Root>
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
    setLiveRange((current) => (
      current[0] === range[0] && current[1] === range[1]
        ? current
        : [range[0], range[1]]
    ));
    // Value-based deps (not `range`) so a new array with the same min/max does not reset the thumbs.
    // eslint-disable-next-line react-hooks/exhaustive-deps -- intentional primitive deps
  }, [range[0], range[1]]);

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
  onWaiverTypeChange,
  onThreatRangeChange,
  onReset,
  idPrefix = 'violations-filter',
}: ViolationsFilterRailProps): JSX.Element {
  const orgLabel = (id: string): string => labels?.organizations[id] ?? id;
  const appLabel = (id: string): string => labels?.applications[id] ?? id;
  const filtersActive = hasActiveViolationFilters(selected);

  return (
    <Box asChild className="nosc-violations-filter-rail" data-testid={`${idPrefix}-rail`}>
      <aside aria-label="Violation filters">
        <Flex align="center" justify="start" mb="4">
          <Button
            variant="outline"
            color="gray"
            size="2"
            disabled={!filtersActive}
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
          <WaiverTypeSection
            facets={facets}
            value={selected.waiverType}
            onWaiverTypeChange={onWaiverTypeChange}
            testId={`${idPrefix}-waiver-type`}
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
            filtersActive={filtersActive}
            onToggle={onToggle}
          />
          <SearchableFilterSection
            title="Applications"
            testId={`${idPrefix}-applications`}
            group="applicationIds"
            entries={toEntries(facets?.applications, selected.applicationIds, appLabel)}
            selected={selected.applicationIds}
            filtersActive={filtersActive}
            onToggle={onToggle}
          />
        </Flex>
      </aside>
    </Box>
  );
}
