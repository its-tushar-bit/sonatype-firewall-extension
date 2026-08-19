/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export type EntityKind = 'application' | 'component' | 'violation' | 'vulnerability';

export interface EntityDetailContextInput {
  readonly current: EntityKind;
  readonly applicationPublicId?: string;
  readonly applicationName?: string;
  readonly componentHash?: string;
  readonly componentDisplayName?: string;
  readonly policyViolationId?: string;
  readonly policyName?: string;
  readonly vulnId?: string;
  readonly stageId?: string;
  readonly scanId?: string;
}

export interface EntityDetailContextNode {
  readonly kind: EntityKind;
  readonly label: string;
  readonly href: string | null;
  readonly isCurrent: boolean;
  readonly isAvailable: boolean;
}

export interface EntityDetailContextChain {
  readonly nodes: readonly EntityDetailContextNode[];
  readonly stageId?: string;
  readonly scanId?: string;
}
