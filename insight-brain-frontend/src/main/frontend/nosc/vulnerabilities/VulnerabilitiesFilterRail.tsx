/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import {
  Badge,
  Button,
  Checkbox,
  Flex,
  RadioGroup,
  ScrollArea,
  Slider,
  Text,
  TextField,
} from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import {
  ecosystemLabel,
  formatCvssScore,
  formatEpssScore,
  hasActiveVulnerabilityFilters,
  scopeLabel,
  SEVERITY_LABELS,
  severityLabel,
} from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesListApi';
import type {
  VulnerabilitiesFilterState,
  VulnerabilitiesListFacets,
  VulnerabilityCvssRange,
  VulnerabilityEpssRange,
  VulnerabilityFilterSetGroup,
  VulnerabilityPublishedWindow,
} from 'MainRoot/nosc/vulnerabilities/vulnerabilityListTypes';
import {
  VULNERABILITY_CVSS_MAX,
  VULNERABILITY_CVSS_MIN,
  VULNERABILITY_CVSS_STEP,
  VULNERABILITY_EPSS_MAX,
  VULNERABILITY_EPSS_MIN,
  VULNERABILITY_EPSS_STEP,
  VULNERABILITY_PUBLISHED_WINDOWS,
} from 'MainRoot/nosc/vulnerabilities/vulnerabilityListTypes';
import type { VulnerabilitiesTab } from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesRoute';
import { normalizeRange } from 'MainRoot/nosc/util/normalizeRange';

import './VulnerabilitiesFilterRail.scss';

/** Radio sentinel for "no published window" (Radix RadioGroup needs a non-empty value). */
const PUBLISHED_WINDOW_ANY = 'any';

export interface VulnerabilitiesFilterRailProps {
  readonly tab: VulnerabilitiesTab;
  readonly facets?: VulnerabilitiesListFacets | null;
  readonly selected: VulnerabilitiesFilterState;
  readonly onToggle: (group: VulnerabilityFilterSetGroup, id: string) => void;
  readonly onCvssRangeChange: (range: VulnerabilityCvssRange) => void;
  readonly onKnownExploitedChange: (value: boolean) => void;
  readonly onMalwareChange: (value: boolean) => void;
  readonly onEpssRangeChange: (range: VulnerabilityEpssRange) => void;
  readonly onPublishedWindowChange: (value: '' | VulnerabilityPublishedWindow) => void;
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
  return normalizeRange(
    next,
    VULNERABILITY_CVSS_MIN,
    VULNERABILITY_CVSS_MAX,
    (n) => Math.round(n * 10) / 10,
  );
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
    setLiveRange((current) => (
      current[0] === range[0] && current[1] === range[1]
        ? current
        : [range[0], range[1]]
    ));
  }, [range[0], range[1]]);

  const legendId = `${testId}-legend`;
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
          {formatCvssScore(VULNERABILITY_CVSS_MIN)}
        </Text>
        <Text size="1" color="gray" data-testid={`${testId}-value`}>
          {formatCvssScore(liveRange[0])} – {formatCvssScore(liveRange[1])}
        </Text>
        <Text size="1" color="gray">
          {formatCvssScore(VULNERABILITY_CVSS_MAX)}
        </Text>
      </Flex>
    </fieldset>
  );
}

function normalizeEpssRange(next: readonly number[]): VulnerabilityEpssRange {
  return normalizeRange(
    next,
    VULNERABILITY_EPSS_MIN,
    VULNERABILITY_EPSS_MAX,
    (n) => Math.round(n * 100) / 100,
  );
}

function EpssRangeSection({
  range,
  onEpssRangeChange,
  testId,
}: {
  readonly range: VulnerabilityEpssRange;
  readonly onEpssRangeChange: (range: VulnerabilityEpssRange) => void;
  readonly testId: string;
}): JSX.Element {
  const [liveRange, setLiveRange] = useState<[number, number]>([range[0], range[1]]);
  useEffect(() => {
    setLiveRange((current) => (
      current[0] === range[0] && current[1] === range[1]
        ? current
        : [range[0], range[1]]
    ));
  }, [range[0], range[1]]);

  const legendId = `${testId}-legend`;
  return (
    <fieldset className="nosc-vulnerabilities-filter-group" data-testid={testId}>
      <legend id={legendId} className="nosc-vulnerabilities-filter-legend">
        EPSS Score
      </legend>
      <Slider
        min={VULNERABILITY_EPSS_MIN}
        max={VULNERABILITY_EPSS_MAX}
        step={VULNERABILITY_EPSS_STEP}
        value={liveRange}
        onValueChange={(next) => setLiveRange(normalizeEpssRange(next))}
        onValueCommit={(next) => onEpssRangeChange(normalizeEpssRange(next))}
        data-testid={`${testId}-slider`}
        aria-labelledby={legendId}
      />
      <Flex justify="between" mt="2">
        <Text size="1" color="gray">
          {formatEpssScore(VULNERABILITY_EPSS_MIN)}
        </Text>
        <Text size="1" color="gray" data-testid={`${testId}-value`}>
          {formatEpssScore(liveRange[0])} – {formatEpssScore(liveRange[1])}
        </Text>
        <Text size="1" color="gray">
          {formatEpssScore(VULNERABILITY_EPSS_MAX)}
        </Text>
      </Flex>
    </fieldset>
  );
}

const PUBLISHED_WINDOW_LABELS: Readonly<Record<VulnerabilityPublishedWindow, string>> = {
  '30d': 'Last 30 days',
  '90d': 'Last 90 days',
  '1y': 'Last year',
  '2y': 'Last 2 years',
};

/**
 * Interactive filter sidebar for Martha Vulnerabilities (severity, CVSS, ecosystem, estate scope on
 * My Scan Data, and Catalog-only KEV / malware / EPSS / CWE / published when tab=catalog).
 */
export default function VulnerabilitiesFilterRail({
  tab,
  facets,
  selected,
  onToggle,
  onCvssRangeChange,
  onKnownExploitedChange,
  onMalwareChange,
  onEpssRangeChange,
  onPublishedWindowChange,
  onReset,
  idPrefix = 'desktop',
}: VulnerabilitiesFilterRailProps): JSX.Element {
  const showEstateScope = tab === 'myScanData';
  const showCatalogRichness = tab === 'catalog';
  const severityOrder = Object.keys(SEVERITY_LABELS);
  const severityEntries = toEntries(
    facets?.severities,
    selected.severities,
    severityLabel,
    severityOrder,
    severityOrder,
  );
  const ecosystemEntries = toEntries(facets?.ecosystems, selected.ecosystems, ecosystemLabel);
  const cweEntries = toEntries(facets?.cwes, selected.cwes, (id) => id);
  const organizationEntries = toEntries(facets?.organizations, selected.organizations, (id) =>
    scopeLabel(facets?.organizationNames, id),
  );
  const applicationEntries = toEntries(facets?.applications, selected.applications, (id) =>
    scopeLabel(facets?.applicationNames, id),
  );
  const stageEntries = toEntries(facets?.stages, selected.stages, (id) =>
    scopeLabel(facets?.stageNames, id),
  );
  const filtersActive = hasActiveVulnerabilityFilters(selected, tab);

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

      {showCatalogRichness && (
        <fieldset
          className="nosc-vulnerabilities-filter-group"
          data-testid={`vulnerabilities-filter-catalog-flags-${idPrefix}`}
        >
          <legend
            id={`vulnerabilities-filter-catalog-flags-legend-${idPrefix}`}
            className="nosc-vulnerabilities-filter-legend"
          >
            Catalog signals
          </legend>
          <Flex
            direction="column"
            gap="2"
            role="group"
            aria-labelledby={`vulnerabilities-filter-catalog-flags-legend-${idPrefix}`}
          >
            <Text as="label" size="2" color="gray">
              <Flex align="center" gap="2">
                <Checkbox
                  checked={selected.knownExploited}
                  onCheckedChange={(value) => onKnownExploitedChange(value === true)}
                  data-testid={`vulnerabilities-filter-kev-${idPrefix}`}
                />
                Known exploited (KEV)
              </Flex>
            </Text>
            <Text as="label" size="2" color="gray">
              <Flex align="center" gap="2">
                <Checkbox
                  checked={selected.malware}
                  onCheckedChange={(value) => onMalwareChange(value === true)}
                  data-testid={`vulnerabilities-filter-malware-${idPrefix}`}
                />
                Malware
              </Flex>
            </Text>
          </Flex>
        </fieldset>
      )}

      {showCatalogRichness && (
        <EpssRangeSection
          range={selected.epssRange}
          onEpssRangeChange={onEpssRangeChange}
          testId={`vulnerabilities-filter-epss-${idPrefix}`}
        />
      )}

      {showCatalogRichness && (
        <fieldset
          className="nosc-vulnerabilities-filter-group"
          data-testid={`vulnerabilities-filter-published-${idPrefix}`}
        >
          <legend
            id={`vulnerabilities-filter-published-legend-${idPrefix}`}
            className="nosc-vulnerabilities-filter-legend"
          >
            Published
          </legend>
          <RadioGroup.Root
            value={selected.publishedWindow || PUBLISHED_WINDOW_ANY}
            onValueChange={(next) =>
              onPublishedWindowChange(
                next === PUBLISHED_WINDOW_ANY ? '' : (next as VulnerabilityPublishedWindow),
              )
            }
            aria-labelledby={`vulnerabilities-filter-published-legend-${idPrefix}`}
            data-testid={`vulnerabilities-filter-published-radio-${idPrefix}`}
          >
            <Flex direction="column" gap="1">
              <Text as="label" size="2" color="gray">
                <Flex align="center" gap="2">
                  <RadioGroup.Item
                    value={PUBLISHED_WINDOW_ANY}
                    data-testid={`vulnerabilities-filter-published-any-${idPrefix}`}
                  />
                  Any time
                </Flex>
              </Text>
              {VULNERABILITY_PUBLISHED_WINDOWS.map((window) => (
                <Text key={window} as="label" size="2" color="gray">
                  <Flex align="center" gap="2">
                    <RadioGroup.Item
                      value={window}
                      data-testid={`vulnerabilities-filter-published-${window}-${idPrefix}`}
                    />
                    {PUBLISHED_WINDOW_LABELS[window]}
                  </Flex>
                </Text>
              ))}
            </Flex>
          </RadioGroup.Root>
        </fieldset>
      )}

      {showCatalogRichness && (
        <SearchableFilterSection
          title="CWE"
          testId={`vulnerabilities-filter-cwe-${idPrefix}`}
          group="cwes"
          entries={cweEntries}
          selected={selected.cwes}
          onToggle={onToggle}
        />
      )}

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
