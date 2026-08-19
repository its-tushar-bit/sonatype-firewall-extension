/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Shared URL query codecs for Nexus One list pages (Violations, Legal, …).
 * Keep parse/build helpers here so clamp/regex behavior cannot drift across lists.
 */

/**
 * Soft ceiling for deep-linked 1-based {@code page} values. Prevents a stale bookmark like
 * {@code ?page=999999} from posting an absurd 0-based index on the first request; containers still
 * response-clamp to the real last page once {@code total} is known.
 */
export const MAX_DEEP_LINK_PAGE = 10_000;

export function asString(value: unknown): string | undefined {
  return typeof value === 'string' && value.length > 0 ? value : undefined;
}

export function parseCsvParam(value: unknown): ReadonlyArray<string> {
  const raw = asString(value);
  if (!raw?.trim()) return [];
  return raw.split(',').map((part) => part.trim()).filter(Boolean);
}

export function serializeCsvParam(values: ReadonlySet<string>): string | undefined {
  if (values.size === 0) return undefined;
  return Array.from(values).sort().join(',');
}

/** Parse a strictly-integer token, or undefined for anything else (e.g. {@code 4abc}, {@code 4.5}). */
export function parseIntegerToken(token: string): number | undefined {
  return /^\d+$/.test(token) ? Number(token) : undefined;
}

/**
 * Parse a {@code "min-max"} threat range param, clamped to {@code [minDomain, maxDomain]} and forced
 * ascending. Falls back to {@code defaultRange} when missing/malformed.
 */
export function parseThreatRangeParam(
  value: unknown,
  options: {
    readonly minDomain: number;
    readonly maxDomain: number;
    readonly defaultRange: readonly [number, number];
  },
): readonly [number, number] {
  const raw = asString(value);
  if (!raw) return options.defaultRange;
  const parts = raw.split('-');
  if (parts.length !== 2) return options.defaultRange;
  const min = parseIntegerToken(parts[0].trim());
  const max = parseIntegerToken(parts[1].trim());
  if (min === undefined || max === undefined) return options.defaultRange;
  const clamp = (n: number): number => Math.min(options.maxDomain, Math.max(options.minDomain, n));
  const lo = clamp(min);
  const hi = clamp(max);
  return [Math.min(lo, hi), Math.max(lo, hi)];
}

export function serializeThreatRangeParam(
  range: readonly [number, number],
  isDefault: (range: readonly [number, number]) => boolean,
): string | undefined {
  return isDefault(range) ? undefined : `${range[0]}-${range[1]}`;
}

/** Convert a 1-based URL {@code page} token to a 0-based API page index. */
export function parsePageIndex(value: unknown): number {
  const pageParam = typeof value === 'string' ? Number.parseInt(value, 10) : 1;
  if (!Number.isFinite(pageParam) || pageParam <= 1) {
    return 0;
  }
  return Math.min(pageParam, MAX_DEEP_LINK_PAGE) - 1;
}
