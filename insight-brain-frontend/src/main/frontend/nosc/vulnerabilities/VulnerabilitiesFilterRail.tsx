/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import { Badge, Button, Checkbox, Flex, ScrollArea, Slider, Text, TextField } from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import {
  ecosystemLabel,
  hasActiveVulnerabilityFilters,
  scopeLabel,
  SEVERITY_LABELS,
  severityLabel,
} from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesListApi';
import type {
  VulnerabilitiesFilterState,
  VulnerabilitiesListFacets,
  VulnerabilityCvssRange,
  VulnerabilityFilterSetGroup,
} from 'MainRoot/nosc/vulnerabilities/vulnerabilityListTypes';
import {
  VULNERABILITY_CVSS_MAX,
  VULNERABILITY_CVSS_MIN,
  VULNERABILITY_CVSS_STEP,
} from 'MainRoot/nosc/vulnerabilities/vulnerabilityListTypes';
import type { VulnerabilitiesTab } from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesRoute';

import './VulnerabilitiesFilterRail.scss';

export interface VulnerabilitiesFilterRailProps {
  readonly tab: VulnerabilitiesTab;
  readonly facets?: VulnerabilitiesListFacets | null;
  readonly selected: VulnerabilitiesFilterState;
  readonly onToggle: (group: VulnerabilityFilterSetGroup, id: string) => void;
  readonly onCvssRangeChange: (range: VulnerabilityCvssRange) => void;
  readonly onReset: () => void;
  readonly idPrefix?: string;
}

type FacetEntry = { readonly id: string; readonly label: string; readonly count: number };

function toEntries(
  counts: Readonly<Record<string, number>> | undefined,
  selected: ReadonlySet<string>,
  labelFor: (id: string) => string,
  alwaysIncludeIds?: ReadonlyArray<string>,
  /** When set, sort by this id order (e.g. severity critical→…→unknown) instead of label A–Z. */
  preferredIdOrder?: ReadonlyArray<string>,
): ReadonlyArray<FacetEntry> {
  const byId = new Map<string, number>(counts ? Object.entries(counts) : []);
  alwaysIncludeIds?.forEach((id) => {
    if (!byId.has(id)) byId.set(id, 0);
  });
  selected.forEach((id) => {
    if (!byId.has(id)) byId.set(id, 0);
  });
  const order = preferredIdOrder
    ? new Map(preferredIdOrder.map((id, index) => [id, index]))
    : null;
  return Array.from(byId.entries())
    .map(([id, count]) => ({ id, label: labelFor(id), count }))
    .sort((a, b) => {
      if (order) {
        const ai = order.get(a.id) ?? Number.MAX_SAFE_INTEGER;
        const bi = order.get(b.id) ?? Number.MAX_SAFE_INTEGER;
        if (ai !== bi) return ai - bi;
      }
      return a.label.localeCompare(b.label);
    });
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
  readonly group: VulnerabilityFilterSetGroup;
  readonly entries: ReadonlyArray<FacetEntry>;
  readonly selected: ReadonlySet<string>;
  readonly onToggle: (group: VulnerabilityFilterSetGroup, id: string) => void;
}): JSX.Element | null {
  if (entries.length === 0) return null;
  return (
    <fieldset className="nosc-vulnerabilities-filter-group" data-testid={testId}>
      <legend className="nosc-vulnerabilities-filter-legend">{title}</legend>
      <ScrollArea type="auto" scrollbars="vertical" className="nosc-vulnerabilities-filter-scroll">
        <Flex direction="column" gap="1" pr="2">
          {entries.map(({ id, label, count }) => (
            <Text
              key={id}
              as="label"
              size="2"
              color="gray"
              className="nosc-vulnerabilities-filter-option"
            >
              <Flex align="start" gap="2" className="nosc-vulnerabilities-filter-option-row">
                <Checkbox
                  checked={selected.has(id)}
                  onCheckedChange={() => onToggle(group, id)}
                  data-testid={`${testId}-option-${id}`}
                />
                <span className="nosc-vulnerabilities-filter-option-label">{label}</span>
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
  readonly group: VulnerabilityFilterSetGroup;
  readonly entries: ReadonlyArray<FacetEntry>;
  readonly selected: ReadonlySet<string>;
  readonly onToggle: (group: VulnerabilityFilterSetGroup, id: string) => void;
}): JSX.Element | null {
  const [query, setQuery] = useState('');
  const selectionEmpty = selected.size === 0;
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
    <fieldset className="nosc-vulnerabilities-filter-group" data-testid={testId}>
      <legend className="nosc-vulnerabilities-filter-legend">{title}</legend>
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
        <ScrollArea type="auto" scrollbars="vertical" className="nosc-vulnerabilities-filter-scroll">
          <Flex direction="column" gap="1" pr="2">
            {visible.map(({ id, label, count }) => (
              <Text
                key={id}
                as="label"
                size="2"
                color="gray"
                className="nosc-vulnerabilities-filter-option"
              >
                <Flex align="start" gap="2" className="nosc-vulnerabilities-filter-option-row">
                  <Checkbox
                    checked={selected.has(id)}
                    onCheckedChange={() => onToggle(group, id)}
                    data-testid={`${testId}-option-${id}`}
                  />
                  <span className="nosc-vulnerabilities-filter-option-label">{label}</span>
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

function normalizeCvssRange(next: readonly number[]): VulnerabilityCvssRange {
  const clamp = (n: number): number =>
    Math.min(VULNERABILITY_CVSS_MAX, Math.max(VULNERABILITY_CVSS_MIN, n));
  const a = clamp(next[0]);
  // Slider may emit a single thumb value during intermediate updates.
  const b = clamp(next.length > 1 ? next[1] : next[0]);
  const round = (n: number): number => Math.round(n * 10) / 10;
  return [round(Math.min(a, b)), round(Math.max(a, b))];
}

function CvssRangeSection({
  range,
  onCvssRangeChange,
  testId,
}: {
  readonly range: VulnerabilityCvssRange;
  readonly onCvssRangeChange: (range: VulnerabilityCvssRange) => void;
  readonly testId: string;
}): JSX.Element {
  const [liveRange, setLiveRange] = useState<[number, number]>([range[0], range[1]]);
  useEffect(() => {
    setLiveRange([range[0], range[1]]);
  }, [range]);

  const legendId = `${testId}-legend`;
  const fmt = (n: number): string => n.toFixed(1);
  return (
    <fieldset className="nosc-vulnerabilities-filter-group" data-testid={testId}>
      <legend id={legendId} className="nosc-vulnerabilities-filter-legend">
        CVSS Score
      </legend>
      <Slider
        min={VULNERABILITY_CVSS_MIN}
        max={VULNERABILITY_CVSS_MAX}
        step={VULNERABILITY_CVSS_STEP}
        value={liveRange}
        onValueChange={(next) => setLiveRange(normalizeCvssRange(next))}
        onValueCommit={(next) => onCvssRangeChange(normalizeCvssRange(next))}
        data-testid={`${testId}-slider`}
        aria-labelledby={legendId}
      />
      <Flex justify="between" mt="2">
        <Text size="1" color="gray">
          {fmt(VULNERABILITY_CVSS_MIN)}
        </Text>
        <Text size="1" color="gray" data-testid={`${testId}-value`}>
          {fmt(liveRange[0])} – {fmt(liveRange[1])}
        </Text>
        <Text size="1" color="gray">
          {fmt(VULNERABILITY_CVSS_MAX)}
        </Text>
      </Flex>
    </fieldset>
  );
}

/**
 * Interactive filter sidebar for Martha V1 Vulnerabilities (severity, CVSS, ecosystem, and the
 * organization / application / stage scope filters from CLM-43211).
 * KEV / malware / CWE / published / patch / policy remain deferred until index-backed.
 */
export default function VulnerabilitiesFilterRail({
  tab,
  facets,
  selected,
  onToggle,
  onCvssRangeChange,
  onReset,
  idPrefix = 'desktop',
}: VulnerabilitiesFilterRailProps): JSX.Element {
  const showEstateScope = tab === 'myScanData';
  const severityOrder = Object.keys(SEVERITY_LABELS);
  const severityEntries = toEntries(
    facets?.severities,
    selected.severities,
    severityLabel,
    severityOrder,
    severityOrder,
  );
  const ecosystemEntries = toEntries(facets?.ecosystems, selected.ecosystems, ecosystemLabel);
  const organizationEntries = toEntries(facets?.organizations, selected.organizations, (id) =>
    scopeLabel(facets?.organizationNames, id),
  );
  const applicationEntries = toEntries(facets?.applications, selected.applications, (id) =>
    scopeLabel(facets?.applicationNames, id),
  );
  const stageEntries = toEntries(facets?.stages, selected.stages, (id) =>
    scopeLabel(facets?.stageNames, id),
  );
  const filtersActive = hasActiveVulnerabilityFilters(selected);

  return (
    <Flex
      direction="column"
      gap="4"
      className="nosc-vulnerabilities-filter-rail"
      data-testid={`vulnerabilities-filter-rail-${idPrefix}`}
    >
      <Flex align="center" justify="between" gap="2">
        <Text size="2" weight="bold">
          Filters
        </Text>
        <Button
          size="1"
          variant="ghost"
          disabled={!filtersActive}
          onClick={onReset}
          data-testid={`vulnerabilities-filter-reset-${idPrefix}`}
        >
          Reset
        </Button>
      </Flex>

      <CheckboxFilterSection
        title="Severity"
        testId={`vulnerabilities-filter-severity-${idPrefix}`}
        group="severities"
        entries={severityEntries}
        selected={selected.severities}
        onToggle={onToggle}
      />

      <CvssRangeSection
        range={selected.cvssRange}
        onCvssRangeChange={onCvssRangeChange}
        testId={`vulnerabilities-filter-cvss-${idPrefix}`}
      />

      <SearchableFilterSection
        title="Ecosystem"
        testId={`vulnerabilities-filter-ecosystem-${idPrefix}`}
        group="ecosystems"
        entries={ecosystemEntries}
        selected={selected.ecosystems}
        onToggle={onToggle}
      />

      {showEstateScope && (
        <SearchableFilterSection
          title="Organizations"
          testId={`vulnerabilities-filter-organizations-${idPrefix}`}
          group="organizations"
          entries={organizationEntries}
          selected={selected.organizations}
          onToggle={onToggle}
        />
      )}

      {showEstateScope && (
        <SearchableFilterSection
          title="Applications"
          testId={`vulnerabilities-filter-applications-${idPrefix}`}
          group="applications"
          entries={applicationEntries}
          selected={selected.applications}
          onToggle={onToggle}
        />
      )}

      {showEstateScope && (
        <CheckboxFilterSection
          title="Stages"
          testId={`vulnerabilities-filter-stages-${idPrefix}`}
          group="stages"
          entries={stageEntries}
          selected={selected.stages}
          onToggle={onToggle}
        />
      )}
    </Flex>
  );
}
