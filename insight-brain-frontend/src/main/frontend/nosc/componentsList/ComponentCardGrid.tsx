/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Card, Flex, Link, Text } from '@radix-ui/themes';
import { ApplicationSeverityBadge } from 'MainRoot/nosc/dashboard/tabs/ApplicationSeverityBadge';
import { estateComponentDetailHref } from 'MainRoot/nosc/components/detail/estateComponentDetailHref';
import { componentCardIdentity } from 'MainRoot/nosc/componentsList/componentCardIdentity';
import { ComponentListRow } from 'MainRoot/nosc/componentsList/componentListTypes';
import { applicationsLabel } from 'MainRoot/nosc/list/applicationsLabel';

function isSafeInAppHref(href: string): boolean {
  // Reject protocol-relative //… and /\\… (browsers normalize \ → / → open redirect).
  return href.startsWith('#/') || (href.startsWith('/') && !/^\/[/\\]/.test(href));
}

/**
 * Prefer a safe API href. Rows with a known estate {@code componentHash} deep-link to estate
 * Component Detail; Catalog / coordinate-only rows stay unlinked unless the API supplies an
 * in-app href (do not treat {@code source === 'local'} alone as proof of a hash).
 */
function componentCardHref(component: ComponentListRow): string | null {
  const href = component.href?.trim();
  if (href && isSafeInAppHref(href)) return href;
  const componentHash = component.componentHash?.trim();
  if (componentHash) {
    return estateComponentDetailHref(componentHash);
  }
  return null;
}

/**
 * Show Applications-parity severity chrome when SQL enrich supplied any score field (including
 * zeros). Catalog / unenriched stubs omit those fields and skip the badge row.
 */
function hasRiskMetrics(component: ComponentListRow): boolean {
  return (
    component.scoreCritical != null
    || component.scoreSevere != null
    || component.scoreModerate != null
    || component.scoreLow != null
  );
}

function ComponentCardBody({ component }: { readonly component: ComponentListRow }): JSX.Element {
  const { title, coordinate } = componentCardIdentity(component);

  return (
    <Flex direction="column" gap="2">
      <Text size="3" weight="medium" data-testid="component-card-name">
        {title}
      </Text>
      {coordinate && (
        <Text size="2" color="gray" data-testid="component-card-subtitle">
          {coordinate}
        </Text>
      )}
      {hasRiskMetrics(component) && (
        <Flex align="center" gap="3" wrap="wrap">
          <Flex gap="1" wrap="wrap" aria-label="Policy violations by severity">
            <ApplicationSeverityBadge value={component.scoreCritical ?? 0} severity="critical" />
            <ApplicationSeverityBadge value={component.scoreSevere ?? 0} severity="severe" />
            <ApplicationSeverityBadge value={component.scoreModerate ?? 0} severity="moderate" />
            <ApplicationSeverityBadge value={component.scoreLow ?? 0} severity="low" />
          </Flex>
          {component.affectedApplications != null && component.affectedApplications > 0 && (
            <Text size="1" color="gray" data-testid="component-card-applications">
              {applicationsLabel(component.affectedApplications)}
            </Text>
          )}
        </Flex>
      )}
      <Flex gap="3" wrap="wrap">
        {component.ecosystem && (
          <Text size="1" color="gray" data-testid="component-card-ecosystem">
            {component.ecosystem}
          </Text>
        )}
        {component.organization && (
          <Text size="1" color="gray" data-testid="component-card-organization">
            {component.organization}
          </Text>
        )}
      </Flex>
    </Flex>
  );
}

function ComponentCard({ component }: { readonly component: ComponentListRow }): JSX.Element {
  const { title } = componentCardIdentity(component);
  const href = componentCardHref(component);

  if (href) {
    return (
      <Card asChild data-testid="component-card" data-component-id={component.id}>
        <Link
          href={href}
          underline="none"
          color="gray"
          highContrast
          data-testid="component-card-link"
          aria-label={`Open component ${title}`}
        >
          <ComponentCardBody component={component} />
        </Link>
      </Card>
    );
  }

  return (
    <Card data-testid="component-card" data-component-id={component.id}>
      <ComponentCardBody component={component} />
    </Card>
  );
}

export interface ComponentCardGridProps {
  readonly components: ReadonlyArray<ComponentListRow>;
}

/** Card grid for Martha V1 Components (CLM-42214 / CLM-43209 / CLM-43210). */
export default function ComponentCardGrid({ components }: ComponentCardGridProps): JSX.Element {
  return (
    <Flex direction="column" gap="3" data-testid="component-card-grid">
      {components.map((component) => (
        <ComponentCard key={`${component.source}:${component.id}`} component={component} />
      ))}
    </Flex>
  );
}
