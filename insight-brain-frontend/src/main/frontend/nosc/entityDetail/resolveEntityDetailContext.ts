/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

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
 * Component and vulnerability detail routes are not registered in Nexus One yet, so
 * {@link hrefFor} returns null for those kinds even when this is true.
 */
function isAvailableFor(kind: EntityKind, input: EntityDetailContextInput): boolean {
  switch (kind) {
    case 'application':
      return Boolean(input.applicationPublicId);
    case 'component':
      return Boolean(input.applicationPublicId && input.componentHash);
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
    case 'component':
      // Component entity detail route is not registered in Nexus One yet.
      return null;
    case 'violation': {
      if (!input.policyViolationId) {
        return null;
      }
      return appendContextQuery(violationDetailHref(input.policyViolationId), input);
    }
    case 'vulnerability':
      // Vulnerability entity detail route is not registered in Nexus One yet.
      return null;
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

export function resolveEntityDetailContext(
  input: EntityDetailContextInput,
): EntityDetailContextChain {
  return {
    nodes: CHAIN_ORDER.map((kind) => nodeFor(kind, input)),
    stageId: input.stageId,
    scanId: input.scanId,
  };
}
