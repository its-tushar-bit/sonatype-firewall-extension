/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Card, Flex, Text } from '@radix-ui/themes';
import { ComponentListRow } from 'MainRoot/nosc/componentsList/componentListTypes';

function ComponentCard({ component }: { readonly component: ComponentListRow }): JSX.Element {
  return (
    <Card data-testid="component-card" data-component-id={component.id}>
      <Flex direction="column" gap="2">
        <Text size="3" weight="medium" data-testid="component-card-name">
          {component.name}
        </Text>
        {component.subtitle && (
          <Text size="2" color="gray" data-testid="component-card-subtitle">
            {component.subtitle}
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
    </Card>
  );
}

export interface ComponentCardGridProps {
  readonly components: ReadonlyArray<ComponentListRow>;
}

/** Card grid for Martha V1 Components (CLM-42214) — catalog row fields. */
export default function ComponentCardGrid({ components }: ComponentCardGridProps): JSX.Element {
  return (
    <Flex direction="column" gap="3" data-testid="component-card-grid">
      {components.map((component) => (
        <ComponentCard key={`${component.source}:${component.id}`} component={component} />
      ))}
    </Flex>
  );
}
