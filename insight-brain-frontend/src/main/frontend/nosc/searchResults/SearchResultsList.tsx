/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Badge, Box, Card, Flex, Skeleton, Text } from '@radix-ui/themes';
import { ActionIcons, DomainIcons } from 'MainRoot/nosc/icons';
import {
  SearchResultItemDTO,
  displayNameFor,
  isApplication,
  isComponent,
  isOrganization,
  isPolicy,
  isPolicyViolation,
  isSbomMetadata,
  isVulnerability,
  isWaiver,
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
 *   │ [icon] {bold name}                            [metric] [metric] │
 *   │        {gray description}                                       │
 *   │        Label: <Badge>  Label: <Badge>  ...                      │
 *   └──────────────────────────────────────────────────────────────────┘
 *
 * Per-entity-type variations:
 *   - Application: stage / category / org metadata; metric cards for
 *     critical violations and last scan date when present.
 *   - Vulnerability: severity / status badges; metric for "Affected apps"
 *   - Component: ecosystem / version / license badges; "Used in N apps"
 *   - Organization / Policy / SBOM: simpler — name + a few badges, no
 *     right-side metrics.
 */

interface SearchResultsListProps {
  readonly results: readonly SearchResultItemDTO[];
  readonly loading: boolean;
  readonly loadError: string | null;
  readonly query: string;
  readonly onResultClick: (r: SearchResultItemDTO) => void;
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
      {results.map((r) => (
        <SearchResultCard
          key={reactKeyFor(r)}
          result={r}
          onClick={() => onResultClick(r)}
        />
      ))}
    </Flex>
  );
}

interface SearchResultCardProps {
  readonly result: SearchResultItemDTO;
  readonly onClick: () => void;
}

function SearchResultCard({ result, onClick }: SearchResultCardProps): JSX.Element {
  const { icon, iconColor, badges, metrics } = describeResult(result);

  return (
    <Card
      size="1"
      role="button"
      tabIndex={0}
      style={{ cursor: 'pointer' }}
      onClick={onClick}
      onKeyDown={activateOnKey(onClick)}
      data-testid={`nosc-search-result-card-${result.itemType}`}
    >
      <Flex
        direction={{ initial: 'column', md: 'row' }}
        align="stretch"
        justify={{ initial: 'start', md: 'between' }}
        gap="4"
      >
        <Flex direction="column" gap="2" flexGrow="1" flexShrink="1" minWidth="0" p="3" justify="between">
          <Flex direction="column" gap="2">
            <Flex align="center" gap="2">
              {React.createElement(icon, { size: 18, color: iconColor, style: { flexShrink: 0 } })}
              <Text size="3" weight="bold" truncate>
                {displayNameFor(result)}
              </Text>
            </Flex>
            {result.vulnerabilityDescription && isVulnerability(result) && (
              <Text size="2" color="gray" truncate>
                {result.vulnerabilityDescription}
              </Text>
            )}
          </Flex>

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

        {metrics.length > 0 && (
          <Flex direction="row" gap="3" flexShrink="0" align="start" justify="end" p="3">
            {metrics.map((m, i) => (
              <Card
                key={i}
                size="1"
                style={{
                  borderLeft: `3px solid ${m.borderColor}`,
                  display: 'flex',
                  minWidth: 80,
                }}
              >
                <Flex direction="column" align="start" justify="between" gap="1" p="1" style={{ flex: 1 }}>
                  <Text size="3" weight="bold" style={{ color: m.valueColor }}>
                    {m.value}
                  </Text>
                  <Text size="1" color="gray">{m.label}</Text>
                </Flex>
              </Card>
            ))}
          </Flex>
        )}
      </Flex>
    </Card>
  );
}

interface ResultDescription {
  icon: React.ComponentType<{ size?: number; color?: string; style?: React.CSSProperties }>;
  iconColor: string;
  badges: { label: string; value: string; color?: 'gray' | 'red' | 'orange' | 'green' | 'blue' | 'purple' | 'amber' }[];
  metrics: { value: string | number; label: string; borderColor: string; valueColor?: string }[];
}

function describeResult(resultDTO: SearchResultItemDTO): ResultDescription {
  if (isApplication(resultDTO)) {
    return {
      icon: DomainIcons.Applications,
      iconColor: 'var(--green-9)',
      badges: [
        ...(resultDTO.applicationPublicId ? [{ label: 'ID', value: resultDTO.applicationPublicId }] : []),
        ...(resultDTO.organizationName ? [{ label: 'Org', value: resultDTO.organizationName }] : []),
        ...(resultDTO.policyEvaluationStage ? [{ label: 'Stage', value: resultDTO.policyEvaluationStage }] : []),
      ],
      metrics: [],
    };
  }
  if (isOrganization(resultDTO)) {
    return {
      icon: DomainIcons.Organizations,
      iconColor: 'var(--indigo-9)',
      badges: [],
      metrics: [],
    };
  }
  if (isComponent(resultDTO)) {
    const eco = resultDTO.componentIdentifier?.format ?? '';
    const version = resultDTO.componentIdentifier?.coordinates?.version ?? '';
    return {
      icon: DomainIcons.Component,
      iconColor: 'var(--blue-9)',
      badges: [
        ...(eco ? [{ label: 'Ecosystem', value: eco }] : []),
        ...(version ? [{ label: 'Version', value: version }] : []),
      ],
      metrics: [],
    };
  }
  if (isVulnerability(resultDTO)) {
    const status = resultDTO.vulnerabilityStatus ?? '';
    return {
      icon: DomainIcons.Vulnerability,
      iconColor: 'var(--red-9)',
      badges: [
        { label: 'Type', value: vulnerabilitySourceLabel(resultDTO.vulnerabilityId), color: 'red' },
        ...(status ? [{ label: 'Status', value: status }] : []),
      ],
      metrics: [],
    };
  }
  if (isPolicy(resultDTO)) {
    return {
      icon: DomainIcons.Policies,
      iconColor: 'var(--purple-9)',
      badges: [
        ...(resultDTO.policyThreatCategory ? [{ label: 'Category', value: resultDTO.policyThreatCategory }] : []),
        ...(resultDTO.policyThreatLevel != null
          ? [{ label: 'Threat Level', value: String(resultDTO.policyThreatLevel) }]
          : []),
      ],
      metrics: [],
    };
  }
  if (isPolicyViolation(resultDTO)) {
    return {
      icon: DomainIcons.Policies,
      iconColor: 'var(--orange-9)',
      badges: [
        ...(resultDTO.componentName ? [{ label: 'Component', value: resultDTO.componentName }] : []),
        ...(resultDTO.applicationName ? [{ label: 'Application', value: resultDTO.applicationName }] : []),
        ...(resultDTO.policyViolationThreatLevel != null
          ? [{ label: 'Threat', value: String(resultDTO.policyViolationThreatLevel), color: 'orange' as const }]
          : []),
      ],
      metrics: [],
    };
  }
  if (isWaiver(resultDTO)) {
    return {
      icon: DomainIcons.Waivers,
      iconColor: 'var(--amber-9)',
      badges: [
        ...(resultDTO.componentName ? [{ label: 'Component', value: resultDTO.componentName }] : []),
        ...(resultDTO.applicationName ? [{ label: 'Application', value: resultDTO.applicationName }] : []),
        ...(resultDTO.policyViolationWaiverStatus
          ? [{ label: 'Status', value: resultDTO.policyViolationWaiverStatus, color: 'amber' as const }]
          : []),
        ...(resultDTO.policyViolationThreatLevel != null
          ? [{ label: 'Threat', value: String(resultDTO.policyViolationThreatLevel) }]
          : []),
      ],
      metrics: [],
    };
  }
  if (isSbomMetadata(resultDTO)) {
    return {
      icon: DomainIcons.SbomMetadata,
      iconColor: 'var(--teal-9)',
      badges: [
        ...(resultDTO.sbomSpecification ? [{ label: 'Format', value: resultDTO.sbomSpecification }] : []),
        ...(resultDTO.applicationName ? [{ label: 'App', value: resultDTO.applicationName }] : []),
      ],
      metrics: [],
    };
  }
  return { icon: ActionIcons.Search, iconColor: 'var(--gray-9)', badges: [], metrics: [] };
}
