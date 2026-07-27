/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Card, Flex, Link, Text } from '@radix-ui/themes';
import { componentCardIdentity } from 'MainRoot/nosc/componentsList/componentCardIdentity';
import { ComponentListRow } from 'MainRoot/nosc/componentsList/componentListTypes';

function isSafeInAppHref(href: string): boolean {
  // Reject protocol-relative //… and /\\… (browsers normalize \ → / → open redirect).
  return href.startsWith('#/') || (href.startsWith('/') && !/^\/[/\\]/.test(href));
}

/**
 * Only link when the catalog API supplies an in-app href. Portfolio component detail
 * requires application + hash ({@code #/applications/{app}/components/{hash}}); list rows
 * usually lack that context, so do not invent a search fallback.
 */
function componentCardHref(component: ComponentListRow): string | null {
  const href = component.href?.trim();
  if (href && isSafeInAppHref(href)) return href;
  return null;
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

/** Card grid for Martha V1 Components (CLM-42214 / CLM-43209) — catalog row fields. */
export default function ComponentCardGrid({ components }: ComponentCardGridProps): JSX.Element {
  return (
    <Flex direction="column" gap="3" data-testid="component-card-grid">
      {components.map((component) => (
        <ComponentCard key={`${component.source}:${component.id}`} component={component} />
      ))}
    </Flex>
  );
}
