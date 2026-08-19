/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { estateComponentDetailHref } from 'MainRoot/nosc/components/detail/estateComponentDetailHref';
import { vulnerabilityDetailHref } from 'MainRoot/nosc/vulnerabilities/detail/vulnerabilityDetailHref';
import { violationDetailHref } from 'MainRoot/nosc/violations/violationDetailHref';
import type {
  EntityDetailContextChain,
  EntityDetailContextInput,
  EntityDetailContextNode,
  EntityKind,
} from './entityDetailTypes';

/**
 * Rail order for the entity context chain. The `_Exhaustive` check below fails
 * compile if a new {@link EntityKind} is added without updating this list.
 */
const CHAIN_ORDER = ['application', 'component', 'violation', 'vulnerability'] as const satisfies ReadonlyArray<EntityKind>;

type MissingKind = Exclude<EntityKind, (typeof CHAIN_ORDER)[number]>;
type _Exhaustive = [MissingKind] extends [never] ? true : never;
const _exhaustiveChainOrder: _Exhaustive = true;
void _exhaustiveChainOrder;

function kindTitleCase(kind: EntityKind): string {
  return kind.charAt(0).toUpperCase() + kind.slice(1);
}

function labelFor(kind: EntityKind, input: EntityDetailContextInput): string {
  switch (kind) {
    case 'application':
      return input.applicationName || input.applicationPublicId || kindTitleCase(kind);
    case 'component':
      return input.componentDisplayName || input.componentHash || kindTitleCase(kind);
    case 'violation':
      return input.policyName || input.policyViolationId || kindTitleCase(kind);
    case 'vulnerability':
      return input.vulnId || kindTitleCase(kind);
  }
}

/**
 * True when enough IDs exist to identify the entity in the rail (label + future link).
 */
function isAvailableFor(kind: EntityKind, input: EntityDetailContextInput): boolean {
  switch (kind) {
    case 'application':
      return Boolean(input.applicationPublicId);
    case 'component':
      return Boolean(input.componentHash);
    case 'violation':
      return Boolean(input.policyViolationId);
    case 'vulnerability':
      return Boolean(input.vulnId);
  }
}

function appendContextQuery(baseHref: string, input: EntityDetailContextInput): string {
  const params = new URLSearchParams();
  if (input.stageId) {
    params.set('stageId', input.stageId);
  }
  if (input.scanId) {
    params.set('scanId', input.scanId);
  }
  const query = params.toString();
  return query ? `${baseHref}?${query}` : baseHref;
}

function hrefFor(kind: EntityKind, input: EntityDetailContextInput): string | null {
  // Current node is not a navigation target.
  if (kind === input.current) {
    return null;
  }

  switch (kind) {
    case 'application': {
      if (!input.applicationPublicId) {
        return null;
      }
      return appendContextQuery(
        `#/applications/${encodeURIComponent(input.applicationPublicId)}`,
        input,
      );
    }
    case 'component': {
      if (!input.componentHash) {
        return null;
      }
      // Hash-only: entity rail lacks Path internal org/app ids required for a sticky pin.
      return estateComponentDetailHref(input.componentHash);
    }
    case 'violation': {
      if (!input.policyViolationId) {
        return null;
      }
      return appendContextQuery(violationDetailHref(input.policyViolationId), input);
    }
    case 'vulnerability': {
      if (!input.vulnId) {
        return null;
      }
      return vulnerabilityDetailHref({
        vulnId: input.vulnId,
        applicationPublicId: input.applicationPublicId,
        componentHash: input.componentHash,
        violationId: input.policyViolationId,
        scanId: input.scanId,
      });
    }
  }
}

function nodeFor(kind: EntityKind, input: EntityDetailContextInput): EntityDetailContextNode {
  return {
    kind,
    label: labelFor(kind, input),
    href: hrefFor(kind, input),
    isCurrent: kind === input.current,
    isAvailable: isAvailableFor(kind, input),
  };
}

/**
 * Filters nodes to only show navigable or current context.
 * Unavailable, non-current nodes (e.g., component with missing hash) are omitted
 * to prevent rendering empty breadcrumb segments.
 *
 * When only a single node remains (e.g., just "Violation" with no app/component context),
 * it is still rendered to provide basic user context. This is intentional behavior
 * confirmed by tests in resolveEntityDetailContext.jestspec.ts.
 *
 * @param nodes - All potential breadcrumb nodes
 * @returns Filtered nodes that should be rendered
 */
function filterNavigableNodes(nodes: EntityDetailContextNode[]): EntityDetailContextNode[] {
  return nodes.filter((node) => node.isAvailable || node.isCurrent);
}

export function resolveEntityDetailContext(
  input: EntityDetailContextInput,
): EntityDetailContextChain {
  const allNodes = CHAIN_ORDER.map((kind) => nodeFor(kind, input));
  const navigableNodes = filterNavigableNodes(allNodes);
  return {
    nodes: navigableNodes,
    stageId: input.stageId,
    scanId: input.scanId,
  };
}
