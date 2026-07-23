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

/**
 * Appends {@code extension}/{@code classifier} as query params to a tab
 * {@code formAction} URL so that the selected artifact survives the form
 * submits produced by filter, sort, and pagination controls. Returns
 * {@code basePath} unchanged when neither field is set.
 */
export function buildArtifactFormAction(
  basePath: string,
  artifact: { extension?: string; classifier?: string }
): string {
  const params = new URLSearchParams();
  if (artifact.extension) params.set('extension', artifact.extension);
  if (artifact.classifier) params.set('classifier', artifact.classifier);
  const query = params.toString();
  return query ? `${basePath}?${query}` : basePath;
}
