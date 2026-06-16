/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/** Best-effort message from an axios (or axios-shaped) error for Nexus One surfaces. */
export function extractAxiosMessage(err: unknown): string {
  if (!err || typeof err !== 'object') return 'Unknown error';
  const e = err as { response?: { data?: unknown }; message?: unknown };
  const data = e.response?.data;
  if (typeof data === 'string' && data.length < 240) return data;
  if (data && typeof data === 'object') {
    const message = (data as { message?: unknown }).message;
    if (typeof message === 'string') return message;
  }
  if (typeof e.message === 'string') return e.message;
  return 'Unknown error';
}
