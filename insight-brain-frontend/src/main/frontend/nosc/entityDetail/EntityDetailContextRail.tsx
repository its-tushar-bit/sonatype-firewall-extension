/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import type { CSSProperties, ReactElement } from 'react';
import { Badge, Flex, Link as RadixLink, Text } from '@radix-ui/themes';
import type { EntityDetailContextChain, EntityDetailContextNode } from './entityDetailTypes';

export interface EntityDetailContextRailProps {
  readonly context: EntityDetailContextChain;
  readonly testId?: string;
}

/** Defense-in-depth: only allow in-app hash or root-relative paths. */
export function isSafeEntityDetailHref(href: string): boolean {
  return href.startsWith('#/') || (href.startsWith('/') && !href.startsWith('//'));
}

/** Scan ids at or below this length fit the badge without truncation. */
const SCAN_BADGE_MAX_FULL_LENGTH = 12;

/** Visible prefix length when truncating a longer scan id (full value stays in title). */
const SCAN_BADGE_TRUNCATED_PREFIX_LENGTH = 8;

function formatScanBadgeLabel(scanId: string): string {
  if (scanId.length <= SCAN_BADGE_MAX_FULL_LENGTH) {
    return scanId;
  }
  return `${scanId.slice(0, SCAN_BADGE_TRUNCATED_PREFIX_LENGTH)}…`;
}

const listStyle: CSSProperties = {
  listStyle: 'none',
  margin: 0,
  padding: 0,
  display: 'flex',
  flexWrap: 'wrap',
  alignItems: 'center',
  gap: 'var(--space-2)',
};

const itemStyle: CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: 'var(--space-2)',
};

function renderNode(node: EntityDetailContextNode): ReactElement {
  if (node.isCurrent) {
    return (
      <Text size="2" weight="medium" aria-current="page">
        {node.label}
      </Text>
    );
  }

  if (node.isAvailable && node.href && isSafeEntityDetailHref(node.href)) {
    return (
      <RadixLink size="2" href={node.href}>
        {node.label}
      </RadixLink>
    );
  }

  return (
    <Text size="2" color="gray">
      {node.label}
    </Text>
  );
}

export function EntityDetailContextRail({
  context,
  testId = 'nosc-entity-context-rail',
}: EntityDetailContextRailProps): ReactElement {
  return (
    <nav aria-label="Entity context" data-testid={testId} style={{ marginBottom: 'var(--space-4)' }}>
      <ol style={listStyle}>
        {context.nodes.map((node, index) => (
          <li key={node.kind} style={itemStyle}>
            {index > 0 && (
              <Text size="2" color="gray" aria-hidden="true">
                →
              </Text>
            )}
            {renderNode(node)}
          </li>
        ))}
      </ol>
      {(context.stageId || context.scanId) && (
        <Flex align="center" gap="2" wrap="wrap" mt="2">
          {context.stageId && (
            <Badge size="1" color="gray" variant="soft" radius="full">
              Stage: {context.stageId}
            </Badge>
          )}
          {context.scanId && (
            <Badge size="1" color="gray" variant="soft" radius="full" title={context.scanId}>
              Scan: {formatScanBadgeLabel(context.scanId)}
            </Badge>
          )}
        </Flex>
      )}
    </nav>
  );
}
