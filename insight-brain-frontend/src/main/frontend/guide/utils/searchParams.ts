/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import type { ReadonlySearchParams } from '@guide/ui-core/adapters';

export function toParamsRecord(searchParams: ReadonlySearchParams): Record<string, string | string[]> {
  const result: Record<string, string | string[]> = {};
  for (const key of new Set(searchParams.keys())) {
    const values = searchParams.getAll(key);
    result[key] = values.length === 1 ? values[0] : values;
  }
  return result;
}

/** Normalizes a single value, array, or undefined to a string array. */
export function toStringArray(v: string | string[] | undefined): string[] {
  return v === undefined ? [] : Array.isArray(v) ? v : [v];
}
