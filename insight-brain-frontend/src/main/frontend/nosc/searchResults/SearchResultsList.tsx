/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Badge, Card, Flex, Skeleton, Text } from '@radix-ui/themes';
import { ActionIcons, DomainIcons } from 'MainRoot/nosc/icons';
import {
  SearchEntityType,
  SearchRow,
  displayNameFor,
  reactKeyFor,
  vulnerabilitySourceLabel,
} from 'MainRoot/nosc/search/searchTypes';
import { activateOnKey } from 'MainRoot/nosc/keyboardActivate';

/**
 * P1-F13: results list for the /preview/search page.
 *
 * Renders one Card per result. Each card mirrors the visual idiom of
 * apps/nexusone-ux-prototype/src/components/applications/guide/
 * GuideApplicationResultCard.tsx:
 *
 *   ┌──────────────────────────────────────────────────────────────────┐
 *   │ [icon] {bold title}                                            │
 *   │        {gray subtitle}                                         │
 *   │        Label: <Badge>  Label: <Badge>  ...                     │
 *   └──────────────────────────────────────────────────────────────────┘
 *
 * The backend supplies the title, subtitle, and an open `fields` bag per row;
 * this renderer picks the icon + a small set of entity-appropriate badges.
 */

interface SearchResultsListProps {
  readonly results: readonly SearchRow[];
  readonly loading: boolean;
  readonly loadError: string | null;
  readonly query: string;
  readonly onResultClick: (r: SearchRow) => void;
}

export function SearchResultsList({
  results,
  loading,
  loadError,
  query,
  onResultClick,
}: SearchResultsListProps): JSX.Element {
  if (loadError) {
    return (
      <Card data-testid="nosc-search-results-error">
        <Flex direction="column" align="center" gap="3" p="6">
          <Text size="3" weight="medium">Search unavailable</Text>
          <Text size="2" color="gray" align="center" style={{ maxWidth: 480 }}>
            {loadError}
          </Text>
        </Flex>
      </Card>
    );
  }

  if (loading) {
    return (
      <Flex direction="column" gap="3" data-testid="nosc-search-results-loading">
        {[0, 1, 2].map((i) => (
          <Card key={i} size="1">
            <Flex direction="column" gap="2" p="3">
              <Skeleton width="240px" height="20px" />
              <Skeleton width="380px" height="14px" />
              <Skeleton width="180px" height="14px" />
            </Flex>
          </Card>
        ))}
      </Flex>
    );
  }

  if (results.length === 0) {
    return (
      <Card data-testid="nosc-search-results-empty">
        <Flex direction="column" align="center" gap="3" p="6">
          <ActionIcons.Search size={24} color="var(--gray-9)" />
          <Text size="3" weight="medium">No results</Text>
          <Text size="2" color="gray" align="center" style={{ maxWidth: 480 }}>
            {query
              ? `Nothing matches "${query}" with the current filters. Try clearing filters or changing your search.`
              : 'Try adjusting your filters or search terms.'}
          </Text>
        </Flex>
      </Card>
    );
  }

  return (
    <Flex direction="column" gap="3" data-testid="nosc-search-results-list">
      {results.map((r, i) => (
        // Position-scoped key: the backend can return the same component id more
        // than once in a page (e.g. one artifact scanned in several apps shares a
        // hash), so reactKeyFor alone is not unique within a response. Prefixing
        // the index keeps React keys distinct and the rendered list 1:1 with rows.
        <SearchResultCard
          key={`${i}:${reactKeyFor(r)}`}
          result={r}
          onClick={() => onResultClick(r)}
        />
      ))}
    </Flex>
  );
}

interface SearchResultCardProps {
  readonly result: SearchRow;
  readonly onClick: () => void;
}

function SearchResultCard({ result, onClick }: SearchResultCardProps): JSX.Element {
  const { icon, iconColor, badges } = describeResult(result);

  return (
    <Card
      size="1"
      role="button"
      tabIndex={0}
      style={{ cursor: 'pointer' }}
      onClick={onClick}
      onKeyDown={activateOnKey(onClick)}
      data-testid={`nosc-search-result-card-${result.type}`}
    >
      <Flex direction="column" gap="2" flexGrow="1" flexShrink="1" minWidth="0" p="3">
        <Flex align="center" gap="2">
          {React.createElement(icon, { size: 18, color: iconColor, style: { flexShrink: 0 } })}
          <Text size="3" weight="bold" truncate>
            {displayNameFor(result)}
          </Text>
        </Flex>
        {result.subtitle && (
          <Text size="2" color="gray" truncate>
            {result.subtitle}
          </Text>
        )}

        {badges.length > 0 && (
          <Flex align="center" gap="2" wrap="wrap">
            {badges.map((b, i) => (
              <React.Fragment key={i}>
                <Text size="1" color="gray">{b.label}:</Text>
                <Badge size="1" color={b.color ?? 'gray'} variant="soft">
                  {b.value}
                </Badge>
              </React.Fragment>
            ))}
          </Flex>
        )}
      </Flex>
    </Card>
  );
}

type BadgeColor = 'gray' | 'red' | 'orange' | 'green' | 'blue' | 'purple' | 'amber';

interface ResultDescription {
  icon: React.ComponentType<{ size?: number; color?: string; style?: React.CSSProperties }>;
  iconColor: string;
  badges: { label: string; value: string; color?: BadgeColor }[];
}

/** Read a field as a display string, or undefined when absent/blank. */
function fieldStr(row: SearchRow, key: string): string | undefined {
  const v = row.fields[key];
  if (v == null) return undefined;
  const s = String(v);
  return s.length > 0 ? s : undefined;
}

const ICON_BY_TYPE: Record<
  SearchEntityType,
  { icon: ResultDescription['icon']; color: string }
> = {
  APPLICATION: { icon: DomainIcons.Applications, color: 'var(--green-9)' },
  COMPONENT: { icon: DomainIcons.Component, color: 'var(--blue-9)' },
  VULNERABILITY: { icon: DomainIcons.Vulnerability, color: 'var(--red-9)' },
  VIOLATION: { icon: DomainIcons.Policies, color: 'var(--orange-9)' },
  WAIVER: { icon: DomainIcons.Waivers, color: 'var(--amber-9)' },
};

function describeResult(row: SearchRow): ResultDescription {
  const chrome = ICON_BY_TYPE[row.type] ?? { icon: ActionIcons.Search, color: 'var(--gray-9)' };
  const badges: ResultDescription['badges'] = [];

  if (row.type === 'APPLICATION') {
    const org = fieldStr(row, 'organizationName');
    const stage = fieldStr(row, 'policyEvaluationStage');
    if (org) badges.push({ label: 'Org', value: org });
    if (stage) badges.push({ label: 'Stage', value: stage });
  } else if (row.type === 'COMPONENT') {
    const eco = fieldStr(row, 'ecosystem') ?? fieldStr(row, 'format');
    const version = fieldStr(row, 'version') ?? fieldStr(row, 'latestStable');
    const license = fieldStr(row, 'license');
    if (eco) badges.push({ label: 'Ecosystem', value: eco });
    if (version) badges.push({ label: 'Version', value: version });
    if (license) badges.push({ label: 'License', value: license });
  } else if (row.type === 'VULNERABILITY') {
    const severity = fieldStr(row, 'cvssSeverity');
    const cvss = fieldStr(row, 'maxCvss');
    badges.push({ label: 'Type', value: vulnerabilitySourceLabel(row.title), color: 'red' });
    if (severity) badges.push({ label: 'Severity', value: severity, color: 'red' });
    if (cvss) badges.push({ label: 'CVSS', value: cvss });
  } else if (row.type === 'VIOLATION') {
    const app = fieldStr(row, 'applicationName') ?? fieldStr(row, 'applicationPublicId');
    const threat = fieldStr(row, 'threatLevel');
    if (app) badges.push({ label: 'Application', value: app });
    if (threat) badges.push({ label: 'Threat', value: threat, color: 'orange' });
  } else if (row.type === 'WAIVER') {
    const app = fieldStr(row, 'applicationName') ?? fieldStr(row, 'applicationPublicId');
    const status = fieldStr(row, 'waiverStatus');
    if (app) badges.push({ label: 'Application', value: app });
    if (status) badges.push({ label: 'Status', value: status, color: 'amber' });
  }

  return { icon: chrome.icon, iconColor: chrome.color, badges };
}
