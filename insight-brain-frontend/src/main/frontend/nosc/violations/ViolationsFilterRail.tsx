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
  ApplicationCategoryOption,
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
import { normalizeRange } from 'MainRoot/nosc/util/normalizeRange';
import './ViolationsFilterRail.scss';

/** Collapsed facet rows before See more (keeps long org/app facet lists usable). */
export const FACET_COLLAPSE_LIMIT = 8;

/**
 * Matches {@code ViolationsListFacetsBuilder.MAX_ORGANIZATION_FACETS} /
 * {@code MAX_APPLICATION_FACETS} (default 15). When the uncapped top-by-count facet map reaches this
 * size, surface a note so estate-scale users know the list is truncated — and that typing in the
 * search box runs server-side name search beyond that top-N set (CLM-42912).
 */
export const FACET_SERVER_CAP = 15;

/** Align with server {@code NameHelper.normalize}: case-insensitive, whitespace-stripped. */
export function normalizeFacetSearchText(value: string): string {
  return value.replace(/\s+/g, '').toLowerCase();
}

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
  /** Controlled Organizations facet search text (debounced server-side by the list container). */
  readonly organizationFacetSearch?: string;
  readonly onOrganizationFacetSearchChange?: (query: string) => void;
  /** Controlled Applications facet search text (debounced server-side by the list container). */
  readonly applicationFacetSearch?: string;
  readonly onApplicationFacetSearchChange?: (query: string) => void;
  /**
   * Application Category options from {@code GET /api/v2/applicationCategories/application}
   * (Classic dashboard tags endpoint). Facets for categories are deferred on the list API, so the
   * rail uses this option list instead of a facet map. When empty/undefined and nothing is selected,
   * the section is hidden.
   */
  readonly applicationCategoryOptions?: ReadonlyArray<ApplicationCategoryOption>;
  /** Controlled client-side search for Application Categories (not sent to the list POST). */
  readonly applicationCategorySearch?: string;
  readonly onApplicationCategorySearchChange?: (query: string) => void;
  /**
   * Disambiguates data-testids/input ids when the rail is rendered twice (desktop rail + mobile
   * drawer). Defaults to the desktop instance.
   */
  readonly idPrefix?: string;
  /** When true, hide Violation State checkboxes (Legal findings have no OPEN/WAIVED). */
  readonly hideStateFilter?: boolean;
  /** When true, hide waiver-type radios (Legal findings have no waiver status). */
  readonly hideWaiverTypeFilter?: boolean;
  /** Title for the threat-category checkbox section (Legal: "License Threat Group"). */
  readonly threatCategorySectionTitle?: string;
  /**
   * When true, facet option labels are the raw category ids (LTG display names).
   * Default false uses {@link threatCategoryLabel} (Policy Type title-case).
   */
  readonly threatCategoryUseIdentityLabels?: boolean;
  /**
   * Override for whether Reset / mobile active-dot consider filters narrowed. When omitted, derived
   * from {@link hasActiveViolationFilters}.
   */
  readonly filtersActive?: boolean;
}

type FacetEntry = { readonly id: string; readonly label: string; readonly count: number };

/**
 * Validator-safe, filterable violation states. LEGACY_VIOLATION filters the pure-legacy population
 * (waived+legacy violations index as Waived by precedence and surface under WAIVED).
 */
const SELECTABLE_VIOLATION_STATES = new Set(['OPEN', 'WAIVED', 'LEGACY_VIOLATION']);

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

/**
 * Per-section "‹ Clear" link, matching the Vulnerabilities filter rail. Shown once the group has a
 * selection; clears only that group by toggling off each selected id (no dedicated clear handler needed).
 */
function SectionClearLink({
  testId,
  group,
  selected,
  onToggle,
}: {
  readonly testId: string;
  readonly group: ViolationFilterSetGroup;
  readonly selected: ReadonlySet<string>;
  readonly onToggle: (group: ViolationFilterSetGroup, id: string) => void;
}): JSX.Element | null {
  if (selected.size === 0) return null;
  return (
    // A real <button> (not a click-only <div>) so it is focusable and activates with
    // Enter/Space for keyboard and screen-reader users (WCAG 2.1.1).
    <button
      type="button"
      onClick={() => selected.forEach((id) => onToggle(group, id))}
      data-testid={`${testId}-clear`}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 'var(--space-2)',
        marginBottom: 'var(--space-2)',
        padding: 0,
        background: 'none',
        border: 'none',
        cursor: 'pointer',
      }}
    >
      <ActionIcons.ChevronLeft size={14} color="var(--blue-11)" />
      <Text size="2" color="blue" style={{ fontWeight: 500 }}>
        Clear
      </Text>
    </button>
  );
}

function CheckboxFilterSection({
  title,
  testId,
  group,
  entries,
  selected,
  onToggle,
  footnote,
}: {
  readonly title: string;
  readonly testId: string;
  readonly group: ViolationFilterSetGroup;
  readonly entries: ReadonlyArray<FacetEntry>;
  readonly selected: ReadonlySet<string>;
  readonly onToggle: (group: ViolationFilterSetGroup, id: string) => void;
  /** Optional gray caption rendered under the options (e.g. a divergence caveat). */
  readonly footnote?: string;
}): JSX.Element | null {
  if (entries.length === 0) return null;
  return (
    <fieldset className="nosc-violations-filter-group" data-testid={testId}>
      <legend className="nosc-violations-filter-legend">{title}</legend>
      <SectionClearLink testId={testId} group={group} selected={selected} onToggle={onToggle} />
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
      {footnote && (
        <Text size="1" color="gray" data-testid={`${testId}-footnote`}>
          {footnote}
        </Text>
      )}
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
  query,
  onQueryChange,
  serverSearchNotes = true,
}: {
  readonly title: string;
  readonly testId: string;
  readonly group: ViolationFilterSetGroup;
  readonly entries: ReadonlyArray<FacetEntry>;
  readonly selected: ReadonlySet<string>;
  /** True when any Violations filter is active (not only this group's selection). */
  readonly filtersActive: boolean;
  readonly onToggle: (group: ViolationFilterSetGroup, id: string) => void;
  /** Controlled search text; parent debounces and sends organizationFacetSearch / applicationFacetSearch. */
  readonly query: string;
  readonly onQueryChange: (query: string) => void;
  /**
   * When true (org/app facets), show top-N / search-cap notes tied to the server facet map.
   * Application Categories load from a tags API (client filter only) — keep notes off.
   */
  readonly serverSearchNotes?: boolean;
}): JSX.Element | null {
  const [expanded, setExpanded] = useState(false);
  // Collapse See more whenever the whole rail returns to the default filter state. Depends on
  // filtersActive (not this group's selection) so Reset still collapses when Orgs/Apps were
  // expanded with zero items selected in that group. Facet search text is owned by the list
  // container (cleared on Reset / last-owner deselect) — not here — because this rail mounts twice
  // (desktop + mobile drawer).
  useEffect(() => {
    if (!filtersActive) setExpanded(false);
  }, [filtersActive]);
  // Keep the section mounted while the user is typing so a zero-result server response does not
  // unmount the search box (and drop their query). Hide only when there is nothing to show and no query.
  if (entries.length === 0 && !query.trim()) return null;

  // Server already narrows by name when query is set (CLM-42912); still keep selected owners visible
  // and apply a light client filter so typing feels immediate before the debounced refetch lands.
  // Match server NameHelper.normalize (whitespace-stripped, case-insensitive) so "zetafinance"
  // still shows "Zeta Finance" after the debounced refetch lands.
  const normalizedQuery = normalizeFacetSearchText(query);
  const filtered = normalizedQuery
    ? entries.filter(
        (entry) =>
          normalizeFacetSearchText(entry.label).includes(normalizedQuery) || selected.has(entry.id),
      )
    : entries;

  const searching = normalizedQuery.length > 0;
  // Intentional: clearing search keeps `expanded` so the user is not forced to re-click See more.
  const visible = searching || expanded
    ? filtered
    : collapseFacetEntries(filtered, selected, FACET_COLLAPSE_LIMIT);
  const canToggleCollapse = !searching && filtered.length > FACET_COLLAPSE_LIMIT;
  // Uncapped top-N map is capped at FACET_SERVER_CAP. When searching, the map is name-match results
  // (also capped) — do not show the top-by-count affordance over search results.
  const showTopByCountNote = serverSearchNotes && !searching && entries.length >= FACET_SERVER_CAP;
  const showSearchCapNote = serverSearchNotes && searching && entries.length >= FACET_SERVER_CAP;

  return (
    <fieldset className="nosc-violations-filter-group" data-testid={testId}>
      <legend className="nosc-violations-filter-legend">{title}</legend>
      <SectionClearLink testId={testId} group={group} selected={selected} onToggle={onToggle} />
      <TextField.Root
        size="1"
        placeholder="Search..."
        aria-label={`Search ${title.toLowerCase()}`}
        value={query}
        onChange={(event) => onQueryChange(event.target.value)}
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
          {showTopByCountNote && (
            <Text size="1" color="gray" data-testid={`${testId}-server-capped`}>
              Showing top {FACET_SERVER_CAP} {title.toLowerCase()} by violation count. Type to search
              by name beyond this list, or narrow other filters.
            </Text>
          )}
          {showSearchCapNote && (
            <Text size="1" color="gray" data-testid={`${testId}-search-capped`}>
              Showing first {FACET_SERVER_CAP} matches. Keep typing to narrow.
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
  return normalizeRange(next, VIOLATION_THREAT_MIN, VIOLATION_THREAT_MAX);
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
 * waiver-type, threat-level, stage, organization, application, and application-category controls
 * and lifts every change to the list container, which refetches. Age stays Kitchen Sink (not
 * indexed). Application Categories use the Classic tags options API when list facets are absent.
 */
export default function ViolationsFilterRail({
  facets,
  labels,
  selected,
  onToggle,
  onWaiverTypeChange,
  onThreatRangeChange,
  onReset,
  organizationFacetSearch = '',
  onOrganizationFacetSearchChange,
  applicationFacetSearch = '',
  onApplicationFacetSearchChange,
  applicationCategoryOptions,
  applicationCategorySearch = '',
  onApplicationCategorySearchChange,
  idPrefix = 'violations-filter',
  hideStateFilter = false,
  hideWaiverTypeFilter = false,
  threatCategorySectionTitle = 'Policy Type',
  threatCategoryUseIdentityLabels = false,
  filtersActive: filtersActiveProp,
}: ViolationsFilterRailProps): JSX.Element {
  const orgLabel = (id: string): string => labels?.organizations[id] ?? id;
  const appLabel = (id: string): string => labels?.applications[id] ?? id;
  const filtersActive = filtersActiveProp ?? hasActiveViolationFilters(selected);
  const categoryLabel = threatCategoryUseIdentityLabels ? (id: string) => id : threatCategoryLabel;
  const applicationCategoryNameById = Object.fromEntries(
    (applicationCategoryOptions ?? []).map((option) => [option.id, option.name]),
  );
  const applicationCategoryLabel = (id: string): string => applicationCategoryNameById[id] ?? id;
  const applicationCategoryCounts = Object.fromEntries(
    (applicationCategoryOptions ?? []).map((option) => [option.id, 0]),
  );

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
          {!hideStateFilter && (
            <CheckboxFilterSection
              title="Violation State"
              testId={`${idPrefix}-state`}
              group="states"
              entries={toEntries(facets?.states, selected.states, violationStateLabel).filter((entry) =>
                SELECTABLE_VIOLATION_STATES.has(entry.id),
              )}
              selected={selected.states}
              onToggle={onToggle}
              footnote={
                facets?.states && 'LEGACY_VIOLATION' in facets.states
                  ? 'A waived legacy violation counts under Waived here, so this Legacy count can be lower than the classic view.'
                  : undefined
              }
            />
          )}
          <CheckboxFilterSection
            title={threatCategorySectionTitle}
            testId={`${idPrefix}-policy-type`}
            group="threatCategories"
            entries={toEntries(facets?.threatCategories, selected.threatCategories, categoryLabel)}
            selected={selected.threatCategories}
            onToggle={onToggle}
          />
          {!hideWaiverTypeFilter && (
            <WaiverTypeSection
              facets={facets}
              value={selected.waiverType}
              onWaiverTypeChange={onWaiverTypeChange}
              testId={`${idPrefix}-waiver-type`}
            />
          )}
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
            query={organizationFacetSearch}
            onQueryChange={onOrganizationFacetSearchChange ?? (() => undefined)}
          />
          <SearchableFilterSection
            title="Applications"
            testId={`${idPrefix}-applications`}
            group="applicationIds"
            entries={toEntries(facets?.applications, selected.applicationIds, appLabel)}
            selected={selected.applicationIds}
            filtersActive={filtersActive}
            onToggle={onToggle}
            query={applicationFacetSearch}
            onQueryChange={onApplicationFacetSearchChange ?? (() => undefined)}
          />
          <SearchableFilterSection
            title="Application Categories"
            testId={`${idPrefix}-app-categories`}
            group="applicationCategoryIds"
            entries={toEntries(
              applicationCategoryCounts,
              selected.applicationCategoryIds,
              applicationCategoryLabel,
            )}
            selected={selected.applicationCategoryIds}
            filtersActive={filtersActive}
            onToggle={onToggle}
            query={applicationCategorySearch}
            onQueryChange={onApplicationCategorySearchChange ?? (() => undefined)}
            serverSearchNotes={false}
          />
        </Flex>
      </aside>
    </Box>
  );
}
