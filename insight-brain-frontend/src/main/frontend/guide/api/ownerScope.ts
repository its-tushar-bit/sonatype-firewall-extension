/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Process-wide holder for the active policy-context owner id — the picker's current selection,
 * read synchronously by {@link apiFetch} to scope Guide data requests.
 *
 * React-free by design: {@code PolicyContext} is the sole writer (synchronously, at its mutation
 * sites — never via a post-commit effect, which would let a remounted view's fetch effect read a
 * stale value before the ancestor effect runs), and {@code apiFetch} is the sole reader. `null`
 * means the root organization (no `ownerId` param sent).
 */
let ownerId: string | null = null;

export function setOwnerScope(id: string | null): void {
  ownerId = id;
}

export function getOwnerScope(): string | null {
  return ownerId;
}

/** @internal Resets the holder to its default. Test-only. */
export function _resetOwnerScopeForTests(): void {
  ownerId = null;
}
