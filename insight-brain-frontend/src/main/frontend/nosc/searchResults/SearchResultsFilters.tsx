/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useMemo, useState } from 'react';
import { Badge, Box, Button, Checkbox, Flex, Slider, Text, VisuallyHidden } from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { FacetBucket } from 'MainRoot/nosc/search/searchTypes';
import {
  FACET_DESCRIPTORS,
  facetPredicate,
  hasPredicate,
  hasThreatLevelField,
  orderedFacetKeys,
  readThreatLevelRange,
  setThreatLevelRange,
  THREAT_LEVEL_FACET_KEYS,
  THREAT_LEVEL_LABEL,
  THREAT_LEVEL_MAX,
  THREAT_LEVEL_MIN,
  togglePredicate,
} from 'MainRoot/nosc/searchResults/facetQuery';

/**
 * Per-tab facet rail for the /search results page.
 *
 * Each entity tab (Applications / Violations / Waivers …) gets its OWN facet set
 * from the backend `facets` map (facet key → buckets); the ALL tab has no facets
 * and renders no rail (handled by the parent). Matches the prototype rail:
 *
 *   - "Reset filters" outline button at top.
 *   - One section per facet key: a bold label + checkbox rows (checkbox +
 *     displayName + a rounded gray count pill on the right).
 *   - "Policy Threat Level" as a 0–10 Radix dual-handle range slider on the tabs
 *     that have a threat-level field. The backend does not bucket a range facet —
 *     the slider is a client-driven filter input that writes that tab's
 *     `<field>:[lo TO hi]` range predicate into the query. A tab that emits a
 *     bucketed `threatLevel` facet renders the slider only, so the same label does
 *     not appear twice.
 *
 * TODO(CLM-42453): the prototype also shows Age / Expiration Date / Reason
 * dropdowns. They are not rendered yet: the global-search grammar has no field
 * backing them, so a predicate built from them compiles to a no-op and surfaces an
 * "unknown filter" warning to the user.
 *
 * Selecting a checkbox toggles the corresponding `field:value` predicate in the
 * query string (round-trips via the shared grammar) and re-navigates, so the
 * server re-queries the narrowed result set. State is derived from the query, so
 * the rail reflects whatever predicates the URL already carries.
 */

interface SearchResultsFiltersProps {
  /** Active entity tab (APPLICATION / VIOLATION / WAIVER …) driving which client-side sections show. */
  readonly tab: string;
  /** Per-tab facet buckets from the /results response, or null when unavailable. */
  readonly facets: Record<string, FacetBucket[]> | null | undefined;
  /** The current query string the facets were computed from. */
  readonly query: string;
  /** Re-run the search with a new query string (a facet selection). */
  readonly onQueryChange: (nextQuery: string) => void;
  /** Reset all facet selections (clears the query's structured predicates). */
  readonly onReset: () => void;
  /**
   * Whether Reset has anything to do. Owned by the parent because a reset also
   * returns to page 1, so it is meaningful past page 1 even with no predicates.
   */
  readonly resetEnabled: boolean;
}

/** Names the visually-hidden reason the reset button is disabled. */
const RESET_DISABLED_NOTE_ID = 'nosc-search-facet-reset-disabled-note';

function bucketLabel(bucket: FacetBucket): string {
  return bucket.displayName != null && bucket.displayName !== '' ? bucket.displayName : bucket.value;
}

/**
 * True when a facet key actually produces a visible checkbox section. This is the
 * same condition CheckboxFacetSection bails on, shared so the parent's grid
 * decision cannot disagree with what the rail renders (a facet key with no
 * descriptor or no buckets must not reserve a rail column).
 */
export function isRenderableFacet(facetKey: string, buckets: readonly FacetBucket[] | undefined): boolean {
  if (THREAT_LEVEL_FACET_KEYS.includes(facetKey)) return false;
  return !!FACET_DESCRIPTORS[facetKey] && !!buckets && buckets.length > 0;
}

/** One bucket's count pill, shared by the toggleable and read-only row shapes. */
function BucketCount({ bucket }: { readonly bucket: FacetBucket }): JSX.Element {
  return (
    <Badge size="1" color="gray" variant="soft" radius="full">
      {bucket.count.toLocaleString()}
    </Badge>
  );
}

/**
 * A single facet section: bold label + one row per bucket.
 *
 * A bucket whose value has no expressible grammar predicate renders as a plain
 * label + count with no control, rather than a checkbox that can never be ticked.
 * The explanatory note renders whenever ANY bucket is read-only, and every read-only
 * row points at it via aria-describedby, so an unfilterable bucket in a mixed section
 * (Waiver Type, where Auto toggles but Manual has no token) is explained rather than
 * reading as a broken control. Counts stay visible as information either way.
 */
function CheckboxFacetSection({
  facetKey,
  buckets,
  query,
  onQueryChange,
}: {
  readonly facetKey: string;
  readonly buckets: readonly FacetBucket[];
  readonly query: string;
  readonly onQueryChange: (next: string) => void;
}): JSX.Element | null {
  const descriptor = FACET_DESCRIPTORS[facetKey];
  if (!isRenderableFacet(facetKey, buckets) || !descriptor) return null;

  const labelId = `nosc-search-facet-${facetKey}-label`;
  const readOnlyNoteId = `nosc-search-facet-${facetKey}-readonly-note`;
  const predicates = buckets.map((bucket) => facetPredicate(facetKey, bucket.value));
  const hasReadOnlyBucket = predicates.some((predicate) => predicate == null);
  const readOnlySection = predicates.every((predicate) => predicate == null);

  return (
    <Box role="group" aria-labelledby={labelId} data-testid={`nosc-search-facet-${facetKey}`}>
      <Text size="2" weight="bold" as="p" id={labelId} mb="2">
        {descriptor.label}
      </Text>
      {hasReadOnlyBucket && (
        <Text size="1" color="gray" as="p" id={readOnlyNoteId} mb="2">
          {readOnlySection
            ? 'These counts are informational and cannot be filtered on.'
            : 'Counts without a checkbox are informational and cannot be filtered on.'}
        </Text>
      )}
      <Flex direction="column" gap="1">
        {buckets.map((bucket, index) => {
          const predicate = predicates[index];
          const label = bucketLabel(bucket);
          if (predicate == null) {
            return (
              <Flex
                key={bucket.value}
                align="center"
                justify="between"
                gap="2"
                aria-describedby={readOnlyNoteId}
                data-testid={`nosc-search-facet-${facetKey}-readonly-${bucket.value}`}
              >
                <Text size="2" truncate>
                  {label}
                </Text>
                <BucketCount bucket={bucket} />
              </Flex>
            );
          }
          const checked = hasPredicate(query, predicate);
          return (
            <Flex key={bucket.value} align="center" justify="between" gap="2" asChild>
              {/* Radix renders Checkbox as <button role="checkbox">, and a wrapping
                  <label> only names native form controls, so aria-label is what
                  actually supplies the accessible name here. */}
              <Text as="label" size="2" style={{ cursor: 'pointer' }}>
                <Flex align="center" gap="2" style={{ minWidth: 0 }}>
                  <Checkbox
                    checked={checked}
                    onCheckedChange={() => onQueryChange(togglePredicate(query, predicate))}
                    aria-label={label}
                  />
                  <Text truncate>{label}</Text>
                </Flex>
                <BucketCount bucket={bucket} />
              </Text>
            </Flex>
          );
        })}
      </Flex>
    </Box>
  );
}

/**
 * 0–10 dual-handle threat-level slider. The predicate it writes is the active
 * tab's threat-level field, since each tab indexes the value under its own field.
 */
function ThreatLevelSection({
  tab,
  query,
  onQueryChange,
}: {
  readonly tab: string;
  readonly query: string;
  readonly onQueryChange: (next: string) => void;
}): JSX.Element {
  const committed = useMemo(() => readThreatLevelRange(query, tab), [query, tab]);
  const [committedLo, committedHi] = committed;
  const [liveRange, setLiveRange] = useState<[number, number]>(committed);
  useEffect(() => {
    setLiveRange([committedLo, committedHi]);
  }, [committedLo, committedHi]);

  // Scoped by tab so the id stays unique if a future view ever mounts more than one
  // rail, which would otherwise break aria-labelledby on the slider.
  const legendId = `nosc-search-facet-threatlevel-legend-${tab}`;
  const normalize = (next: number[]): [number, number] => {
    const lo = Math.max(THREAT_LEVEL_MIN, Math.min(THREAT_LEVEL_MAX, next[0]));
    const hi = Math.max(THREAT_LEVEL_MIN, Math.min(THREAT_LEVEL_MAX, next.length > 1 ? next[1] : next[0]));
    return [Math.min(lo, hi), Math.max(lo, hi)];
  };

  return (
    <Box role="group" aria-labelledby={legendId}>
      <Text size="2" weight="bold" as="p" id={legendId} mb="2">
        {THREAT_LEVEL_LABEL}
      </Text>
      <Slider
        min={THREAT_LEVEL_MIN}
        max={THREAT_LEVEL_MAX}
        step={1}
        value={liveRange}
        onValueChange={(next) => setLiveRange(normalize(next))}
        onValueCommit={(next) => {
          const [lo, hi] = normalize(next);
          onQueryChange(setThreatLevelRange(query, tab, lo, hi));
        }}
        aria-labelledby={legendId}
      />
      <Flex justify="between" mt="2">
        <Text size="1" color="gray">
          {THREAT_LEVEL_MIN}
        </Text>
        <Text size="1" color="gray">
          {liveRange[0]} – {liveRange[1]}
        </Text>
        <Text size="1" color="gray">
          {THREAT_LEVEL_MAX}
        </Text>
      </Flex>
    </Box>
  );
}

export function SearchResultsFilters({
  tab,
  facets,
  query,
  onQueryChange,
  onReset,
  resetEnabled,
}: SearchResultsFiltersProps): JSX.Element | null {
  const showThreatLevel = hasThreatLevelField(tab);
  // A checkbox section renders only for a facet key with a known descriptor and at
  // least one bucket. Threat level is excluded here because the slider owns it —
  // rendering both would show "Policy Threat Level" twice on the same rail.
  const facetKeys = (facets ? orderedFacetKeys(Object.keys(facets)) : []).filter((key) =>
    isRenderableFacet(key, facets?.[key])
  );

  // Nothing to render when no facet section survives the filter above AND there is
  // no client-side section for this tab — the parent renders results full-width.
  if (facetKeys.length === 0 && !showThreatLevel) {
    return null;
  }

  return (
    <Box p="1">
      <Flex align="center" justify="start" mb="4">
        {/* Disabled when a reset would be a no-op, so it cannot add a back-stack
            entry that navigates nowhere. The description says why, since a disabled
            control otherwise gives AT no reason. */}
        <Button
          variant="outline"
          color="gray"
          size="2"
          onClick={onReset}
          disabled={!resetEnabled}
          aria-describedby={resetEnabled ? undefined : RESET_DISABLED_NOTE_ID}
        >
          <ActionIcons.Refresh size={12} />
          Reset filters
        </Button>
        {!resetEnabled && <VisuallyHidden id={RESET_DISABLED_NOTE_ID}>No filters applied</VisuallyHidden>}
      </Flex>

      <Flex direction="column" gap="4">
        {facetKeys.map((key) => (
          <CheckboxFacetSection
            key={key}
            facetKey={key}
            buckets={facets?.[key] ?? []}
            query={query}
            onQueryChange={onQueryChange}
          />
        ))}

        {showThreatLevel && <ThreatLevelSection tab={tab} query={query} onQueryChange={onQueryChange} />}
      </Flex>
    </Box>
  );
}
