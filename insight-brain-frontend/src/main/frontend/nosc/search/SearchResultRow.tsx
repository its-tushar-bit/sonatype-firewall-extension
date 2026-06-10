/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Badge, Skeleton } from '@radix-ui/themes';
import { DomainIcons } from 'MainRoot/nosc/icons';
import {
  SearchResultItemDTO,
  displayNameFor,
  isApplication,
  isComponent,
  isOrganization,
  isPolicy,
  isVulnerability,
} from 'MainRoot/nosc/search/searchTypes';

/**
 * P1-F13 / CLM-39549: per-row renderer for the multi-entity global
 * search dropdown.
 *
 * Hard-ported from Sonatype Guide's SearchSuggestionItem:
 *   apps/seaworthy/ui/src/components/search/SearchSuggestionItem.tsx
 *
 * Visual contract:
 *
 *   [icon-20px]  {displayName} (size 14 / weight 600 / gray-12)
 *                {subtitle}    (size 12 / weight 400 / gray-11)   [type-badge]
 *
 * All styling lives in SearchOmnibar.css. This component is pure
 * composition + content. The `.nosc-search-row` class on the parent <li>
 * handles padding, hover, divider, selection. Per-element classes here
 * handle internal alignment.
 *
 * Entity color is communicated by the leading icon (green-9 = app,
 * indigo-9 = org, blue-9 = component, red-9 = vuln, purple-9 = policy).
 * The right-hand Badge is always gray/soft for visual consistency.
 */

/**
 * Maps a vulnerability refId to its source label, mirroring the prefix logic in
 * VulnerabilityUrlBuilder.java (Source enum: CVE- / GHSA- / SONATYPE-). Falls
 * back to "Sonatype" for unrecognized ids, matching that builder's default
 * source. Keeps the badge honest instead of always claiming "CVE".
 */
export function vulnerabilitySourceLabel(vulnerabilityId: string | undefined): string {
  const id = (vulnerabilityId ?? '').toUpperCase();
  if (id.startsWith('CVE-')) return 'CVE';
  if (id.startsWith('GHSA-')) return 'GHSA';
  if (id.startsWith('SONATYPE-')) return 'Sonatype';
  return 'Sonatype';
}

interface SearchResultRowProps {
  readonly result: SearchResultItemDTO;
}

export function SearchResultRow({ result }: SearchResultRowProps): JSX.Element | null {
  if (isApplication(result)) {
    return (
      <RowChrome testId="application">
        <Icon icon={<DomainIcons.Applications size={16} color="var(--green-9)" />} />
        <Body
          name={displayNameFor(result)}
          subtitle={[result.applicationPublicId, result.organizationName, result.applicationVersion]
            .filter(Boolean)
            .join(' \u00b7 ')}
        />
        <Pill label="App" />
      </RowChrome>
    );
  }

  if (isOrganization(result)) {
    return (
      <RowChrome testId="organization">
        <Icon icon={<DomainIcons.Organizations size={16} color="var(--indigo-9)" />} />
        <Body name={displayNameFor(result)} subtitle="Organization" />
        <Pill label="Org" />
      </RowChrome>
    );
  }

  if (isComponent(result)) {
    const ecosystem = result.componentIdentifier?.format ?? '';
    const version = result.componentIdentifier?.coordinates?.version ?? '';
    return (
      <RowChrome testId="component">
        <Icon icon={<DomainIcons.Component size={16} color="var(--blue-9)" />} />
        <Body
          name={displayNameFor(result)}
          subtitle={[ecosystem, version].filter(Boolean).join(' \u00b7 ')}
        />
        <Pill label={ecosystem || 'Component'} />
      </RowChrome>
    );
  }

  if (isVulnerability(result)) {
    return (
      <RowChrome testId="vulnerability">
        <Icon icon={<DomainIcons.Vulnerability size={16} color="var(--red-9)" />} />
        <Body
          name={displayNameFor(result)}
          subtitle={result.vulnerabilityDescription ?? 'Security vulnerability'}
        />
        <Pill label={vulnerabilitySourceLabel(result.vulnerabilityId)} />
      </RowChrome>
    );
  }

  if (isPolicy(result)) {
    const threatLevel = result.policyThreatLevel != null ? `Threat ${result.policyThreatLevel}` : '';
    const category = result.policyThreatCategory ?? '';
    return (
      <RowChrome testId="policy">
        <Icon icon={<DomainIcons.Policies size={16} color="var(--purple-9)" />} />
        <Body
          name={displayNameFor(result)}
          subtitle={[category, threatLevel].filter(Boolean).join(' \u00b7 ')}
        />
        <Pill label="Policy" />
      </RowChrome>
    );
  }

  // Defensive: a type we don't render. Should never happen because
  // useGlobalSearch filters with isRenderedType.
  return null;
}

interface RowChromeProps {
  readonly testId: string;
  readonly children: React.ReactNode;
}

function RowChrome({ testId, children }: RowChromeProps): JSX.Element {
  return (
    <div className="nosc-search-row__inner" data-testid={`nosc-search-row-${testId}`}>
      {children}
    </div>
  );
}

function Icon({ icon }: { readonly icon: React.ReactNode }): JSX.Element {
  return <div className="nosc-search-row__icon">{icon}</div>;
}

function Body({ name, subtitle }: { readonly name: string; readonly subtitle: string }): JSX.Element {
  return (
    <div className="nosc-search-row__body">
      <div className="nosc-search-row__name">{name}</div>
      {subtitle && <div className="nosc-search-row__subtitle">{subtitle}</div>}
    </div>
  );
}

function Pill({ label }: { readonly label: string }): JSX.Element {
  return (
    <div className="nosc-search-row__badge">
      <Badge size="1" color="gray" variant="soft" radius="full">
        {label}
      </Badge>
    </div>
  );
}

/**
 * Skeleton row shown while fetching. One per visible bucket. Mirrors
 * Guide's `SearchSuggestionItemSkeleton`.
 */
export function SearchResultRowSkeleton(): JSX.Element {
  return (
    <div className="nosc-search-row__inner">
      <div className="nosc-search-row__icon">
        <Skeleton width="16px" height="16px" />
      </div>
      <div className="nosc-search-row__body">
        <Skeleton width="140px" height="14px" />
        <Skeleton width="200px" height="12px" style={{ marginTop: 6 }} />
      </div>
    </div>
  );
}
